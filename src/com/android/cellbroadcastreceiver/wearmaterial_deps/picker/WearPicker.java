package com.google.android.clockwork.common.wearable.wearmaterial.picker;

import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;
import static android.view.View.MeasureSpec.UNSPECIFIED;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static com.google.android.clockwork.common.wearable.wearmaterial.util.ThemeUtils.applyThemeOverlay;
import static com.google.android.clockwork.common.wearable.wearmaterial.util.TypedArrayUtils.getStringAttr;
import static java.lang.Math.max;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.view.accessibility.AccessibilityManager.AccessibilityStateChangeListener;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.DrawableRes;
import androidx.annotation.MainThread;
import androidx.annotation.VisibleForTesting;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.google.android.clockwork.common.wearable.wearmaterial.button.WearCircularButton;
import com.google.android.clockwork.common.wearable.wearmaterial.picker.CenteredRecyclerView.OnHighlightedItemIndexChanged;
import com.google.android.clockwork.common.wearable.wearmaterial.util.TextUtils;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This shows a horizontal list of {@link WearPickerColumn}s allowing a user to select a value from
 * each of these {@link WearPickerColumn}s.
 */
public final class WearPicker extends ConstraintLayout {

  /**
   * Interface for a callback to be invoked when the active {@link WearPickerColumn} has changed.
   */
  public interface OnActivePickerColumnChanged {

    /** Called when the {@link WearPickerColumn} at the given {@code columnIndex} becomes active. */
    void onActivePickerColumnChanged(int columnIndex);
  }

  /**
   * Interface for a callback to be invoked when the highlighted item of a {@link WearPickerColumn}
   * has changed.
   */
  public interface OnPickerColumnItemChanged {

    /**
     * Called when the item at the given {@code columnItemIndex} of the {@link WearPickerColumn} at
     * the given {@code columnIndex} becomes highlighted.
     */
    void onPickerColumnItemChanged(int columnIndex, int columnItemIndex);
  }

  /** Interface for a callback to be invoked when the Action Button is clicked. */
  public interface OnActionButtonClickListener {

    /** Called when the Action Button of the {@code pickerRow} is clicked. */
    void onClick(WearPicker wearPicker);
  }

  /** Interface for a callback to be invoked when the text of the Toggle View has changed. */
  public interface OnToggleTextChanged {

    /**
     * Called when the text in the Toggle View has changed.
     *
     * @param toggleTextIndex The index of the text in the {@code List} provided by the last call to
     *     {@link #setToggleTexts(List)}.
     */
    void onToggleTextChanged(int toggleTextIndex);
  }

  private static final int ACTION_NONE = 0;
  private static final int ACTION_DONE = 1;
  private static final int ACTION_FORCED_PROGRESS = 2;

  private static final int MEASURE_SPEC_UNBOUND = makeMeasureSpec(0, UNSPECIFIED);
  private static final long A11Y_FOCUS_ACTION_DELAY_MS = 600;

  private final Handler uiHandler;

  private TextView toggleView;
  private TextView centerColumnLabel;

  /**
   * The horizontally scrolling {@link CenteredRecyclerView} that shows all the {@code
   * PickerColumn}s.
   */
  private CenteredRecyclerView pickerColumns;

  /** The Action Button allowing the user to confirm their selection(s). */
  private WearCircularButton actionButton;

  /** The {@code TextView} used to render the separator between the {@code PickerColumn}s. */
  private TextView separatorStringRenderer;

  private int separatorSize = LayoutParams.WRAP_CONTENT;

  /**
   * A nullable non-empty list of {@code CharSequence}s whose texts are shown in the {@link
   * #toggleView}.
   */
  private @Nullable List<CharSequence> toggleTexts;

  /** The index of the element in {@link #toggleTexts} being shown in the {@link #toggleView}. */
  private int toggleTextIndex = NO_POSITION;

  /**
   * If the {@link #toggleView} has 'wrap_content' for its layout-width, we'd need to measure all
   * the texts provided by {@link #setToggleTexts(List)} to avoid changes to its width when the user
   * toggles from one value to the next. This field remembers whether this measurement is necessary
   * or not.
   */
  private boolean measureToggleViewForAllToggleTexts;

  /**
   * Draws the contents of the {@link #separatorStringRenderer} between the {@code PickerColumn}s.
   */
  private @Nullable HorizontalTextItemDecoration rowItemDecoration;

  private @Nullable OnActionButtonClickListener onActionButtonClickListener;

  /**
   * Determines the action to be taken when {@link #actionButton} is clicked. It can have one of the
   * {@code ACTION_xxx} values.
   */
  private int actionButtonAction = ACTION_NONE;

  private @Nullable Drawable actionButtonDrawable;

  private @Nullable Drawable forcedProgressButtonDrawable;

  private boolean isHapticsEnabled = true;
  private boolean isA11yEnabled;

  private CharSequence actionButtonNextColumnContentDescription;
  private CharSequence actionButtonDoneContentDescription;
  private CharSequence toggleActionAnnouncement;
  private CharSequence actionButtonActionAnnouncement;
  private CharSequence nextColumnActionAnnouncement;

  private AccessibilityStateChangeListener a11yStateChangeListener;
  private OnPreDrawListener onPreDraw;
  private Runnable setA11yFocusToTopRunnable;

  /**
   * A set of listeners that must be notified when the highlighted item in a {@code PickerColumn}
   * changes.
   */
  private final Map<OnActivePickerColumnChanged, OnHighlightedItemIndexChanged>
      onActivePickerColumnChangedListeners = new ArrayMap<>();

  private final Set<OnPickerColumnItemChanged> onPickerColumnItemChangedListeners =
      new ArraySet<>();

  private final Set<OnToggleTextChanged> onToggleTextChangedListeners = new ArraySet<>();

  @SuppressWarnings("nullness:methodref.receiver.bound")
  private final Runnable updateActionButtonVisibility = this::updateActionButton;

  private final A11yManager a11yManager;

  public WearPicker(Context context) {
    this(context, null);
  }

  public WearPicker(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, R.attr.wearPickerStyle);
  }

  // Suppress these warnings, because at some point super-class methods are called
  // which cannot be annotated (with '@UnknownInitialization' for example).
  @SuppressWarnings({"nullness:argument", "nullness:method.invocation"})
  public WearPicker(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(applyThemeOverlay(context, defStyleAttr, R.style.WearPickerDefault), attrs, defStyleAttr);

    a11yManager = new A11yManager(context);

    uiHandler = new Handler();
    isA11yEnabled = a11yManager.isA11yEnabled();

    initialize(attrs, defStyleAttr);
    setWillNotDraw(true);
  }

  private void initialize(@Nullable AttributeSet attrs, int defStyleAttr) {
    Context context = getContext();

    inflateViews(context);

    initializeThemedAttributes(context);
    initializeAttributes(context, attrs, defStyleAttr);

    a11yStateChangeListener = this::onA11yEnabledChanged;
    onPreDraw = this::onPreDraw;
    setA11yFocusToTopRunnable = this::setA11yFocusToTop;
  }

  private boolean onPreDraw() {
    WearPickerColumnAdapter<?> adapter = getColumnAdapter(getActivePickerColumnIndex());
    if (adapter == null) {
      return true;
    }
    int index = adapter.getHighlightedItemIndex();
    // Only animate if we're not starting from index 0
    if (index != 0) {
      setPickerColumnItem(getActivePickerColumnIndex(), index - 1);
      postDelayed(() -> setPickerColumnItem(getActivePickerColumnIndex(), index), 100);
    }
    // Remove the PreDraw listener so we don't animate after the first render
    this.getRootView().getViewTreeObserver().removeOnPreDrawListener(onPreDraw);
    return true;
  }

  // dereference of possibly-null reference toggleView.getLayoutParams()
  @SuppressWarnings("nullness:dereference.of.nullable")
  private void inflateViews(Context context) {
    LayoutInflater.from(context).inflate(R.layout.wear_picker_row, this, true);

    toggleView = findViewById(R.id.wear_picker_toggle);
    centerColumnLabel = findViewById(R.id.picker_center_column_label);
    pickerColumns = findViewById(R.id.wear_picker_column_list);
    actionButton = findViewById(R.id.wear_picker_action_button);
    separatorStringRenderer = findViewById(R.id.wear_separator_text);

    pickerColumns.setHasFixedSize(true);
    pickerColumns.getRecycledViewPool().setMaxRecycledViews(RowAdapter.PICKER_COLUMN_VIEW_TYPE, 0);
    pickerColumns.setExtraLayoutSpace(context.getResources().getDisplayMetrics().widthPixels);

    toggleView.setOnClickListener(this::onToggleViewClicked);
    measureToggleViewForAllToggleTexts = toggleView.getLayoutParams().width < 0;

    toggleView.setSelected(true);
    centerColumnLabel.setActivated(true);

    addOnActivePickerColumnIndexChangedListener(this::onActivePickerColumnChanged);

    actionButton.setOnClickListener(this::onActionButtonClick);

    setupAccessibility();
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    a11yManager.addStateChangeListener(a11yStateChangeListener);
    this.getRootView().getViewTreeObserver().addOnPreDrawListener(onPreDraw);
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    a11yManager.removeStateChangeListener(a11yStateChangeListener);
    this.getRootView().getViewTreeObserver().removeOnPreDrawListener(onPreDraw);
  }

  private void onToggleViewClicked(View view) {
    List<CharSequence> toggleTexts = this.toggleTexts;
    if (toggleTexts == null) {
      return;
    }

    toggleTextIndex = (toggleTextIndex + 1) % toggleTexts.size();

    notifyToggleTextChanged();
  }

  private void onActivePickerColumnChanged(int columnIndex) {
    RowAdapter rowAdapter = (RowAdapter) pickerColumns.getAdapter();
    if (rowAdapter == null) {
      return;
    }

    rowAdapter.notifyDataSetChanged();
    updateActionButton();
    notifyCenterColumnLabelChanged();

    if (a11yManager.isA11yEnabled()) {
      removeCallbacks(setA11yFocusToTopRunnable);
      postDelayed(setA11yFocusToTopRunnable, A11Y_FOCUS_ACTION_DELAY_MS);
    }
  }

  private void setA11yFocusToTop() {
    View view = toggleView.getVisibility() == View.VISIBLE ? toggleView : centerColumnLabel;
    view.performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null);
  }

  private void onActionButtonClick(View actionButton) {
    switch (actionButtonAction) {
      case ACTION_DONE:
        if (onActionButtonClickListener != null) {
          onActionButtonClickListener.onClick(this);
        }
        break;

      case ACTION_FORCED_PROGRESS:
        int currentActiveColumn = getActivePickerColumnIndex();
        if (currentActiveColumn >= 0 && currentActiveColumn < getColumnAdapterCount() - 1) {
          setActivePickerColumnIndex(currentActiveColumn + 1);
        }
        break;

      default:
        break;
    }
  }

  private void initializeThemedAttributes(Context context) {
    Theme theme = context.getTheme();
    setActionButtonContentDescription(
        getStringAttr(theme, R.attr.contentDescriptionForActionButton));
    setNextColumnButtonContentDescription(
        getStringAttr(theme, R.attr.contentDescriptionForNextColumnButton));
    setActionButtonActionForA11y(getStringAttr(theme, R.attr.accessibilityActionForActionButton));
    setNextColumnActionForA11y(getStringAttr(theme, R.attr.accessibilityActionForNextColumnButton));
    setToggleActionForA11y(getStringAttr(theme, R.attr.accessibilityActionForToggleChange));
  }

  private void initializeAttributes(
      Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WearPicker, defStyleAttr, 0);
    try {
      setActionButtonDrawable(a.getDrawable(R.styleable.WearPicker_wearActionButton));
      setForcedProgressDrawable(a.getDrawable(R.styleable.WearPicker_wearForcedProgressButton));
      setSeparatorString(
          a.getString(R.styleable.WearPicker_wearSeparatorString),
          a.getLayoutDimension(R.styleable.WearPicker_wearSeparatorSize, separatorSize));
      isHapticsEnabled =
          a.getBoolean(R.styleable.WearPicker_isPickerHapticsEnabled, isHapticsEnabled);
    } finally {
      a.recycle();
    }
  }

  @VisibleForTesting
  void setupAccessibility() {
    toggleView.setAccessibilityDelegate(
        new AccessibilityDelegate() {

          @Override
          public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(host, info);
            info.setClassName(Button.class.getName());
            info.setSelected(false);
            info.setClickable(true);
            // Replace the toggle-view's click-action utterance with the configured announcement.
            info.removeAction(AccessibilityAction.ACTION_CLICK);
            info.addAction(
                new AccessibilityAction(
                    AccessibilityAction.ACTION_CLICK.getId(), toggleActionAnnouncement));
          }
        });

    actionButton.setAccessibilityDelegate(
        new AccessibilityDelegate() {

          @Override
          public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(host, info);

            boolean isForcedProgress = actionButtonAction == ACTION_FORCED_PROGRESS;
            info.setClassName(Button.class.getName());
            info.setContentDescription(
                isForcedProgress
                    ? actionButtonNextColumnContentDescription
                    : actionButtonDoneContentDescription);
            info.setClickable(true);
            // Replace the action-button's click-action utterance with the configured announcement.
            info.removeAction(AccessibilityAction.ACTION_CLICK);
            info.addAction(
                new AccessibilityAction(
                    AccessibilityAction.ACTION_CLICK.getId(),
                    isForcedProgress
                        ? nextColumnActionAnnouncement
                        : actionButtonActionAnnouncement));
          }
        });
  }

  private void setActionButtonContentDescription(@Nullable CharSequence contentDescription) {
    actionButtonDoneContentDescription =
        contentDescription == null
            ? getResources().getString(R.string.wear_picker_a11y_action_button_done)
            : contentDescription;
  }

  private void setNextColumnButtonContentDescription(@Nullable CharSequence contentDescription) {
    actionButtonNextColumnContentDescription =
        contentDescription == null
            ? getResources().getString(R.string.wear_picker_a11y_action_button_accept)
            : contentDescription;
  }

  private void setActionButtonActionForA11y(@Nullable CharSequence actionAnnouncement) {
    actionButtonActionAnnouncement =
        actionAnnouncement == null
            ? getResources().getString(R.string.wear_picker_a11y_action_done)
            : actionAnnouncement;
  }

  private void setNextColumnActionForA11y(@Nullable CharSequence actionAnnouncement) {
    nextColumnActionAnnouncement =
        actionAnnouncement == null
            ? getResources().getString(R.string.wear_picker_a11y_action_next_column)
            : actionAnnouncement;
  }

  private void setToggleActionForA11y(@Nullable CharSequence actionAnnouncement) {
    toggleActionAnnouncement =
        actionAnnouncement == null
            ? getResources().getString(R.string.wear_picker_a11y_action_toggle)
            : actionAnnouncement;
  }

  @Override
  public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
    super.onInitializeAccessibilityNodeInfo(info);

    if (Build.VERSION.SDK_INT >= VERSION_CODES.P) {
      // The WearPicker is a Pane and its value is the name/label of the current column and
      // the content of that column's currently highlighted item.
      CharSequence paneTitle = getPaneTitleForA11y();
      if (paneTitle != null) {
        info.setPaneTitle(paneTitle);
      }
    }
  }

  /** Returns the proper pane-title for accessibility. */
  @TargetApi(VERSION_CODES.O)
  private @Nullable CharSequence getPaneTitleForA11y() {
    int columnIndex = getActivePickerColumnIndex();
    WearPickerColumnAdapter<?> adapter = columnIndex < 0 ? null : getColumnAdapter(columnIndex);
    if (adapter == null) {
      return null;
    }
    return android.text.TextUtils.isEmpty(adapter.getColumnDescription())
        ? adapter.getLabel()
        : adapter.getColumnDescription();
  }

  /**
   * Provides the {@link WearPickerColumnAdapter}s for the row of {@link WearPickerColumn}s.
   *
   * <p>Each item in the {@code pickerColumnAdapters} represents one {@code PickerColumn}.
   *
   * @see #getColumnAdapterCount()
   */
  public void setColumnAdapters(List<? extends WearPickerColumnAdapter<?>> pickerColumnAdapters) {
    setRowAdapter(
        new RowAdapter(
            pickerColumnAdapters,
            this::notifyPickerColumnItemChanged,
            this::notifyPickerColumnItemA11ySelected,
            this::setActivePickerColumnIndex));
  }

  @VisibleForTesting
  void setRowAdapter(RowAdapter rowAdapter) {
    rowAdapter.setShowLabelOnCollapsed(toggleTexts != null);
    rowAdapter.setHapticsEnabled(isHapticsEnabled);
    rowAdapter.setA11yEnabled(isA11yEnabled);
    pickerColumns.setAdapter(rowAdapter);
    if (rowAdapter.getItemCount() > 0) {
      setActivePickerColumnIndex(0);
    }
  }

  /**
   * Returns the list of texts that are shown (one at at time) in the Toggle View.
   *
   * <p>The returned list is a nullable non-empty list.
   *
   * @see #setToggleTexts(List)
   */
  public @Nullable List<CharSequence> getToggleTexts() {
    return toggleTexts;
  }

  /**
   * Sets the list of texts to be shown (one at a time) in the Toggle View.
   *
   * @see #getToggleTexts()
   */
  public void setToggleTexts(@Nullable List<CharSequence> toggleTexts) {
    if (Objects.equals(this.toggleTexts, toggleTexts)) {
      return;
    }

    if (toggleTexts != null && !toggleTexts.isEmpty()) {
      this.toggleTexts = toggleTexts;
      this.toggleTextIndex = 0;
      if (measureToggleViewForAllToggleTexts) {
        measureAllToggleTexts(toggleTexts);
      }
    } else {
      this.toggleTexts = null;
      this.toggleTextIndex = NO_POSITION;
    }

    RowAdapter rowAdapter = (RowAdapter) pickerColumns.getAdapter();
    if (rowAdapter != null) {
      rowAdapter.setShowLabelOnCollapsed(this.toggleTexts != null);
      rowAdapter.notifyDataSetChanged();
    }

    notifyToggleTextChanged();
    notifyCenterColumnLabelChanged();
  }

  // dereference of possibly-null reference layoutParams
  // incompatible argument for parameter arg0 of setLayoutParams.
  @SuppressWarnings({"nullness:dereference.of.nullable", "nullness:argument"})
  private void measureAllToggleTexts(List<CharSequence> toggleTexts) {
    if (toggleTexts.size() < 2) {
      return;
    }

    int maxWidth = 0;
    for (CharSequence text : toggleTexts) {
      toggleView.setText(text);
      toggleView.measure(MEASURE_SPEC_UNBOUND, MEASURE_SPEC_UNBOUND);
      maxWidth = max(maxWidth, toggleView.getMeasuredWidth());
    }

    ViewGroup.LayoutParams layoutParams = toggleView.getLayoutParams();
    layoutParams.width = maxWidth;
    toggleView.setLayoutParams(layoutParams);
  }

  /**
   * Specifies the text that needs to be rendered between the {@code PickerColumns}.
   *
   * @param separatorText The text that must be rendered between the items or {@code null} if
   *     nothing must be rendered
   * @param separatorSize The amount of space between the {@code PickerColumns} in which the {@code
   *     separatorText} will be rendered. Its default value is {@link LayoutParams#WRAP_CONTENT}
   */
  @MainThread
  public void setSeparatorString(@Nullable CharSequence separatorText, int separatorSize) {
    if (rowItemDecoration == null
        || this.separatorSize != separatorSize
        || TextUtils.contentsMayDiffer(separatorText, separatorStringRenderer.getText())) {
      updateItemDecoration(separatorText, separatorSize);
    }
  }

  private void updateItemDecoration(@Nullable CharSequence separatorText, int separatorSize) {
    if (rowItemDecoration != null) {
      pickerColumns.removeItemDecoration(rowItemDecoration);
      rowItemDecoration = null;
    }

    separatorStringRenderer.setText(separatorText);
    this.separatorSize = separatorSize;

    if (!isA11yEnabled && separatorText != null && separatorText.length() > 0) {
      rowItemDecoration = new HorizontalTextItemDecoration(separatorStringRenderer, separatorSize);
      pickerColumns.addItemDecoration(rowItemDecoration);
    }
  }

  /**
   * Specifies the text that needs to be rendered between the {@code PickerColumns}.
   *
   * @param separatorText The text that must be rendered between the items or {@code null} if
   *     nothing must be rendered
   */
  @MainThread
  public void setSeparatorString(@Nullable CharSequence separatorText) {
    setSeparatorString(separatorText, LayoutParams.WRAP_CONTENT);
  }

  /**
   * Returns the number of {@link WearPickerColumnAdapter}s currently shown in this {@code
   * PickerRow}.
   *
   * @see #setColumnAdapters(List)
   */
  public int getColumnAdapterCount() {
    RowAdapter adapter = (RowAdapter) pickerColumns.getAdapter();
    return adapter != null ? adapter.getItemCount() : 0;
  }

  /**
   * Returns the index of the currently active {@link WearPickerColumn}.
   *
   * <p>It will return the value {@code NO_POSITION} when this index cannot be determined.
   */
  public int getActivePickerColumnIndex() {
    return pickerColumns.getHighlightedItemIndex();
  }

  /** Makes the {@code PickerColumn} at the provided {@code index} active. */
  public void setActivePickerColumnIndex(int index) {
    pickerColumns.setHighlightedItemIndex(index);
  }

  /**
   * Registers a new {@code listener} that is called when the currently active {@link
   * WearPickerColumn} changes.
   */
  @MainThread
  public void addOnActivePickerColumnIndexChangedListener(OnActivePickerColumnChanged listener) {
    OnHighlightedItemIndexChanged pickerColumnListener =
        putIfAbsent(
            onActivePickerColumnChangedListeners, listener, listener::onActivePickerColumnChanged);

    pickerColumns.addOnHighlightedItemIndexChangedListener(pickerColumnListener);
  }

  /**
   * Unregisters a registered {@code listener}.
   *
   * @see #addOnActivePickerColumnIndexChangedListener(OnActivePickerColumnChanged)
   */
  @MainThread
  public void removeOnActivePickerColumnIndexChangedListener(OnActivePickerColumnChanged listener) {
    OnHighlightedItemIndexChanged pickerColumnListener =
        onActivePickerColumnChangedListeners.remove(listener);
    if (pickerColumnListener != null) {
      pickerColumns.removeOnHighlightedItemIndexChangedListener(pickerColumnListener);
    }
  }

  /**
   * Returns the index of the highlighted item of the {@link WearPickerColumn} at the given {@code
   * columnIndex}.
   *
   * <p>It will return the value {@code NO_POSITION} when this index cannot be determined.
   */
  public int getPickerColumnItem(int columnIndex) {
    WearPickerColumnAdapter<?> columnAdapter = getColumnAdapter(columnIndex);
    return (columnAdapter != null) ? columnAdapter.getHighlightedItemIndex() : NO_POSITION;
  }

  /**
   * Highlights an item of one of the {@link WearPickerColumn}s.
   *
   * @param columnIndex The index of the {@code PickerColumn} whose item will be highlighted
   * @param columnItemIndex the index of item to be highlighted
   */
  public void setPickerColumnItem(int columnIndex, int columnItemIndex) {
    RowAdapter rowAdapter = (RowAdapter) pickerColumns.getAdapter();
    if (rowAdapter == null) {
      return;
    }
    WearPickerColumnAdapter<?> columnAdapter = rowAdapter.getColumnAdapter(columnIndex);
    if (columnAdapter == null || columnAdapter.getHighlightedItemIndex() == columnItemIndex) {
      return;
    }

    columnAdapter.postPickerColumnItemIndex(columnItemIndex);
    rowAdapter.notifyItemChanged(columnIndex);
    // Alas, this call to 'requestLayout()' below is required.
    // For some reason, just calling 'rowAdapter.notifyItemChanged(columnIndex)' is not enough.
    // Calling 'rowAdapter.notifyDataSetChanged()' works as well, but calling
    // 'rowAdapter.notifyItemChanged(columnIndex)' followed by a 'requestLayout()' seems to involve
    // the least amount of code to be executed.
    pickerColumns.requestLayout();
  }

  /**
   * Registers a new {@code listener} that is called when a {@link WearPickerColumn}'s highlighted
   * item changes.
   */
  @MainThread
  public void addOnPickerColumnItemChangedListener(OnPickerColumnItemChanged listener) {
    onPickerColumnItemChangedListeners.add(listener);
  }

  /**
   * Unregisters a registered {@code listener}.
   *
   * @see #addOnPickerColumnItemChangedListener(OnPickerColumnItemChanged)
   */
  @MainThread
  public void removeOnPickerColumnItemChangedListener(OnPickerColumnItemChanged listener) {
    onPickerColumnItemChangedListeners.remove(listener);
  }

  @VisibleForTesting
  void notifyPickerColumnItemChanged(int columnIndex, int itemIndex) {
    for (OnPickerColumnItemChanged listener : onPickerColumnItemChangedListeners) {
      listener.onPickerColumnItemChanged(columnIndex, itemIndex);
    }
  }

  @VisibleForTesting
  void notifyPickerColumnItemA11ySelected() {
    if (!a11yManager.isA11yEnabled()) {
      return;
    }

    actionButton.performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null);
  }

  /**
   * Returns the index of the text shown in the Toggle View. The index is its position in the list
   * provided by the last call to {@link #setToggleTexts(List)}.
   */
  public int getToggleTextIndex() {
    return toggleTextIndex;
  }

  /**
   * Changes the {@code index} of the text to be shown in the Toggle View. The index is its position
   * in the list provided by the last call to {@link #setToggleTexts(List)}. The Toggle View will be
   * updated to show the new text.
   *
   * <p>If no list of toggle-texts is provided or the {@code index} is less than 0 or larger than
   * the last index in the provided list of toggle-texts, this method will do nothing.
   *
   * @see #setToggleTexts(List)
   */
  public void setToggleTextIndex(int index) {
    if (toggleTexts == null || index < 0 || index >= toggleTexts.size()) {
      return;
    }

    if (this.toggleTextIndex != index) {
      this.toggleTextIndex = index;
      notifyToggleTextChanged();
    }
  }

  /**
   * Registers a new {@code listener} that is called when the index of the text shown in the Toggle
   * View changes.
   */
  @MainThread
  public void addOnToggleTextChangedListener(OnToggleTextChanged listener) {
    onToggleTextChangedListeners.add(listener);
  }

  /**
   * Unregisters a registered {@code listener}.
   *
   * @see #addOnToggleTextChangedListener(OnToggleTextChanged)
   */
  @MainThread
  public void removeOnToggleTextChangedListener(OnToggleTextChanged listener) {
    onToggleTextChangedListeners.remove(listener);
  }

  private void notifyToggleTextChanged() {
    List<CharSequence> toggleTexts = this.toggleTexts;
    if (toggleTexts == null || toggleTextIndex == NO_POSITION) {
      toggleView.setVisibility(View.GONE);
      toggleView.setText("");
      return;
    }

    toggleView.setVisibility(View.VISIBLE);
    toggleView.setText(toggleTexts.get(toggleTextIndex));

    for (OnToggleTextChanged listener : onToggleTextChangedListeners) {
      listener.onToggleTextChanged(toggleTextIndex);
    }
  }

  private void notifyCenterColumnLabelChanged() {
    WearPickerColumnAdapter<?> columnAdapter = getColumnAdapter(getActivePickerColumnIndex());
    CharSequence columnLabel = columnAdapter != null ? columnAdapter.getLabel() : null;
    CharSequence columnLabelDescription = getPaneTitleForA11y();

    if (toggleView.getVisibility() == View.VISIBLE) {
      centerColumnLabel.setVisibility(View.GONE);

      if (Build.VERSION.SDK_INT >= VERSION_CODES.P) {
        // The column-label will be augmented with the content of the currently highlighted
        // item of the column. See onInitializeAccessibilityNodeInfo, which handles this
        // augmentation.
        setAccessibilityPaneTitle(columnLabelDescription);
      }
    } else {
      centerColumnLabel.setVisibility(View.VISIBLE);
      centerColumnLabel.setText(columnLabel);
      centerColumnLabel.setContentDescription(columnLabelDescription);
    }
  }

  private @Nullable WearPickerColumnAdapter<?> getColumnAdapter(int position) {
    RowAdapter rowAdapter = (RowAdapter) pickerColumns.getAdapter();
    if (rowAdapter == null) {
      return null;
    }

    return rowAdapter.getColumnAdapter(position);
  }

  /**
   * Changes the Action Button's icon to show the provided {@code drawable}.
   *
   * <p>If the {@code drawable} is {@code null}, then the Action Button will be hidden.
   */
  public void setActionButtonDrawable(@Nullable Drawable drawable) {
    actionButtonDrawable = drawable;
    updateActionButton();
  }

  /**
   * Changes the Action Button's icon to show the provided {@code resourceId}.
   *
   * <p>If the {@code resourceId} is {@code 0}, then the Action Button will be hidden.
   */
  public void setActionButtonResource(@DrawableRes int resourceId) {
    if (resourceId != 0) {
      setActionButtonDrawable(getResources().getDrawable(resourceId, getContext().getTheme()));
    } else {
      setActionButtonDrawable(null);
    }
  }

  public void setOnActionButtonClickListener(@Nullable OnActionButtonClickListener listener) {
    onActionButtonClickListener = listener;
  }

  /**
   * Changes the Forced Progress Button's icon to show the provided {@code drawable}.
   *
   * <p>As long as the Action Button is hidden, the Forced Progress Button will remain hidden as
   * well.
   *
   * <p>If the {@code drawable} is {@code null}, then the Forced Progress Button will not be shown.
   */
  public void setForcedProgressDrawable(@Nullable Drawable drawable) {
    forcedProgressButtonDrawable = drawable;
    updateActionButton();
  }

  /**
   * Changes the Forced Progress Button's icon to show the provided {@code resourceId}.
   *
   * <p>As long as the Action Button remains hidden, the Forced Progress Button will remain hidden
   * as well.
   *
   * <p>If the {@code resourceId} is {@code 0}, then the Forced Progress Button will not be shown.
   */
  public void setForcedProgressResource(@DrawableRes int resourceId) {
    if (resourceId != 0) {
      setForcedProgressDrawable(getResources().getDrawable(resourceId, getContext().getTheme()));
    } else {
      setForcedProgressDrawable(null);
    }
  }

  /**
   * Returns true if the {@code PickerRow} will show the Forced Progress Button until the last
   * {@code PickerColumn} in the row becomes active.
   */
  public boolean hasForcedProgress() {
    return getColumnAdapterCount() > 1
        && pickerColumns.canScroll()
        && actionButtonDrawable != null
        && forcedProgressButtonDrawable != null;
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);

    // When the layout changes, forced-progress may not be needed if all PickerColumns fit.
    uiHandler.removeCallbacks(updateActionButtonVisibility);
    uiHandler.postAtFrontOfQueue(updateActionButtonVisibility);
  }

  private void updateActionButton() {
    if (actionButtonDrawable == null) {
      // Without an Action Button icon or with an empty adapter, hide both buttons.
      actionButtonAction = ACTION_NONE;
      actionButton.setIconDrawable(null);
      actionButton.setVisibility(GONE);
    } else if (hasForcedProgress()) {
      // Show either the Action Button or the Forced Progress Button, based on the index of the
      // currently active PickerColumn.
      boolean showActionButton = getActivePickerColumnIndex() == getColumnAdapterCount() - 1;
      actionButtonAction = showActionButton ? ACTION_DONE : ACTION_FORCED_PROGRESS;
      actionButton.setIconDrawable(
          showActionButton ? actionButtonDrawable : forcedProgressButtonDrawable);
      actionButton.setChecked(showActionButton);
      actionButton.setVisibility(VISIBLE);
    } else {
      actionButtonAction = ACTION_DONE;
      actionButton.setIconDrawable(actionButtonDrawable);
      actionButton.setChecked(true);
      actionButton.setVisibility(VISIBLE);
    }
  }

  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  int getActionButtonAction() {
    return actionButtonAction;
  }

  /**
   * This method has been added because {@link Map#putIfAbsent(Object, Object)} is not available for
   * this project.
   *
   * @see Map#putIfAbsent(Object, Object)
   */
  private static <K, V> V putIfAbsent(Map<K, V> map, K key, V newValue) {
    V value = map.get(key);
    if (value != null) {
      return value;
    }

    map.put(key, newValue);
    return newValue;
  }

  private void onA11yEnabledChanged(boolean enabled) {
    if (enabled == isA11yEnabled) {
      return;
    }
    // It's necessary to keep track of the A11y state with a local variable, because the
    // StateChangeListener is called before A11yManager.isTouchExplorationEnabled() is
    // updated. Without this, every inquiry to a11yManager.isA11yEnabled() would return
    // "false", even though Talkback was just enabled.
    isA11yEnabled = enabled;
    RowAdapter adapter = pickerColumns != null ? ((RowAdapter) pickerColumns.getAdapter()) : null;
    if (adapter == null) {
      return;
    }
    int focusedColumn = getActivePickerColumnIndex();
    int[] columns = new int[getColumnAdapterCount()];
    for (int i = 0; i < columns.length; i++) {
      columns[i] = getPickerColumnItem(i);
    }
    setColumnAdapters(adapter.getClearedPickerColumnAdapters());
    if (rowItemDecoration != null) {
      if (enabled) {
        pickerColumns.removeItemDecoration(rowItemDecoration);
      } else {
        pickerColumns.addItemDecoration(rowItemDecoration);
      }
    }
    setupAccessibility();
    for (int i = 0; i < columns.length; i++) {
      setPickerColumnItem(i, columns[i]);
    }
    setActivePickerColumnIndex(focusedColumn);
    updateItemDecoration(separatorStringRenderer.getText(), separatorSize);
  }
}
