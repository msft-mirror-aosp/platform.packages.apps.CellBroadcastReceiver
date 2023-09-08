package com.google.android.clockwork.common.wearable.wearmaterial.button;

import android.transition.Transition;
import android.transition.Transition.TransitionListener;
import android.view.View;

/**
 * Listener that changes an animated {@code view}'s transient state.
 *
 * <p>When the transition starts, the {@code view}'s transient state is set.
 *
 * <p>When the transition ends, the {@code view}'s transient state is cleared.
 *
 * <p>Setting the transient state prevents ListViews, RecyclerViews (and possibly other host-views)
 * from recycling or re-binding the view (or the (grand-)parents of the view).
 */
final class TransientStateTransitionListener implements TransitionListener {

  private final View view;

  public TransientStateTransitionListener(View view) {
    this.view = view;
  }

  @Override
  public void onTransitionStart(Transition transition) {
    view.setHasTransientState(true);
  }

  @Override
  public void onTransitionEnd(Transition transition) {
    transition.removeListener(this);
    view.setHasTransientState(false);
  }

  @Override
  public void onTransitionCancel(Transition transition) {}

  @Override
  public void onTransitionPause(Transition transition) {}

  @Override
  public void onTransitionResume(Transition transition) {}
}
