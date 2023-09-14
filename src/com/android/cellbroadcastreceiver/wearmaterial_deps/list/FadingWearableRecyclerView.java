package com.google.android.clockwork.common.wearable.wearmaterial.list;

import static java.lang.Math.max;
import static java.lang.Math.min;

import android.animation.Animator.AnimatorListener;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewPropertyAnimator;
import android.view.ViewTreeObserver;
import android.view.animation.AlphaAnimation;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.EdgeEffect;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.ViewConfigurationCompat;
import com.google.android.clockwork.common.wearable.wearmaterial.animations.Durations;
import com.google.android.clockwork.common.wearable.wearmaterial.list.OverScrollEdgeEffect.OverScrollListener;
import com.google.android.clockwork.common.wearable.wearmaterial.list.ViewGroupFader.AnimationCallback;
import com.google.android.clockwork.common.wearable.wearmaterial.rotaryinput.RotaryInputHapticsHelper;
import com.google.android.clockwork.common.wearable.wearmaterial.rotaryinput.RotaryInputLinearSnapHelper;
import com.google.android.clockwork.common.wearable.wearmaterial.rotarymotioneventprocessor.RotaryMotionEventProcessor;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/** RecyclerView for Wear that includes scaling and fading animations */
public class FadingWearableRecyclerView extends RecyclerView implements OverScrollListener {
  /** Haptics strategy for rotary input. */
  public enum RotaryInputHapticsStrategy {
    OFF,
    ON_FIXED_INTERVAL,
  }

  private static final String TAG = "FadingWearableRV";
  private static final int NO_VALUE = Integer.MIN_VALUE;

  private final Context context;
  private final int screenWidth;
  private final int screenHeight;
  private final float scaledVerticalScrollFactor;
  @VisibleForTesting RotaryMotionEventProcessor rotaryMotionEventProcessor;
  @VisibleForTesting @Nullable RotaryInputLinearSnapHelper rotaryInputLinearSnapHelper;
  @VisibleForTesting RotaryInputHapticsHelper rotaryInputHapticsHelper;
  @VisibleForTesting TestingWrapper testingWrapper;

  private ViewGroupFader fader;
  private boolean showStartupAnimation = false;
  private int startupAnimationOffset;
  private long startupAnimationDelay = Durations.FLASH;
  private long startupAnimationDuration = Durations.QUICK;
  private Interpolator startupAnimationInterpolator = new PathInterpolator(0.2f, 0.2f, 0, 1);
  private final Interpolator startupAlphaAnimationInterpolator =
      new PathInterpolator(0.33f, 0.0f, 0.67f, 0.2f);
  private AnimatorListener startupAnimatorListener;
  private boolean edgeItemsCenteringEnabled;
  private boolean centerEdgeItemsWhenThereAreChildren;
  private boolean skipFirstItemWhenCentering = false;
  private int originalPaddingTop = NO_VALUE;
  private int originalPaddingBottom = NO_VALUE;
  private float topPaddingPercent = NO_VALUE;
  private float bottomPaddingPercent = NO_VALUE;
  private boolean snappingEnabled = false;
  private RotaryInputHapticsStrategy rotaryInputHapticsStrategy;
  private float squeezeScaleY;
  private int cachedScrollExtent;

  @VisibleForTesting
  static final RotaryInputHapticsStrategy DEFAULT_ROTARY_HAPTICS_STRATEGY =
      RotaryInputHapticsStrategy.ON_FIXED_INTERVAL;

  /** Pre-draw listener which is used to adjust the padding on this view before its first draw. */
  @SuppressWarnings("nullness:method.invocation")
  private final ViewTreeObserver.OnPreDrawListener paddingPreDrawListener =
      new ViewTreeObserver.OnPreDrawListener() {
        @Override
        public boolean onPreDraw() {
          if (centerEdgeItemsWhenThereAreChildren && getChildCount() > 0) {
            setupCenteredPadding();
            centerEdgeItemsWhenThereAreChildren = false;
            return false;
          }
          return true;
        }
      };

  public FadingWearableRecyclerView(Context context) {
    this(context, null);
  }

  public FadingWearableRecyclerView(Context context, @Nullable AttributeSet attributeSet) {
    this(context, attributeSet, 0);
  }

  @SuppressWarnings({"nullness:assignment", "nullness:argument", "nullness:method.invocation"})
  public FadingWearableRecyclerView(
      Context context, @Nullable AttributeSet attributeSet, int defStyleAttr) {
    super(context, attributeSet, defStyleAttr);
    this.context = context;
    testingWrapper = new TestingWrapper();

    DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
    screenWidth = displayMetrics.widthPixels;
    screenHeight = displayMetrics.heightPixels;
    startupAnimationOffset = screenHeight;

    scaledVerticalScrollFactor =
        ViewConfigurationCompat.getScaledVerticalScrollFactor(
            ViewConfiguration.get(context), context);

    rotaryMotionEventProcessor =
        new RotaryMotionEventProcessor(context, new FadingRotaryVelocityView());

    fader =
        new ViewGroupFader(
            this,
            new AnimationCallback() {
              @Override
              public boolean shouldFadeFromTop(View view) {
                // check if parent is recycler view otherwise it crashes on getChildAdapterPosition
                if (!isParentFadingWearableRecyclerView(view)) {
                  return true;
                }
                return (getChildAdapterPosition(view) != 0);
              }

              @Override
              public boolean shouldFadeFromBottom(View view) {
                // check if parent is recycler view otherwise it crashes on getChildAdapterPosition
                if (!isParentFadingWearableRecyclerView(view)) {
                  return true;
                }
                return (getChildAdapterPosition(view)
                    != Objects.requireNonNull(getAdapter()).getItemCount() - 1);
              }

              @Override
              public void viewHasBecomeFullSize(View view) {}

              private boolean isParentFadingWearableRecyclerView(View view) {
                return view.getParent() == FadingWearableRecyclerView.this;
              }
            },
            new ViewGroupFader.DefaultViewBoundsProvider());
    init(context, attributeSet, defStyleAttr);
  }

  /** if applicable, set up the centered padding for the list items */
  private void setupCenteredPadding() {
    // Recalculate centering padding if the dataset changes
    Objects.requireNonNull(getAdapter())
        .registerAdapterDataObserver(
            new AdapterDataObserver() {
              @Override
              public void onChanged() {
                super.onChanged();
                calculateAndSetDynamicPadding(getChildHeightIfExists(0), getChildHeightIfExists(1));
              }

              @Override
              public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                if (positionStart <= 1) {
                  calculateAndSetDynamicPadding(
                      getChildHeightIfExists(0), getChildHeightIfExists(1));
                }
              }

              @Override
              public void onItemRangeRemoved(int positionStart, int itemCount) {
                super.onItemRangeRemoved(positionStart, itemCount);
                if (positionStart <= 1 && getItemAnimator() != null) {
                  int firstIndex = 0;
                  int secondIndex = 1;
                  if (positionStart == 0) {
                    firstIndex = itemCount;
                    secondIndex = itemCount + 1;
                  } else if (positionStart == 1) {
                    secondIndex = itemCount + 1;
                  }

                  calculateAndSetDynamicPadding(
                      getChildHeightIfExists(firstIndex), getChildHeightIfExists(secondIndex));
                } else {
                  calculateAndSetDynamicPadding(
                      getChildHeightIfExists(0), getChildHeightIfExists(1));
                }
              }
            });

    if (getChildCount() < 1
        || !edgeItemsCenteringEnabled
        || (getChildCount() < 2 && skipFirstItemWhenCentering)) {
      return;
    }

    calculateAndSetDynamicPadding(getChildHeightIfExists(0), getChildHeightIfExists(1));
  }

  /**
   * Calculate and set the padding the user has set for the top and bottom of the list, or to center
   * the first item
   */
  @VisibleForTesting
  public void calculateAndSetDynamicPadding(int firstChildHeight, int secondChildHeight) {
    int newTopPadding;
    if (topPaddingPercent == NO_VALUE) {
      // if the user has not specified a top padding percent, we will fall back to centering
      int height;
      int firstItemAdjustment = 0;
      if (skipFirstItemWhenCentering) {
        height = secondChildHeight;
        firstItemAdjustment = firstChildHeight;
      } else {
        height = firstChildHeight;
      }
      // This is enough padding to center the child view in the parent.
      newTopPadding = max((int) ((getHeight() * 0.5f) - (height * 0.5f) - firstItemAdjustment), 0);
    } else {
      newTopPadding = max((int) (screenHeight * topPaddingPercent), 0);
    }

    int newBottomPadding =
        bottomPaddingPercent == NO_VALUE
            ? getPaddingBottom()
            : max((int) (screenHeight * bottomPaddingPercent), 0);

    // If we don't have the padding we want, we'll change it
    if (getPaddingTop() != newTopPadding || getPaddingBottom() != newBottomPadding) {
      originalPaddingTop = getPaddingTop();
      originalPaddingBottom = getPaddingBottom();

      // Set our desired padding for the top and bottom of the list
      setPadding(getPaddingLeft(), newTopPadding, getPaddingRight(), newBottomPadding);

      // The focused child should be in the center, so force a scroll to it.
      LayoutManager layoutManager = getLayoutManager();
      if (layoutManager != null) {
        View focusedChild = getFocusedChild();
        int focusedPosition = (focusedChild != null) ? layoutManager.getPosition(focusedChild) : 0;
        layoutManager.scrollToPosition(focusedPosition);
      }
    }
  }

  private void setupOriginalPadding() {
    if (originalPaddingTop == NO_VALUE) {
      return;
    } else {
      setPadding(getPaddingLeft(), originalPaddingTop, getPaddingRight(), originalPaddingBottom);
    }
  }

  /**
   * Use this method to configure the {@link FadingWearableRecyclerView} to always align the first
   * and last items with the vertical center of the screen. This effectively moves the start and end
   * of the list to the middle of the screen if the user has scrolled so far. It takes the height of
   * the children into account so that they are correctly centered.
   *
   * @param isEnabled set to true if you wish to align the edge children (first and last) with the
   *     center of the screen.
   */
  public void setEdgeItemsCenteringEnabled(boolean isEnabled) {
    edgeItemsCenteringEnabled = isEnabled;
    if (edgeItemsCenteringEnabled) {
      if (getChildCount() > 0) {
        setupCenteredPadding();
      } else {
        centerEdgeItemsWhenThereAreChildren = true;
      }
    } else {
      setupOriginalPadding();
      centerEdgeItemsWhenThereAreChildren = false;
    }
  }

  /**
   * Returns whether the view is currently configured to center the edge children. See {@link
   * #setEdgeItemsCenteringEnabled} for details.
   */
  public boolean isEdgeItemsCenteringEnabled() {
    return edgeItemsCenteringEnabled;
  }

  /**
   * Will the first item in the list be skipped when centering (i.e. the second item in the list
   * will be centered on screen)?
   */
  public boolean isFirstItemSkippedWhenCentering() {
    return skipFirstItemWhenCentering;
  }

  /**
   * Skip the first item in the list when centering (i.e. the second item in the list will be
   * centered on the screen). Centering in FadingWearableRecyclerView only adds padding to the
   * beginning of the list. If the first and second items are large enough that centering would move
   * the first item off screen the list will instead appear as if edgeItemsCentering is disabled.
   *
   * <p>Setting skipFirstItemWhenCentering to true assumes there are at least 2 items in the list.
   * If there are less than 2 items in the list, edgeItemsCentering will have no effect.
   */
  public void setSkipFirstItemWhenCentering(boolean skipFirstItemWhenCentering) {
    this.skipFirstItemWhenCentering = skipFirstItemWhenCentering;
  }

  public ViewGroupFader getFader() {
    return fader;
  }

  /** Set a new ViewGroupFader for this RecyclerView */
  public void setFader(ViewGroupFader fader) {
    this.fader = fader;
    setItemAnimator(FadingWearableItemAnimator.create(fader));
  }

  /** Return if this list will show a startup animation */
  public boolean willShowStartupAnimation() {
    return showStartupAnimation;
  }

  /** Set whether this list should show a startup animation */
  public void setShowStartupAnimation(boolean showStartupAnimation) {
    this.showStartupAnimation = showStartupAnimation;
  }

  /** Get the starting offset of the startup animation (in px) */
  public int getStartupAnimationOffsetPx() {
    return startupAnimationOffset;
  }

  /** Set the starting offset of the startup animation (in px) */
  public void setStartupAnimationOffsetPx(int pixels) {
    this.startupAnimationOffset = pixels;
  }

  /** Set the starting offset of the startup animation (as a percentage of view height) */
  public void setStartUpAnimationOffsetPercent(float percent) {
    this.startupAnimationOffset = (int) (percent * screenHeight);
  }

  /** Set the startup animation delay in ms. The default delay is Durations.RAPID */
  public void setStartupAnimationDelay(long delay) {
    this.startupAnimationDelay = delay;
  }

  /** Set the startup animation duration in ms. The default duration is Durations.STANDARD */
  public void setStartupAnimationDuration(long duration) {
    this.startupAnimationDuration = duration;
  }

  /** Set the interpolator for the startup animation. */
  public void setStartupAnimationInterpolator(Interpolator interpolator) {
    this.startupAnimationInterpolator = interpolator;
  }

  /** Set an AnimatorListener on the startup animation */
  public void setStartupAnimatorListener(AnimatorListener listener) {
    this.startupAnimatorListener = listener;
  }

  /** Whether snapping is enabled for this list. */
  public boolean isSnappingEnabled() {
    return snappingEnabled;
  }

  /** Set whether snapping should be enabled for this list. The snapping is disabled by default. */
  public void setSnappingEnabled(boolean enabled) {
    snappingEnabled = enabled;
    if (enabled && rotaryInputLinearSnapHelper == null) {
      rotaryInputLinearSnapHelper = new RotaryInputLinearSnapHelper(this);
    }
    if (rotaryInputLinearSnapHelper != null) {
      rotaryInputLinearSnapHelper.setSnappingEnabled(enabled);
    }
  }

  /**
   * Whether smooth scrolling is enabled on devices with a low-res rotary encoder.
   *
   * <p>Note: This method does not check whether the device has a low-res rotary encoder or not, so
   * may still return true on a device with a high-res rotary encoder.
   *
   * @deprecated {@link FadingWearableRecyclerView} LowResEncoderSmoothScrolling is deprecated. It
   *     is now handled natively in {@link RecyclerView}. This method will always return false.
   */
  @Deprecated
  public boolean isLowResEncoderSmoothScrollingEnabled() {
    return false;
  }

  /**
   * Set whether smooth scrolling is enabled on devices with a low-res rotary encoder.
   *
   * @deprecated {@link FadingWearableRecyclerView} LowResEncoderSmoothScrolling is deprecated. It
   *     is now handled natively in {@link RecyclerView}.
   */
  @Deprecated
  public void setLowResEncoderSmoothScrollingEnabled(boolean enabled) {}

  /** Get the rotary input haptics strategy for this list. */
  public RotaryInputHapticsStrategy getRotaryInputHapticsStrategy() {
    return this.rotaryInputHapticsStrategy;
  }

  /** Set the rotary input haptics strategy for this list. */
  public void setRotaryInputHapticsStrategy(RotaryInputHapticsStrategy rotaryInputHapticsStrategy) {
    this.rotaryInputHapticsStrategy = rotaryInputHapticsStrategy;

    if (rotaryInputHapticsStrategy != RotaryInputHapticsStrategy.OFF
        && rotaryInputHapticsHelper == null) {
      rotaryInputHapticsHelper = new RotaryInputHapticsHelper(context, this);
    }
    if (rotaryInputHapticsHelper != null) {
      rotaryInputHapticsHelper.setFixedIntervalHapticsEnabled(
          rotaryInputHapticsStrategy == RotaryInputHapticsStrategy.ON_FIXED_INTERVAL);
      rotaryInputHapticsHelper.setEndOfListHapticsEnabled(
          rotaryInputHapticsStrategy != RotaryInputHapticsStrategy.OFF);
    }
  }

  /**
   * Returns true if Rotary Scroll functionality can be used. Returns false if the Rotary Scroll
   * system is not available on this device and platform combination.
   */
  public boolean isRotaryScrollFlingAvailable() {
    return rotaryMotionEventProcessor.isRotaryScrollFlingAvailable();
  }

  /**
   * Returns true if Rotary Scroll has been explicitly enabled for this instance.
   *
   * <p>NOTE: Rotary fling will still not work when enabled if {@link
   * #isRotaryScrollFlingAvailable()} is false.
   */
  public boolean isRotaryScrollFlingEnabled() {
    return rotaryMotionEventProcessor.isRotaryScrollFlingEnabled();
  }

  /**
   * Set whether to enable or disable the rotary scroll fling for this list
   *
   * <p>NOTE: Rotary fling will still not work when enabled if {@link
   * #isRotaryScrollFlingAvailable()} is false.
   */
  public void setRotaryScrollFlingEnabled(boolean enabled) {
    rotaryMotionEventProcessor.setRotaryScrollFlingEnabled(enabled);
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();

    if (showStartupAnimation) {
      runStartupAnimation();
    }

    getViewTreeObserver().addOnPreDrawListener(paddingPreDrawListener);
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();

    getViewTreeObserver().removeOnPreDrawListener(paddingPreDrawListener);
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    super.onLayout(changed, l, t, r, b);
    fader.updateFade();
  }

  @Override
  public void invalidate() {
    super.invalidate();
    fader.updateFade();
  }

  /** get the height of the child at index, or 0 if the child doesn't exist */
  private int getChildHeightIfExists(int index) {
    return getChildAt(index) == null ? 0 : getChildAt(index).getHeight();
  }

  private void init(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    TypedArray a =
        context
            .getTheme()
            .obtainStyledAttributes(attrs, R.styleable.FadingWearableRecyclerView, defStyleAttr, 0);

    try {
      setShowStartupAnimation(
          a.getBoolean(R.styleable.FadingWearableRecyclerView_showStartUpAnimation, false));
      setEdgeItemsCenteringEnabled(
          a.getBoolean(R.styleable.FadingWearableRecyclerView_centerEdgeItems, false));
      setSkipFirstItemWhenCentering(
          a.getBoolean(R.styleable.FadingWearableRecyclerView_skipFirstItemWhenCentering, false));
      topPaddingPercent =
          min(a.getFloat(R.styleable.FadingWearableRecyclerView_topPaddingPercent, NO_VALUE), 1);
      bottomPaddingPercent =
          min(a.getFloat(R.styleable.FadingWearableRecyclerView_bottomPaddingPercent, NO_VALUE), 1);

      float startPaddingPercent =
          min(a.getFloat(R.styleable.FadingWearableRecyclerView_startPaddingPercent, NO_VALUE), 1);
      float endPaddingPercent =
          min(a.getFloat(R.styleable.FadingWearableRecyclerView_endPaddingPercent, NO_VALUE), 1);
      setStartAndEndPadding(startPaddingPercent, endPaddingPercent);

      setSnappingEnabled(
          a.getBoolean(R.styleable.FadingWearableRecyclerView_snappingEnabled, snappingEnabled));

      RotaryInputHapticsStrategy rotaryHapticsStrategy =
          RotaryInputHapticsStrategy.values()[
              a.getInteger(
                  R.styleable.FadingWearableRecyclerView_rotaryInputHapticStrategy,
                  DEFAULT_ROTARY_HAPTICS_STRATEGY.ordinal())];
      setRotaryInputHapticsStrategy(rotaryHapticsStrategy);

      setRotaryScrollFlingEnabled(
          a.getBoolean(R.styleable.FadingWearableRecyclerView_rotaryScrollFlingEnabled, true));
    } finally {
      a.recycle();
    }

    // TODO(b/203563439): Remove WearableFadingItemAnimator once fix once we fix the vinson test.
    // See b/205005285.
    setItemAnimator(FadingWearableItemAnimator.create(fader));

    // Using custom EdgeEffects is deprecated starting with Wear T.
    boolean validVersion = VERSION.SDK_INT <= VERSION_CODES.R;
    if (validVersion && getOverScrollMode() != OVER_SCROLL_NEVER) {
      setEdgeEffectFactory(
          new RecyclerView.EdgeEffectFactory() {
            @Override
            protected EdgeEffect createEdgeEffect(RecyclerView view, int direction) {
              return new OverScrollEdgeEffect(view.getContext(), FadingWearableRecyclerView.this);
            }
          });
    }
  }

  /**
   * Listens to squeeze scale changes when overscrolling vertically in order to update the scroll
   * offset and extent.
   *
   * <p>This allows for a squeezing overscroll effect on the scroll bar. When not overscrolling this
   * should change to 0 so that the regular scroll offset and extent calculations are used.
   *
   * @param squeezeScaleY - The scale at which the vertical scroll is being squeezed when
   *     overscrolling.
   */
  @Override
  public void onSqueezeScaleYChanged(float squeezeScaleY) {
    this.squeezeScaleY = squeezeScaleY;
  }

  @Override
  public int computeVerticalScrollOffset() {
    int scrollOffset = super.computeVerticalScrollOffset();
    if (scrollOffset > 0) {
      return (int) ((cachedScrollExtent * squeezeScaleY) + scrollOffset);
    }
    return scrollOffset;
  }

  @Override
  public int computeVerticalScrollExtent() {
    // TODO(b/223262648): Need to enlarge the scroll bar to ensure there is space to squeeze.
    int scrollExtent = super.computeVerticalScrollExtent();
    this.cachedScrollExtent = scrollExtent;
    return (int) (scrollExtent * (1f - squeezeScaleY));
  }

  private void setStartAndEndPadding(float startPaddingPercent, float endPaddingPercent) {
    int startPadding =
        startPaddingPercent == NO_VALUE
            ? getPaddingStart()
            : max((int) (screenWidth * startPaddingPercent), 0);
    int endPadding =
        endPaddingPercent == NO_VALUE
            ? getPaddingEnd()
            : max((int) (screenWidth * endPaddingPercent), 0);
    setPaddingRelative(startPadding, getPaddingTop(), endPadding, getPaddingBottom());
  }

  @Override
  public void onScrolled(int dx, int dy) {
    super.onScrolled(dx, dy);

    fader.updateFade();
    if (rotaryInputHapticsHelper != null) {
      rotaryInputHapticsHelper.onScrolled(dx, dy);
    }

    if (rotaryInputLinearSnapHelper != null) {
      rotaryInputLinearSnapHelper.onScrolled();
    }
  }

  /** Run the startup animation: scrolling onto screen */
  public void runStartupAnimation() {
    setTranslationY(startupAnimationOffset);
    ViewPropertyAnimator animator = animate().withLayer();
    animator.translationY(0f);
    if (startupAnimatorListener != null) {
      animator.setListener(startupAnimatorListener);
    }
    animator.setUpdateListener(valueAnimator -> fader.updateFade());
    animator.setStartDelay(startupAnimationDelay);
    animator.setDuration(startupAnimationDuration);
    animator.setInterpolator(startupAnimationInterpolator);
    animator.start();

    AlphaAnimation alphaAnimation = new AlphaAnimation(0.0f, 1.0f);
    alphaAnimation.setDuration(Durations.QUICK);
    alphaAnimation.setFillAfter(true);
    alphaAnimation.setStartOffset(startupAnimationDelay);
    alphaAnimation.setInterpolator(startupAlphaAnimationInterpolator);
    startAnimation(alphaAnimation);
  }

  /**
   * Evaluate the provided generic MotionEvent.
   *
   * <p>For a hardware scroll, there are two types of scrolling effects: Linear and Fling.
   *
   * <p>A linear scroll is always handled when a hardware scroll occurs, providing an incremental
   * linear scroll of the given list.
   *
   * <p>If the hardware scroll is rotated significantly, then the fling scroll can be handled,
   * causing the scroll to naturally "fling" the list.
   *
   * <p>NOTE: Handling both scroll effects at the same time does not impact the overall fling
   * experience because the linear scroll is insignificant compared to the required amount of
   * rotation to trigger a fling scroll effect. (The linear scroll handles slower rotation scenarios
   * gracefully.)
   *
   * @param event MotionEvent that is being handled
   * @return True if handled. False otherwise.
   */
  @Override
  public boolean onGenericMotionEvent(MotionEvent event) {
    boolean eventHandled = testingWrapper.onSuperGenericMotionEvent(this, event);

    // Evaluate if fling should be handled in addition to the genericMotionEvent.
    eventHandled |= rotaryMotionEventProcessor.onGenericMotionEvent(event);

    if (rotaryInputLinearSnapHelper != null) {
      // TODO(b/260582559): Fix helper to work properly with RSB Fling implementation
      rotaryInputLinearSnapHelper.onGenericMotionEvent(event);
    }
    if (rotaryInputHapticsHelper != null) {
      rotaryInputHapticsHelper.onGenericMotionEvent(event);
    }
    return eventHandled;
  }

  /**
   * Intentional two levels of indirection to properly test that the super.onGenericMotionEvent is
   * called properly.
   *
   * @param event MotionEvent that is being handled
   * @return True if handled. False otherwise.
   */
  private boolean onSuperGenericMotionEvent(MotionEvent event) {
    return super.onGenericMotionEvent(event);
  }

  // Suppress ClickableViewAccessibility warning as we're offloading to RecyclerView's onTouchEvent
  // to handle click events for accessibility.
  @SuppressWarnings("ClickableViewAccessibility")
  @Override
  public boolean onTouchEvent(MotionEvent event) {
    boolean value = super.onTouchEvent(event);
    if (rotaryInputHapticsHelper != null) {
      rotaryInputHapticsHelper.onTouchEvent(event);
    }

    if (rotaryInputLinearSnapHelper != null) {
      rotaryInputLinearSnapHelper.onTouchEvent();
    }

    return value;
  }

  /**
   * Implements the required RotaryVelocityView.
   *
   * <p>NOTE: This is a nested implementation of the interface to avoid requiring all consumers to
   * have a dependency on {@link RotaryMotionEventProcessor}.
   */
  private class FadingRotaryVelocityView implements RotaryMotionEventProcessor.RotaryVelocityView {

    @Override
    public float getScaledVerticalScrollFactor() {
      return scaledVerticalScrollFactor;
    }

    @Override
    public int getMinFlingVelocity() {
      return FadingWearableRecyclerView.this.getMinFlingVelocity();
    }

    @Override
    public void stopScroll() {
      FadingWearableRecyclerView.this.stopScroll();
    }

    @Override
    public boolean fling(int velocityX, int velocityY) {
      return FadingWearableRecyclerView.this.fling(velocityX, velocityY);
    }
  }

  @VisibleForTesting
  static class TestingWrapper {
    /**
     * Used to confirm that the super.onGenericMotionEvent is called properly in tests.
     *
     * @param event the current MotionEvent
     */
    public boolean onSuperGenericMotionEvent(FadingWearableRecyclerView view, MotionEvent event) {
      return view.onSuperGenericMotionEvent(event);
    }
  }
}
