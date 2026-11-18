package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

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
}
