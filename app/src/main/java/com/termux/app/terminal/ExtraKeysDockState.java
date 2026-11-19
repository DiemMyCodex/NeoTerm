package com.termux.app.terminal;

/**
 * Pure description of the two extra-keys dock layouts.
 *
 * <p>The activity applies these integers verbatim to real {@code LayoutParams}. Extracting the
 * contract keeps the non-animated width snap / re-measure fix untouched while making it testable
 * on the JVM. The integer values mirror the Android framework constants
 * ({@code MATCH_PARENT=-1}, {@code WRAP_CONTENT=-2}, {@code VISIBLE=0}, {@code GONE=8}).</p>
 */
public final class ExtraKeysDockState {

    static final int MATCH_PARENT = -1;
    static final int WRAP_CONTENT = -2;
    static final int VISIBLE = 0;
    static final int GONE = 8;
    static final float PAGER_WEIGHT = 1f;

    public final int dockWidth;
    public final int pagerWidth;
    public final float pagerWeight;
    public final int pagerVisibility;

    private ExtraKeysDockState(int dockWidth, int pagerWidth, float pagerWeight, int pagerVisibility) {
        this.dockWidth = dockWidth;
        this.pagerWidth = pagerWidth;
        this.pagerWeight = pagerWeight;
        this.pagerVisibility = pagerVisibility;
    }

    /** Full-width dock; the pager is visible and owns the row via 0dp + weight=1. */
    static ExtraKeysDockState expanded() {
        return new ExtraKeysDockState(MATCH_PARENT, 0, PAGER_WEIGHT, VISIBLE);
    }

    /** Compact pill pinned right; the pager is GONE so its width never animates. */
    static ExtraKeysDockState collapsed() {
        return new ExtraKeysDockState(WRAP_CONTENT, 0, 0f, GONE);
    }

    public static ExtraKeysDockState forCollapsed(boolean collapsed) {
        return collapsed ? collapsed() : expanded();
    }
}
