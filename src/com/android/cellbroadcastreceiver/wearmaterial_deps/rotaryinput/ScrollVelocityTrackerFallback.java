package com.google.android.clockwork.common.wearable.wearmaterial.rotaryinput;

import static android.view.MotionEvent.AXIS_SCROLL;
import static com.google.common.base.Preconditions.checkNotNull;

import android.view.MotionEvent;

/**
 * A fallback implementation of {@link ScrollVelocityTracker} to be used if no real implementation
 * is available.
 *
 * <p>Tracks velocity for a single pointer ID only.
 *
 * <p>The impulse strategy is based on the strategy implemented in Android framework (see
 * VelocityTracker.cpp), and the customization of the strategy to support differential axes like
 * {@link MotionEvent.AXIS_SCROLL} is based on the work done in b/32830165 for Android U.
 */
public final class ScrollVelocityTrackerFallback implements ScrollVelocityTracker {
  private static final long RANGE_MS = 100L;
  private static final int HISTORY_SIZE = 20;
  /**
   * If there's no data beyond this period of time, we assume that the previous chain of motion from
   * the pointer has stopped, and we handle subsequent data points separately.
   */
  private static final long ASSUME_POINTER_STOPPED_MS = 40L;

  /**
   * A circular buffer of all currently valid velocity data points to consider for velocity
   * computation, with capacity of {@link HISTORY_SIZE}.
   */
  private final ScrollVelocityDataPoint[] dataPoints = new ScrollVelocityDataPoint[HISTORY_SIZE];

  /** Cached value of the last computed velocity, for a O(1) get operation. */
  private float lastComputedVelocity = 0f;

  /** Number of data points that are potential velocity calculation candidates. */
  private int dataPointsBufferSize = 0;
  /**
   * The last index in the circular buffer where a data point was added. Irrelevant if {@code
   * dataPointsBufferSize} == 0.
   */
  private int dataPointsBufferLastUsedIndex = 0;

  @SuppressWarnings("InlinedApi") // AXIS_SCROLL is inlined and exists on API 23
  @Override
  public void addMovement(MotionEvent event) {
    long eventTime = event.getEventTime();
    if (dataPointsBufferSize != 0
        && (eventTime - dataPoints[dataPointsBufferLastUsedIndex].eventTime
            > ASSUME_POINTER_STOPPED_MS)) {
      // There has been at least `ASSUME_POINTER_STOPPED_MS` since the last recorded event. When
      // this happens, consider that the pointer has stopped until this new event. Thus, clear all
      // past events.
      clear();
    }

    dataPointsBufferLastUsedIndex = (dataPointsBufferLastUsedIndex + 1) % HISTORY_SIZE;
    // We do not need to increase size if the size is already `HISTORY_SIZE`, since we always will
    // have at most `HISTORY_SIZE` data points stored, due to the circular buffer.
    if (dataPointsBufferSize != HISTORY_SIZE) {
      dataPointsBufferSize += 1;
    }

    if (dataPoints[dataPointsBufferLastUsedIndex] == null) {
      dataPoints[dataPointsBufferLastUsedIndex] =
          new ScrollVelocityDataPoint(event.getAxisValue(AXIS_SCROLL), eventTime);
    } else {
      ScrollVelocityDataPoint existingDp = dataPoints[dataPointsBufferLastUsedIndex];
      existingDp.scrollAmount = event.getAxisValue(AXIS_SCROLL);
      existingDp.eventTime = eventTime;
    }
  }

  @Override
  public void computeCurrentVelocity(int units) {
    computeCurrentVelocity(units, Float.MAX_VALUE);
  }

  @Override
  public void computeCurrentVelocity(int units, float maxVelocity) {
    lastComputedVelocity = getCurrentVelocity() * units;

    // Fix the velocity as per the max velocity (i.e. clamp it between [-maxVelocity, maxVelocity])
    if (lastComputedVelocity < -Math.abs(maxVelocity)) {
      lastComputedVelocity = -Math.abs(maxVelocity);
    } else if (lastComputedVelocity > Math.abs(maxVelocity)) {
      lastComputedVelocity = Math.abs(maxVelocity);
    }
  }

  @Override
  public float getScrollVelocity() {
    return getScrollVelocity(/* id= */ 0);
  }

  @Override
  public float getScrollVelocity(int id) {
    return id == 0 ? lastComputedVelocity : 0;
  }

  @Override
  public void clear() {
    dataPointsBufferSize = 0;
    lastComputedVelocity = 0;
  }

  @Override
  public void recycle() {
    // No-op. Used in the platform VelocityTracker, as that uses a pool of objects that get recycled
    // and reused.
  }

  private float getCurrentVelocity() {
    // At least 2 data points needed to get Impulse velocity.
    if (dataPointsBufferSize < 2) {
      return 0f;
    }

    // The first valid index that contains a data point that should be part of the velocity
    // calculation, as long as it's within `RANGE_MS` from the latest data point.
    int firstValidIndex =
        (dataPointsBufferLastUsedIndex + HISTORY_SIZE - (dataPointsBufferSize - 1)) % HISTORY_SIZE;
    long lastEventTime = dataPoints[dataPointsBufferLastUsedIndex].eventTime;
    while (lastEventTime - dataPoints[firstValidIndex].eventTime > RANGE_MS) {
      // Decrementing the size is equivalent to practically "removing" this data point.
      dataPointsBufferSize--;
      // Increment the `firstValidIndex`, since we just found out that the current `firstValidIndex`
      // is not valid (not within `RANGE_MS`).
      firstValidIndex = (firstValidIndex + 1) % HISTORY_SIZE;
    }

    // At least 2 data points needed to get Impulse velocity.
    if (dataPointsBufferSize < 2) {
      return 0;
    }

    if (dataPointsBufferSize == 2) {
      ScrollVelocityDataPoint first = checkNotNull(dataPoints[firstValidIndex]);
      ScrollVelocityDataPoint last = checkNotNull(dataPoints[(firstValidIndex + 1) % HISTORY_SIZE]);
      if (first.eventTime == last.eventTime) {
        return 0f;
      }
      return last.scrollAmount / (last.eventTime - first.eventTime);
    }

    float work = 0;
    int numDataPointsProcessed = 0;
    // Loop from the `firstValidIndex`, to the "second to last" valid index. We need to go only to
    // the "second to last" element, since the body of the loop checks against the next data point,
    // so we cannot go all the way to the end.
    for (int i = 0; i < dataPointsBufferSize - 1; i++) {
      int currentIndex = i + firstValidIndex;
      ScrollVelocityDataPoint dataPoint = checkNotNull(dataPoints[currentIndex % HISTORY_SIZE]);
      int nextIndex = (currentIndex + 1) % HISTORY_SIZE;

      // Duplicate timestamp. Skip this data point.
      if (dataPoints[nextIndex].eventTime == dataPoint.eventTime) {
        continue;
      }

      numDataPointsProcessed++;
      float vPrev = kineticEnergyToVelocity(work);
      float delta = dataPoints[nextIndex].scrollAmount;
      float vCurr = delta / (dataPoints[nextIndex].eventTime - dataPoint.eventTime);

      work += (vCurr - vPrev) * Math.abs(vCurr);

      // Note that we are intentionally checking against `numDataPointsProcessed`, instead of just
      // checking `i` against `firstValidIndex`. This is to cover cases where there are multiple
      // data points that have the same timestamp as the one at `firstValidIndex`.
      if (numDataPointsProcessed == 1) {
        work = work * 0.5f;
      }
    }

    return kineticEnergyToVelocity(work);
  }

  /** Based on the formula: Kinetic Energy = (0.5 * mass * velocity^2), with mass = 1. */
  private static float kineticEnergyToVelocity(float work) {
    return (work < 0 ? -1.0f : 1.0f) * (float) Math.sqrt(2f * Math.abs(work));
  }

  private static final class ScrollVelocityDataPoint {
    float scrollAmount;
    long eventTime;

    private ScrollVelocityDataPoint(float scrollAmount, long eventTime) {
      this.scrollAmount = scrollAmount;
      this.eventTime = eventTime;
    }
  }
}
