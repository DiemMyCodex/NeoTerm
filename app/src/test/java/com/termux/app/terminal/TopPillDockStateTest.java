package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class TopPillDockStateTest {

    @Test
    public void defaultStateIsExpandedFullWidth() {
        TopPillDockState state = TopPillDockState.forCollapsed(false);

        Assert.assertEquals(-1, state.wrapperWidth);       // MATCH_PARENT
        Assert.assertEquals(0, state.barVisibility);       // VISIBLE
        Assert.assertEquals(0, state.tabsVisibility);      // VISIBLE
        Assert.assertEquals(0, state.paletteVisibility);   // VISIBLE
        Assert.assertEquals(0, state.addVisibility);       // VISIBLE
        Assert.assertEquals(0, state.collapseVisibility);  // VISIBLE
        Assert.assertEquals(8, state.miniPillVisibility);  // GONE
        Assert.assertEquals(8, state.statusDotVisibility);// GONE
        Assert.assertEquals(8, state.expandVisibility);   // GONE
    }

    @Test
    public void collapsedStateShrinksWrapperAndShowsOnlyMiniPill() {
        TopPillDockState state = TopPillDockState.forCollapsed(true);

        Assert.assertEquals(-2, state.wrapperWidth);       // WRAP_CONTENT -> row 0 pass-through
        Assert.assertEquals(8, state.barVisibility);       // GONE
        Assert.assertEquals(8, state.tabsVisibility);      // GONE
        Assert.assertEquals(8, state.paletteVisibility);   // GONE
        Assert.assertEquals(8, state.addVisibility);       // GONE
        Assert.assertEquals(8, state.collapseVisibility);  // GONE
        Assert.assertEquals(0, state.miniPillVisibility);  // VISIBLE
        Assert.assertEquals(0, state.statusDotVisibility);// VISIBLE
        Assert.assertEquals(0, state.expandVisibility);   // VISIBLE
    }

    @Test
    public void forCollapsedMapsBothBooleans() {
        assertSameState(TopPillDockState.collapsed(), TopPillDockState.forCollapsed(true));
        assertSameState(TopPillDockState.expanded(), TopPillDockState.forCollapsed(false));
    }

    @Test
    public void roundTripToggleReturnsToOriginalState() {
        TopPillDockState start = TopPillDockState.forCollapsed(false);
        TopPillDockState collapsed = TopPillDockState.forCollapsed(true);
        TopPillDockState reExpanded = TopPillDockState.forCollapsed(false);
        TopPillDockState reCollapsed = TopPillDockState.forCollapsed(true);

        Assert.assertEquals(-2, collapsed.wrapperWidth);
        assertSameState(start, reExpanded);
        assertSameState(collapsed, reCollapsed);
    }

    private static void assertSameState(TopPillDockState a, TopPillDockState b) {
        Assert.assertEquals(a.wrapperWidth, b.wrapperWidth);
        Assert.assertEquals(a.barVisibility, b.barVisibility);
        Assert.assertEquals(a.tabsVisibility, b.tabsVisibility);
        Assert.assertEquals(a.paletteVisibility, b.paletteVisibility);
        Assert.assertEquals(a.addVisibility, b.addVisibility);
        Assert.assertEquals(a.collapseVisibility, b.collapseVisibility);
        Assert.assertEquals(a.miniPillVisibility, b.miniPillVisibility);
        Assert.assertEquals(a.statusDotVisibility, b.statusDotVisibility);
        Assert.assertEquals(a.expandVisibility, b.expandVisibility);
    }
}
