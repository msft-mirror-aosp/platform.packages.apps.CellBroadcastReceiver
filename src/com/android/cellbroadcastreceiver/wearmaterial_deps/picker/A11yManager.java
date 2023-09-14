package com.google.android.clockwork.common.wearable.wearmaterial.picker;

import static android.content.Context.ACCESSIBILITY_SERVICE;

import android.content.Context;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityManager.AccessibilityStateChangeListener;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

/** Class that provides common accessibility related functions for this picker package. */
final class A11yManager {

  /** Separator for sentences as defined by The DEFAULT_BREAKING_SEPARATOR in the Talkback code. */
  static final String DEFAULT_BREAKING_SEPARATOR = ", ";

  private final @MonotonicNonNull AccessibilityManager a11yManager;

  A11yManager(Context context) {
    a11yManager = (AccessibilityManager) context.getSystemService(ACCESSIBILITY_SERVICE);
  }

  /** Returns true if the user has enabled Talkback. */
  boolean isA11yEnabled() {
    return a11yManager != null
        && a11yManager.isEnabled()
        && a11yManager.isTouchExplorationEnabled();
  }

  /** Adds the provided {@link AccessibilityStateChangeListener} to the a11yManager */
  void addStateChangeListener(AccessibilityStateChangeListener listener) {
    if (a11yManager != null) {
      a11yManager.addAccessibilityStateChangeListener(listener);
    }
  }

  /** Removes the provided {@link AccessibilityStateChangeListener} from the a11yManager */
  void removeStateChangeListener(AccessibilityStateChangeListener listener) {
    if (a11yManager != null) {
      a11yManager.removeAccessibilityStateChangeListener(listener);
    }
  }
}
