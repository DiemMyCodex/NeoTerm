package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.terminal.TerminalSession;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Polls the terminal foreground process group so tab pills can show a live busy dot.
 *
 * <p>No external dependency and no blocking work on the main thread: a single daemon
 * thread reads the tiny {@code /proc/<shell-pid>/stat} entry once a second and posts a
 * callback for the adapter to diff its cached states.</p>
 */
final class SessionProcessMonitor {

    private static final long POLL_INTERVAL_MS = 1000L;

    interface Callback {
        void onPoll();
    }

    private final ScheduledExecutorService mExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "session-process-monitor");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean mRunning = new AtomicBoolean(false);

    void start(@NonNull Callback callback) {
        if (!mRunning.compareAndSet(false, true)) return;

        mExecutor.scheduleAtFixedRate(() -> {
            try {
                callback.onPoll();
            } catch (RuntimeException ignored) {
                // A bad frame must never kill the polling thread.
            }
        }, 0, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    void stop() {
        if (mRunning.compareAndSet(true, false)) mExecutor.shutdownNow();
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
}
