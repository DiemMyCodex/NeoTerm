package com.termux.app.terminal;

import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Tactile add/remove animation for the floating session tab bar.
 *
 * <p>New tabs spring in from 0.82f and fade in; removed tabs shrink and fade out.
 * We own the view animators and dispatch completion ourselves, so no new library
 * is needed beyond the RecyclerView already used by the app.</p>
 */
public class SessionTabItemAnimator extends DefaultItemAnimator {

    private static final long ADD_DURATION_MS = 220L;
    private static final long REMOVE_DURATION_MS = 160L;

    @Override
    public boolean animateAdd(@NonNull RecyclerView.ViewHolder holder) {
        final View view = holder.itemView;
        view.animate().cancel();
        view.setAlpha(0f);
        view.setScaleX(0.82f);
        view.setScaleY(0.82f);
        view.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setInterpolator(new OvershootInterpolator(1.15f))
            .setDuration(ADD_DURATION_MS)
            .withEndAction(() -> {
                resetViewTransform(view);
                dispatchAddFinished(holder);
            })
            .start();
        return true;
    }

    @Override
    public boolean animateRemove(@NonNull RecyclerView.ViewHolder holder) {
        final View view = holder.itemView;
        view.animate().cancel();
        view.animate()
            .alpha(0f)
            .scaleX(0.82f)
            .scaleY(0.82f)
            .setInterpolator(new DecelerateInterpolator())
            .setDuration(REMOVE_DURATION_MS)
            .withEndAction(() -> {
                resetViewTransform(view);
                dispatchRemoveFinished(holder);
            })
            .start();
        return true;
    }

    private static void resetViewTransform(@NonNull View view) {
        // Views are recycled, never leave a fade/scale behind.
        view.setAlpha(1f);
        view.setScaleX(1f);
        view.setScaleY(1f);
    }
}
