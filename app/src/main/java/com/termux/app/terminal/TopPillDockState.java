package com.termux.app.terminal;

/**
 * Pure, testable description of the floating session pill's two layouts.
 *
 * <p>The activity applies these integers verbatim to real {@code LayoutParams} and view
 * visibilities. Extracting the contract keeps the collapse snap non-animated (no main-thread
 * jank) while making the exact width/visibility matrix testable on the JVM. The values mirror
 * the Android framework constants ({@code MATCH_PARENT=-1}, {@code WRAP_CONTENT=-2},
 * {@code VISIBLE=0}, {@code GONE=8}).</p>
 *
 * <p>No Android UI imports by design: this is the top-pill counterpart of
 * {@link ExtraKeysDockState}.</p>
 */
public final class TopPillDockState {

    public static final int MATCH_PARENT = -1;
    public static final int WRAP_CONTENT = -2;
    public static final int VISIBLE = 0;
    public static final int GONE = 8;

    /** Outer wrapper width. WRAP_CONTENT is what exposes TerminalView row 0 on the left. */
    public final int wrapperWidth;

    /** Expanded full-width glass bar. */
    public final int barVisibility;
    public final int tabsVisibility;
    public final int paletteVisibility;
    public final int addVisibility;
    public final int collapseVisibility;

    /** Collapsed top-right mini pill: status dot + expand chevron only. */
    public final int miniPillVisibility;
    public final int statusDotVisibility;
    public final int expandVisibility;

    private TopPillDockState(int wrapperWidth, int barVisibility, int tabsVisibility,
                             int paletteVisibility, int addVisibility, int collapseVisibility,
                             int miniPillVisibility, int statusDotVisibility, int expandVisibility) {
        this.wrapperWidth = wrapperWidth;
        this.barVisibility = barVisibility;
        this.tabsVisibility = tabsVisibility;
        this.paletteVisibility = paletteVisibility;
        this.addVisibility = addVisibility;
        this.collapseVisibility = collapseVisibility;
        this.miniPillVisibility = miniPillVisibility;
        this.statusDotVisibility = statusDotVisibility;
        this.expandVisibility = expandVisibility;
    }

    /** Default: full-width bar, every chrome control visible, mini pill hidden. */
    public static TopPillDockState expanded() {
        return new TopPillDockState(MATCH_PARENT,
            VISIBLE, VISIBLE, VISIBLE, VISIBLE, VISIBLE,
            GONE, GONE, GONE);
    }

    /** Compact top-right pill: wrap content, hide every expanded control, show dot + chevron. */
    public static TopPillDockState collapsed() {
        return new TopPillDockState(WRAP_CONTENT,
            GONE, GONE, GONE, GONE, GONE,
            VISIBLE, VISIBLE, VISIBLE);
    }

    public static TopPillDockState forCollapsed(boolean collapsed) {
        return collapsed ? collapsed() : expanded();
    }
}
