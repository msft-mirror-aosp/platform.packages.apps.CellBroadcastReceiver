package com.google.android.clockwork.common.wearable.wearmaterial.button;

import static com.google.android.clockwork.common.wearable.wearmaterial.button.ContentChangeTransition.captureValues;
import static com.google.android.clockwork.common.wearable.wearmaterial.button.ContentChangeTransition.getTransitionPropertyNames;
import static com.google.android.clockwork.common.wearable.wearmaterial.button.ContentChangeTransition.shouldTransitionRun;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.transition.Transition;
import android.transition.TransitionValues;
import android.util.IntProperty;
import android.util.Property;
import android.view.ViewGroup;
import androidx.annotation.VisibleForTesting;
import org.checkerframework.checker.nullness.qual.Nullable;

/** A fade-out like {@link Transition} that runs as part of a {@link ContentChangeTransition}. */
final class ContentFadeOutTransition extends Transition {

  private static final String PROP_NAME_NAMESPACE = ContentFadeOutTransition.class.getName();
  private static final String PROP_NAME_SNAPSHOT = PROP_NAME_NAMESPACE + ":snapshot";
  private static final int DRAWABLE_TRANSLUCENT_ALPHA = 25;
  private static final String PROPERTY_NAME_ALPHA = "alpha";
  private static final Property<WearSnapshot, Integer> ALPHA_PROPERTY = createAlphaProperty();

  @Override
  public boolean isTransitionRequired(
      @Nullable TransitionValues startValues, @Nullable TransitionValues newValues) {
    return startValues != null && newValues != null && shouldTransitionRun(startValues, newValues);
  }

  @Override
  public @Nullable Animator createAnimator(
      ViewGroup sceneRoot, TransitionValues startValues, TransitionValues endValues) {
    WearSnapshot snapShot = (WearSnapshot) startValues.values.remove(PROP_NAME_SNAPSHOT);
    if (snapShot == null) {
      return null;
    }

    Animator fadeOut = ObjectAnimator.ofInt(snapShot, ALPHA_PROPERTY, DRAWABLE_TRANSLUCENT_ALPHA);
    fadeOut.addListener(new OverlayAnimatorListener(snapShot));
    fadeOut.addListener(new HardwareFadeAnimatorListener(startValues.view));

    addListener(new TransientStateTransitionListener(startValues.view));
    return fadeOut;
  }

  @Override
  public void captureStartValues(TransitionValues transitionValues) {
    captureValues(transitionValues);
    captureSnapshot(transitionValues);
  }

  @Override
  public void captureEndValues(TransitionValues transitionValues) {
    captureValues(transitionValues);
  }

  @Override
  public String[] getTransitionProperties() {
    return getTransitionPropertyNames();
  }

  /**
   * Creates a {@link WearSnapshot} from the view of {@code transitionValues} and captures it into
   * {@code transitionValues}.
   */
  private static void captureSnapshot(TransitionValues transitionValues) {
    WearSnapshot snapshot = WearSnapshot.create(transitionValues.view);
    if (snapshot != null) {
      transitionValues.values.put(PROP_NAME_SNAPSHOT, snapshot);
    }
  }

  @VisibleForTesting
  static Property<WearSnapshot, Integer> createAlphaProperty() {
    if (VERSION.SDK_INT >= VERSION_CODES.N) {
      return new AlphaPropertyV24();
    } else {
      return new AlphaProperty();
    }
  }

  /** Representation of the {@link WearSnapshot}'s {@code alpha} property. */
  @VisibleForTesting
  static final class AlphaProperty extends Property<WearSnapshot, Integer> {

    AlphaProperty() {
      super(Integer.class, PROPERTY_NAME_ALPHA);
    }

    @Override
    public void set(WearSnapshot object, Integer value) {
      object.setAlpha(value);
    }

    @Override
    public Integer get(WearSnapshot object) {
      return object.getAlpha();
    }
  }

  /** Representation of the {@link WearSnapshot}'s {@code alpha} property. */
  @VisibleForTesting
  @TargetApi(value = VERSION_CODES.N)
  static final class AlphaPropertyV24 extends IntProperty<WearSnapshot> {

    AlphaPropertyV24() {
      super(PROPERTY_NAME_ALPHA);
    }

    @Override
    public void setValue(WearSnapshot object, int value) {
      object.setAlpha(value);
    }

    @Override
    public Integer get(WearSnapshot object) {
      return object.getAlpha();
    }
  }

  /** Listener that handles the adding and removal of the {@code snapshot} to the overlay. */
  private static final class OverlayAnimatorListener extends AnimatorListenerAdapter {

    private final WearSnapshot snapShot;

    OverlayAnimatorListener(WearSnapshot snapShot) {
      this.snapShot = snapShot;
    }

    @Override
    public void onAnimationStart(Animator animation) {
      snapShot.prepareToDraw();
      snapShot.addToOverlay();
    }

    @Override
    public void onAnimationEnd(Animator animation) {
      snapShot.removeFromOverlay();
    }

    @Override
    public void onAnimationCancel(Animator animation) {
      snapShot.removeFromOverlay();
    }
  }
}
