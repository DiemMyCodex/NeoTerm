package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * Pure formatting/selection rules for the floating session tab bar.
 *
 * <p>Kept free of Android view types so the label and active-selection contracts can be
 * verified with plain JVM unit tests. Visual truncation itself is declared on
 * {@code item_session_tab.xml} ({@code maxWidth} + {@code ellipsize=end}).</p>
 */
final class SessionTabUiLogic {

    private SessionTabUiLogic() {
    }

    /**
     * Defensive cap for pathological session names. Normal labels are far below this limit and
     * are visually truncated by the TextView's own {@code maxWidth=160dp / ellipsize=end}, so
     * this never changes what the user sees.
     */
    static final int MAX_LABEL_CHARS = 120;

    /** "N: name" for a renamed session, else the shell title, else "N: shell". */
    @NonNull
    static String buildLabel(int position, @Nullable String sessionName, @Nullable String title) {
        String prefix = (position + 1) + ": ";
        if (sessionName != null && !sessionName.isEmpty()) return prefix + ellipsizeEnd(sessionName);
        if (title != null && !title.isEmpty()) return prefix + ellipsizeEnd(title);
        return prefix + "shell";
    }

    /** Trim to at most {@link #MAX_LABEL_CHARS} characters, marking a cut with an ellipsis. */
    @NonNull
    static String ellipsizeEnd(@Nullable String value) {
        if (value == null || value.isEmpty()) return "";
        if (value.length() <= MAX_LABEL_CHARS) return value;
        return value.substring(0, MAX_LABEL_CHARS - 1) + "\u2026";
    }

    /** Index of the active handle, or -1 when absent/no active session. */
    static int activePosition(@NonNull List<String> handles, @Nullable String activeHandle) {
        if (activeHandle == null) return -1;
        for (int i = 0; i < handles.size(); i++) {
            if (activeHandle.equals(handles.get(i))) return i;
        }
        return -1;
    }
}
