package com.termux.app.terminal.theme;

/**
 * Immutable, pure-JVM description of one synchronized 2026 theme.
 *
 * <p>Carries both the terminal ANSI palette and the Liquid Glass chrome accents so a single
 * selection can be applied everywhere. Contains no Android types by design: this class and
 * {@link ThemeRegistry} are unit-tested directly on the JVM.</p>
 */
public final class TerminalThemeSpec {

    /** Number of ANSI colors in a terminal palette (indices 0..15). */
    public static final int ANSI_COLORS = 16;

    public final String id;
    public final String displayName;
    public final int background;
    public final int foreground;
    public final int cursor;
    public final Chrome chrome;

    private final int[] mAnsi;

    public TerminalThemeSpec(String id, String displayName, String[] ansi,
                             String background, String foreground, String cursor, Chrome chrome) {
        if (ansi == null || ansi.length != ANSI_COLORS)
            throw new IllegalArgumentException("A theme needs exactly 16 ANSI colors: " + id);
        this.id = id;
        this.displayName = displayName;
        this.mAnsi = new int[ANSI_COLORS];
        for (int i = 0; i < ANSI_COLORS; i++) this.mAnsi[i] = Argb.parse(ansi[i]);
        this.background = Argb.parse(background);
        this.foreground = Argb.parse(foreground);
        this.cursor = Argb.parse(cursor);
        this.chrome = chrome == null ? Chrome.defaults().build() : chrome;
    }

    /** Defensive copy of the 16 ANSI colors. */
    public int[] ansiColors() {
        return mAnsi.clone();
    }

    public int ansiColor(int index) {
        return mAnsi[index];
    }

    /** All chrome colors needed to restyle the glass pill, tabs, glow, dock and keycaps. */
    public static final class Chrome {
        public final int pillSurface;
        public final int dockSurface;
        public final int menuSurface;
        public final int stroke;
        public final int softFill;
        public final int inactiveTabFill;
        public final int ambientGlow;
        public final int activeTabFill;
        public final int activeTabText;
        public final int inactiveTabText;
        public final int activeClose;
        public final int inactiveClose;
        public final int keycapFill;
        public final int keycapStroke;
        public final int keycapText;
        public final int statusIdle;
        public final int statusBusy;

        private Chrome(Builder b) {
            pillSurface = b.pillSurface;
            dockSurface = b.dockSurface;
            menuSurface = b.menuSurface;
            stroke = b.stroke;
            softFill = b.softFill;
            inactiveTabFill = b.inactiveTabFill;
            ambientGlow = b.ambientGlow;
            activeTabFill = b.activeTabFill;
            activeTabText = b.activeTabText;
            inactiveTabText = b.inactiveTabText;
            activeClose = b.activeClose;
            inactiveClose = b.inactiveClose;
            keycapFill = b.keycapFill;
            keycapStroke = b.keycapStroke;
            keycapText = b.keycapText;
            statusIdle = b.statusIdle;
            statusBusy = b.statusBusy;
        }

        /** Values mirror the PR #6 neoterm_* tokens (OLED Obsidian). */
        public static Builder defaults() {
            return new Builder()
                .pillSurface("#CC18181B")
                .dockSurface("#CC141417")
                .menuSurface("#E618181B")
                .stroke("#26FFFFFF")
                .softFill("#24FFFFFF")
                .inactiveTabFill("#14FFFFFF")
                .ambientGlow("#18FFFFFF")
                .activeTabFill("#FFFFFF")
                .activeTabText("#09090B")
                .inactiveTabText("#A1A1AA")
                .activeClose("#52525B")
                .inactiveClose("#71717A")
                .keycapFill("#1F1F23")
                .keycapStroke("#2E2E34")
                .keycapText("#FFF4F5")
                .statusIdle("#22C55E")
                .statusBusy("#F59E0B");
        }

        public static final class Builder {
            int pillSurface, dockSurface, menuSurface, stroke, softFill, inactiveTabFill;
            int ambientGlow, activeTabFill, activeTabText, inactiveTabText;
            int activeClose, inactiveClose, keycapFill, keycapStroke, keycapText;
            int statusIdle, statusBusy;

            public Builder pillSurface(String v) { pillSurface = Argb.parse(v); return this; }
            public Builder dockSurface(String v) { dockSurface = Argb.parse(v); return this; }
            public Builder menuSurface(String v) { menuSurface = Argb.parse(v); return this; }
            public Builder stroke(String v) { stroke = Argb.parse(v); return this; }
            public Builder softFill(String v) { softFill = Argb.parse(v); return this; }
            public Builder inactiveTabFill(String v) { inactiveTabFill = Argb.parse(v); return this; }
            public Builder ambientGlow(String v) { ambientGlow = Argb.parse(v); return this; }
            public Builder activeTabFill(String v) { activeTabFill = Argb.parse(v); return this; }
            public Builder activeTabText(String v) { activeTabText = Argb.parse(v); return this; }
            public Builder inactiveTabText(String v) { inactiveTabText = Argb.parse(v); return this; }
            public Builder activeClose(String v) { activeClose = Argb.parse(v); return this; }
            public Builder inactiveClose(String v) { inactiveClose = Argb.parse(v); return this; }
            public Builder keycapFill(String v) { keycapFill = Argb.parse(v); return this; }
            public Builder keycapStroke(String v) { keycapStroke = Argb.parse(v); return this; }
            public Builder keycapText(String v) { keycapText = Argb.parse(v); return this; }
            public Builder statusIdle(String v) { statusIdle = Argb.parse(v); return this; }
            public Builder statusBusy(String v) { statusBusy = Argb.parse(v); return this; }

            public Chrome build() {
                return new Chrome(this);
            }
        }
    }
}
