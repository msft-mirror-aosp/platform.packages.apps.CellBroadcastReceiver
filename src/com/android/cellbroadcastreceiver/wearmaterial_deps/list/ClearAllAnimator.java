package com.google.android.clockwork.common.wearable.wearmaterial.list;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import androidx.annotation.VisibleForTesting;
import com.google.android.clockwork.common.wearable.wearmaterial.animations.Durations;
import java.util.ArrayList;

/** An animator that plays a stylized removal of all of the elements from a RecyclerView */
public class ClearAllAnimator {

  @VisibleForTesting static final long CLEAR_ANIM_OFFSET_TIME_MS = 35;
  private static final Interpolator CASCADING_ALPHA_INTERPOLATOR =
      new PathInterpolator(0f, 0f, 0.2f, 1f);
  private static final Interpolator CASCADING_TRANSLATION_INTERPOLATOR =
      new PathInterpolator(0.4f, 0f, 0.2f, 1f);
  private static final float ANIMATION_TRANSLATION_X = 100f;
  private final RecyclerView recyclerView;
  private final AnimatorSet clearAllSet;
  private final ElementAnimator elementAnimator;

  /**
   * @param recyclerView the Recyclerview containing list elements to clear
   * @param elementAnimator the Interface, for applying Clear All animation to view, which has a
   *     unique animation separate from generic list elements.
   */
  @SuppressWarnings("nullness:method.invocation")
  public ClearAllAnimator(RecyclerView recyclerView, ElementAnimator elementAnimator) {
    super();
    this.recyclerView = recyclerView;
    this.clearAllSet = new AnimatorSet();
    this.elementAnimator = elementAnimator;
    buildAnimation();
  }

  public void addListener(AnimatorListener listener) {
    if (listener != null) {
      clearAllSet.addListener(listener);
    }
  }

  /** Play an animation to clear all the list items off the screen */
  public void start() {
    clearAllSet.start();
  }

  /** Returns {@code true} if the clear all animation has started. */
  public boolean isStarted() {
    return clearAllSet.isStarted();
  }

  /** Cleans up the clear all animator and removes all listener. */
  public void cleanUp() {
    clearAllSet.cancel();
    clearAllSet.removeAllListeners();
  }

  /** Construct the Animations that make up the clear all */
  private void buildAnimation() {
    ArrayList<Animator> animations = new ArrayList<>();
    for (int i = 0; i < recyclerView.getChildCount(); i++) {
      if (elementAnimator.isAnimationEnabledForPosition(i)) {
        // The buttons only animates in opacity
        animations.add(getClearAllAnimator(recyclerView.getChildAt(i)));
      } else {
        // every other element animates translation and opacity in a cascading delay
        animations.add(getCascadingTranslateAnimator(recyclerView.getChildAt(i), i));
        animations.add(getCascadingAlphaAnimator(recyclerView.getChildAt(i), i));
      }
    }
    // suppress layout while the animation is running to prevent change in view items
    clearAllSet.addListener(
        new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animation) {
            super.onAnimationEnd(animation);
            recyclerView.suppressLayout(false);
          }

          @Override
          public void onAnimationStart(Animator animation) {
            super.onAnimationStart(animation);
            recyclerView.suppressLayout(true);
          }
        });
    clearAllSet.playTogether(animations);
  }

  @VisibleForTesting
  ObjectAnimator getClearAllAnimator(View view) {
    ObjectAnimator alphaAnimation = ObjectAnimator.ofFloat(view, View.ALPHA, 0f);
    alphaAnimation.setDuration(Durations.STANDARD);
    alphaAnimation.setStartDelay(Durations.RAPID);
    alphaAnimation.setInterpolator(CASCADING_ALPHA_INTERPOLATOR);
    return alphaAnimation;
  }

  @VisibleForTesting
  ObjectAnimator getCascadingAlphaAnimator(View view, int cascadePosition) {
    ObjectAnimator alphaAnimation = ObjectAnimator.ofFloat(view, View.ALPHA, 0f);
    alphaAnimation.setDuration(Durations.QUICK);
    alphaAnimation.setStartDelay(CLEAR_ANIM_OFFSET_TIME_MS * cascadePosition);
    alphaAnimation.setInterpolator(CASCADING_ALPHA_INTERPOLATOR);
    return alphaAnimation;
  }

  @VisibleForTesting
  ObjectAnimator getCascadingTranslateAnimator(View view, int cascadePosition) {
    ObjectAnimator translateAnimation =
        ObjectAnimator.ofFloat(view, View.TRANSLATION_X, ANIMATION_TRANSLATION_X);
    translateAnimation.setDuration(Durations.CASUAL);
    translateAnimation.setStartDelay(CLEAR_ANIM_OFFSET_TIME_MS * cascadePosition);
    translateAnimation.setInterpolator(CASCADING_TRANSLATION_INTERPOLATOR);
    return translateAnimation;
  }

  /** Helper interface for applying Clear All animation, as per recyclerview element position. */
  public interface ElementAnimator {

    boolean isAnimationEnabledForPosition(int position);
  }
}
