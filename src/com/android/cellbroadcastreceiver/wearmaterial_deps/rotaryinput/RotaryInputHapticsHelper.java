package com.google.android.clockwork.common.wearable.wearmaterial.rotaryinput;

import static android.view.MotionEvent.AXIS_SCROLL;
import static com.google.common.base.Preconditions.checkNotNull;

import android.content.Context;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.SystemClock;
import android.os.VibrationEffect.Composition;
import android.os.Vibrator;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.LayoutManager;
import androidx.recyclerview.widget.RecyclerView.OnScrollListener;
import androidx.recyclerview.widget.RecyclerView.SimpleOnItemTouchListener;
import android.view.HapticFeedbackConstants;
import android.view.InputDevice;
import android.view.InputDevice.MotionRange;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.InputDeviceCompat;
import androidx.core.widget.NestedScrollView;
import androidx.core.widget.NestedScrollView.OnScrollChangeListener;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This class handles triggering of haptics for Rotary Input for RecyclerView, ScrollView,
 * HorizontalScrollView & NestedScrollView. Haptics are triggered in following ways:
 *
 * <ol>
 *   <li>Triggered repeatedly after fixed scroll distances of rotary input. For this, call
 *       setFixedIntervalHapticsEnabled(true), and either use injectHaptics() or manually forward
 *       onGenericMotionEvent() (if injectHaptics conflicts with other listeners in your component).
 *   <li>Triggered on reaching edges of RecyclerView/ScrollView. For this, call
 *       setEndOfListHapticsEnabled(true), and either use injectHaptics() or manually forward
 *       onScrolled() & onTouchEvent() (if injectHaptics conflicts with other listeners in your
 *       component).
 *   <li>Call triggerTickHaptic() or triggerClickHaptic() directly. Caller can decide when to
 *       trigger haptic events in this case. Advisable to setFixedIntervalHapticsEnabled(false) &
 *       setEndOfListHapticsEnabled(false) to avoid any conflicts.
 * </ol>
 *
 * <p>This class also supports handling rotary input for custom views. In those cases, only fixed
 * interval haptics is supported. End of list haptics is not supported regardless of whether end of
 * list haptics is enabled.
 */
public class RotaryInputHapticsHelper {
  private static final String TAG = "RotaryHapticsHelper";

  /** Configuration that customizes the behavior of {@link RotaryInputHapticsHelper}. */
  public abstract static class Configuration {

    /**
     * Returns true if the helper should suppress fix interval haptics for the given motion event.
     * Suppressing the current motion event doesn't disable fixed interval haptics. Callers must
     * call {@link #setFixedIntervalHapticsEnabled} to manually disable it.
     *
     * @param event the motion event that triggers a haptics attempt.
     * @return true if the helper should suppress triggering haptics of the given {@code event}. The
     *     default implementation always return {@code false}.
     */
    public boolean shouldSuppressFixedIntervalHaptics(MotionEvent event) {
      return false;
    }

    /** Returns the scroll direction of the given motion event. */
    protected int getScrollDirectionFromEvent(MotionEvent event) {
      return -event.getAxisValue(MotionEvent.AXIS_SCROLL) > 0 ? 1 : -1;
    }
  }

  private enum ScrollEdge {
    NONE,
    START,
    END
  }

  private final View rootView;
  private final Configuration configuration;

  private final Vibrator vibrator;
  private @Nullable ExecutorService executorService;
  private Future<?> lastHapticCall;

  private boolean fixedIntervalHapticsEnabled = true;
  private boolean endOfListHapticsEnabled = true;

  private final float scaledScrollFactor;
  private float totalRadiansScrolled = 0.0f;
  private int lastScrollDirection = 1;

  private boolean isLastInputRotary = true;
  private boolean fixedIntervalHapticsTriggered = false;
  private long lastEndOfListHapticTimeMs;
  private ScrollEdge lastEndOfListHapticEdge;
  private final boolean areCompositionHapticsSupported;

  private static final int END_OF_LIST_HAPTIC_GUARD_MS = 500;
  private static final int SCROLL_RADIANS_PER_HAPTIC = 45;

  // TODO(yeabkal): use constants directly from HapticFeedbackConstants class once they are made
  // public
  private static final int TICK_HAPTIC_FEEDBACK_CONSTANT =
      VERSION.SDK_INT <= VERSION_CODES.R ? 10002 : 18;
  private static final int CLICK_HAPTIC_FEEDBACK_CONSTANT =
      VERSION.SDK_INT <= VERSION_CODES.R ? 10003 : 20;

  /**
   * Returns an instance that isn't based on a known scrolling view. Users won't be able to inject
   * haptics with any of the {@code injectHaptics} methods, and instead have to forward motion
   * events to {@link #onGenericMotionEvent(MotionEvent)} manually. End of list haptics won't be
   * triggered for non-scrolling views, and fixed interval haptics won't stop unless disabled with
   * {@link #setFixedIntervalHapticsEnabled}, or suppressed by {@link
   * Configuration#shouldSuppressFixedIntervalHaptics}
   *
   * @see Configuration
   */
  public static RotaryInputHapticsHelper forNonScrollingViews(
      View rootView, Configuration configuration) {
    return new RotaryInputHapticsHelper(rootView.getContext(), rootView, configuration);
  }

  /** Creates an instance for a supported scrolling view type. */
  public RotaryInputHapticsHelper(Context context, View scrollingView) {
    this(context, scrollingView, new ScrollViewConfiguration(scrollingView));
  }

  @SuppressWarnings({"nullness:argument", "nullness:assignment", "nullness:method.invocation"})
  private RotaryInputHapticsHelper(Context context, View rootView, Configuration configuration) {
    this.rootView = rootView;
    this.configuration = configuration;

    scaledScrollFactor =
        (VERSION.SDK_INT >= VERSION_CODES.R)
            ? ViewConfiguration.get(context).getScaledVerticalScrollFactor()
            : 1;

    vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    areCompositionHapticsSupported = areCompositionBasedHapticsSupported();

    // Disable system default haptics to avoid duplicate vibrations.
    rootView.setHapticFeedbackEnabled(!areCompositionHapticsSupported);
  }

  /** Returns if fixed interval haptics is enabled. */
  public boolean isFixedIntervalHapticsEnabled() {
    return fixedIntervalHapticsEnabled;
  }

  /**
   * Enable or disable fixed interval based haptics. When enabled, haptics are triggered repeatedly
   * after fixed scroll distances on scrolling via rotary input.
   */
  public void setFixedIntervalHapticsEnabled(boolean fixedIntervalHapticsEnabled) {
    this.fixedIntervalHapticsEnabled = fixedIntervalHapticsEnabled;
  }

  /** Returns if end of list haptics is enabled. */
  public boolean isEndOfListHapticsEnabled() {
    return endOfListHapticsEnabled;
  }

  /** Enable or disable end of list haptics. */
  public void setEndOfListHapticsEnabled(boolean endOfListHapticsEnabled) {
    this.endOfListHapticsEnabled = endOfListHapticsEnabled;
  }

  /**
   * Whether vibration composition based haptics are supported. Haptics are only triggered on
   * devices where the value returned here is true, otherwise this class's functionality is a no-op.
   */
  public boolean areCompositionBasedHapticsSupported() {
    if (VERSION.SDK_INT < VERSION_CODES.R) {
      return false;
    }
    // ShadowVibrator in Robolectric doesn't support areAllPrimitivesSupported yet, so checking for
    // exception. This avoids the alternative of mocking this method in test setup, which would
    // require all direct & indirect consumers (eg: FadingWearableRecyclerView) of this class to
    // also do the mocking. Actual on-device calls will never throw an exception here, only tests
    // might.
    try {
      return vibrator != null
          && vibrator.areAllPrimitivesSupported(
              Composition.PRIMITIVE_TICK, Composition.PRIMITIVE_CLICK);
    } catch (RuntimeException e) {
      return false;
    }
  }

  /**
   * Handle generic motion events. Caller must must call injectHaptics() or manually forward
   * RecyclerView's/ScrollView's onGenericMotionEvent to this for fixed interval haptics to work.
   */
  public void onGenericMotionEvent(MotionEvent event) {
    if (VERSION.SDK_INT < VERSION_CODES.R) {
      return;
    }
    if (fixedIntervalHapticsEnabled
        && event.getAction() == MotionEvent.ACTION_SCROLL
        && ((event.getSource() & InputDeviceCompat.SOURCE_ROTARY_ENCODER) != 0)) {
      isLastInputRotary = true;
      triggerFixedIntervalHaptics(event);
    }
  }

  /**
   * Handle onScrolled event. Caller must call injectHaptics() or manually forward RecyclerView's
   * onScrolled to this for end of list haptics to work.
   */
  public void onScrolled(int dx, int dy) {
    if (VERSION.SDK_INT < VERSION_CODES.R) {
      return;
    }
    maybeTriggerEndOfListHaptics(dx, dy, /* checkScrollDistance= */ true);
  }

  /**
   * Handle onScrolled event. Caller must call injectHaptics() or manually forward ScrollView's
   * onScrolled to this for end of list haptics to work.
   */
  public void onScrolled() {
    if (VERSION.SDK_INT < VERSION_CODES.R) {
      return;
    }
    maybeTriggerEndOfListHaptics(/* dx= */ 0, /* dy= */ 0, /* checkScrollDistance= */ false);
  }

  /**
   * Handle touch events. Caller must call injectHaptics() or manually forward
   * RecyclerView's/ScrollView's onTouchEvent to this for end of list haptics to work.
   */
  public void onTouchEvent(MotionEvent event) {
    if (VERSION.SDK_INT < VERSION_CODES.R) {
      return;
    }
    isLastInputRotary = false;
  }

  /** Trigger primitive tick haptic immediately. */
  public void triggerTickHaptic() {
    triggerHapticPrimitive(TICK_HAPTIC_FEEDBACK_CONSTANT);
  }

  /** Trigger primitive click haptic immediately. */
  public void triggerClickHaptic() {
    triggerHapticPrimitive(CLICK_HAPTIC_FEEDBACK_CONSTANT);
  }

  /** Inject rotary input haptics for RecyclerView. */
  public void injectHaptics(RecyclerView recyclerView) {
    if (recyclerView == null) {
      return;
    }
    recyclerView.addOnScrollListener(
        new OnScrollListener() {
          @Override
          public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            RotaryInputHapticsHelper.this.onScrolled(dx, dy);
          }
        });

    recyclerView.setOnGenericMotionListener(
        (view, event) -> {
          onGenericMotionEvent(event);
          return false;
        });

    recyclerView.addOnItemTouchListener(
        new SimpleOnItemTouchListener() {
          @Override
          public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
            RotaryInputHapticsHelper.this.onTouchEvent(e);
            return false;
          }
        });
  }

  /** Inject rotary input haptics for ScrollView. */
  @SuppressWarnings("ClickableViewAccessibility") // not consuming event in onTouchEvent
  public void injectHaptics(ScrollView scrollView) {
    if (scrollView == null) {
      return;
    }
    scrollView.getViewTreeObserver().addOnScrollChangedListener(this::onScrolled);
    scrollView.setOnGenericMotionListener(
        (view, event) -> {
          onGenericMotionEvent(event);
          return false;
        });
    scrollView.setOnTouchListener(
        (view, event) -> {
          onTouchEvent(event);
          return false;
        });
  }

  /** Inject rotary input haptics for HorizontalScrollView. */
  @SuppressWarnings("ClickableViewAccessibility") // not consuming event in onTouchEvent
  public void injectHaptics(HorizontalScrollView scrollView) {
    if (scrollView == null) {
      return;
    }
    scrollView.getViewTreeObserver().addOnScrollChangedListener(this::onScrolled);
    scrollView.setOnGenericMotionListener(
        (view, event) -> {
          onGenericMotionEvent(event);
          return false;
        });
    scrollView.setOnTouchListener(
        (view, event) -> {
          onTouchEvent(event);
          return false;
        });
  }

  /** Inject rotary input haptics for NestedScrollView. */
  @SuppressWarnings("ClickableViewAccessibility") // not consuming event in onTouchEvent
  public void injectHaptics(NestedScrollView scrollView) {
    if (scrollView == null) {
      return;
    }
    scrollView.setOnScrollChangeListener(
        (OnScrollChangeListener)
            (nestedScrollView, scrollX, scrollY, oldScrollX, oldScrollY) -> onScrolled());
    scrollView.setOnGenericMotionListener(
        (view, event) -> {
          onGenericMotionEvent(event);
          return false;
        });
    scrollView.setOnTouchListener(
        (view, event) -> {
          onTouchEvent(event);
          return false;
        });
  }

  private void triggerHapticPrimitive(int hapticConstant) {
    if (VERSION.SDK_INT < VERSION_CODES.R || !areCompositionHapticsSupported) {
      return;
    }
    // In Wear R, the View haptic-feedback API is a slow 2-way binder call. Thus, calling it
    // repeatedly in the main thread during scroll haptics produces UI jank. Starting Wear T, this
    // API has been made 1-way and is much faster (takes ~5-6% of the time that the 2-way version
    // takes), so there is no need to call in asynchronously from here.
    if (VERSION.SDK_INT == VERSION_CODES.R) {
      triggerHapticPrimitiveAsync(hapticConstant);
    } else {
      triggerHapticPrimitiveSync(hapticConstant);
    }
  }

  @SuppressWarnings({"ExecutorTaskName", "TikTok.AndroidContextLeak"})
  private void triggerHapticPrimitiveAsync(int hapticConstant) {
    if (lastHapticCall != null) {
      lastHapticCall.cancel(true);
    }
    if (executorService == null) {
      executorService = Executors.newSingleThreadExecutor();
    }
    lastHapticCall =
        checkNotNull(executorService).submit(() -> triggerHapticPrimitiveSync(hapticConstant));
  }

  private void triggerHapticPrimitiveSync(int hapticConstant) {
    // We disabled the view's haptic feedback above to avoid firing system default
    // haptics. Bypass that with FLAG_IGNORE_VIEW_SETTING to fire the haptic here.
    rootView.performHapticFeedback(
        hapticConstant, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
  }

  @VisibleForTesting // Needed due to differences in real & Robolectric MotionEvent class.
  @Nullable
  @SuppressWarnings("InlinedApi") // AXIS_SCROLL is inlined and exists on API 23
  Float getUnitsPerRadian(MotionEvent event) {
    InputDevice inputDevice = event.getDevice();
    if (inputDevice == null) {
      return null;
    }

    MotionRange motionRange = inputDevice.getMotionRange(AXIS_SCROLL);
    if (motionRange == null) {
      return null;
    }
    return motionRange.getResolution();
  }

  @SuppressWarnings("InlinedApi") // AXIS_SCROLL is inlined and exists on API 23
  private void triggerFixedIntervalHaptics(MotionEvent event) {
    Float unitsPerRadian = getUnitsPerRadian(event);
    if (unitsPerRadian == null) {
      return;
    }

    fixedIntervalHapticsTriggered = true;

    float axisScroll = event.getAxisValue(MotionEvent.AXIS_SCROLL);
    int currentScrollDirection = configuration.getScrollDirectionFromEvent(event);
    if (configuration.shouldSuppressFixedIntervalHaptics(event)) {
      return;
    }

    if (currentScrollDirection != lastScrollDirection) {
      totalRadiansScrolled = 0.0f;
    }
    lastScrollDirection = currentScrollDirection;

    float movementRadians = (axisScroll * scaledScrollFactor) / unitsPerRadian;
    totalRadiansScrolled += movementRadians;

    if (Math.abs(totalRadiansScrolled) > SCROLL_RADIANS_PER_HAPTIC) {
      totalRadiansScrolled = totalRadiansScrolled % SCROLL_RADIANS_PER_HAPTIC;
      triggerTickHaptic();
    }
  }

  @VisibleForTesting
  void maybeTriggerEndOfListHaptics(int dx, int dy, boolean checkScrollDistance) {
    if (!(configuration instanceof ScrollViewConfiguration)) {
      // Only supports end-of-list haptics for known scroll views.
      return;
    }
    ScrollViewConfiguration scrollViewConfiguration = (ScrollViewConfiguration) configuration;

    if (!endOfListHapticsEnabled || !isLastInputRotary) {
      return;
    }

    // If both fixed interval and end of list haptics are enabled, we want to make these atomic,
    // either both should fire or none. A common cause for fixed interval haptics not being fired is
    // the onGenericMotionEvent not being forwarded by the calling class.
    if (fixedIntervalHapticsEnabled && !fixedIntervalHapticsTriggered) {
      return;
    }

    boolean isAtEdgeOfList = false;
    ScrollEdge scrollEdge = ScrollEdge.NONE;

    if (scrollViewConfiguration.isAtStartOfList(dx, dy, checkScrollDistance)) {
      isAtEdgeOfList = true;
      scrollEdge = ScrollEdge.START;
    } else if (scrollViewConfiguration.isAtEndOfList(dx, dy, checkScrollDistance)) {
      isAtEdgeOfList = true;
      scrollEdge = ScrollEdge.END;
    }

    if (isAtEdgeOfList) {
      long currentTime = SystemClock.elapsedRealtime();
      if (lastEndOfListHapticTimeMs == 0
          || (currentTime - lastEndOfListHapticTimeMs > END_OF_LIST_HAPTIC_GUARD_MS)
          || lastEndOfListHapticEdge != scrollEdge) {
        lastEndOfListHapticTimeMs = currentTime;
        lastEndOfListHapticEdge = scrollEdge;
        triggerClickHaptic();
      }
    }
  }

  public void triggerEndOfListHaptics() {
    long currentTime = SystemClock.elapsedRealtime();
    if (lastEndOfListHapticTimeMs == 0
        || (currentTime - lastEndOfListHapticTimeMs > END_OF_LIST_HAPTIC_GUARD_MS)) {
      lastEndOfListHapticTimeMs = currentTime;
      triggerClickHaptic();
    }
  }

  private static class ScrollViewConfiguration extends Configuration {

    private enum ScrollOrientation {
      UNKNOWN,
      HORIZONTAL,
      VERTICAL
    }

    private final View scrollingView;

    ScrollViewConfiguration(View scrollingView) {
      if (!(scrollingView instanceof RecyclerView
          || scrollingView instanceof ScrollView
          || scrollingView instanceof HorizontalScrollView
          || scrollingView instanceof NestedScrollView)) {
        throw new UnsupportedOperationException(
            "RotaryInputHapticsHelper only supports RecyclerView, ScrollView, HorizontalScrollView"
                + " & NestedScrollView");
      }
      this.scrollingView = scrollingView;
    }

    @SuppressWarnings("RedundantIfStatement") // for readability.
    @Override
    public boolean shouldSuppressFixedIntervalHaptics(MotionEvent event) {
      int scrollDirection = getScrollDirectionFromEvent(event);
      if (scrollDirection > 0 && isAtEndOfList(0, 0, false)) {
        return true;
      }
      if (scrollDirection < 0 && isAtStartOfList(0, 0, false)) {
        return true;
      }
      return false;
    }

    boolean isAtStartOfList(int dx, int dy, boolean checkScrollDistance) {
      boolean isAtStart = false;
      if (getScrollOrientation() == ScrollOrientation.VERTICAL) {
        isAtStart = !scrollingView.canScrollVertically(-1); // top edge
        // Checking scroll distance being non-zero improves edge detection in RecyclerView.
        // However, this is not available in ScrollView hence we skip it for onScrolled call paths
        // for ScrollView.
        if (checkScrollDistance) {
          isAtStart = isAtStart && dy < 0;
        }
      } else if (getScrollOrientation() == ScrollOrientation.HORIZONTAL) {
        isAtStart = !scrollingView.canScrollHorizontally(-1); // left edge
        if (checkScrollDistance) {
          isAtStart = isAtStart && dx < 0;
        }
      }
      return isAtStart;
    }

    boolean isAtEndOfList(int dx, int dy, boolean checkScrollDistance) {
      boolean isAtEnd = false;
      if (getScrollOrientation() == ScrollOrientation.VERTICAL) {
        isAtEnd = !scrollingView.canScrollVertically(1); // bottom edge
        // Checking scroll distance being non-zero improves edge detection in RecyclerView.
        // However, this is not available in ScrollView hence we skip it for onScrolled call paths
        // for
        // ScrollView.
        if (checkScrollDistance) {
          isAtEnd = isAtEnd && dy > 0;
        }
      } else if (getScrollOrientation() == ScrollOrientation.HORIZONTAL) {
        isAtEnd = !scrollingView.canScrollHorizontally(1); // right edge
        if (checkScrollDistance) {
          isAtEnd = isAtEnd && dx > 0;
        }
      }
      return isAtEnd;
    }

    private ScrollOrientation getScrollOrientation() {
      if (scrollingView instanceof RecyclerView) {
        LayoutManager layoutManager = ((RecyclerView) scrollingView).getLayoutManager();
        if (layoutManager == null) {
          return ScrollOrientation.UNKNOWN;
        }
        if (layoutManager.canScrollVertically()) {
          return ScrollOrientation.VERTICAL;
        }
        if (layoutManager.canScrollHorizontally()) {
          return ScrollOrientation.HORIZONTAL;
        }
      }

      if (scrollingView instanceof ScrollView) {
        return ScrollOrientation.VERTICAL;
      }
      if (scrollingView instanceof HorizontalScrollView) {
        return ScrollOrientation.HORIZONTAL;
      }
      if (scrollingView instanceof NestedScrollView) {
        return ScrollOrientation.VERTICAL;
      }

      return ScrollOrientation.UNKNOWN;
    }
  }
}
