package com.termux.app.terminal;

import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.TerminalSession;

import java.util.Collections;
import java.util.List;

/**
 * Adapter for the persistent top session tab bar.
 *
 * <p>The backing list is the exact list owned by {@link com.termux.app.TermuxService}
 * ({@code TermuxService#getTermuxSessions()}). This adapter never copies or owns session state,
 * so it cannot drift from the service or the existing drawer list.</p>
 *
 * <p>All tab views are non-focusable: tapping them must not move focus away from
 * {@link com.termux.view.TerminalView}, whose focus listener drives soft keyboard visibility.</p>
 */
public class TermuxSessionTabAdapter extends RecyclerView.Adapter<TermuxSessionTabAdapter.TabViewHolder> {

    private final TermuxActivity mActivity;
    private List<TermuxSession> mSessions = Collections.emptyList();
    private final int mTextColor;
    private final int mActiveBackgroundColor;
    private final int mInactiveTextColor;
    private int mLastItemCount;

    public TermuxSessionTabAdapter(@NonNull TermuxActivity activity) {
        mActivity = activity;

        TypedValue typedValue = new TypedValue();
        int color = Color.WHITE;
        if (activity.getTheme().resolveAttribute(R.attr.termuxActivityDrawerImageTint, typedValue, true)) {
            if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT && typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT)
                color = typedValue.data;
            else if (typedValue.resourceId != 0)
                color = ContextCompat.getColor(activity, typedValue.resourceId);
        }

        mTextColor = color;
        mActiveBackgroundColor = ColorUtils.setAlphaComponent(color, 38);
        mInactiveTextColor = ColorUtils.setAlphaComponent(color, 160);
    }

    /** Bind to the service-owned session list. Call once after the service connects. */
    public void setSessions(@Nullable List<TermuxSession> sessions) {
        mSessions = sessions == null ? Collections.emptyList() : sessions;
        notifyDataSetChanged();
    }

    /** Wire the adapter to the horizontal RecyclerView and auto-scroll on newly added tabs. */
    public void attachTo(@NonNull final RecyclerView recyclerView) {
        recyclerView.setLayoutManager(new LinearLayoutManager(mActivity, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setAdapter(this);
        recyclerView.setFocusable(false);
        recyclerView.setFocusableInTouchMode(false);
        mLastItemCount = getItemCount();

        registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                mLastItemCount = getItemCount();
                scrollToLastTab(recyclerView);
            }

            @Override
            public void onChanged() {
                // TermuxService notifies with notifyDataSetChanged() when a session is created,
                // so count comparison is needed to detect a genuinely new tab.
                int count = getItemCount();
                if (count > mLastItemCount) scrollToLastTab(recyclerView);
                mLastItemCount = count;
            }
        });
    }

    private void scrollToLastTab(@NonNull RecyclerView recyclerView) {
        recyclerView.post(() -> {
            int last = getItemCount() - 1;
            if (last >= 0) recyclerView.smoothScrollToPosition(last);
        });
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
        holder.itemView.setBackgroundColor(isActive ? mActiveBackgroundColor : Color.TRANSPARENT);
        holder.title.setTextColor(isActive ? mTextColor : mInactiveTextColor);
        holder.title.setText(buildLabel(position, session));
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

    class TabViewHolder extends RecyclerView.ViewHolder {

        final TextView title;
        final ImageButton close;

        TabViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.session_tab_title);
            close = itemView.findViewById(R.id.session_tab_close);

            // Belt-and-braces: layout already declares these, enforce in code too.
            itemView.setFocusable(false);
            itemView.setFocusableInTouchMode(false);
            title.setFocusable(false);
            title.setFocusableInTouchMode(false);
            close.setFocusable(false);
            close.setFocusableInTouchMode(false);

            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                TerminalSession session = getTerminalSessionAt(position);
                if (session == null) return;
                mActivity.getTermuxTerminalSessionClient().setCurrentSession(session);
                mActivity.restoreTerminalFocus();
            });

            close.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                TerminalSession session = getTerminalSessionAt(position);
                if (session == null) return;
                mActivity.getTermuxTerminalSessionClient().closeSession(session);
                mActivity.restoreTerminalFocus();
            });
        }
    }
}
