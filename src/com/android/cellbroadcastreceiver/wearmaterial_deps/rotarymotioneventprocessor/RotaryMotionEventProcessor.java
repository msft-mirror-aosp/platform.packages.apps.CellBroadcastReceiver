package com.google.android.clockwork.common.wearable.wearmaterial.rotarymotioneventprocessor;

import android.content.Context;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.view.MotionEvent;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.InputDeviceCompat;
import com.google.android.clockwork.common.wearable.wearmaterial.rotaryinput.ScrollVelocityTracker;
import com.google.android.clockwork.common.wearable.wearmaterial.rotaryinput.ScrollVelocityTrackerFallback;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

/**
 * This algorithm ensures that it strictly guards from velocity going in the opposite direction of
 * the current fling.
 */
public class RotaryMotionEventProcessor {
  /**
   * A platform feature key for rotary encoder fling, used in Wear R/T as a global guard of rotary
   * encoder fling across Wear surfaces.
   *
   * <p>NOTE: This is not a standard Android feature and will only be used for Wear R/T.
   */
  private static final String PLATFORM_ROTARY_ENCODER_FLING_FEATURE =
      "android.software.rotary_encoder_fling";

  private static final int VELOCITY_IN_PIXELS_PER_SECOND = 1000;

  private final RotaryVelocityView rotaryVelocityView;
  private final ScrollVelocityTracker scrollVelocityTracker;
  private final boolean rotaryScrollFlingAvailable;

  private boolean rotaryScrollFlingEnabled;

  private float lastScrollVelocity;
  private float lastFlingVelocity;

  public RotaryMotionEventProcessor(Context context, RotaryVelocityView rotaryVelocityView) {
    this(context, rotaryVelocityView, new ScrollVelocityTrackerFallback());
  }

  @VisibleForTesting
  RotaryMotionEventProcessor(
      Context context,
      RotaryVelocityView rotaryVelocityView,
      ScrollVelocityTracker scrollVelocityTracker) {
    this.rotaryVelocityView = rotaryVelocityView;
    this.scrollVelocityTracker = scrollVelocityTracker;
    rotaryScrollFlingAvailable =
        (VERSION.SDK_INT == VERSION_CODES.R || VERSION.SDK_INT == VERSION_CODES.TIRAMISU)
            && context.getPackageManager().hasSystemFeature(PLATFORM_ROTARY_ENCODER_FLING_FEATURE);
  }

  /**
   * Returns {@code true} if Rotary Scroll functionality can be used. Returns {@code false} if the
   * Rotary Scroll system is not available on this device and platform combination.
   */
  public boolean isRotaryScrollFlingAvailable() {
    return rotaryScrollFlingAvailable;
  }

  /**
   * Returns {@code true} if Rotary Scroll has been explicitly enabled for this instance.
   *
   * <p>NOTE: Rotary fling will still not work when enabled if {@link
   * #isRotaryScrollFlingAvailable()} is false.
   */
  public boolean isRotaryScrollFlingEnabled() {
    return rotaryScrollFlingEnabled;
  }

  /**
   * Set whether to enable or disable the rotary scroll fling for this list
   *
   * <p>NOTE: Rotary fling will still not work when enabled if {@link
   * #isRotaryScrollFlingAvailable()} is false.
   */
  public void setRotaryScrollFlingEnabled(boolean enabled) {
    rotaryScrollFlingEnabled = enabled;
  }

  /**
   * Core handler for consuming {@code MotionEvent} instances to scroll or fling the corresponding
   * RecyclerView.
   *
   * @return {@code true} if this processor handles the MotionEvent.
   */
  @CanIgnoreReturnValue
  public boolean onGenericMotionEvent(MotionEvent motionEvent) {
    if (!canFling(motionEvent)) {
      return false;
    }

    boolean eventHandled = false;

    float velocity = getUpdatedPlatformBasedVelocity(motionEvent);
    float signum = Math.signum(velocity);

    // If we get back to back velocities of the same direction, that differ from the fling
    // velocity's direction, stop scroll, regardless of their comparison to the fling velocity
    // threshold.
    if (signum == Math.signum(lastScrollVelocity) && signum != Math.signum(lastFlingVelocity)) {
      rotaryVelocityView.stopScroll();
    }

    if (Math.abs(velocity) >= Math.abs(rotaryVelocityView.getMinFlingVelocity())) {
      rotaryVelocityView.stopScroll();
      eventHandled = rotaryVelocityView.fling(0, (int) velocity);
      lastFlingVelocity = velocity;
    }

    lastScrollVelocity = velocity;
    return eventHandled;
  }

  private boolean canFling(MotionEvent motionEvent) {
    if (!rotaryScrollFlingAvailable) {
      // Fling functionality is not available.
      return false;
    }

    if (!rotaryScrollFlingEnabled) {
      // Fling is not enabled.
      return false;
    }

    if (motionEvent.getAction() != MotionEvent.ACTION_SCROLL) {
      // Only handle ACTION_SCROLL motion events.
      return false;
    }

    if (!motionEvent.isFromSource(InputDeviceCompat.SOURCE_ROTARY_ENCODER)) {
      // Only handle events from a rotary encoder.
      return false;
    }

    return true;
  }

  private float getUpdatedPlatformBasedVelocity(MotionEvent motionEvent) {
    scrollVelocityTracker.addMovement(motionEvent);
    scrollVelocityTracker.computeCurrentVelocity(VELOCITY_IN_PIXELS_PER_SECOND);
    float velocity = scrollVelocityTracker.getScrollVelocity();
    // Invert the sign of the vertical scroll to align the scroll orientation for Android Views.
    velocity *= -rotaryVelocityView.getScaledVerticalScrollFactor();
    return velocity;
  }

  /** Interface for Views that leverage Rotary Fling mechanism. */
  public interface RotaryVelocityView {
    /**
     * Returns amount to scroll in response to a vertical {@link MotionEvent#ACTION_SCROLL} event.
     * Multiply this by the event's axis value to obtain the number of pixels to be scrolled.
     */
    float getScaledVerticalScrollFactor();

    /** Returns the minimum velocity to start a fling. */
    int getMinFlingVelocity();

    /** Stop any current scroll in progress. */
    void stopScroll();

    /**
     * Begin a standard fling with an initial velocity along each axis in pixels per second. If the
     * velocity given is below the system-defined minimum this method will return false and no fling
     * will occur.
     *
     * @param velocityX Initial horizontal velocity in pixels per second
     * @param velocityY Initial vertical velocity in pixels per second
     * @return {@code true} if the fling was started, {@code false} if the velocity was too low to
     *     fling.
     */
    boolean fling(int velocityX, int velocityY);
  }
}
