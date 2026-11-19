package com.termux.app.terminal.theme;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import androidx.viewpager.widget.ViewPager;

import androidx.annotation.NonNull;
import androidx.core.widget.ImageViewCompat;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.terminal.TermuxSessionTabAdapter;
import com.termux.shared.shell.command.runner.terminal.TermuxSession;
import com.termux.shared.termux.extrakeys.ExtraKeysView;
import com.termux.terminal.TerminalColors;
import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalSession;
import com.termux.terminal.TextStyle;
import com.termux.view.TerminalView;

/**
 * Applies a {@link TerminalThemeSpec} to every visible surface in one synchronous pass.
 *
 * <p>There is no polling, no background work and no activity recreation. The four surfaces are:
 * the emulator palettes of <em>all</em> open sessions, the glass chrome drawables, the tab adapter
 * and the keycaps. Persistence lives in a tiny private {@link SharedPreferences} file under
 * {@link ThemeRegistry#PREF_ACTIVE_THEME}.</p>
 */
public final class ThemeApplier {

    private static final String PREFS_FILE = "neoterm_theme";

    private final TermuxActivity mActivity;
    private TerminalThemeSpec mCurrent;

    public ThemeApplier(@NonNull TermuxActivity activity) {
        mActivity = activity;
    }

    /** The active theme, resolving the persisted id (including corrupt values) on first use. */
    public TerminalThemeSpec current() {
        if (mCurrent == null) mCurrent = ThemeRegistry.get(loadThemeId());
        return mCurrent;
    }

    public String loadThemeId() {
        SharedPreferences prefs = mActivity.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        return prefs.getString(ThemeRegistry.PREF_ACTIVE_THEME, ThemeRegistry.DEFAULT_THEME_ID);
    }

    /** Called once from {@code onCreate()} after all chrome views have been inflated. */
    public void applyPersisted() {
        apply(current());
    }

    /** Apply, persist and repaint. Safe to call from the palette tap or the styling reload path. */
    public void apply(@NonNull TerminalThemeSpec theme) {
        mCurrent = theme;
        mActivity.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE).edit()
            .putString(ThemeRegistry.PREF_ACTIVE_THEME, theme.id)
            .apply();

        applyTerminalPalette(theme);
        applyChrome(theme);
        applyAdapter(theme);
        applyKeycaps(theme);
    }

    /** Apply the current palette to a keycap view that was just instantiated by the pager. */
    public void applyKeycapsTo(@NonNull ExtraKeysView view) {
        TerminalThemeSpec.Chrome c = current().chrome;
        view.applyThemeKeycaps(c.keycapFill, c.keycapStroke, c.keycapText, c.keycapText);
    }

    private void applyTerminalPalette(TerminalThemeSpec theme) {
        int[] defaults = TerminalColors.COLOR_SCHEME.mDefaultColors;
        int[] ansi = theme.ansiColors();
        System.arraycopy(ansi, 0, defaults, 0, TerminalThemeSpec.ANSI_COLORS);
        defaults[TextStyle.COLOR_INDEX_FOREGROUND] = theme.foreground;
        defaults[TextStyle.COLOR_INDEX_BACKGROUND] = theme.background;
        defaults[TextStyle.COLOR_INDEX_CURSOR] = theme.cursor;

        // Every emulator snapshots the global defaults when it is created. Reset them all so a
        // session opened before the switch cannot show the old palette when it becomes current.
        TermuxService service = mActivity.getTermuxService();
        if (service != null) {
            for (TermuxSession session : service.getTermuxSessions()) {
                if (session != null) resetEmulator(session.getTerminalSession());
            }
        }

        TerminalView view = mActivity.getTerminalView();
        if (view != null) {
            if (view.mEmulator != null) view.mEmulator.mColors.reset();
            view.invalidate();
        }
        mActivity.getTermuxTerminalSessionClient().updateBackgroundColor();
    }

    private static void resetEmulator(TerminalSession session) {
        if (session == null) return;
        TerminalEmulator emulator = session.getEmulator();
        if (emulator != null) emulator.mColors.reset();
    }

    private void applyChrome(TerminalThemeSpec theme) {
        TerminalThemeSpec.Chrome c = theme.chrome;
        int hairline = mActivity.getResources().getDimensionPixelSize(R.dimen.neoterm_hairline);

        View bar = mActivity.findViewById(R.id.terminal_session_tab_bar);
        tintShape(bar, c.pillSurface, c.stroke, hairline);

        View dock = mActivity.findViewById(R.id.terminal_extra_keys_dock);
        tintShape(dock, c.dockSurface, c.stroke, hairline);

        View glow = mActivity.findViewById(R.id.terminal_top_ambient_glow);
        tintGradient(glow, c.ambientGlow);

        ImageButton paletteButton = mActivity.findViewById(R.id.theme_switcher_button);
        tintShape(paletteButton, c.softFill, c.stroke, hairline);
        if (paletteButton != null)
            ImageViewCompat.setImageTintList(paletteButton, ColorStateList.valueOf(c.activeTabFill));

        // The + button uses the same translucent glass fill. Keep its glyph white for contrast.
        ImageButton addButton = mActivity.findViewById(R.id.new_session_tab_button);
        tintShape(addButton, c.softFill, c.stroke, hairline);
    }

    private void applyAdapter(TerminalThemeSpec theme) {
        TermuxSessionTabAdapter adapter = mActivity.getSessionTabAdapter();
        if (adapter != null) adapter.applyTheme(theme.chrome);
    }

    private void applyKeycaps(TerminalThemeSpec theme) {
        ViewPager pager = mActivity.getTerminalToolbarViewPager();
        if (pager != null) {
            for (int i = 0; i < pager.getChildCount(); i++)
                applyKeycapsToViewHierarchy(pager.getChildAt(i), theme.chrome);
        }
        ExtraKeysView direct = mActivity.getExtraKeysView();
        if (direct != null) applyKeycapsTo(direct);
    }

    private void applyKeycapsToViewHierarchy(View view, TerminalThemeSpec.Chrome c) {
        if (view instanceof ExtraKeysView) {
            view.applyThemeKeycaps(c.keycapFill, c.keycapStroke, c.keycapText, c.keycapText);
            return;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++)
                applyKeycapsToViewHierarchy(group.getChildAt(i), c);
        }
    }

    /** Retint a shape drawable, including the nested shape inside a ripple/layer drawable. */
    private static void tintShape(View view, int fill, int stroke, int strokeWidth) {
        if (view == null) return;
        tintShapeDrawable(view.getBackground(), fill, stroke, strokeWidth);
    }

    private static void tintShapeDrawable(Drawable drawable, int fill, int stroke, int strokeWidth) {
        if (drawable instanceof GradientDrawable) {
            GradientDrawable shape = (GradientDrawable) drawable.mutate();
            shape.setColor(fill);
            shape.setStroke(strokeWidth, stroke);
        } else if (drawable instanceof LayerDrawable) {
            LayerDrawable layer = (LayerDrawable) drawable;
            for (int i = 0; i < layer.getNumberOfLayers(); i++) {
                Drawable child = layer.getDrawable(i);
                if (child != null) tintShapeDrawable(child, fill, stroke, strokeWidth);
            }
        } else if (drawable instanceof InsetDrawable) {
            Drawable child = ((InsetDrawable) drawable).getDrawable();
            if (child != null) tintShapeDrawable(child, fill, stroke, strokeWidth);
        }
    }

    private static void tintGradient(View view, int color) {
        if (view == null) return;
        Drawable background = view.getBackground();
        if (background instanceof GradientDrawable) {
            GradientDrawable shape = (GradientDrawable) background.mutate();
            shape.setColors(new int[] {color, Color.TRANSPARENT});
        }
    }
}
