package com.google.android.clockwork.common.wearable.wearmaterial.rotaryinput;

import static java.lang.Math.abs;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build.VERSION_CODES;
import android.view.MotionEvent;
import androidx.annotation.VisibleForTesting;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Accumulator to trigger events in a listener based on {@link MotionEvent} from rotary input.
 *
 * <p>This should normally be passed events received from {@link
 * android.view.View#onGenericMotionEvent(MotionEvent)}
 */
@TargetApi(VERSION_CODES.R)
public final class RotaryInputAccumulator {
  @VisibleForTesting static final int DEFAULT_EVENT_ACCUMULATION_THRESHOLD_MS = 200;
  @VisibleForTesting static final float DEFAULT_MIN_VALUE_CHANGE_DISTANCE_PX = 48f;
  @VisibleForTesting static final int DEFAULT_RATE_LIMIT_COOL_DOWN_MS = 300;

  private final RotaryInputEventReader rotaryInputEventReader;
  private final long eventAccumulationThresholdMs;
  private final float minValueChangeDistancePx;
  private final long rateLimitCoolDownMs;

  private @Nullable Listener listener;
  private float accumulatedDistance = 0f;
  private long lastAccumulatedEventTimeMs = 0;
  private boolean rateLimitingEnabled;
  private long lastUpdateTimeMs = 0;
  private float valueScaleFactor = 1f;

  public static Builder builder(Context context) {
    return new Builder(context);
  }

  @Deprecated // deprecated, use {@link RotaryInputAccumulator.Builder} to construct new instances.
  public RotaryInputAccumulator(Context context) {
    this(
        context,
        DEFAULT_EVENT_ACCUMULATION_THRESHOLD_MS,
        DEFAULT_MIN_VALUE_CHANGE_DISTANCE_PX,
        DEFAULT_RATE_LIMIT_COOL_DOWN_MS);
  }

  @VisibleForTesting
  RotaryInputAccumulator(RotaryInputEventReader rotaryInputEventReader) {
    this.rotaryInputEventReader = rotaryInputEventReader;
    this.eventAccumulationThresholdMs = DEFAULT_EVENT_ACCUMULATION_THRESHOLD_MS;
    this.minValueChangeDistancePx = DEFAULT_MIN_VALUE_CHANGE_DISTANCE_PX;
    this.rateLimitCoolDownMs = DEFAULT_RATE_LIMIT_COOL_DOWN_MS;
  }

  private RotaryInputAccumulator(
      Context context,
      long eventAccumulationThresholdMs,
      float minValueChangeDistancePx,
      long rateLimitCoolDownMs) {
    this.rotaryInputEventReader = new RotaryInputEventReader(context);
    this.eventAccumulationThresholdMs = eventAccumulationThresholdMs;
    this.minValueChangeDistancePx = minValueChangeDistancePx;
    this.rateLimitCoolDownMs = rateLimitCoolDownMs;
  }

  /**
   * Process a {@link MotionEvent}.
   *
   * @param event the {@link MotionEvent} to be processed.
   * @return {@code true} when the event has produced change in the accumulator.
   */
  public boolean onGenericMotionEvent(MotionEvent event) {
    if (!rotaryInputEventReader.isRotaryScrollEvent(event)) {
      return false;
    }

    long eventTimeMs = event.getEventTime();
    long timeSinceLastAccumulatedMs = eventTimeMs - lastAccumulatedEventTimeMs;
    lastAccumulatedEventTimeMs = eventTimeMs;
    float distance = rotaryInputEventReader.getScrollDistance(event);
    if (timeSinceLastAccumulatedMs > eventAccumulationThresholdMs) {
      accumulatedDistance = distance;
    } else {
      accumulatedDistance += distance;
    }

    onEventAccumulated(eventTimeMs);
    return true;
  }

  /**
   * Sets scale factor when mapping rotary input scroll distance to value changed.
   *
   * @param valueScaleFactor value changed per pixel scrolled.
   */
  public void setValueChangePerPixelScrolled(float valueScaleFactor) {
    this.valueScaleFactor = valueScaleFactor;
  }

  /**
   * Sets whether rate limiting is enabled.
   *
   * @param rateLimitingEnabled {@code true} when value updates should be rate limited to no more
   *     than one event during each time window of {@link #DEFAULT_RATE_LIMIT_COOL_DOWN_MS}.
   */
  public void setRateLimitingEnabled(boolean rateLimitingEnabled) {
    this.rateLimitingEnabled = rateLimitingEnabled;
  }

  /**
   * Sets the listener to receive value change events.
   *
   * @param listener the listener.
   */
  public void setListener(@Nullable Listener listener) {
    this.listener = listener;
  }

  private void onEventAccumulated(long eventTimeMs) {
    if (abs(accumulatedDistance) < minValueChangeDistancePx
        || (rateLimitingEnabled && eventTimeMs - lastUpdateTimeMs < rateLimitCoolDownMs)
        || listener == null) {
      return;
    }

    listener.onValueChange(accumulatedDistance * valueScaleFactor);
    lastUpdateTimeMs = eventTimeMs;
    accumulatedDistance = 0;
  }

  /** Listener to hook into the accumulator to receive value updates. */
  public interface Listener {

    /**
     * When accumulated motion events resulted in a value change.
     *
     * @param change the value change.
     */
    void onValueChange(float change);
  }

  /** Builder for constructing a {@link RotaryInputAccumulator}. */
  public static class Builder {

    private final Context context;

    private long eventAccumulationThresholdMs = DEFAULT_EVENT_ACCUMULATION_THRESHOLD_MS;
    private float minValueChangeDistancePx = DEFAULT_MIN_VALUE_CHANGE_DISTANCE_PX;
    private long rateLimitCoolDownMs = DEFAULT_RATE_LIMIT_COOL_DOWN_MS;

    private Builder(Context context) {
      this.context = context;
    }

    /**
     * Sets time threshold below which events are accumulated.
     *
     * @param eventAccumulationThresholdMs threshold for accumulation in milliseconds.
     */
    @CanIgnoreReturnValue
    public Builder setEventAccumulationThresholdMs(long eventAccumulationThresholdMs) {
      this.eventAccumulationThresholdMs = eventAccumulationThresholdMs;
      return this;
    }

    /**
     * Sets minimum distance for value change.
     *
     * @param minValueChangeDistancePx threshold for triggering value change in pixels.
     */
    @CanIgnoreReturnValue
    public Builder setMinValueChangeDistancePx(float minValueChangeDistancePx) {
      this.minValueChangeDistancePx = minValueChangeDistancePx;
      return this;
    }

    /**
     * Sets cool down time when rate limiting is enabled.
     *
     * @param rateLimitCoolDownMs milliseconds between events when rate limiting is enabled.
     */
    @CanIgnoreReturnValue
    public Builder setRateLimitCoolDownMs(long rateLimitCoolDownMs) {
      this.rateLimitCoolDownMs = rateLimitCoolDownMs;
      return this;
    }

    public RotaryInputAccumulator build() {
      return new RotaryInputAccumulator(
          context, eventAccumulationThresholdMs, minValueChangeDistancePx, rateLimitCoolDownMs);
    }
  }
}
