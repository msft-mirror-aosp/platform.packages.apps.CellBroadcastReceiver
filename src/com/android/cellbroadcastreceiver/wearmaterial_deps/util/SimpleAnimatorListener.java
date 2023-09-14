package com.google.android.clockwork.common.wearable.wearmaterial.util;

import android.animation.Animator;
import android.annotation.TargetApi;
import android.os.Build;

/**
 * Convenience class for listening for Animator events that implements the AnimatorListener
 * interface and allows extending only methods that are necessary.
 */
@TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
public class SimpleAnimatorListener implements Animator.AnimatorListener {

  private boolean wasCancelled;

  @Override
  public void onAnimationCancel(Animator animator) {
    wasCancelled = true;
  }

  @Override
  public void onAnimationEnd(Animator animator) {
    if (!wasCancelled) {
      onAnimationComplete(animator);
    }
  }

  @Override
  public void onAnimationRepeat(Animator animator) {}

  @Override
  public void onAnimationStart(Animator animator) {
    wasCancelled = false;
  }

  /** Called when the animation finishes. Not called if the animation was canceled. */
  public void onAnimationComplete(Animator animator) {}

  /**
   * Provides information if the animation was cancelled.
   *
   * @return True if animation was cancelled.
   */
  public boolean wasCanceled() {
    return wasCancelled;
  }
}
