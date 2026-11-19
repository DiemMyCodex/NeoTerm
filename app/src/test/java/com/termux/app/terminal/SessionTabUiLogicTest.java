package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class SessionTabUiLogicTest {

    @Test
    public void labelUsesSessionNameFirst() {
        Assert.assertEquals("3: build", SessionTabUiLogic.buildLabel(2, "build", "htop"));
    }

    @Test
    public void labelFallsBackToShellTitleThenShell() {
        Assert.assertEquals("1: htop", SessionTabUiLogic.buildLabel(0, null, "htop"));
        Assert.assertEquals("2: shell", SessionTabUiLogic.buildLabel(1, "", ""));
    }

    @Test
    public void oversizedLabelIsEllipsized() {
        String longName = String.join("", Collections.nCopies(20, "abcdefghij")); // 200 chars
        String label = SessionTabUiLogic.buildLabel(0, longName, null);

        Assert.assertEquals(SessionTabUiLogic.MAX_LABEL_CHARS + 3, label.length()); // "1: " + cap
        Assert.assertTrue(label.endsWith("\u2026"));
    }

    @Test
    public void shortLabelIsUntouched() {
        Assert.assertEquals("normal-name", SessionTabUiLogic.ellipsizeEnd("normal-name"));
        Assert.assertEquals("", SessionTabUiLogic.ellipsizeEnd(null));
    }

    @Test
    public void activePositionFollowsHandleIdentity() {
        Assert.assertEquals(1, SessionTabUiLogic.activePosition(
            Arrays.asList("a", "b", "c"), "b"));
        Assert.assertEquals(0, SessionTabUiLogic.activePosition(
            Arrays.asList("a", null, "c"), "a"));
        Assert.assertEquals(-1, SessionTabUiLogic.activePosition(
            Arrays.asList("a", "b"), "missing"));
        Assert.assertEquals(-1, SessionTabUiLogic.activePosition(
            Arrays.asList("a", "b"), null));
    }
}
