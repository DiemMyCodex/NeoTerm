package com.termux.app.terminal;

import androidx.annotation.NonNull;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class SessionProcessMonitorTest {

    @Test
    public void shellOwningItsTtyIsIdle() {
        // pid 100 (sh), pgrp 100, tty 34816, tpgid 100.
        SessionProcessMonitor.ProcStat stat = SessionProcessMonitor.parseProcStat(
            "100 (sh) S 1 100 100 34816 100 0 0 0 0");

        Assert.assertNotNull(stat);
        Assert.assertFalse(SessionProcessMonitor.hasForegroundChild(stat));
    }

    @Test
    public void childProcessGroupOwningTtyIsBusy() {
        // Same shell, but a child process group 142 owns the foreground tty.
        SessionProcessMonitor.ProcStat stat = SessionProcessMonitor.parseProcStat(
            "100 (my shell) S 1 100 100 34816 142 0 0 0 0");

        Assert.assertNotNull(stat);
        Assert.assertTrue(SessionProcessMonitor.hasForegroundChild(stat));
    }

    @Test
    public void statWithoutControllingTtyIsIdle() {
        SessionProcessMonitor.ProcStat stat = SessionProcessMonitor.parseProcStat(
            "100 (sh) S 1 100 100 0 0 0 0 0 0");

        Assert.assertNotNull(stat);
        Assert.assertFalse(SessionProcessMonitor.hasForegroundChild(stat));
    }

    @Test
    public void malformedStatIsIgnored() {
        Assert.assertNull(SessionProcessMonitor.parseProcStat("not-a-proc-stat"));
    }

    @Test
    public void diffReportsNewChangedAndRemovedStates() {
        Map<String, Boolean> previous = state("a", true);
        Map<String, Boolean> current = new HashMap<>();
        current.put("a", false);
        current.put("b", true);

        Map<String, Boolean> changed = SessionProcessMonitor.diffStates(previous, current);

        Assert.assertEquals(2, changed.size());
        Assert.assertEquals(Boolean.FALSE, changed.get("a"));
        Assert.assertEquals(Boolean.TRUE, changed.get("b"));

        // Sessions that disappeared are reported once as idle so their dots reset.
        Map<String, Boolean> removed = SessionProcessMonitor.diffStates(current, Collections.emptyMap());
        Assert.assertEquals(2, removed.size());
        Assert.assertEquals(Boolean.FALSE, removed.get("a"));
        Assert.assertEquals(Boolean.FALSE, removed.get("b"));
    }

    @Test
    public void diffIgnoresUnchangedStates() {
        Assert.assertTrue(SessionProcessMonitor.diffStates(state("a", true), state("a", true)).isEmpty());
        Assert.assertTrue(SessionProcessMonitor.diffStates(
            Collections.emptyMap(), Collections.emptyMap()).isEmpty());
    }

    @Test
    public void startStopLifecycleIsIdempotent() {
        FakeScheduler scheduler = new FakeScheduler();
        SessionProcessMonitor monitor = new SessionProcessMonitor(
            Collections::emptyMap, changed -> { }, scheduler);

        Assert.assertFalse(monitor.isRunning());
        monitor.start();
        Assert.assertTrue(monitor.isRunning());
        Assert.assertEquals(1, scheduler.schedules);
        Assert.assertEquals(SessionProcessMonitor.POLL_INTERVAL_MS, scheduler.periodMs);
        Assert.assertTrue("poll interval must stay in the 1500-2000ms battery band",
            scheduler.periodMs >= 1500L && scheduler.periodMs <= 2000L);
        Assert.assertEquals(0L, scheduler.initialDelayMs);

        monitor.start(); // duplicate start must not create a second executor
        Assert.assertEquals(1, scheduler.schedules);

        monitor.stop();
        Assert.assertFalse(monitor.isRunning());
        Assert.assertEquals(1, scheduler.shutdowns);

        monitor.stop(); // duplicate stop must not shut down twice
        Assert.assertEquals(1, scheduler.shutdowns);
    }

    @Test
    public void pollOncePostsOnlyRealStateChanges() {
        AtomicReference<Map<String, Boolean>> states = new AtomicReference<>(new HashMap<>());
        List<Map<String, Boolean>> seen = new ArrayList<>();
        FakeScheduler scheduler = new FakeScheduler();
        SessionProcessMonitor monitor = new SessionProcessMonitor(
            states::get, changed -> seen.add(new HashMap<>(changed)), scheduler);

        monitor.start();
        states.set(state("a", true));
        scheduler.tick();
        Assert.assertEquals(1, seen.size());
        Assert.assertEquals(Boolean.TRUE, seen.get(0).get("a"));

        // Repeated identical scan: zero callbacks, so zero adapter redraws on the main thread.
        scheduler.tick();
        scheduler.tick();
        Assert.assertEquals(1, seen.size());

        states.set(state("a", false));
        scheduler.tick();
        Assert.assertEquals(2, seen.size());
        Assert.assertEquals(Boolean.FALSE, seen.get(1).get("a"));

        monitor.stop();
        states.set(state("a", true));
        scheduler.tick(); // stopped: no scan, no callback
        Assert.assertEquals(2, seen.size());
    }

    @Test
    public void nullSnapshotIsTreatedAsEmpty() {
        AtomicReference<Map<String, Boolean>> states = new AtomicReference<>(state("a", true));
        List<Map<String, Boolean>> seen = new ArrayList<>();
        FakeScheduler scheduler = new FakeScheduler();
        SessionProcessMonitor monitor = new SessionProcessMonitor(
            states::get, seen::add, scheduler);

        monitor.start();
        scheduler.tick();
        Assert.assertEquals(1, seen.size());

        // A null snapshot means "no sessions": the previous dot must reset to idle.
        states.set(null);
        scheduler.tick();
        Assert.assertEquals(2, seen.size());
        Assert.assertEquals(Boolean.FALSE, seen.get(1).get("a"));
    }

    @Test
    public void failingCallbackDoesNotKillThePollLoop() {
        FakeScheduler scheduler = new FakeScheduler();
        SessionProcessMonitor monitor = new SessionProcessMonitor(
            () -> state("a", true), changed -> { throw new IllegalStateException("ui gone"); }, scheduler);

        monitor.start();
        scheduler.tick(); // must not throw
        Assert.assertTrue(monitor.isRunning());
    }

    private static Map<String, Boolean> state(String handle, boolean busy) {
        Map<String, Boolean> state = new HashMap<>();
        state.put(handle, busy);
        return state;
    }

    /** Deterministic stand-in for the background scheduled executor. */
    private static final class FakeScheduler implements SessionProcessMonitor.Scheduler {
        Runnable task;
        long initialDelayMs;
        long periodMs;
        int schedules;
        int shutdowns;

        @Override
        public void scheduleAtFixedRate(@NonNull Runnable task, long initialDelayMs, long periodMs) {
            this.task = task;
            this.initialDelayMs = initialDelayMs;
            this.periodMs = periodMs;
            schedules++;
        }

        @Override
        public void shutdownNow() {
            shutdowns++;
        }

        void tick() {
            if (task != null) task.run();
        }
    }
}
