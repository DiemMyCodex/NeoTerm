package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.TerminalSession;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Polls the terminal foreground process group so tab pills can show a live busy dot.
 *
 * <p>All {@code /proc} reads happen on one daemon thread. The callback is only invoked when a
 * session actually changes between idle and busy, so the main thread never redraws pills on
 * every tick. Polling is explicitly started/stopped with the activity lifecycle.</p>
 */
final class SessionProcessMonitor {

    /** Slow enough for battery/120Hz rendering, fast enough for a live status dot. */
    static final long POLL_INTERVAL_MS = 1800L;

    /** Supplies a snapshot of handle → busy states. Called ONLY on the background thread. */
    interface StateProvider {
        @NonNull
        Map<String, Boolean> getStates();
    }

    /** Receives only the entries whose state changed since the previous poll. */
    interface Callback {
        void onStatesChanged(@NonNull Map<String, Boolean> changed);
    }

    /** Injectable so JVM tests can drive polls without real threads. */
    interface Scheduler {
        void scheduleAtFixedRate(@NonNull Runnable task, long initialDelayMs, long periodMs);

        void shutdownNow();
    }

    private final StateProvider mStateProvider;
    private final Callback mCallback;
    private final Scheduler mScheduler;
    private final Map<String, Boolean> mLastStates = new HashMap<>();
    private final AtomicBoolean mRunning = new AtomicBoolean(false);

    SessionProcessMonitor(@NonNull StateProvider stateProvider, @NonNull Callback callback) {
        this(stateProvider, callback, new ExecutorScheduler());
    }

    SessionProcessMonitor(@NonNull StateProvider stateProvider, @NonNull Callback callback,
                          @NonNull Scheduler scheduler) {
        mStateProvider = stateProvider;
        mCallback = callback;
        mScheduler = scheduler;
    }

    void start() {
        if (!mRunning.compareAndSet(false, true)) return;
        mLastStates.clear();
        mScheduler.scheduleAtFixedRate(this::pollOnce, 0, POLL_INTERVAL_MS);
    }

    void stop() {
        if (!mRunning.compareAndSet(true, false)) return;
        mScheduler.shutdownNow();
    }

    boolean isRunning() {
        return mRunning.get();
    }

    /** Background entry point. Tests may call it directly with a fake scheduler. */
    void pollOnce() {
        if (!mRunning.get()) return;

        Map<String, Boolean> current = mStateProvider.getStates();
        if (current == null) current = Collections.emptyMap();

        Map<String, Boolean> changed = diffStates(mLastStates, current);
        mLastStates.clear();
        mLastStates.putAll(current);
        if (changed.isEmpty()) return;

        try {
            mCallback.onStatesChanged(changed);
        } catch (RuntimeException ignored) {
            // A bad UI callback must never kill the polling thread.
        }
    }

    /** Returns changed entries only; a removed session is reported as idle. */
    @NonNull
    static Map<String, Boolean> diffStates(@NonNull Map<String, Boolean> previous,
                                           @NonNull Map<String, Boolean> current) {
        Map<String, Boolean> changed = new HashMap<>();
        for (Map.Entry<String, Boolean> entry : current.entrySet()) {
            Boolean before = previous.get(entry.getKey());
            if (!previous.containsKey(entry.getKey()) || !Objects.equals(before, entry.getValue()))
                changed.put(entry.getKey(), entry.getValue());
        }
        for (String handle : previous.keySet()) {
            if (!current.containsKey(handle)) changed.put(handle, false);
        }
        return changed;
    }

    /** Snapshot the service-owned sessions and read /proc for each one. Background only. */
    @NonNull
    static Map<String, Boolean> scanSessions(@Nullable List<TermuxSession> sessions) {
        Map<String, Boolean> states = new HashMap<>();
        if (sessions == null) return states;

        for (TermuxSession wrapper : new ArrayList<>(sessions)) {
            TerminalSession session = wrapper == null ? null : wrapper.getTerminalSession();
            if (session == null) continue;
            states.put(session.mHandle, isForegroundChildRunning(session));
        }
        return states;
    }

    /** True when a child process group, not the shell itself, owns the session tty. */
    static boolean isForegroundChildRunning(@Nullable TerminalSession session) {
        if (session == null || !session.isRunning()) return false;

        int pid = session.getPid();
        if (pid < 1) return false;

        return hasForegroundChild(readProcStat(pid));
    }

    static boolean hasForegroundChild(@Nullable ProcStat shellStat) {
        return shellStat != null
            && shellStat.tpgid > 0
            && shellStat.ttyNr > 0
            && shellStat.tpgid != shellStat.pgrp;
    }

    /** Parses the stable numeric tail of /proc/[pid]/stat (comm can contain spaces). */
    @Nullable
    static ProcStat readProcStat(int pid) {
        return parseProcStat(readFirstLine("/proc/" + pid + "/stat"));
    }

    @Nullable
    static ProcStat parseProcStat(@Nullable String line) {
        if (line == null) return null;

        int commEnd = line.lastIndexOf(')');
        if (commEnd < 0 || commEnd + 1 >= line.length()) return null;

        // After ")" the fields are: state, ppid, pgrp, session, tty_nr, tpgid, ...
        String[] fields = line.substring(commEnd + 2).trim().split("\\s+");
        if (fields.length < 6) return null;

        try {
            char state = fields[0].charAt(0);
            long pgrp = Long.parseLong(fields[2]);
            long ttyNr = Long.parseLong(fields[4]);
            long tpgid = Long.parseLong(fields[5]);
            return new ProcStat(state, pgrp, ttyNr, tpgid);
        } catch (NumberFormatException | IndexOutOfBoundsException ignored) {
            return null;
        }
    }

    @Nullable
    private static String readFirstLine(@NonNull String path) {
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            return reader.readLine();
        } catch (IOException | SecurityException ignored) {
            return null;
        }
    }

    static final class ProcStat {
        final char state;
        final long pgrp;
        final long ttyNr;
        final long tpgid;

        ProcStat(char state, long pgrp, long ttyNr, long tpgid) {
            this.state = state;
            this.pgrp = pgrp;
            this.ttyNr = ttyNr;
            this.tpgid = tpgid;
        }
    }

    private static final class ExecutorScheduler implements Scheduler {

        private ScheduledExecutorService mExecutor;

        @Override
        public void scheduleAtFixedRate(@NonNull Runnable task, long initialDelayMs, long periodMs) {
            if (mExecutor != null) return;
            mExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "session-process-monitor");
                thread.setDaemon(true);
                return thread;
            });
            mExecutor.scheduleAtFixedRate(task, initialDelayMs, periodMs, TimeUnit.MILLISECONDS);
        }

        @Override
        public void shutdownNow() {
            if (mExecutor == null) return;
            mExecutor.shutdownNow();
            mExecutor = null;
        }
    }
}
