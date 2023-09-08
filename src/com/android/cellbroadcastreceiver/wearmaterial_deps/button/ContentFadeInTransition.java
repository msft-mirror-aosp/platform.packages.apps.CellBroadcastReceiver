package com.google.android.clockwork.common.wearable.wearmaterial.button;

import static com.google.android.clockwork.common.wearable.wearmaterial.button.ContentChangeTransition.captureValues;
import static com.google.android.clockwork.common.wearable.wearmaterial.button.ContentChangeTransition.getTransitionPropertyNames;
import static com.google.android.clockwork.common.wearable.wearmaterial.button.ContentChangeTransition.shouldTransitionRun;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.transition.Fade;
import android.transition.TransitionValues;
import android.view.View;
import android.view.ViewGroup;
import org.checkerframework.checker.nullness.qual.Nullable;

/** A {@link Fade}-In transition that runs as part of a {@link ContentChangeTransition}. */
final class ContentFadeInTransition extends Fade {

  private static final float VIEW_TRANSLUCENT_ALPHA = 0.1f;
  private static final float VIEW_OPAQUE_ALPHA = 1;

  ContentFadeInTransition() {
    super(Fade.MODE_IN);
  }

  @Override
  public boolean isTransitionRequired(
      @Nullable TransitionValues startValues, @Nullable TransitionValues newValues) {
    return startValues != null && newValues != null && shouldTransitionRun(startValues, newValues);
  }

  // createAnimator can and must be able to return null.
  // Incompatible parameter type for startValues.
  // Incompatible parameter type for endValues.
  @SuppressWarnings({"override.return.invalid", "nullness:override.param.invalid"})
  @Override
  public @Nullable Animator createAnimator(
      ViewGroup sceneRoot, TransitionValues startValues, TransitionValues endValues) {
    View view = endValues.view;
    Animator animator = onAppear(sceneRoot, view, startValues, endValues);
    if (animator == null) {
      return null;
    }

    // The 'onAppear' returns an ObjectAnimator that starts a 0 and ends with 1.
    // However, we need to start with 0.1 (and end with 1). Make that adjustment:
    adjustAnimationValues(animator);

    addListener(new TransientStateTransitionListener(view));
    return animator;
  }

  private void adjustAnimationValues(Animator animator) {
    if (animator instanceof ObjectAnimator) {
      ((ObjectAnimator) animator).setFloatValues(VIEW_TRANSLUCENT_ALPHA, VIEW_OPAQUE_ALPHA);
    }
  }

  @Override
  public void captureStartValues(TransitionValues transitionValues) {
    super.captureStartValues(transitionValues);
    captureValues(transitionValues);
  }

  @Override
  public void captureEndValues(TransitionValues transitionValues) {
    super.captureEndValues(transitionValues);
    captureValues(transitionValues);
  }

  @Override
  public String[] getTransitionProperties() {
    return getTransitionPropertyNames();
  }
}
