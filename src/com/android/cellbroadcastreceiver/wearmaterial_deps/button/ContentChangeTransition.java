package com.google.android.clockwork.common.wearable.wearmaterial.button;

import android.content.Context;
import android.transition.Transition;
import android.transition.TransitionSet;
import android.transition.TransitionValues;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Interpolator;
import android.widget.TextView;
import androidx.annotation.VisibleForTesting;
import androidx.interpolator.view.animation.FastOutLinearInInterpolator;
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator;
import com.google.android.clockwork.common.wearable.wearmaterial.animations.Durations;
import com.google.android.clockwork.common.wearable.wearmaterial.util.TextUtils;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

/** A {@link TransitionSet} that runs when a {@link View}'s content has changed. */
public final class ContentChangeTransition extends TransitionSet {

  private static final Interpolator INTERPOLATOR_STANDARD_IN = new LinearOutSlowInInterpolator();
  private static final Interpolator INTERPOLATOR_STANDARD_OUT = new FastOutLinearInInterpolator();
  private static final String PROP_NAME_NAMESPACE = ContentChangeTransition.class.getName();
  private static final String PROP_NAME_CONTENT_VERSION = PROP_NAME_NAMESPACE + ":content-version";
  private static final String PROP_NAME_VISIBILITY = PROP_NAME_NAMESPACE + ":visibility";
  private static final String[] PROP_NAMES = {PROP_NAME_VISIBILITY, PROP_NAME_CONTENT_VERSION};

  /**
   * Sets {@code text} as the new text of {@code textView}.
   *
   * <p>This method prevents this {@link Transition} from being run if it can determine that the
   * current text of {@code textView} is the same as the newly provided {@code text}.
   */
  static void setText(TextView textView, CharSequence text) {
    if (TextUtils.contentsMayDiffer(textView.getText(), text)) {
      textView.setText(text);
      updateContentVersion(textView);
    }
  }

  /**
   * Marks {@code view} with an updated content-version.
   *
   * <p>When the 'before'/'start' content-version is the same as the 'after'/'end' content-version,
   * this {@link Transition} will not be run on the {@code view}.
   */
  static void updateContentVersion(View view) {
    view.setTag(R.id.tag_content_version, View.generateViewId());
  }

  /** Captures content-change related property-values into {@code transitionValues}. */
  static void captureValues(TransitionValues transitionValues) {
    View view = transitionValues.view;
    Integer version = getContentVersion(view);

    transitionValues.values.put(
        PROP_NAME_CONTENT_VERSION, version == null ? View.generateViewId() : version);
    transitionValues.values.put(PROP_NAME_VISIBILITY, view.getVisibility() == View.VISIBLE);
  }

  /**
   * Returns the array of the names of the properties that are important to determine if a
   * content-change transition is needed or not.
   */
  static String[] getTransitionPropertyNames() {
    return PROP_NAMES;
  }

  /**
   * Returns true if the {@code ContentChangeTransition} should run, based on the {@code
   * startValues} and {@code endValues}.
   *
   * <p>The names of all the properties examined in this method must be returned by {@link
   * #getTransitionProperties()}.
   */
  static boolean shouldTransitionRun(TransitionValues startValues, TransitionValues endValues) {
    Map<String, ?> startProps = startValues.values;
    Map<String, ?> endProps = endValues.values;

    if (startValues.view != endValues.view) {
      return false;
    }

    boolean isStartContentVisible = isViewVisible(startProps);
    boolean isEndContentVisible = isViewVisible(endProps);
    if (!isStartContentVisible || !isEndContentVisible) {
      return false;
    }

    int startContentVersion = getContentVersion(startProps);
    int endContentVersion = getContentVersion(endProps);
    return startContentVersion != endContentVersion;
  }

  /** Returns true only if the {@code properties} determine the {@code view} is visible. */
  static boolean isViewVisible(Map<String, ?> properties) {
    Boolean isVisible = (Boolean) properties.get(PROP_NAME_VISIBILITY);
    return isVisible != null && isVisible;
  }

  /** Returns the content-version in the {@code properties} of a {@code view}. */
  static int getContentVersion(Map<String, ?> properties) {
    Integer contentVersion = (Integer) properties.get(PROP_NAME_CONTENT_VERSION);
    return contentVersion == null ? View.generateViewId() : contentVersion;
  }

  /** Returns the content-version of the {@code view}. */
  @VisibleForTesting
  static @Nullable Integer getContentVersion(View view) {
    return (Integer) view.getTag(R.id.tag_content_version);
  }

  @SuppressWarnings({"method.invocation", "methodref.receiver.bound"})
  public ContentChangeTransition(Context context, AttributeSet attrs) {
    super(context, attrs);
    addFadeOutAndFadeInTransitions();
  }

  private void addFadeOutAndFadeInTransitions() {
    setOrdering(TransitionSet.ORDERING_SEQUENTIAL);

    ContentFadeOutTransition fadeOut = new ContentFadeOutTransition();
    fadeOut.setDuration(Durations.RAPID);
    fadeOut.setInterpolator(INTERPOLATOR_STANDARD_OUT);
    addTransition(fadeOut);

    ContentFadeInTransition fadeIn = new ContentFadeInTransition();
    fadeIn.setDuration(Durations.STANDARD);
    fadeIn.setInterpolator(INTERPOLATOR_STANDARD_IN);
    addTransition(fadeIn);
  }
}
