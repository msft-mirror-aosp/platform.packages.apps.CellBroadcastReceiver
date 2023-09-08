package com.google.android.clockwork.common.wearable.wearmaterial.picker;

import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE;
import static android.view.View.MeasureSpec.UNSPECIFIED;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static com.google.android.clockwork.common.wearable.wearmaterial.util.ThemeUtils.applyThemeOverlay;
import static java.lang.Math.abs;
import static java.lang.Math.max;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.LayoutManager;
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.google.android.clockwork.common.wearable.wearmaterial.picker.CenteredRecyclerView.OnHighlightedItemIndexChanged;
import com.google.android.clockwork.common.wearable.wearmaterial.picker.CenteredRecyclerView.OnItemA11ySelected;
import com.google.android.clockwork.common.wearable.wearmaterial.rotaryinput.RotaryInputHapticsHelper;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This layout shows a list of items in a {@link CenteredRecyclerView} and an optional text-label
 * above it. It makes sure that the list of items is centered vertically and that there is enough
 * room for the text-label to be shown.
 *
 * @param <A> The type of the {@link CenteredRecyclerViewAdapter} used by the {@link
 *     CenteredRecyclerView} to render its list items.
 */
public final class WearPickerColumn<A extends CenteredRecyclerViewAdapter<?>> extends FrameLayout {

  /** The appearance of this {@link WearPickerColumn}. */
  public enum ColumnAppearance {
    /** The {@link WearPickerColumn} is expanded. */
    EXPANDED,

    /** The {@link WearPickerColumn} is collapsed. */
    COLLAPSED
  }

  private static final int MEASURE_SPEC_UNBOUND = makeMeasureSpec(0, UNSPECIFIED);

  /**
   * Pre-allocated and used by {@link #onMeasure(int, int)} to keep track of the current state of
   * measurement and to avoid unnecessary object allocations during measurement.
   */
  private final MeasureState tmpMeasureState = new MeasureState();

  /** Used by {@link #onLayout(boolean, int, int, int, int)} to store this view's area. */
  private final Rect tmpLayoutRect = new Rect();

  private final A11yManager a11yManager;

  private TextView label;

  private boolean isHapticsEnabled = true;

  /** The list of items that the user can interact with. */
  private CenteredRecyclerView list;

  /** Transition animation used when setting the appearance to {@link ColumnAppearance#COLLAPSED} */
  private Transition collapseTransition;

  /** Transition animation used when setting the appearance to {@link ColumnAppearance#EXPANDED} */
  private Transition expandTransition;

  /** The appearance of this {@link WearPickerColumn}. */
  private ColumnAppearance columnAppearance = ColumnAppearance.COLLAPSED;

  /** The {@code View} used in measuring the widest width possible. */
  private @Nullable View widestView;

  /** Determines if the label should be shown above the 'collapsed' list or not. */
  private boolean isLabelShownOnCollapsedList;

  private final OnItemTouchListener onItemTouchListener =
      new OnItemTouchListener() {

        private boolean gestureStarted;

        @SuppressWarnings({"nullness:method.invocation", "nullness:dereference.of.nullable"})
        @Override
        public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
          if (isActivated() || a11yManager.isA11yEnabled()) {
            return false;
          }

          int action = motionEvent.getAction();
          if (action == MotionEvent.ACTION_DOWN) {
            gestureStarted = true;
          }

          if (gestureStarted && gestureDetector.onTouchEvent(motionEvent)) {
            // A tap, vertical scroll or vertical fling has been detected. Activate this column.
            gestureStarted = false;
            callOnClick();
          }

          if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            gestureStarted = false;
          }

          return false;
        }

        @Override
        public void onTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {}

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean b) {}
      };

  private final GestureDetector gestureDetector;

  @SuppressWarnings("nullness:methodref.receiver.bound")
  private final WearPickerRotaryListener rotaryListener =
      new WearPickerRotaryListener(this::setHapticsHelper);

  public WearPickerColumn(Context context) {
    this(context, null);
  }

  public WearPickerColumn(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  // Suppress these warnings, because at some point super-class methods are called
  // which cannot be annotated (with '@UnknownInitialization' for example).
  @SuppressWarnings({"nullness:argument", "nullness:method.invocation"})
  public WearPickerColumn(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(applyThemeOverlay(context, defStyleAttr, R.style.WearPickerDefault), attrs, defStyleAttr);

    a11yManager = new A11yManager(context);

    // Create a gesture detector to detect taps, vertical scrolls and vertical flings.
    gestureDetector =
        new GestureDetector(
            getContext(),
            new SimpleOnGestureListener() {

              @Override
              public boolean onSingleTapUp(MotionEvent e) {
                return true;
              }

              // Incompatible parameter type for e1.
              @SuppressWarnings("nullness:override.param.invalid")
              @Override
              public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
                return abs(dx) < abs(dy);
              }

              // Incompatible parameter type for e1.
              @SuppressWarnings("nullness:override.param.invalid")
              @Override
              public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                return abs(vx) < abs(vy);
              }
            });

    initialize(attrs, defStyleAttr);
  }

  private void initialize(@Nullable AttributeSet attrs, int defStyleAttr) {
    Context context = getContext();

    inflateViews(context);
    initializeAttributes(attrs, defStyleAttr, context);
    inflateTransitions(context);

    setWillNotDraw(true);

    list.addOnItemTouchListener(onItemTouchListener);

    if (columnAppearance == ColumnAppearance.COLLAPSED) {
      list.collapseVignette(false);
    } else {
      list.expandVignette(false);
    }
    updateLabelVisibility();
  }

  private void inflateViews(Context context) {
    LayoutInflater.from(context).inflate(R.layout.wear_picker_column, this, true);

    label = findViewById(R.id.wear_picker_column_label);
    list = findViewById(R.id.wear_picker_column_expanded);
    list.setOnGenericMotionListener(rotaryListener);
    list.setHasFixedSize(true);

    LayoutManager layoutManager = list.getLayoutManager();
    if (layoutManager instanceof CenteredLinearLayoutManager) {
      CenteredLinearLayoutManager centerLayoutManager = (CenteredLinearLayoutManager) layoutManager;
      centerLayoutManager.setItemMovedListener(new SkewMovedItemViewListener());
    }
  }

  private void inflateTransitions(Context context) {
    TransitionInflater inflater = TransitionInflater.from(context);
    collapseTransition = inflater.inflateTransition(R.transition.wear_picker_column_collapse);
    expandTransition = inflater.inflateTransition(R.transition.wear_picker_column_expand);
  }

  private void initializeAttributes(
      @Nullable AttributeSet attrs, int defStyleAttr, Context context) {

    TypedArray a =
        context.obtainStyledAttributes(attrs, R.styleable.WearPickerColumn, defStyleAttr, 0);
    try {
      setLabel(a.getString(R.styleable.WearPickerColumn_android_label));
      setHapticsEnabled(
          a.getBoolean(R.styleable.WearPickerColumn_isPickerHapticsEnabled, isHapticsEnabled));
    } finally {
      a.recycle();
    }
  }

  private void setInputEventsSubscribers() {
    Adapter<?> adapter = list.getAdapter();
    if (adapter instanceof CenteredRecyclerViewAdapter) {
      CenteredRecyclerViewAdapter<?> centeredRecyclerViewAdapter =
          (CenteredRecyclerViewAdapter<?>) adapter;
      rotaryListener.setRotaryInputSubscriber(centeredRecyclerViewAdapter);
      list.setTouchInputSubscriber(centeredRecyclerViewAdapter);
    }
  }

  /**
   * Returns the index of the currently highlighted (and centered) item.
   *
   * <p>If there is none, -1, is returned.<br>
   * Note that this is only possible if this PickerColumn has no adapter or before this PickerColumn
   * has been laid out.
   */
  public int getHighlightedIndex() {
    return list.getHighlightedItemIndex();
  }

  /**
   * Changes the highlighted item to the one with the given index.
   *
   * <p>This will scroll the list so that the highlighted item will be centered.
   *
   * @param index The index of the new item that must be highlighted.
   */
  public void setHighlightedIndex(int index) {
    list.setHighlightedItemIndex(index);
  }

  /**
   * Registers a {@link OnHighlightedItemIndexChanged} listener.
   *
   * <p>If there is a currently highlighted/centered item, the given listener will be called
   * immediately,
   */
  public void addOnHighlightedItemIndexChangedListener(OnHighlightedItemIndexChanged listener) {
    list.addOnHighlightedItemIndexChangedListener(listener);
  }

  /** Unregisters a {@link OnHighlightedItemIndexChanged} listener. */
  public void removeOnHighlightedItemIndexChangedListener(OnHighlightedItemIndexChanged listener) {
    list.removeOnHighlightedItemIndexChangedListener(listener);
  }

  /** Registers a {@link OnItemA11ySelected} listener. */
  void addOnItemA11ySelectedListener(OnItemA11ySelected listener) {
    list.addOnItemA11ySelectedListener(listener);
  }

  /** Unregisters a {@link OnItemA11ySelected} listener. */
  void removeOnItemA11ySelectedListener(OnItemA11ySelected listener) {
    list.removeOnItemA11ySelectedListener(listener);
  }

  /**
   * Returns the previously set adapter or null if no adapter is set.
   *
   * @see #setAdapter(CenteredRecyclerViewAdapter)
   */
  @SuppressWarnings({"unchecked", "cast.unsafe"})
  public @Nullable A getAdapter() {
    return (A) list.getAdapter();
  }

  /**
   * Sets a new adapter to provide child views on demand.
   *
   * <p>When the adapter is changed, all existing views are recycled back to the pool. If the pool
   * has only one adapter, it will be cleared.
   *
   * @param adapter The new adapter to set, or null to remove any adapter
   */
  public void setAdapter(@Nullable A adapter) {
    updateWidestViewFromAdapter(adapter);
    list.setAdapter(adapter);
    setInputEventsSubscribers();
  }

  private void updateWidestViewFromAdapter(@Nullable A adapter) {
    Adapter<?> currentAdapter = list.getAdapter();
    if (adapter == currentAdapter) {
      return;
    }

    if (adapter instanceof WearPickerColumnAdapter<?>) {
      WearPickerColumnAdapter<?> pickerColumnAdapter = (WearPickerColumnAdapter<?>) adapter;
      ViewHolder widestViewHolder = pickerColumnAdapter.onCreateWidestViewHolder(this);
      widestView = widestViewHolder != null ? widestViewHolder.itemView : null;
    } else {
      widestView = null;
    }
    requestLayout();
  }

  /** Returns true if the label on top of the collapsed list is shown. */
  public boolean isLabelShownOnCollapsedList() {
    return isLabelShownOnCollapsedList;
  }

  /**
   * Shows or hides the label on top of the collapsed list.
   *
   * @param isLabelShown True to show the label on top of the collapsed list, false to always hide
   *     the label
   */
  public void setIsLabelShownOnCollapsedList(boolean isLabelShown) {
    if (this.isLabelShownOnCollapsedList != isLabelShown) {
      this.isLabelShownOnCollapsedList = isLabelShown;
      updateLabelVisibility();
    }
  }

  private void updateLabelVisibility() {
    int visibility = View.GONE;
    if (isLabelShownOnCollapsedList) {
      // If label is hidden (not visible), we still must take its width into account. Make it
      // INVISIBLE instead of GONE.
      visibility = columnAppearance == ColumnAppearance.COLLAPSED ? View.VISIBLE : View.INVISIBLE;
    }
    label.setVisibility(visibility);
  }

  /** Sets the {@code text} of the label on top of the list. */
  public void setLabel(@Nullable CharSequence text) {
    label.setText(text);
  }

  /** Returns the content description of the column. */
  public @Nullable CharSequence getColumnDescription() {
    return list.getContentDescription();
  }

  /** Sets the content description of the column. */
  public void setColumnDescription(@Nullable CharSequence text) {
    list.setContentDescription(text);
  }

  /** Returns the current text of the label that is on top of the list. */
  public @Nullable CharSequence getLabel() {
    return label.getText();
  }

  /**
   * Returns the appearance of this {@link WearPickerColumn}.
   *
   * @see #setColumnAppearance(ColumnAppearance)
   */
  public ColumnAppearance getColumnAppearance() {
    return columnAppearance;
  }

  /**
   * Sets the {@code appearance} of {@link WearPickerColumn}.
   *
   * @see #getColumnAppearance()
   */
  public void setColumnAppearance(ColumnAppearance appearance) {
    if (this.columnAppearance == appearance) {
      return;
    }

    columnAppearance = appearance;

    boolean runAnimations = isLaidOut();

    if (columnAppearance == ColumnAppearance.COLLAPSED) {
      if (runAnimations) {
        TransitionManager.endTransitions(this);
        TransitionManager.beginDelayedTransition(this, collapseTransition);
      }

      endScrollingAtHighlightedItem();
      list.collapseVignette(runAnimations);
    } else {
      if (runAnimations) {
        TransitionManager.endTransitions(this);
        TransitionManager.beginDelayedTransition(this, expandTransition);
      }

      list.expandVignette(runAnimations);
    }

    updateLabelVisibility();
    requestLayout();
  }

  private void endScrollingAtHighlightedItem() {
    if (list.getScrollState() != SCROLL_STATE_IDLE) {
      list.stopScroll();
      list.smoothScrollToPosition(getHighlightedIndex());
    }
  }

  void setHapticsEnabled(boolean hapticsEnabled) {
    if (hapticsEnabled == isHapticsEnabled) {
      return;
    }

    isHapticsEnabled = hapticsEnabled;

    if (!isHapticsEnabled) {
      setHapticsHelper(null);
    }
  }

  private void setHapticsHelper(@Nullable RotaryInputHapticsHelper hapticsHelper) {
    if (!isHapticsEnabled && hapticsHelper != null) {
      return;
    }

    Adapter<?> adapter = list.getAdapter();
    if (adapter instanceof CenteredRecyclerViewAdapter) {
      ((CenteredRecyclerViewAdapter<?>) adapter).setHapticsHelper(hapticsHelper);
    }
  }

  @Override
  public void dispatchSetActivated(boolean activated) {
    super.dispatchSetActivated(activated);
    if (activated) {
      // Even when setting android:duplicateParentStateEnabled="false", the label gets activated.
      // Set it to false forcefully here.
      label.setActivated(false);
    }
    // Only active (and on-screen) WearPickerColumns can participate in a11y-navigation.
    list.setImportantForAccessibility(
        activated ? View.IMPORTANT_FOR_ACCESSIBILITY_AUTO : View.IMPORTANT_FOR_ACCESSIBILITY_NO);
  }

  /**
   * This is a custom {@link #onMeasure(int, int)} implementation that makes sure the embedded
   * {@link #list} is properly centered within this view and that the {@link #label} is
   * bottom-aligned to the list's top-coordinate.
   *
   * <p>A custom {@code onMeasure} was written because ensuring the centering of the lists without
   * risking the top-label being pushed above this view's top is very expensive when done with
   * existing layouts such as {@link android.widget.LinearLayout}, {@link
   * androidx.constraintlayout.widget.ConstraintLayout}, etc.
   *
   * <p>Note: This implementation causes the layout-xml's abstraction to leak into this method's
   * code. When changing this method's implementation, look at {@link R.layout#wear_picker_column}
   * as well to make sure they keep working well together.
   *
   * @param widthMeasureSpec This view's measure-specification for its width
   * @param heightMeasureSpec This view's measure-specification for its height
   */
  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    tmpMeasureState.reset();

    measureWidestView();
    int heightUsed = measureLabel(widthMeasureSpec, heightMeasureSpec);
    heightUsed += measureList(widthMeasureSpec, heightMeasureSpec);

    tmpMeasureState.applyMeasuredDimensions(this, widthMeasureSpec, heightMeasureSpec, heightUsed);
  }

  /**
   * Measures the {@link #widestView}'s width and assigns it to the width of the layout params of
   * {@link #list}, determining the width of this Picker Column.
   */
  private void measureWidestView() {
    int layoutParamWidth;
    View widestView = this.widestView;
    if (a11yManager.isA11yEnabled()) {
      layoutParamWidth = LayoutParams.MATCH_PARENT;
    } else if (widestView != null) {
      widestView.measure(MEASURE_SPEC_UNBOUND, MEASURE_SPEC_UNBOUND);
      layoutParamWidth = widestView.getMeasuredWidth();
    } else {
      layoutParamWidth = LayoutParams.WRAP_CONTENT;
    }
    updateLayoutWidth(list, layoutParamWidth);
  }

  /**
   * Measures the {@link #label} and returns the reduction of the height that can no longer be used
   * for the remaining measurements.
   *
   * @param widthMeasureSpec This view's measure-specification for its width
   * @param heightMeasureSpec This view's measure-specification for its height
   * @return The reduction in height
   */
  private int measureLabel(int widthMeasureSpec, int heightMeasureSpec) {
    tmpMeasureState.measure(this, widthMeasureSpec, heightMeasureSpec, label, 0);

    // Return twice the label height (space of label above and below the lists),
    // so that the lists will always be at the center of this view without them pushing
    // the label outside the area of this view. If it's not visible, the label should not take up
    // any height.
    return label.getVisibility() == View.VISIBLE ? tmpMeasureState.usedHeight * 2 : 0;
  }

  /**
   * Measures the {@link #list} and returns the reduction of the height that can no longer be used
   * for the remaining measurements.
   *
   * @param widthMeasureSpec This view's measure-specification for its width
   * @param heightMeasureSpec This view's measure-specification for its height
   * @return The reduction in height
   */
  private int measureList(int widthMeasureSpec, int heightMeasureSpec) {
    int heightOfExpandedList =
        tmpMeasureState.measure(this, widthMeasureSpec, heightMeasureSpec, list, 0);
    return max(heightOfExpandedList, getListItemHeight());
  }

  /**
   * Lays out the children measured by {@link #onMeasure(int, int)}.
   *
   * <p>Note: This implementation causes the layout-xml's abstraction to leak into this method's
   * code. When changing this method's implementation, look at {@link R.layout#wear_picker_column}
   * as well to make sure they keep working well together.
   *
   * @see FrameLayout#onLayout(boolean, int, int, int, int)
   */
  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    list.setExtraLayoutSpace((bottom - top) / 2);

    tmpLayoutRect.left = getPaddingLeft();
    tmpLayoutRect.right = right - left - getPaddingRight();

    tmpLayoutRect.top = getPaddingTop();
    tmpLayoutRect.bottom = bottom - top - getPaddingBottom();

    // First layout the lists and get their top-coordinate.
    layoutChild(tmpLayoutRect, 0, list);
    int collapsedTop = (getHeight() - getListItemHeight()) / 2;

    // The 'label' bottom is aligned to top of the collapsed list.
    int labelTopOffset = collapsedTop - label.getMeasuredHeight();
    layoutChild(tmpLayoutRect, labelTopOffset, label);
  }

  private int getListItemHeight() {
    int childCount = list.getChildCount();
    return childCount == 0 ? 0 : list.getChildAt(childCount / 2).getMeasuredHeight();
  }

  /**
   * Lays out the given {@code child} according to the child's {@link LayoutParams} and offsets the
   * {@code child}'s top by the given {@code offsetTop}.
   *
   * <p>This layout calculation is modeled after the {@link FrameLayout#onLayout(boolean, int, int,
   * int, int)}, supporting margins and horizontal and vertical layout {@link Gravity}.
   *
   * @param parentRect The area of this parent-view
   * @param offsetTop The amount of pixels that the given {@code child} must move downwards
   * @param child The child that is being laid-out
   * @return The top-coordinate of the {@code child} that is has been laid-out. If this {@code
   *     child} cannot be laid-out, {@link Integer#MAX_VALUE} will be returned
   */
  @SuppressLint("RtlHardcoded")
  private int layoutChild(Rect parentRect, int offsetTop, View child) {
    if (child.getVisibility() != GONE) {
      LayoutParams lp = (LayoutParams) child.getLayoutParams();

      // dereference of possibly-null reference lp
      @SuppressWarnings("nullness:dereference.of.nullable")
      int gravity = lp.gravity;
      if (gravity == -1) {
        gravity = Gravity.TOP | Gravity.START;
      }

      int layoutDirection = getLayoutDirection();
      int absoluteGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection);
      int verticalGravity = gravity & Gravity.VERTICAL_GRAVITY_MASK;

      int width = child.getMeasuredWidth();
      int childLeft;

      switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
        case Gravity.CENTER_HORIZONTAL:
          childLeft =
              parentRect.left
                  + (parentRect.right - parentRect.left - width) / 2
                  + lp.leftMargin
                  - lp.rightMargin
                  + (width % 2);
          break;
        case Gravity.RIGHT:
          childLeft = parentRect.right - width - lp.rightMargin;
          break;
        case Gravity.LEFT:
        default:
          childLeft = parentRect.left + lp.leftMargin;
      }

      int height = child.getMeasuredHeight();
      int childTop;

      switch (verticalGravity) {
        case Gravity.CENTER_VERTICAL:
          childTop =
              parentRect.top
                  + (parentRect.bottom - parentRect.top - height) / 2
                  + lp.topMargin
                  - lp.bottomMargin
                  + (height % 2);
          break;
        case Gravity.BOTTOM:
          childTop = parentRect.bottom - height - lp.bottomMargin;
          break;
        case Gravity.TOP:
        default:
          childTop = parentRect.top + lp.topMargin;
      }

      child.layout(
          childLeft, offsetTop + childTop, childLeft + width, offsetTop + childTop + height);
      return childTop;
    }

    return Integer.MAX_VALUE;
  }

  // dereference of possibly-null reference layoutParams
  // incompatible argument for parameter arg0 of setLayoutParams.
  @SuppressWarnings({"nullness:dereference.of.nullable", "nullness:argument"})
  private static void updateLayoutWidth(View view, int newLayoutWidth) {
    ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
    layoutParams.width = newLayoutWidth;
    view.setLayoutParams(layoutParams);
  }

  /**
   * This class keeps track of the state of child View measurements during a call to {@link
   * #onMeasure(int, int)}.
   */
  private static class MeasureState {

    /** The current measured maximum width during a call to {@link #onMeasure(int, int)}. */
    private int maxWidth;
    /** The current measured maximum height during a call to {@link #onMeasure(int, int)}. */
    private int maxHeight;
    /**
     * The width used by the latest call to {@link #measure(WearPickerColumn, int, int, View, int)}.
     */
    private int usedWidth;
    /**
     * The height used by the latest call to {@link #measure(WearPickerColumn, int, int, View,
     * int)}.
     */
    private int usedHeight;
    /** The cumulative measure-states of the measured children. */
    private int childState;

    /** Resets this instance. Call this as soon as {@link #onMeasure(int, int)} is called. */
    void reset() {
      maxWidth = 0;
      maxHeight = 0;
      usedWidth = 0;
      usedHeight = 0;
      childState = 0;
    }

    /**
     * Measures the given {@code child} and returns the space (height) that this measured child will
     * occupy.
     *
     * @param parent The parent of the {@code child} being measure
     * @param widthMeasureSpec This parent-view's measure-specification for its width
     * @param heightMeasureSpec This parent-view's measure-specification for its height
     * @param child The child View to be measured
     * @param heightUsed The height already used by other child-views
     * @return The measured height (including layout-margins)
     */
    // dereference of possibly-null reference lp
    @SuppressWarnings("nullness:dereference.of.nullable")
    int measure(
        WearPickerColumn<?> parent,
        int widthMeasureSpec,
        int heightMeasureSpec,
        View child,
        int heightUsed) {
      if (child.getVisibility() != View.GONE) {
        parent.measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, heightUsed);

        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        usedWidth = child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
        usedHeight = child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
        childState = combineMeasuredStates(childState, child.getMeasuredState());
      } else {
        usedWidth = 0;
        usedHeight = 0;
      }

      adjustMaxWidth();
      return usedHeight;
    }

    /** Applies the result of the latest measurement to update the maximum width. */
    private void adjustMaxWidth() {
      maxWidth = max(maxWidth, usedWidth);
    }

    /** Applies the measurement to this {@code parent}'s measured dimensions. */
    void applyMeasuredDimensions(
        WearPickerColumn<?> parent, int widthMeasureSpec, int heightMeasureSpec, int heightUsed) {
      increaseMaxHeight(heightUsed);

      adjustAreaForPadding(parent);

      adjustAreaForMinimumSize(parent);

      setMeasuredDimension(parent, widthMeasureSpec, heightMeasureSpec);
    }

    /** Increases the maximum height by the specified amount of pixels. */
    private void increaseMaxHeight(int heightToAdd) {
      maxHeight += heightToAdd;
    }

    /** Applies the {@code parent}'s padding to adjust the maximum width and height. */
    private void adjustAreaForPadding(View parent) {
      maxWidth += parent.getPaddingLeft() + parent.getPaddingRight();
      maxHeight += parent.getPaddingTop() + parent.getPaddingBottom();
    }

    /** Applies the parent's minimum dimensions to adjust the maximum width and height. */
    private void adjustAreaForMinimumSize(WearPickerColumn<?> parent) {
      maxWidth = max(maxWidth, parent.getSuggestedMinimumWidth());
      maxHeight = max(maxHeight, parent.getSuggestedMinimumHeight());
    }

    /**
     * Sets the current maximum width and height as this view's measured dimensions.
     *
     * @param widthMeasureSpec This parent-view's measure-specification for its width
     * @param heightMeasureSpec This parent-view's measure-specification for its height
     */
    private void setMeasuredDimension(
        WearPickerColumn<?> parent, int widthMeasureSpec, int heightMeasureSpec) {
      int heightState = childState << MEASURED_HEIGHT_STATE_SHIFT;
      parent.setMeasuredDimension(
          resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
          resolveSizeAndState(maxHeight, heightMeasureSpec, heightState));
    }
  }
}
