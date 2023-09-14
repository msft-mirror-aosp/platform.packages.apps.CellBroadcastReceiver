package com.google.android.clockwork.common.wearable.wearmaterial.preference;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.CallSuper;
import androidx.wear.widget.SwipeDismissFrameLayout;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * {@link WearPreferenceFragment} that supports swipe-to-dismiss.
 *
 * <p>Similar to SwipeDismissPreferenceFragment but using a WearPreferenceFragment instead of
 * PreferenceFragmentCompat
 */
public class SwipeDismissWearPreferenceFragment extends WearPreferenceFragment {
  private SwipeDismissFrameLayout swipeLayout;

  @SuppressWarnings("nullness:method.invocation")
  private final SwipeDismissFrameLayout.Callback swipeCallback =
      new SwipeDismissFrameLayout.Callback() {
        @Override
        public void onSwipeStarted(SwipeDismissFrameLayout layout) {
          SwipeDismissWearPreferenceFragment.this.onSwipeStart();
        }

        @Override
        public void onSwipeCanceled(SwipeDismissFrameLayout layout) {
          SwipeDismissWearPreferenceFragment.this.onSwipeCancelled();
        }

        @Override
        public void onDismissed(SwipeDismissFrameLayout layout) {
          SwipeDismissWearPreferenceFragment.this.onDismiss();
        }
      };

  // No Additional work needed. Override is enforced by abstract definition.
  // Incompatible parameter type for savedInstanceState.
  // Incompatible parameter type for rootKey.
  @SuppressWarnings("nullness:override.param")
  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {}

  @SuppressWarnings("nullness:argument")
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    swipeLayout = new SwipeDismissFrameLayout(getActivity());
    swipeLayout.addCallback(swipeCallback);
    View contents = super.onCreateView(inflater, swipeLayout, savedInstanceState);
    swipeLayout.setBackgroundColor(getBackgroundColor());
    swipeLayout.addView(contents);
    setDivider(null);
    return swipeLayout;
  }

  @Override
  public void onResume() {
    super.onResume();
    getListView().requestFocus();
  }

  /** Called when the fragment is dismissed with a swipe. */
  @CallSuper
  public void onDismiss() {
    getParentFragmentManager().popBackStackImmediate();
  }

  /** Called when a swipe-to-dismiss gesture is started. */
  public void onSwipeStart() {}

  /** Called when a swipe-to-dismiss gesture is cancelled. */
  public void onSwipeCancelled() {}

  /**
   * Sets whether or not the preferences list can be focused. If {@code focusable} is false, any
   * existing focus will be cleared.
   */
  public void setFocusable(boolean focusable) {
    if (focusable) {
      swipeLayout.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
      swipeLayout.setFocusable(true);
      getListView().requestFocus();
    } else {
      // Prevent any child views from receiving focus.
      swipeLayout.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
      swipeLayout.setFocusable(false);
      swipeLayout.clearFocus();
    }
  }

  // dereference of possibly-null reference getActivity()
  @SuppressWarnings("nullness:dereference.of.nullable")
  private int getBackgroundColor() {
    TypedValue value = new TypedValue();
    getActivity().getTheme().resolveAttribute(android.R.attr.colorBackground, value, true);
    return value.data;
  }
}
