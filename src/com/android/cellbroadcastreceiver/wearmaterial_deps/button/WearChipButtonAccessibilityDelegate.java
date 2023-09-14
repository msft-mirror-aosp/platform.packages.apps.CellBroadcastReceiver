package com.google.android.clockwork.common.wearable.wearmaterial.button;

import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Accessibility Delegate that handles accessibility for {@link WearChipButton}s. */
abstract class WearChipButtonAccessibilityDelegate extends AccessibilityDelegateCompat {

  public static final String TAG = "WearChipButtonAccessibi";

  /** Describes the accessibility state for a {@link WearChipButton}. */
  static final class State {
    /**
     * Separator for sentences as defined by The DEFAULT_BREAKING_SEPARATOR in
     * google3/java/com/google/android/accessibility/utils/StringBuilderUtils.java
     */
    private static final String DEFAULT_BREAKING_SEPARATOR = ", ";

    /** Class name to be assigned to the node or event. */
    final CharSequence className;

    /** The primary-text to be used for the node or event. */
    final CharSequence primaryText;

    /** The optional secondary-text to be used for the node or event. */
    final CharSequence secondaryText;

    /** Determines if the button can be checkable or not. */
    final boolean isCheckable;

    State(
        @Nullable CharSequence className,
        @Nullable CharSequence primaryText,
        @Nullable CharSequence secondaryText,
        boolean isCheckable) {
      this.className = className == null ? Button.class.getName() : className;
      this.primaryText = primaryText == null ? "" : primaryText;
      this.secondaryText = secondaryText == null ? "" : secondaryText;
      this.isCheckable = isCheckable;
    }

    @Nullable
    CharSequence getAccessibilityText() {
      if (primaryText.length() == 0) {
        return null;
      }
      return secondaryText.length() == 0
          ? primaryText
          : primaryText + DEFAULT_BREAKING_SEPARATOR + secondaryText;
    }

    @Override
    public String toString() {
      return "State{"
          + "className="
          + className
          + ", primaryText="
          + primaryText
          + ", secondaryText="
          + secondaryText
          + ", isCheckable="
          + isCheckable
          + '}';
    }
  }

  private final WearChipButton host;

  WearChipButtonAccessibilityDelegate(WearChipButton host) {
    this.host = host;
  }

  @Override
  public void onInitializeAccessibilityEvent(View view, AccessibilityEvent event) {
    super.onInitializeAccessibilityEvent(view, event);
    populateEvent(event);
  }

  @Override
  public void onPopulateAccessibilityEvent(View view, AccessibilityEvent event) {
    super.onPopulateAccessibilityEvent(view, event);
    populateEvent(event);
  }

  private void populateEvent(AccessibilityEvent event) {
    State state = onUpdateAccessibilityState();

    event.setClassName(state.className);

    if (state.primaryText.length() > 0) {
      event.getText().add(state.primaryText);
    }
    if (state.secondaryText.length() > 0) {
      event.getText().add(state.secondaryText);
    }
    event.setChecked(state.isCheckable && host.isChecked());
  }

  @SuppressWarnings("nullness:argument") // nodeInfo.setText does accept nullable values.
  @Override
  public void onInitializeAccessibilityNodeInfo(View view, AccessibilityNodeInfoCompat nodeInfo) {
    super.onInitializeAccessibilityNodeInfo(view, nodeInfo);

    State state = onUpdateAccessibilityState();

    nodeInfo.setClassName(state.className);

    nodeInfo.setText(state.getAccessibilityText());
    nodeInfo.setCheckable(state.isCheckable && host.isCheckable());
    nodeInfo.setChecked(state.isCheckable && host.isChecked());
  }

  /**
   * Returns a new {@link State} reflecting the current state of the {@link WearChipButton} or its
   * inner selection-control-container.
   */
  abstract State onUpdateAccessibilityState();
}
