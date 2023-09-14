package com.google.android.clockwork.common.wearable.wearmaterial.picker;

import static com.google.android.clockwork.common.wearable.wearmaterial.picker.PaddingMeasurements.offsetChildrenToEnsureCentering;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.round;
import static java.lang.Math.signum;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnPreDrawListener;
import androidx.annotation.MainThread;
import com.google.android.clockwork.common.wearable.wearmaterial.util.BlendContentDrawable;

import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This is a {@link RecyclerView} that also has these behaviors:
 *
 * <ul>
 *   <li>Requires that its adapter must be sub-class of {@link CenteredRecyclerViewAdapter}.
 *   <li>Assigns a vertical or horizontal {@link LinearLayoutManager} to itself, depending on its
 *       configured orientation.
 *   <li>Adds padding to the top and bottom (vertical orientation) or to the left and right
 *       (horizontal orientation) in such a way that the first and the last items in this
 *       CenteredRecyclerView will be centered.
 *   <li>Modifies the scrolling behavior so that an item is always centered in its layout. In
 *       addition, the scrolling has a bouncy, spring-like over-scroll behavior.
 *   <li>Provides a registration for callbacks that will be called when its highlighted (and
 *       centered) item changes.
 * </ul>
 */
public class CenteredRecyclerView extends RecyclerView {

  /** Interface for a callback to be invoked when the highlighted item has changed. */
  public interface OnHighlightedItemIndexChanged {

    /**
     * Called when a highlighted item for a {@link CenteredRecyclerView} has changed.
     *
     * @param index The index of the newly highlighted item
     */
    void onHighlightedItemIndexChanged(int index);
  }

  /**
   * Interface for a callback to be invoked when the highlighted item has been double-tapped
   * (selected) in Talkback-mode.
   */
  interface OnItemA11ySelected {

    /** Called when an item for a {@link CenteredRecyclerView} has been selected. */
    void onItemA11ySelected();
  }

  private static final int VIGNETTE_EXPANDED_ALPHA = 0;
  private static final int VIGNETTE_COLLAPSED_ALPHA = 255;
  private static final int NO_SIZE = -1;
  private static final int DIRECTION_BACKWARD = -1;
  private static final int DIRECTION_FORWARD = 1;

  private final Handler uiHandler;

  /** A set of listeners that must be notified when the currently highlighted item changes. */
  private final Set<OnHighlightedItemIndexChanged> onHighlightedItemIndexChangedListeners =
      new ArraySet<>();

  /**
   * A set of listeners that must be notified when an item has been double-tapped in Talkback-mode.
   */
  private final Set<OnItemA11ySelected> onItemA11ySelectedListeners = new ArraySet<>();

  /** Allows the measurement for the necessary vertical or horizontal padding. */
  private final PaddingMeasurements paddingMeasurements;

  // Suppression needed to correct linter: Method ref is called later, not during construction.
  @SuppressWarnings("nullness:methodref.receiver.bound")
  private final Runnable setupCenteredPadding = this::setupCenteredPadding;

  @SuppressWarnings("nullness:methodref.receiver.bound")
  OnPreDrawListener onPreDraw = this::onPreDraw;

  private final Animator vignetteExpandAnimation;

  private final Animator vignetteCollapseAnimation;

  private final A11yManager a11yManager;

  /** Remembers for which height or width the necessary 'centering' padding has been added. */
  private int sizeForWhichItemsAreCentered = NO_SIZE;

  /** Determines if this RecyclerView must ensure that a highlighted items is centered or not. */
  private boolean areItemsCentered;

  /** The amount of padding to use so that highlighted items are properly centered. */
  private int desiredPadding;

  /**
   * A helper that allows items of this RecyclerView to always scroll/snap to the center of its
   * layout. *
   */
  private @Nullable CenteredLinearSnapHelper snapHelper = null;

  /** The {@code scrollSpeedFactor} that will be assigned to the {@link #snapHelper}. */
  private float scrollSpeedFactor = 1f;

  /** The {@code scrollDecelerationFactor} that will be assigned to the {@link #snapHelper}. */
  private float scrollDecelerationFactor = 1f;

  /** If set to false, this recycler view cannot be flung by the user. */
  private boolean isFlingEnabled = true;

  private @Nullable BlendContentDrawable blendedVignetteDrawable;

  private @Nullable Animator vignetteAnimation;

  private @Nullable OnTouchListener touchInputSubscriber;

  public CenteredRecyclerView(Context context) {
    this(context, null, 0);
  }

  public CenteredRecyclerView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  // Suppression needed to satisfy linter:
  // Constructor's code is safe but I can't annotate the super-class' methods that are called by it.
  @SuppressWarnings({"nullness:argument", "nullness:method.invocation"})
  @SuppressLint("ClickableViewAccessibility") // No need to handle clicks when a11y is enabled.
  public CenteredRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);

    a11yManager = new A11yManager(context);

    uiHandler = new Handler();

    setClipToPadding(false);

    vignetteExpandAnimation =
        AnimatorInflater.loadAnimator(getContext(), R.animator.wear_vignette_expansion);
    vignetteCollapseAnimation =
        AnimatorInflater.loadAnimator(getContext(), R.animator.wear_vignette_collapse);

    // A 'null' item-animator is a no-op item-animator.
    // The no-op animator prevents unwanted default list-item animations.
    setItemAnimator(null);

    LayoutManager.Properties properties = LayoutManager.getProperties(context, attrs, defStyle, 0);

    paddingMeasurements =
        PaddingMeasurements.getAndPreparePaddingMeasurements(this, properties.orientation);

    initializeAttributes(attrs, defStyle);

    setOverScrollMode(OVER_SCROLL_NEVER);

    setAccessibilityDelegateCompat(new CenteredRecyclerViewAccessibilityDelegate(this));
  }

  /**
   * Handles the OnPreDraw by allowing the initial draw only after any desired padding has been
   * applied.
   */
  private boolean onPreDraw() {
    boolean isReadyToBeDrawn =
        !areItemsCentered || paddingMeasurements.getPaddingSize(this) == desiredPadding;
    if (isReadyToBeDrawn) {
      // ViewTreeObserver needs to be fetched from the root view. See b/37068521#comment3
      this.getRootView().getViewTreeObserver().removeOnPreDrawListener(onPreDraw);
    }
    return false;
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    // ViewTreeObserver needs to be fetched from the root view. See b/37068521#comment3
    this.getRootView().getViewTreeObserver().removeOnPreDrawListener(onPreDraw);
  }

  private void initializeAttributes(@Nullable AttributeSet attrs, int defStyleAttr) {
    Context context = getContext();

    TypedArray a =
        context.obtainStyledAttributes(attrs, R.styleable.CenteredRecyclerView, defStyleAttr, 0);
    try {
      scrollSpeedFactor =
          a.getFloat(R.styleable.CenteredRecyclerView_scrollSpeedFactor, scrollSpeedFactor);

      scrollDecelerationFactor =
          a.getFloat(
              R.styleable.CenteredRecyclerView_scrollDecelerationFactor, scrollDecelerationFactor);

      isFlingEnabled =
          a.getBoolean(R.styleable.CenteredRecyclerView_isFlingEnabled, isFlingEnabled);

      Drawable blendedVignetteDrawable = a.getDrawable(R.styleable.CenteredRecyclerView_vignette);
      if (blendedVignetteDrawable instanceof BlendContentDrawable) {
        this.blendedVignetteDrawable = (BlendContentDrawable) blendedVignetteDrawable;
        this.blendedVignetteDrawable.setContentProvider(super::dispatchDraw);
        this.blendedVignetteDrawable.setCallback(this);

        PickerVignetteDrawable vignetteDrawable = getVignetteDrawable(this.blendedVignetteDrawable);
        vignetteExpandAnimation.setTarget(vignetteDrawable);
        vignetteCollapseAnimation.setTarget(vignetteDrawable);
      }
    } finally {
      a.recycle();
    }
  }

  private void assignCenteredPadding() {
    if (sizeForWhichItemsAreCentered == NO_SIZE) {
      sizeForWhichItemsAreCentered = 0;
    }
    int size = paddingMeasurements.getSize(this);
    if (sizeForWhichItemsAreCentered != size && getChildCount() > 0) {
      sizeForWhichItemsAreCentered = size;

      areItemsCentered = canScroll();

      desiredPadding =
          PaddingMeasurements.getDesiredPadding(paddingMeasurements, this, areItemsCentered);

      uiHandler.removeCallbacks(setupCenteredPadding);
      uiHandler.postAtFrontOfQueue(setupCenteredPadding);
    }
  }

  /**
   * Assigns the appropriate amount of vertical or horizontal padding so that the first and last
   * item of this RecyclerView will be able to scroll to its center, or, if no scrolling is needed,
   * that <em>all</em> items are visible and placed in its center.
   */
  private void setupCenteredPadding() {
    boolean refreshScrollPosition = false;

    if (paddingMeasurements.getPaddingSize(this) != desiredPadding) {
      paddingMeasurements.setPaddingSize(this, desiredPadding);
      refreshScrollPosition = true;
    }

    if (areItemsCentered && snapHelper == null) {
      // Center the child view in the parent.
      // Makes sure that the scrolling of the items snaps the items to the center.
      snapHelper = new CenteredLinearSnapHelper(scrollSpeedFactor, scrollDecelerationFactor);
      refreshScrollPosition = true;
    }

    CenteredRecyclerViewAdapter<?> adapter = (CenteredRecyclerViewAdapter<?>) getAdapter();
    if (adapter != null) {
      // Remove the scroll-listener, and add it (back) only if the items are centered, i.e. this
      // RecyclerView scrolls to allow the user to browse through all list-items.
      clearScrollListeners(adapter);
      if (areItemsCentered) {
        setScrollListeners(adapter);
      }
    }

    if (refreshScrollPosition) {
      ensureScrollPosition();
    }
  }

  /** Returns whether this {@code CenteredRecyclerView} allows the user to scroll or not. */
  boolean canScroll() {
    return PaddingMeasurements.canScroll(paddingMeasurements, this);
  }

  public boolean areItemsCentered() {
    return areItemsCentered;
  }

  private void clearScrollListeners(CenteredRecyclerViewAdapter<?> adapter) {
    removeOnScrollListener(adapter.onScrollListener);
    if (snapHelper != null) {
      snapHelper.attachToRecyclerView(null);
    }
  }

  private void setScrollListeners(CenteredRecyclerViewAdapter<?> adapter) {
    // Do not change the order; first add this scroll-listener, then attach to the RecyclerView.
    addOnScrollListener(adapter.onScrollListener);
    if (snapHelper != null) {
      snapHelper.attachToRecyclerView(this);
    }
  }

  
  @Override
  public boolean fling(int velocityX, int velocityY) {
    if (isFlingEnabled) {
      return super.fling(velocityX, velocityY);
    }
    return a11yManager.isA11yEnabled() && flingWhenA11yEnabled(velocityX, velocityY);
  }

  private boolean flingWhenA11yEnabled(int velocityX, int velocityY) {
    int minFlingVelocity = getMinFlingVelocity();
    if (abs(velocityX) < minFlingVelocity && abs(velocityY) < minFlingVelocity) {
      return false;
    }
    int direction = (velocityX < 0 || velocityY < 0) ? DIRECTION_BACKWARD : DIRECTION_FORWARD;
    setHighlightedItemIndex(getHighlightedItemIndex() + direction);
    return true;
  }

  @Override
  public void smoothScrollToPosition(int position) {
    LayoutManager layoutManager = getLayoutManager();
    if (layoutManager == null) {
      return;
    }

    CenteredSmoothScroller scroller = new CenteredSmoothScroller(getContext(), scrollSpeedFactor);
    scroller.setTargetPosition(position);
    layoutManager.startSmoothScroll(scroller);
  }

  /**
   * Returns the index of the currently highlighted (and centered) item.
   *
   * <p>If there is none, {@link #NO_POSITION}, is returned.<br>
   * Note that this is only possible if this RecyclerView has no adapter or before this RecyclerView
   * has been laid out.
   */
  public int getHighlightedItemIndex() {
    CenteredRecyclerViewAdapter<?> adapter = (CenteredRecyclerViewAdapter<?>) getAdapter();
    return adapter != null ? adapter.getHighlightedItemIndex() : NO_POSITION;
  }

  /**
   * Changes the highlighted item to the one with the given index.
   *
   * <p>This will scroll the list so that the highlighted item will be centered.
   *
   * @param index The index of the new item that must be highlighted
   */
  public void setHighlightedItemIndex(int index) {
    CenteredRecyclerViewAdapter<?> adapter = (CenteredRecyclerViewAdapter<?>) getAdapter();
    if (adapter == null || index < 0 || index >= adapter.getItemCount()) {
      return;
    }

    if (adapter.setTargetHighlightedItemIndex(index)) {
      if (snapHelper != null) {
        snapHelper.mustWaitForSecondStateIdleEvent = false;
      }

      RecyclerViewUtils.snapScrollToPosition(this, index);
    }
  }

  /**
   * Registers a {@link OnHighlightedItemIndexChanged} listener.
   *
   * <p>If there is a currently highlighted/centered item, the given listener will be called
   * immediately,
   */
  @MainThread
  public void addOnHighlightedItemIndexChangedListener(OnHighlightedItemIndexChanged listener) {
    int highlightedItemIndex = getHighlightedItemIndex();
    if (highlightedItemIndex != NO_POSITION) {
      listener.onHighlightedItemIndexChanged(highlightedItemIndex);
    }

    onHighlightedItemIndexChangedListeners.add(listener);
  }

  /** Unregisters a {@link OnHighlightedItemIndexChanged} listener. */
  @MainThread
  public void removeOnHighlightedItemIndexChangedListener(OnHighlightedItemIndexChanged listener) {
    onHighlightedItemIndexChangedListeners.remove(listener);
  }

  /**
   * Notifies all the registered {@link OnHighlightedItemIndexChanged} listeners that the current
   * highlighted item has changed.
   *
   * @param index The index of the newly highlighted item
   */
  void notifyHighlightedItemIndexChanged(int index) {
    for (OnHighlightedItemIndexChanged listener : onHighlightedItemIndexChangedListeners) {
      listener.onHighlightedItemIndexChanged(index);
    }
  }

  /** Registers a {@link OnItemA11ySelected} listener. */
  @MainThread
  void addOnItemA11ySelectedListener(OnItemA11ySelected listener) {
    onItemA11ySelectedListeners.add(listener);
  }

  /** Unregisters a {@link OnItemA11ySelected} listener. */
  @MainThread
  void removeOnItemA11ySelectedListener(OnItemA11ySelected listener) {
    onItemA11ySelectedListeners.remove(listener);
  }

  /**
   * Notifies all the registered {@link OnItemA11ySelected} listeners that an item has been
   * double-tapped in Talkback-mode.
   */
  void notifyItemA11ySelected() {
    for (OnItemA11ySelected listener : onItemA11ySelectedListeners) {
      listener.onItemA11ySelected();
    }
  }

  // Suppression needed to correct linter:
  // "rawtypes" is needed, but was still marked as redundant.
  // This is due to the fact that the 'RecyclerView.setAdapter' defines its 'adapter' parameter
  // to be of the raw-type 'RecyclerView.Adapter' (and not as a parameterized type).
  @SuppressWarnings({"rawtypes", "RedundantSuppression"})
  @Override
  public void setAdapter(@androidx.annotation.Nullable Adapter adapter) {
    CenteredRecyclerViewAdapter<?> oldAdapter = (CenteredRecyclerViewAdapter<?>) getAdapter();
    if (adapter == oldAdapter) {
      return;
    }

    sizeForWhichItemsAreCentered = NO_SIZE;
    areItemsCentered = false;
    desiredPadding = 0;

    if (oldAdapter != null) {
      oldAdapter.clear();
      clearScrollListeners(oldAdapter);
      snapHelper = null;
    }

    // ViewTreeObserver needs to be fetched from the root view. See b/37068521#comment3
    ViewTreeObserver viewTreeObserver = this.getRootView().getViewTreeObserver();
    viewTreeObserver.removeOnPreDrawListener(onPreDraw);
    viewTreeObserver.addOnPreDrawListener(onPreDraw);

    super.setAdapter(adapter);
  }

  void setTouchInputSubscriber(OnTouchListener subscriber) {
    touchInputSubscriber = subscriber;
  }

  /**
   * Updates the layout and state of this {@code CenteredRecyclerView} and its {@code
   * CenteredRecyclerViewAdapter} after its layout manager has successfully laid out all its
   * children. It ensures that the current highlighted item view is properly centered.
   */
  void ensureLayoutIsCentered() {
    if (!areItemsCentered) {
      return;
    }

    LayoutManager layoutManager = getLayoutManager();
    CenteredRecyclerViewAdapter<?> adapter = (CenteredRecyclerViewAdapter<?>) getAdapter();
    if (layoutManager == null || adapter == null) {
      return;
    }

    int highlightedItemIndex = getHighlightedItemIndex();
    View highlightedItemView = layoutManager.findViewByPosition(highlightedItemIndex);
    if (highlightedItemView != null) {
      if (offsetChildrenToEnsureCentering(paddingMeasurements, this, highlightedItemView)) {
        // This is needed because sometimes the call to 'offsetChildrenToBeCentered' can leave a
        // child-view in a state that when the user scrolls the child-view, it jumps back to its
        // scroll-start position. A call to 'forceLayout' prevents this situation.
        forceLayout();

        // Makes sure the ItemDecorations are drawn at the correct coordinates.
        invalidate();
      }

      if (adapter.setVisibleHighlightedItemIndex(highlightedItemIndex)) {
        notifyHighlightedItemIndexChanged(highlightedItemIndex);
      }
    }
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    ensureScrollPosition();
  }

  /** Ensures that this {@code CenteredRecyclerView} highlights the correct item view. */
  private void ensureScrollPosition() {
    CenteredRecyclerViewAdapter<?> adapter = (CenteredRecyclerViewAdapter<?>) getAdapter();
    if (adapter != null) {
      int index = max(adapter.getHighlightedItemIndex(), 0);
      if (adapter.setTargetHighlightedItemIndex(index)) {
        if (snapHelper != null) {
          snapHelper.mustWaitForSecondStateIdleEvent = false;
        }
        adapter.cancelScroll();
        scrollToPosition(index);
      }
    }
  }

  /**
   * Returns true if the {@link #snapHelper} sends a second {@code STATE_IDLE} scroll-event before a
   * highlighted index has been changed.
   */
  boolean mustWaitForSecondStateIdleEvent() {
    return snapHelper == null || snapHelper.mustWaitForSecondStateIdleEvent;
  }

  /**
   * Resets the {@link #snapHelper}'s {@code mustWaitForSecondStateIdleEvent} back to its default
   * value {@code true}.
   */
  void setMustWaitForSecondStateIdleEvent() {
    if (snapHelper != null) {
      snapHelper.mustWaitForSecondStateIdleEvent = true;
    }
  }

  /**
   * Adds an amount of extra layout space to the {@code RecyclerView}'s layout-manager. This will
   * cause off-screen content/list-items to be rendered as well.
   *
   * <p>In itself, a large amount of {@code extraLayoutSpace} (in pixels) is not detrimental to
   * performance. However, in combination <em>with</em> a large amount of list-items (the {@link
   * Adapter#getItemCount()} returns a large number), performance will be negatively impacted.
   */
  public void setExtraLayoutSpace(int extraLayoutSpace) {
    LayoutManager layoutManager = getLayoutManager();
    if (layoutManager instanceof CenteredLinearLayoutManager) {
      ((CenteredLinearLayoutManager) layoutManager).setExtraLayoutSpace(extraLayoutSpace);
    }
  }

  /**
   * Expands the list's vignette so that all items are visible.
   *
   * @param animate If true, animate the expansion. Otherwise, just change it immediately.
   */
  public void expandVignette(boolean animate) {
    PickerVignetteDrawable vignetteDrawable = getVignetteDrawable();
    if (vignetteDrawable == null) {
      return;
    }

    if (vignetteAnimation != null) {
      vignetteAnimation.cancel();
      vignetteAnimation = null;
    }

    if (animate) {
      vignetteExpandAnimation.start();
      vignetteAnimation = vignetteExpandAnimation;
    } else {
      vignetteDrawable.setVignetteAlpha(VIGNETTE_EXPANDED_ALPHA);
    }
  }

  /**
   * Collapses the list's vignette so that only the center item is visible.
   *
   * @param animate If true, animate the collapse. Otherwise, just change it immediately.
   */
  public void collapseVignette(boolean animate) {
    PickerVignetteDrawable vignetteDrawable = getVignetteDrawable();
    if (vignetteDrawable == null) {
      return;
    }

    if (vignetteAnimation != null) {
      vignetteAnimation.cancel();
      vignetteAnimation = null;
    }

    if (animate) {
      vignetteCollapseAnimation.start();
      vignetteAnimation = vignetteCollapseAnimation;
    } else {
      vignetteDrawable.setVignetteAlpha(VIGNETTE_COLLAPSED_ALPHA);
    }
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    super.onLayout(changed, l, t, r, b);
    if (changed || sizeForWhichItemsAreCentered == NO_SIZE) {
      assignCenteredPadding();
      updateVignetteBounds();
    }
  }

  @Override
  protected boolean verifyDrawable(Drawable who) {
    return who == blendedVignetteDrawable || super.verifyDrawable(who);
  }

  @Override
  protected void dispatchDraw(Canvas canvas) {
    BlendContentDrawable blendContentDrawable = this.blendedVignetteDrawable;
    if (blendContentDrawable == null) {
      super.dispatchDraw(canvas);
    } else {
      blendContentDrawable.draw(canvas);
    }
  }

  // Calling super.onTouchEvent which should handle click events for accessibility.
  @SuppressWarnings("ClickableViewAccessibility")
  @Override
  public boolean onTouchEvent(MotionEvent event) {
    boolean value = super.onTouchEvent(event);
    if (touchInputSubscriber != null) {
      touchInputSubscriber.onTouch(this, event);
    }
    return value;
  }

  private void updateVignetteBounds() {
    if (getChildCount() < 2) {
      return;
    }

    BlendContentDrawable blendedVignetteDrawable = this.blendedVignetteDrawable;
    if (blendedVignetteDrawable != null) {
      blendedVignetteDrawable.setBounds(0, 0, getWidth(), getHeight());

      // The area of the highlighted item, which is in the middle of this view and has the
      // height of a list-item, should be clear of any vignetting.
      PickerVignetteDrawable vignetteDrawable = getVignetteDrawable(blendedVignetteDrawable);
      if (vignetteDrawable != null) {
        int viewSize = paddingMeasurements.getSize(this);
        int itemSize = paddingMeasurements.getSize(getChildAt(0));
        vignetteDrawable.setClearArea((viewSize - itemSize) / 2, (viewSize + itemSize) / 2);
      }
    }
  }

  private @Nullable PickerVignetteDrawable getVignetteDrawable() {
    BlendContentDrawable blendedVignetteDrawable = this.blendedVignetteDrawable;
    return blendedVignetteDrawable == null ? null : getVignetteDrawable(blendedVignetteDrawable);
  }

  private static @Nullable PickerVignetteDrawable getVignetteDrawable(
      BlendContentDrawable blendContentDrawable) {
    Drawable drawable = blendContentDrawable.getDrawable();
    if (drawable instanceof PickerVignetteDrawable) {
      return (PickerVignetteDrawable) drawable;
    }

    return null;
  }

  /** Returns the distance between the {@code itemView}'s center and this RecyclerView's center. */
  int getDistanceFromCenter(@Nullable View itemView) {
    if (itemView == null) {
      return 0;
    }

    int center = PaddingMeasurements.getDesiredPadding(paddingMeasurements, this, true);

    int itemCenter = paddingMeasurements.getCenter(itemView);
    float translation = paddingMeasurements.getTranslation(itemView);
    int itemTranslation = (int) (round(abs(translation)) * signum(translation));

    return itemCenter + itemTranslation - center;
  }
}
