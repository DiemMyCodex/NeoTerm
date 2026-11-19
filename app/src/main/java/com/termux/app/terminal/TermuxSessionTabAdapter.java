package com.termux.app.terminal;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.TerminalSession;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter for the floating top session tab bar.
 *
 * <p>The backing list is the exact list owned by {@link com.termux.app.TermuxService}
 * ({@code TermuxService#getTermuxSessions()}). This adapter never copies or owns session state,
 * so it cannot drift from the service or the existing drawer list.</p>
 *
 * <p>All tab views are non-focusable: tapping them must not move focus away from
 * {@link com.termux.view.TerminalView}, whose focus listener drives soft keyboard visibility.</p>
 */
public class TermuxSessionTabAdapter extends RecyclerView.Adapter<TermuxSessionTabAdapter.TabViewHolder> {

    private static final int ACTIVE_TEXT_COLOR = Color.parseColor("#09090B");
    private static final int INACTIVE_TEXT_COLOR = Color.parseColor("#A1A1AA");
    private static final int ACTIVE_CLOSE_COLOR = Color.parseColor("#52525B");
    private static final int INACTIVE_CLOSE_COLOR = Color.parseColor("#71717A");

    private static final int MENU_RENAME = 0;
    private static final int MENU_DUPLICATE = 1;
    private static final int MENU_CLOSE_OTHERS = 2;
    private static final long PULSE_DURATION_MS = 800L;

    private final TermuxActivity mActivity;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final Map<String, Boolean> mForegroundStates = new HashMap<>();
    private final SessionProcessMonitor mProcessMonitor = new SessionProcessMonitor();

    private List<TermuxSession> mSessions = Collections.emptyList();
    private RecyclerView mRecyclerView;
    private int mLastItemCount;
    private int mLastActivePosition = -1;
    private int mIdleDotColor;
    private int mBusyDotColor;

    public TermuxSessionTabAdapter(@NonNull TermuxActivity activity) {
        mActivity = activity;
        mIdleDotColor = ContextCompat.getColor(activity, R.color.session_status_idle);
        mBusyDotColor = ContextCompat.getColor(activity, R.color.session_status_busy);
        mProcessMonitor.start(() -> mMainHandler.post(() -> refreshForegroundStates(true)));
    }

    /** Stops the /proc poller when the activity is torn down. */
    public void release() {
        mProcessMonitor.stop();
    }

    /** Bind to the service-owned session list. Call once after the service connects. */
    public void setSessions(@Nullable List<TermuxSession> sessions) {
        mSessions = sessions == null ? Collections.emptyList() : sessions;
        mForegroundStates.clear();
        mLastActivePosition = -1;
        notifyDataSetChanged();
    }

    /** Wire the adapter to the horizontal RecyclerView, spacing and spring animator. */
    public void attachTo(@NonNull final RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(mActivity, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setItemAnimator(new SessionTabItemAnimator());
        recyclerView.setAdapter(this);
        recyclerView.setFocusable(false);
        recyclerView.setFocusableInTouchMode(false);

        final int gapPx = Math.round(6 * recyclerView.getResources().getDisplayMetrics().density);
        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                       @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                int position = parent.getChildAdapterPosition(view);
                outRect.left = position > 0 ? gapPx : 0;
            }
        });

        mLastItemCount = getItemCount();

        registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                mLastItemCount = getItemCount();
                ensureActiveTabFullyVisible();
            }

            @Override
            public void onChanged() {
                // TermuxService notifies with notifyDataSetChanged() when a session is created,
                // so count comparison is needed to detect a genuinely new tab.
                int count = getItemCount();
                if (count > mLastItemCount) ensureActiveTabFullyVisible();
                mLastItemCount = count;
            }
        });
    }

    /**
     * Park the active pill flush with the trailing edge. When the active tab is the last one,
     * nothing remains to its right, so getRightFadingEdgeStrength() is 0 and its white pill and
     * X button can never be dimmed by the fading edge.
     */
    public void ensureActiveTabFullyVisible() {
        if (mRecyclerView == null) return;
        mRecyclerView.post(() -> {
            int count = getItemCount();
            if (count == 0) return;
            RecyclerView.LayoutManager lm = mRecyclerView.getLayoutManager();
            if (!(lm instanceof LinearLayoutManager)) return;

            int active = getActiveSessionPosition();
            alignTabToEnd((LinearLayoutManager) lm, active >= 0 ? active : count - 1);
        });
    }

    private void alignTabToEnd(@NonNull LinearLayoutManager layoutManager, int position) {
        View child = layoutManager.findViewByPosition(position);
        if (child == null) {
            // Not measured yet: let the LayoutManager resolve the target, then snap to end.
            LinearSmoothScroller scroller = new LinearSmoothScroller(mRecyclerView.getContext()) {
                @Override protected int getHorizontalSnapPreference() {
                    return SNAP_TO_END;
                }
            };
            scroller.setTargetPosition(position);
            layoutManager.startSmoothScroll(scroller);
            return;
        }
        int contentWidth = mRecyclerView.getWidth()
            - mRecyclerView.getPaddingLeft() - mRecyclerView.getPaddingRight();
        layoutManager.scrollToPositionWithOffset(position, contentWidth - child.getWidth());
    }

    private int getActiveSessionPosition() {
        TerminalSession current = mActivity.getCurrentSession();
        if (current == null) return -1;
        for (int i = 0; i < mSessions.size(); i++) {
            TerminalSession session = getTerminalSessionAt(i);
            if (session == current) return i;
        }
        return -1;
    }

    /** Keep the currently selected pill visible after creation or switch. */
    private void scrollToActiveTab(int position) {
        if (position < 0 || position == mLastActivePosition || mRecyclerView == null) return;
        mLastActivePosition = position;
        ensureActiveTabFullyVisible();
    }

    @NonNull
    @Override
    public TabViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mActivity).inflate(R.layout.item_session_tab, parent, false);
        return new TabViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TabViewHolder holder, int position) {
        TerminalSession session = getTerminalSessionAt(position);
        TerminalSession current = mActivity.getCurrentSession();
        boolean isActive = session != null && session == current;

        holder.itemView.setSelected(isActive);
        holder.title.setTextColor(isActive ? ACTIVE_TEXT_COLOR : INACTIVE_TEXT_COLOR);
        holder.title.setTypeface(null, isActive ? Typeface.BOLD : Typeface.NORMAL);
        holder.title.setText(buildLabel(position, session));
        ImageViewCompat.setImageTintList(holder.close,
            ColorStateList.valueOf(isActive ? ACTIVE_CLOSE_COLOR : INACTIVE_CLOSE_COLOR));
        holder.bindStatus(session, isBusy(session));

        if (isActive) scrollToActiveTab(position);
    }

    @Override
    public int getItemCount() {
        return mSessions.size();
    }

    @NonNull
    private String buildLabel(int position, @Nullable TerminalSession session) {
        String prefix = (position + 1) + ": ";
        if (session == null) return prefix + "?";

        String name = session.mSessionName;
        if (name != null && !name.isEmpty()) return prefix + name;

        String title = session.getTitle();
        if (title != null && !title.isEmpty()) return prefix + title;

        return prefix + "shell";
    }

    @Nullable
    private TerminalSession getTerminalSessionAt(int position) {
        if (position < 0 || position >= mSessions.size()) return null;
        TermuxSession termuxSession = mSessions.get(position);
        return termuxSession == null ? null : termuxSession.getTerminalSession();
    }

    private void refreshForegroundStates(boolean notifyIfChanged) {
        boolean changed = false;
        for (int i = 0; i < mSessions.size(); i++) {
            TerminalSession session = getTerminalSessionAt(i);
            if (session == null) continue;

            boolean busy = SessionProcessMonitor.isForegroundChildRunning(session);
            Boolean previous = mForegroundStates.put(session.mHandle, busy);
            if (previous == null || previous != busy) changed = true;
        }
        if (changed && notifyIfChanged) notifyDataSetChanged();
    }

    private boolean isBusy(@Nullable TerminalSession session) {
        if (session == null) return false;
        Boolean busy = mForegroundStates.get(session.mHandle);
        return busy != null && busy;
    }

    private void showTabMenu(@NonNull View anchor, @Nullable TerminalSession session) {
        if (session == null) return;

        anchor.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);

        String[] labels = {
            mActivity.getString(R.string.action_rename_session),
            mActivity.getString(R.string.action_duplicate_session),
            mActivity.getString(R.string.action_close_other_tabs)
        };

        final boolean[] actionSelected = {false};
        final ListPopupWindow popup = new ListPopupWindow(mActivity);
        popup.setAnchorView(anchor);
        popup.setAdapter(new ArrayAdapter<>(mActivity, R.layout.item_session_menu, labels));
        popup.setWidth(Math.max(anchor.getWidth(), dpToPx(196)));
        popup.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        popup.setModal(true);
        popup.setBackgroundDrawable(ContextCompat.getDrawable(mActivity, R.drawable.session_menu_background));
        popup.setOnItemClickListener((parent, view, position, id) -> {
            actionSelected[0] = true;
            popup.dismiss();
            switch (position) {
                case MENU_RENAME:
                    mActivity.getTermuxTerminalSessionClient().renameSession(session);
                    break;
                case MENU_DUPLICATE:
                    mActivity.getTermuxTerminalSessionClient().duplicateSession(session);
                    break;
                case MENU_CLOSE_OTHERS:
                    mActivity.getTermuxTerminalSessionClient().closeOtherSessions(session);
                    break;
                default:
                    break;
            }
        });
        // Plain dismiss (back/outside tap) must hand focus straight back to the terminal.
        // Actions that open their own dialog restore focus in their own dismissal callback.
        popup.setOnDismissListener(() -> {
            if (!actionSelected[0]) mActivity.restoreTerminalFocusAndIme(mActivity.shouldRestoreIme());
        });
        popup.show();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * mActivity.getResources().getDisplayMetrics().density);
    }

    class TabViewHolder extends RecyclerView.ViewHolder {

        final View statusDot;
        final TextView title;
        final ImageButton close;

        private final AlphaAnimation mPulseAnimation;

        TabViewHolder(@NonNull View itemView) {
            super(itemView);
            statusDot = itemView.findViewById(R.id.session_tab_status_dot);
            title = itemView.findViewById(R.id.session_tab_title);
            close = itemView.findViewById(R.id.session_tab_close);

            mPulseAnimation = new AlphaAnimation(0.4f, 1.0f);
            mPulseAnimation.setDuration(PULSE_DURATION_MS);
            mPulseAnimation.setRepeatCount(Animation.INFINITE);
            mPulseAnimation.setRepeatMode(Animation.REVERSE);

            // Belt-and-braces: the layout already declares these, enforce in code too.
            itemView.setFocusable(false);
            itemView.setFocusableInTouchMode(false);
            itemView.setOnTouchListener(new TactilePressTouchListener());
            statusDot.setFocusable(false);
            statusDot.setFocusableInTouchMode(false);
            title.setFocusable(false);
            title.setFocusableInTouchMode(false);
            close.setFocusable(false);
            close.setFocusableInTouchMode(false);

            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                TerminalSession session = getTerminalSessionAt(position);
                if (session == null) return;
                mActivity.getTermuxTerminalSessionClient().setCurrentSession(session);
                mActivity.restoreTerminalFocusAndIme(mActivity.shouldRestoreIme());
            });

            itemView.setOnLongClickListener(v -> {
                int position = getBindingAdapterPosition();
                showTabMenu(v, getTerminalSessionAt(position));
                return true;
            });

            close.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                TerminalSession session = getTerminalSessionAt(position);
                if (session == null) return;
                mActivity.getTermuxTerminalSessionClient().closeSession(session);
                mActivity.restoreTerminalFocusAndIme(mActivity.shouldRestoreIme());
            });
        }

        void bindStatus(@Nullable TerminalSession session, boolean busy) {
            if (busy) {
                setDotColor(mBusyDotColor);
                if (statusDot.getAnimation() == null) statusDot.startAnimation(mPulseAnimation);
            } else {
                statusDot.clearAnimation();
                statusDot.setAlpha(1f);
                setDotColor(mIdleDotColor);
            }
        }

        private void setDotColor(int color) {
            Drawable background = statusDot.getBackground();
            if (background instanceof GradientDrawable) {
                GradientDrawable drawable = (GradientDrawable) background.mutate();
                drawable.setColor(color);
            } else {
                statusDot.setBackgroundTintList(ColorStateList.valueOf(color));
            }
        }
    }
}
