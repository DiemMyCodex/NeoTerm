package com.termux.app.terminal.theme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central registry for the four synchronized 2026 themes.
 *
 * <p>Pure JVM: no Android, no resources, no I/O. Unknown or null ids always fall back to
 * OLED Obsidian so a corrupt preference can never break a cold launch.</p>
 */
public final class ThemeRegistry {

    public static final String PREF_ACTIVE_THEME = "neoterm_active_theme";
    public static final String DEFAULT_THEME_ID = "obsidian";

    private static final Map<String, TerminalThemeSpec> THEMES = new LinkedHashMap<>();

    static {
        register(obsidian());
        register(catppuccinMocha());
        register(tokyoNight());
        register(dracula());
    }

    private ThemeRegistry() {
    }

    private static void register(TerminalThemeSpec theme) {
        THEMES.put(theme.id, theme);
    }

    /** Never returns null: unknown/null ids resolve to OLED Obsidian. */
    public static TerminalThemeSpec get(String id) {
        TerminalThemeSpec theme = id == null ? null : THEMES.get(id);
        return theme == null ? THEMES.get(DEFAULT_THEME_ID) : theme;
    }

    public static boolean isValid(String id) {
        return id != null && THEMES.containsKey(id);
    }

    /** Stable display order: Obsidian, Catppuccin Mocha, Tokyo Night, Dracula. */
    public static List<TerminalThemeSpec> all() {
        return Collections.unmodifiableList(new ArrayList<>(THEMES.values()));
    }

    private static TerminalThemeSpec obsidian() {
        // Crisp high-contrast ANSI palette.
        String[] ansi = {
            "#000000", "#FF5C57", "#5AF78E", "#F3F99D",
            "#57C7FF", "#FF6AC1", "#5AEEEE", "#F4F4F5",
            "#545457", "#FF7873", "#7DFA9B", "#F7FCA1",
            "#7AD1FF", "#FF8AD0", "#7BF7F7", "#FFFFFF"
        };
        return new TerminalThemeSpec("obsidian", "OLED Obsidian", ansi,
            "#000000", "#F4F4F5", "#38BDF8",
            TerminalThemeSpec.Chrome.defaults()
                .ambientGlow("#18FFFFFF")
                .activeTabFill("#FFFFFF")
                .build());
    }

    private static TerminalThemeSpec catppuccinMocha() {
        String[] ansi = {
            "#11111B", "#F38BA8", "#A6E3A1", "#F9E2AF",
            "#89B4FA", "#CBA6F7", "#94E2D5", "#CDD6F4",
            "#585B70", "#F38BA8", "#A6E3A1", "#F9E2AF",
            "#89B4FA", "#CBA6F7", "#94E2D5", "#F5E0DC"
        };
        return new TerminalThemeSpec("catppuccin_mocha", "Catppuccin Mocha", ansi,
            "#11111B", "#CDD6F4", "#F5E0DC",
            TerminalThemeSpec.Chrome.defaults()
                .pillSurface("#CC181825")
                .dockSurface("#CC181825")
                .menuSurface("#E6181825")
                .inactiveTabFill("#14CBA6F7")
                .ambientGlow("#26CBA6F7")
                .activeTabFill("#CBA6F7")
                .activeTabText("#11111B")
                .keycapFill("#1E1E2E")
                .keycapStroke("#313244")
                .build());
    }

    private static TerminalThemeSpec tokyoNight() {
        String[] ansi = {
            "#16161E", "#F7768E", "#9ECE6A", "#E0AF68",
            "#7AA2F7", "#BB9AF7", "#7DCFFF", "#C0CAF5",
            "#565A6E", "#F7768E", "#9ECE6A", "#E0AF68",
            "#7AA2F7", "#BB9AF7", "#7DCFFF", "#FFFFFF"
        };
        return new TerminalThemeSpec("tokyo_night", "Tokyo Night", ansi,
            "#16161E", "#C0CAF5", "#7AA2F7",
            TerminalThemeSpec.Chrome.defaults()
                .pillSurface("#CC16161E")
                .dockSurface("#CC16161E")
                .menuSurface("#E616161E")
                .inactiveTabFill("#147AA2F7")
                .ambientGlow("#267AA2F7")
                .activeTabFill("#7AA2F7")
                .activeTabText("#16161E")
                .keycapFill("#1F2335")
                .keycapStroke("#292E42")
                .build());
    }

    private static TerminalThemeSpec dracula() {
        String[] ansi = {
            "#191A21", "#FF5555", "#50FA7B", "#F1FA8C",
            "#BD93F9", "#FF79C6", "#8BE9FD", "#F8F8F2",
            "#6272A4", "#FF6E6E", "#69FF94", "#FFFFA5",
            "#D6ACFF", "#FF92DF", "#A4FFFF", "#FFFFFF"
        };
        return new TerminalThemeSpec("dracula", "Dracula", ansi,
            "#191A21", "#F8F8F2", "#FF79C6",
            TerminalThemeSpec.Chrome.defaults()
                .pillSurface("#CC191A21")
                .dockSurface("#CC191A21")
                .menuSurface("#E6191A21")
                .inactiveTabFill("#14BD93F9")
                .ambientGlow("#26BD93F9")
                .activeTabFill("#BD93F9")
                .activeTabText("#191A21")
                .keycapFill("#282A36")
                .keycapStroke("#44475A")
                .build());
    }
}
