package com.termux.app.terminal.theme;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ThemeRegistryTest {

    private static int alpha(int color) {
        return (color >>> 24) & 0xFF;
    }

    @Test
    public void allFourThemesResolveInStableOrder() {
        List<TerminalThemeSpec> themes = ThemeRegistry.all();
        Assert.assertEquals(4, themes.size());
        Assert.assertEquals("obsidian", themes.get(0).id);
        Assert.assertEquals("catppuccin_mocha", themes.get(1).id);
        Assert.assertEquals("tokyo_night", themes.get(2).id);
        Assert.assertEquals("dracula", themes.get(3).id);

        Set<String> ids = new HashSet<>();
        for (TerminalThemeSpec theme : themes) {
            Assert.assertTrue("duplicate id " + theme.id, ids.add(theme.id));
            Assert.assertNotNull(theme.displayName);
            Assert.assertFalse(theme.displayName.isEmpty());
        }
    }

    @Test
    public void everyThemeHasAValidCompletePalette() {
        for (TerminalThemeSpec theme : ThemeRegistry.all()) {
            int[] ansi = theme.ansiColors();
            Assert.assertEquals("ANSI palette length for " + theme.id,
                TerminalThemeSpec.ANSI_COLORS, ansi.length);
            Set<Integer> distinct = new HashSet<>();
            for (int i = 0; i < ansi.length; i++) {
                Assert.assertEquals("ANSI color " + i + " of " + theme.id + " must be opaque",
                    0xFF, alpha(ansi[i]));
                distinct.add(ansi[i]);
            }
            Assert.assertTrue("ANSI palette must contain real colors for " + theme.id,
                distinct.size() >= 10);
            Assert.assertEquals(ansi[0], theme.ansiColor(0));
            Assert.assertEquals(ansi[15], theme.ansiColor(15));
            Assert.assertEquals(theme.background, ansi[0]);
            Assert.assertEquals(theme.foreground, ansi[7]);

            Assert.assertEquals(0xFF, alpha(theme.background));
            Assert.assertEquals(0xFF, alpha(theme.foreground));
            Assert.assertEquals(0xFF, alpha(theme.cursor));
        }
    }

    @Test
    public void everyThemeHasValidChromeColors() {
        for (TerminalThemeSpec theme : ThemeRegistry.all()) {
            TerminalThemeSpec.Chrome c = theme.chrome;
            // Glass surfaces and glow are translucent; actionable fills/text are opaque.
            Assert.assertTrue("pillSurface alpha for " + theme.id, alpha(c.pillSurface) > 0 && alpha(c.pillSurface) < 0xFF);
            Assert.assertTrue("dockSurface alpha for " + theme.id, alpha(c.dockSurface) > 0 && alpha(c.dockSurface) < 0xFF);
            Assert.assertTrue("menuSurface alpha for " + theme.id, alpha(c.menuSurface) > 0 && alpha(c.menuSurface) < 0xFF);
            Assert.assertTrue("ambientGlow alpha for " + theme.id, alpha(c.ambientGlow) > 0 && alpha(c.ambientGlow) < 0xFF);
            Assert.assertTrue("stroke alpha for " + theme.id, alpha(c.stroke) > 0 && alpha(c.stroke) < 0xFF);
            Assert.assertEquals(0xFF, alpha(c.activeTabFill));
            Assert.assertEquals(0xFF, alpha(c.activeTabText));
            Assert.assertEquals(0xFF, alpha(c.inactiveTabText));
            Assert.assertEquals(0xFF, alpha(c.keycapFill));
            Assert.assertEquals(0xFF, alpha(c.keycapStroke));
            Assert.assertEquals(0xFF, alpha(c.keycapText));
            Assert.assertEquals(0xFF, alpha(c.statusIdle));
            Assert.assertEquals(0xFF, alpha(c.statusBusy));
        }
    }

    @Test
    public void obsidianSignatureMatchesSpec() {
        TerminalThemeSpec t = ThemeRegistry.get("obsidian");
        Assert.assertEquals("OLED Obsidian", t.displayName);
        Assert.assertEquals(Argb.parse("#000000"), t.background);
        Assert.assertEquals(Argb.parse("#F4F4F5"), t.foreground);
        Assert.assertEquals(Argb.parse("#38BDF8"), t.cursor);
        Assert.assertEquals(Argb.parse("#CC18181B"), t.chrome.pillSurface);
        Assert.assertEquals(Argb.parse("#18FFFFFF"), t.chrome.ambientGlow);
        Assert.assertEquals(Argb.parse("#FFFFFF"), t.chrome.activeTabFill);
        Assert.assertEquals(Argb.parse("#09090B"), t.chrome.activeTabText);
        Assert.assertEquals(Argb.parse("#1F1F23"), t.chrome.keycapFill);
        Assert.assertEquals(Argb.parse("#2E2E34"), t.chrome.keycapStroke);
    }

    @Test
    public void catppuccinSignatureMatchesSpec() {
        TerminalThemeSpec t = ThemeRegistry.get("catppuccin_mocha");
        Assert.assertEquals("Catppuccin Mocha", t.displayName);
        Assert.assertEquals(Argb.parse("#11111B"), t.background);
        Assert.assertEquals(Argb.parse("#CDD6F4"), t.foreground);
        Assert.assertEquals(Argb.parse("#F5E0DC"), t.cursor);
        Assert.assertEquals(Argb.parse("#F38BA8"), t.ansiColor(1));
        Assert.assertEquals(Argb.parse("#A6E3A1"), t.ansiColor(2));
        Assert.assertEquals(Argb.parse("#F9E2AF"), t.ansiColor(3));
        Assert.assertEquals(Argb.parse("#89B4FA"), t.ansiColor(4));
        Assert.assertEquals(Argb.parse("#CBA6F7"), t.ansiColor(5));
        Assert.assertEquals(Argb.parse("#94E2D5"), t.ansiColor(6));
        Assert.assertEquals(Argb.parse("#CC181825"), t.chrome.pillSurface);
        Assert.assertEquals(Argb.parse("#26CBA6F7"), t.chrome.ambientGlow);
        Assert.assertEquals(Argb.parse("#CBA6F7"), t.chrome.activeTabFill);
        Assert.assertEquals(Argb.parse("#11111B"), t.chrome.activeTabText);
        Assert.assertEquals(Argb.parse("#1E1E2E"), t.chrome.keycapFill);
        Assert.assertEquals(Argb.parse("#313244"), t.chrome.keycapStroke);
    }

    @Test
    public void tokyoNightSignatureMatchesSpec() {
        TerminalThemeSpec t = ThemeRegistry.get("tokyo_night");
        Assert.assertEquals("Tokyo Night", t.displayName);
        Assert.assertEquals(Argb.parse("#16161E"), t.background);
        Assert.assertEquals(Argb.parse("#C0CAF5"), t.foreground);
        Assert.assertEquals(Argb.parse("#7AA2F7"), t.cursor);
        Assert.assertEquals(Argb.parse("#F7768E"), t.ansiColor(1));
        Assert.assertEquals(Argb.parse("#9ECE6A"), t.ansiColor(2));
        Assert.assertEquals(Argb.parse("#E0AF68"), t.ansiColor(3));
        Assert.assertEquals(Argb.parse("#7AA2F7"), t.ansiColor(4));
        Assert.assertEquals(Argb.parse("#BB9AF7"), t.ansiColor(5));
        Assert.assertEquals(Argb.parse("#7DCFFF"), t.ansiColor(6));
        Assert.assertEquals(Argb.parse("#CC16161E"), t.chrome.pillSurface);
        Assert.assertEquals(Argb.parse("#267AA2F7"), t.chrome.ambientGlow);
        Assert.assertEquals(Argb.parse("#7AA2F7"), t.chrome.activeTabFill);
        Assert.assertEquals(Argb.parse("#16161E"), t.chrome.activeTabText);
        Assert.assertEquals(Argb.parse("#1F2335"), t.chrome.keycapFill);
        Assert.assertEquals(Argb.parse("#292E42"), t.chrome.keycapStroke);
    }

    @Test
    public void draculaSignatureMatchesSpec() {
        TerminalThemeSpec t = ThemeRegistry.get("dracula");
        Assert.assertEquals("Dracula", t.displayName);
        Assert.assertEquals(Argb.parse("#191A21"), t.background);
        Assert.assertEquals(Argb.parse("#F8F8F2"), t.foreground);
        Assert.assertEquals(Argb.parse("#FF79C6"), t.cursor);
        Assert.assertEquals(Argb.parse("#FF5555"), t.ansiColor(1));
        Assert.assertEquals(Argb.parse("#50FA7B"), t.ansiColor(2));
        Assert.assertEquals(Argb.parse("#F1FA8C"), t.ansiColor(3));
        Assert.assertEquals(Argb.parse("#BD93F9"), t.ansiColor(4));
        Assert.assertEquals(Argb.parse("#FF79C6"), t.ansiColor(5));
        Assert.assertEquals(Argb.parse("#8BE9FD"), t.ansiColor(6));
        Assert.assertEquals(Argb.parse("#CC191A21"), t.chrome.pillSurface);
        Assert.assertEquals(Argb.parse("#26BD93F9"), t.chrome.ambientGlow);
        Assert.assertEquals(Argb.parse("#BD93F9"), t.chrome.activeTabFill);
        Assert.assertEquals(Argb.parse("#191A21"), t.chrome.activeTabText);
        Assert.assertEquals(Argb.parse("#282A36"), t.chrome.keycapFill);
        Assert.assertEquals(Argb.parse("#44475A"), t.chrome.keycapStroke);
    }

    @Test
    public void unknownAndNullIdsFallBackToObsidian() {
        TerminalThemeSpec fallback = ThemeRegistry.get(ThemeRegistry.DEFAULT_THEME_ID);
        Assert.assertSame(fallback, ThemeRegistry.get(null));
        Assert.assertSame(fallback, ThemeRegistry.get(""));
        Assert.assertSame(fallback, ThemeRegistry.get("does-not-exist"));
        Assert.assertSame(fallback, ThemeRegistry.get("OBSIDIAN"));

        Assert.assertTrue(ThemeRegistry.isValid("dracula"));
        Assert.assertFalse(ThemeRegistry.isValid("nope"));
        Assert.assertFalse(ThemeRegistry.isValid(null));
    }

    @Test
    public void defensiveCopiesProtectTheRegistry() {
        TerminalThemeSpec first = ThemeRegistry.get("obsidian");
        int[] copy = first.ansiColors();
        copy[0] = 0;
        Assert.assertNotEquals(0, first.ansiColor(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void incompletePaletteIsRejected() {
        new TerminalThemeSpec("bad", "Bad", new String[15], "#000000", "#FFFFFF", "#FFFFFF", null);
    }
}
