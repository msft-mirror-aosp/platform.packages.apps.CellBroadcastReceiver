package com.google.android.clockwork.common.wearable.wearmaterial.button;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;

/**
 * An {@link android.animation.Animator.AnimatorListener} that optimizes fading when the faded
 * {@code view} overlaps with another view.
 */
final class HardwareFadeAnimatorListener extends AnimatorListenerAdapter {

  private final View view;
  private boolean hasLayerTypeChanged = false;

  HardwareFadeAnimatorListener(View view) {
    this.view = view;
  }

  @Override
  public void onAnimationStart(Animator animator) {
    if (view.hasOverlappingRendering() && view.getLayerType() == View.LAYER_TYPE_NONE) {
      hasLayerTypeChanged = true;
      view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }
  }

  @Override
  public void onAnimationEnd(Animator animator) {
    if (hasLayerTypeChanged) {
      hasLayerTypeChanged = false;
      view.setLayerType(View.LAYER_TYPE_NONE, null);
    }
  }
}
