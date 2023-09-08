package com.google.android.clockwork.common.wearable.wearmaterial.util;

/**
 * The interface that any class should implement when its instances are targets for a {@link
 * RepeatableAnimator} and need initialization just before the animation (re-)starts.
 */
public interface RepeatableAnimatorTarget {

  /** Initializes this target just before it is (re-)started by a {@link RepeatableAnimator}. */
  void initialize();
}
