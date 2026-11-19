package com.termux.app.terminal;

import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

/**
 * Reusable press-to-spring touch feedback for pill chrome (tab pills, + button).
 *
 * <p>Returns {@code false} for every event so the view's existing click and
 * long-click listeners still receive the gesture untouched.</p>
 */
public class TactilePressTouchListener implements View.OnTouchListener {

    private static final long PRESS_DURATION_MS = 80L;
    private static final long RELEASE_DURATION_MS = 180L;
    private static final float PRESSED_SCALE = 0.93f;
    private static final OvershootInterpolator SPRING = new OvershootInterpolator(1.4f);

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                view.animate().cancel();
                view.animate()
                    .scaleX(PRESSED_SCALE)
                    .scaleY(PRESSED_SCALE)
                    .setDuration(PRESS_DURATION_MS)
                    .start();
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                view.animate().cancel();
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setInterpolator(SPRING)
                    .setDuration(RELEASE_DURATION_MS)
                    .start();
                break;

            default:
                break;
        }
        // Never consume: click/long-click handlers must still fire.
        return false;
    }
}
