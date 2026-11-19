package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class ExtraKeysDockStateTest {

    @Test
    public void expandedStateEnforcesFullWidthVisibleWeightedPager() {
        ExtraKeysDockState state = ExtraKeysDockState.expanded();

        Assert.assertEquals(-1, state.dockWidth);        // MATCH_PARENT
        Assert.assertEquals(0, state.pagerWidth);        // 0dp so the weight applies
        Assert.assertEquals(1f, state.pagerWeight, 0f);  // weight=1
        Assert.assertEquals(0, state.pagerVisibility);   // VISIBLE
    }

    @Test
    public void collapsedStateEnforcesWrapContentAndHiddenPager() {
        ExtraKeysDockState state = ExtraKeysDockState.collapsed();

        Assert.assertEquals(-2, state.dockWidth);  // WRAP_CONTENT
        Assert.assertEquals(8, state.pagerVisibility); // GONE
        // Width is never animated in the collapsed state - only GONE + WRAP_CONTENT.
        Assert.assertEquals(0, state.pagerWidth);
    }

    @Test
    public void forCollapsedMapsBothBooleans() {
        Assert.assertEquals(ExtraKeysDockState.collapsed().dockWidth,
            ExtraKeysDockState.forCollapsed(true).dockWidth);
        Assert.assertEquals(ExtraKeysDockState.collapsed().pagerVisibility,
            ExtraKeysDockState.forCollapsed(true).pagerVisibility);
        Assert.assertEquals(ExtraKeysDockState.expanded().dockWidth,
            ExtraKeysDockState.forCollapsed(false).dockWidth);
        Assert.assertEquals(ExtraKeysDockState.expanded().pagerVisibility,
            ExtraKeysDockState.forCollapsed(false).pagerVisibility);
    }
}
