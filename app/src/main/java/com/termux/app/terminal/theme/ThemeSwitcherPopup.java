package com.termux.app.terminal.theme;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.termux.R;
import com.termux.app.TermuxActivity;

/**
 * Focus-safe glass palette popup anchored to the 34dp palette button in the top pill.
 *
 * <p>The window is deliberately non-focusable so the terminal never loses the IME; outside
 * touches dismiss it and every dismissal path hands focus back to {@code TerminalView}.</p>
 */
public final class ThemeSwitcherPopup {

    private static final int ROW_TEXT_COLOR = 0xFFF4F4F5;

    private final TermuxActivity mActivity;
    private final ThemeApplier mApplier;
    private PopupWindow mPopup;
    private boolean mSelectionHandled;

    public ThemeSwitcherPopup(@NonNull TermuxActivity activity, @NonNull ThemeApplier applier) {
        mActivity = activity;
        mApplier = applier;
    }

    public void show(@NonNull View anchor) {
        if (mPopup != null && mPopup.isShowing()) {
            mPopup.dismiss();
            return;
        }

        mSelectionHandled = false;
        anchor.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);

        int padding = dp(8);
        LinearLayout rows = new LinearLayout(mActivity);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setPadding(padding, padding, padding, padding);
        rows.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        rows.setFocusable(false);
        rows.setFocusableInTouchMode(false);

        String activeId = mApplier.current().id;
        for (TerminalThemeSpec theme : ThemeRegistry.all())
            rows.addView(createRow(theme, activeId.equals(theme.id)));

        mPopup = new PopupWindow(mActivity);
        mPopup.setContentView(rows);
        mPopup.setWidth(mActivity.getResources().getDimensionPixelSize(R.dimen.neoterm_theme_menu_width));
        mPopup.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mPopup.setBackgroundDrawable(ContextCompat.getDrawable(mActivity, R.drawable.theme_menu_background));
        mPopup.setFocusable(false);
        mPopup.setOutsideTouchable(true);
        mPopup.setTouchable(true);
        mPopup.setElevation(mActivity.getResources().getDimension(R.dimen.neoterm_pill_elevation));
        mPopup.setOnDismissListener(() -> {
            if (!mSelectionHandled) restoreFocus();
        });
        mPopup.showAsDropDown(anchor, 0, dp(4));
    }

    private View createRow(TerminalThemeSpec theme, boolean selected) {
        View row = LayoutInflater.from(mActivity).inflate(R.layout.item_theme_option, null, false);
        row.setFocusable(false);
        row.setFocusableInTouchMode(false);

        int strokeWidth = mActivity.getResources().getDimensionPixelSize(R.dimen.neoterm_hairline);
        setSwatch(row.findViewById(R.id.theme_swatch_base), theme.background, theme.chrome.stroke, strokeWidth);
        setSwatch(row.findViewById(R.id.theme_swatch_accent), theme.cursor, theme.chrome.stroke, strokeWidth);

        TextView title = row.findViewById(R.id.theme_option_title);
        title.setText(theme.displayName);
        title.setTextColor(selected ? theme.chrome.activeTabFill : ROW_TEXT_COLOR);
        title.setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

        ImageView check = row.findViewById(R.id.theme_option_check);
        check.setVisibility(selected ? View.VISIBLE : View.GONE);
        ImageViewCompat.setImageTintList(check,
            ColorStateList.valueOf(theme.chrome.activeTabFill));

        row.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            mSelectionHandled = true;
            mApplier.apply(theme);
            if (mPopup != null) mPopup.dismiss();
            restoreFocus();
        });
        return row;
    }

    private static void setSwatch(View view, int fill, int stroke, int strokeWidth) {
        Drawable background = view.getBackground();
        if (background instanceof GradientDrawable) {
            GradientDrawable shape = (GradientDrawable) background.mutate();
            shape.setColor(fill);
            shape.setStroke(strokeWidth, stroke);
        }
    }

    private void restoreFocus() {
        mActivity.restoreTerminalFocusAndIme(mActivity.shouldRestoreIme());
    }

    private int dp(int value) {
        return Math.round(mActivity.getResources().getDisplayMetrics().density * value);
    }
}
