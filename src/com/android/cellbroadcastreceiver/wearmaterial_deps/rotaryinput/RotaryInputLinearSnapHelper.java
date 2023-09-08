package com.google.android.clockwork.common.wearable.wearmaterial.rotaryinput;

import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;
import android.view.MotionEvent;
import android.view.View;
import androidx.core.view.InputDeviceCompat;

/**
 * This class enables list item snapping with for a RecyclerView for both touch and rotary RSB
 * inputs. This is built on top of LinearSnapHelper, which handles snap for touch inputs but does
 * not handle snap for rotary inputs.<br>
 * To use this class with RecyclerView:<br>
 * 1. Create an instance with RecyclerView: RotaryInputLinearSnapHelper(RecyclerView recyclerView)
 * <br>
 * 2. Call this class's onGenerticMotionEvent from RecyclerView's onGenericMotionEvent to forward
 * RecyclerView's incoming events.<br>
 */
public class RotaryInputLinearSnapHelper {
  private static final String TAG = "RotaryInputSnapHelper";
  /**
   * This is the time in ms since last rotary scroll event was seen. After this delay, we treat
   * current batch of rotary events as ended, and trigger the snap scrolling.
   */
  private static final long ROTARY_INPUT_SCROLL_SNAP_DELAY_MS = 80;

  private final RecyclerView recyclerView;
  private final LinearSnapHelper linearSnapHelper;

  @SuppressWarnings("nullness:methodref.receiver.bound")
  private final Runnable rotaryInputScrollEndRunner = this::snapToTargetExistingView;

  private boolean snappingEnabled;

  private boolean isLastInputRotary = false;

  public RotaryInputLinearSnapHelper(RecyclerView recyclerView) {
    this.recyclerView = recyclerView;
    linearSnapHelper = new LinearSnapHelper();
  }

  /** Whether snapping is enabled for this list. */
  public boolean isSnappingEnabled() {
    return snappingEnabled;
  }

  /** Set whether snapping should be enabled for this list. The snapping is disabled by default. */
  public void setSnappingEnabled(boolean enabled) {
    snappingEnabled = enabled;
    linearSnapHelper.attachToRecyclerView(snappingEnabled ? recyclerView : null);
  }

  /**
   * Handle generic motion events. This must be called from RecyclerView's onGenericMotionEvent to
   * enable snap functionality.
   */
  public void onGenericMotionEvent(MotionEvent event) {
    isLastInputRotary = isRotaryInput(event);
  }

  /**
   * This handles ensuring that the final update of a given scroll handles the snap functionality.
   * This is called everytime the RecyclerView still has velocity to scroll.
   */
  public void onScrolled() {
    if (isLastInputRotary) {
      onRotaryInputScrollEvent();
    }
  }

  /** This handles canceling the snap evaluation when a touch event occurs. */
  public void onTouchEvent() {
    recyclerView.removeCallbacks(rotaryInputScrollEndRunner);
    isLastInputRotary = false;
  }

  private boolean isRotaryInput(MotionEvent event) {
    if (!snappingEnabled) {
      return false;
    }

    if (event.getAction() != MotionEvent.ACTION_SCROLL) {
      return false;
    }

    return (event.getSource() & InputDeviceCompat.SOURCE_ROTARY_ENCODER) != 0;
  }

  private void onRotaryInputScrollEvent() {
    recyclerView.removeCallbacks(rotaryInputScrollEndRunner);
    recyclerView.postDelayed(rotaryInputScrollEndRunner, ROTARY_INPUT_SCROLL_SNAP_DELAY_MS);
  }

  /**
   * This is a utility method duplicated and slightly modified from {@link
   * SnapHelper.snapToTargetExistingView} used by {@link LinearSnapHelper}, which is called for
   * touch scroll snapping. We're essentially calling here the same snap functionality for rotary
   * scroll as touch scroll.
   */
  private void snapToTargetExistingView() {
    if (!snappingEnabled || linearSnapHelper == null) {
      return;
    }
    RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
    if (layoutManager == null) {
      return;
    }
    View snapView = linearSnapHelper.findSnapView(layoutManager);
    if (snapView == null) {
      return;
    }
    int[] snapDistance = linearSnapHelper.calculateDistanceToFinalSnap(layoutManager, snapView);
    if (snapDistance[0] != 0 || snapDistance[1] != 0) {
      recyclerView.smoothScrollBy(snapDistance[0], snapDistance[1]);
    }

    // Reset input evaluation
    isLastInputRotary = false;
  }
}
