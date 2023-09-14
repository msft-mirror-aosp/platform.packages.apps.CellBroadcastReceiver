package com.google.android.clockwork.common.wearable.wearmaterial.button;

import static com.google.android.clockwork.common.wearable.wearmaterial.button.WearChipButton.LayoutStrategy.FIXED;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.text.TextUtils;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.VisibleForTesting;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.view.ViewCompat;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A button widget to support the Wear 3 Material Chip styled button. This button in its full form
 * will have an icon, primary text, secondary text and selection controls.
 *
 * <p>All attributes from {@link R.styleable#WearChipButton} are supported.
 *
 * <p>If you want to override the stateful colors for the background and the icon, these are the
 * default ones for a {@code WearChipButton}:
 *
 * <ul>
 *   <li>Background Color
 *       <ul>
 *         <li>Enabled and Checked<br>
 *             {@code ?attr/colorSurface}
 *         <li>Otherwise<br>
 *             {@code ?attr/colorSurface}
 *       </ul>
 *   <li>Background Color for a button with style {@code WearChipButtonAccent.Checkable}
 *       <ul>
 *         <li>Enabled and Checked<br>
 *             {@code ?attr/colorSecondary}
 *         <li>Otherwise<br>
 *             {@code ?attr/colorSurface}
 *       </ul>
 *   <li>Icon Tint
 *       <ul>
 *         <li>Enabled and Checked<br>
 *             No tinting
 *         <li>Otherwise<br>
 *             No tinting
 *       </ul>
 * </ul>
 */
public final class WearChipButton extends WearButton {

  private static final String WEAR_CHIP_BUTTON_TRANSITION = "WearChipButton:Transition";

  /** The type of control shown at the end of this button. */
  public enum ControlType {
    NONE(0),
    CHECKBOX(R.layout.wear_chip_checkbox),
    RADIO(R.layout.wear_chip_radio),
    SWITCH(R.layout.wear_chip_toggle),
    ICON(R.layout.wear_chip_icon);

    private final int layoutId; // layout to be lazily inflated into selection control container.

    ControlType(int layoutId) {
      this.layoutId = layoutId;
    }
  }

  /** The text alignment on primaryText */
  public enum TextHorizontalPos {
    START,
    CENTER
  }

  /**
   * Representation of the {@code WearChipButton}'s {@link R.attr#layoutStrategy
   * app:layoutStrategy}.
   */
  enum LayoutStrategy {
    FIXED(0),
    STRETCHED(1);

    static LayoutStrategy get(int value) {
      return (value == 1) ? STRETCHED : FIXED;
    }

    final int value;

    LayoutStrategy(int value) {
      this.value = value;
    }
  }

  private static final int DEFAULT_MAX_FIXED_SECONDARY_LINES = 1;
  private static final int DEFAULT_MAX_STRETCHED_SECONDARY_LINES = 2;

  private final LayoutStrategy layoutStrategy;

  private final int defaultPadding;

  private final ConstraintSet constraintSet = new ConstraintSet();

  private final OnPreDrawListener preDrawListener =
      new OnPreDrawListener() {

        @SuppressWarnings("method.invocation.invalid")
        @Override
        public boolean onPreDraw() {
          getRootView().getViewTreeObserver().removeOnPreDrawListener(this);
          isReadyToBeDrawn = true;
          return true;
        }
      };

  /** Flag for if wear chip button has a second action */
  private boolean hasEndOnclickListener;

  private ControlType controlType = ControlType.NONE;
  private TextView primaryText;
  private TextView secondaryText;
  private FrameLayout selectionControlContainer;
  private View startClickableView;
  private @Nullable CompoundButton selectionControl;
  private @Nullable ImageView controlIcon;
  private @Nullable CharSequence selectionControlContentDescription;
  @DrawableRes private int controlIconResId;
  private @Nullable ColorStateList controlIconTint;
  private int primaryTextMaxLines;
  private int primaryTextMaxLinesWithSecondary;
  private int secondaryTextMaxLines;
  private int iconOnlyPadding;
  private int iconAndTextPadding;

  private @Nullable Drawable startAccent;
  private @Nullable Drawable startAccentRtl;
  private @Nullable Drawable endAccent;
  private @Nullable Drawable endAccentRtl;
  private int resolvedLayoutDirection = LAYOUT_DIRECTION_LTR;

  private boolean constraintSetNeedsHorizontalGuidelines = true;

  private boolean enableAutomaticStateTransitions = true;
  private Transition stateTransitions;
  private boolean isReadyToBeDrawn;

  /**
   * If this boolean is true, the layout of this button is being initialized. Defer any relatively
   * expensive constraint and margin updates until later when they will be executed with this
   * boolean set to false.
   */
  private boolean layoutIsBeingInitialized;

  public WearChipButton(Context context) {
    this(context, null);
  }

  public WearChipButton(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, R.attr.wearChipButtonStyle);
  }

  @SuppressWarnings({"nullness:argument", "nullness:method.invocation"})
  public WearChipButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    layoutStrategy = getLayoutStrategy(attrs, defStyleAttr);

    defaultPadding =
        getResources()
            .getDimensionPixelSize(
                hasFixedLayout()
                    ? R.dimen.wear_button_start_padding
                    : R.dimen.wear_multiline_button_start_padding);
    iconOnlyPadding = defaultPadding;
    iconAndTextPadding = defaultPadding;

    createViews();
    initializeTransitions();
    initFromAttributes(attrs, defStyleAttr);
    updateTextMargin(primaryText);
    updateTextMargin(secondaryText);

    initAccessibilityDelegates();
  }

  private LayoutStrategy getLayoutStrategy(@Nullable AttributeSet attrs, int defStyleAttr) {
    TypedArray a =
        getContext()
            .getTheme()
            .obtainStyledAttributes(
                attrs, R.styleable.WearChipButton, defStyleAttr, R.style.WearChipButtonDefault);

    try {
      return LayoutStrategy.get(a.getInt(R.styleable.WearChipButton_layoutStrategy, FIXED.value));
    } finally {
      a.recycle();
    }
  }

  @VisibleForTesting
  boolean hasFixedLayout() {
    return layoutStrategy == FIXED;
  }

  // dereference of possibly-null reference lp
  // incompatible argument for parameter arg0 of setLayoutParams.
  @SuppressWarnings({"nullness:dereference.of.nullable", "nullness:argument"})
  @Override
  protected void initFromAttributes(@Nullable AttributeSet attrs, int defStyleAttr) {
    layoutIsBeingInitialized = true;

    super.initFromAttributes(attrs, defStyleAttr);

    TypedArray a =
        getContext()
            .getTheme()
            .obtainStyledAttributes(
                attrs, R.styleable.WearChipButton, defStyleAttr, R.style.WearChipButtonDefault);

    try {
      primaryTextMaxLines =
          getContext()
              .getResources()
              .getInteger(
                  hasFixedLayout()
                      ? R.integer.wear_button_primary_text_max_lines
                      : R.integer.wear_multiline_button_primary_text_max_lines);

      primaryTextMaxLinesWithSecondary =
          getContext()
              .getResources()
              .getInteger(
                  hasFixedLayout()
                      ? R.integer.wear_button_primary_text_max_lines_with_secondary
                      : R.integer.wear_multiline_button_primary_text_max_lines_with_secondary);

      if (a.hasValue(R.styleable.WearChipButton_primaryText)) {
        setPrimaryText(a.getString(R.styleable.WearChipButton_primaryText));
      }

      if (a.hasValue(R.styleable.WearChipButton_primaryTextAppearance)) {
        int appearance = a.getResourceId(R.styleable.WearChipButton_primaryTextAppearance, 0);
        primaryText.setTextAppearance(appearance);
        primaryText.setMaxLines(primaryTextMaxLines);
      }

      if (a.hasValue(R.styleable.WearChipButton_primaryTextColor)) {
        ColorStateList colorStateList =
            a.getColorStateList(R.styleable.WearChipButton_primaryTextColor);
        primaryText.setTextColor(
            colorStateList != null ? colorStateList : ColorStateList.valueOf(Color.WHITE));
      }

      int posOrdinal = a.getInt(R.styleable.WearChipButton_primaryTextHorizontalPosition, 0);
      if (posOrdinal >= 0 && posOrdinal < TextHorizontalPos.values().length) {
        TextHorizontalPos position = TextHorizontalPos.values()[posOrdinal];
        setPrimaryTextHorizontalPosition(position);
      }

      if (a.hasValue(R.styleable.WearChipButton_primaryTextMaxLines)) {
        primaryTextMaxLines =
            a.getInt(R.styleable.WearChipButton_primaryTextMaxLines, primaryTextMaxLines);
        primaryTextMaxLinesWithSecondary = primaryTextMaxLines;
        primaryText.setMaxLines(primaryTextMaxLines);
      }

      if (a.hasValue(R.styleable.WearChipButton_secondaryText)) {
        setSecondaryText(a.getString(R.styleable.WearChipButton_secondaryText));
      }

      if (a.hasValue(R.styleable.WearChipButton_secondaryTextAppearance)) {
        int appearance = a.getResourceId(R.styleable.WearChipButton_secondaryTextAppearance, 0);
        secondaryText.setTextAppearance(appearance);
      }

      if (a.hasValue(R.styleable.WearChipButton_secondaryTextColor)) {
        ColorStateList colorStateList =
            a.getColorStateList(R.styleable.WearChipButton_secondaryTextColor);
        secondaryText.setTextColor(
            colorStateList != null ? colorStateList : ColorStateList.valueOf(Color.WHITE));
      }

      if (!a.getBoolean(R.styleable.WearChipButton_secondaryTextEnabled, true)) {
        secondaryText.setVisibility(GONE);
      }

      int iconSize = getSizeOf(R.dimen.wear_chip_button_icon_size);
      if (a.hasValue(R.styleable.WearChipButton_wear_icon_size)) {
        iconSize = a.getDimensionPixelSize(R.styleable.WearChipButton_wear_icon_size, iconSize);
        ViewGroup.LayoutParams lp = icon.getLayoutParams();
        lp.width = iconSize;
        lp.height = iconSize;
        icon.setLayoutParams(lp);
      }

      if (a.hasValue(R.styleable.WearChipButton_iconOnlyPadding)) {
        iconOnlyPadding =
            a.getDimensionPixelSize(R.styleable.WearChipButton_iconOnlyPadding, defaultPadding);
      }

      if (a.hasValue(R.styleable.WearChipButton_iconAndTextPadding)) {
        iconAndTextPadding =
            a.getDimensionPixelSize(R.styleable.WearChipButton_iconAndTextPadding, defaultPadding);
      }

      secondaryTextMaxLines =
          a.getInt(
              R.styleable.WearChipButton_secondaryTextMaxLines,
              hasFixedLayout()
                  ? DEFAULT_MAX_FIXED_SECONDARY_LINES
                  : DEFAULT_MAX_STRETCHED_SECONDARY_LINES);
      secondaryText.setMaxLines(secondaryTextMaxLines);

      ControlType controlType = ControlType.NONE;
      int controlTypeOrdinal = a.getInt(R.styleable.WearChipButton_controlType, 0);
      if (controlTypeOrdinal >= 0 && controlTypeOrdinal < ControlType.values().length) {
        controlType = ControlType.values()[controlTypeOrdinal];
      }
      setControlType(controlType);

      if (a.hasValue(R.styleable.WearChipButton_controlIcon)) {
        setControlIcon(a.getResourceId(R.styleable.WearChipButton_controlIcon, 0));
      }
      if (a.hasValue(R.styleable.WearChipButton_selectionControlContentDescription)) {
        setSelectionControlContentDescription(
            a.getString(R.styleable.WearChipButton_selectionControlContentDescription));
      }
      if (a.hasValue(R.styleable.WearChipButton_controlIconTint)) {
        setControlIconTint(a.getColorStateList(R.styleable.WearChipButton_controlIconTint));
      }

      enableAutomaticStateTransitions =
          a.getBoolean(
              R.styleable.WearChipButton_enableAutomaticStateTransitions,
              enableAutomaticStateTransitions);
    } finally {
      layoutIsBeingInitialized = false;
      a.recycle();
    }
    executeContentConstraintUpdates();
  }

  private void executeContentConstraintUpdates() {
    updateTextMargin(primaryText);
    updateTextMargin(secondaryText);
    updateContentConstraints();
  }

  /*
   * Implements the abstract function from base class WearButton, to inflate a chip styled layout
   */
  private void createViews() {
    LayoutInflater.from(getContext()).inflate(R.layout.wear_chip_button_layout, this, true);

    icon = findViewById(R.id.wear_chip_icon);
    primaryText = findViewById(R.id.wear_chip_primary_text);
    secondaryText = findViewById(R.id.wear_chip_secondary_text);
    selectionControlContainer = findViewById(R.id.wear_chip_selection_control_container);
    startClickableView = findViewById(R.id.wear_chip_start);

    if (hasFixedLayout()) {
      return;
    }

    updateConstraintsForStretchedButton();
  }

  private void updateConstraintsForStretchedButton() {
    constraintSet.clone(this);

    constraintSet.setGuidelineBegin(
        R.id.start_guideline, getSizeOf(R.dimen.wear_multiline_button_start_padding));
    constraintSet.setGuidelineEnd(
        R.id.end_guideline, getSizeOf(R.dimen.wear_multiline_button_end_padding));
    constraintSet.setGuidelineBegin(
        R.id.top_guideline, getSizeOf(R.dimen.wear_multiline_button_top_padding));
    constraintSet.setGuidelineEnd(
        R.id.bottom_guideline, getSizeOf(R.dimen.wear_multiline_button_bottom_padding));

    constraintSet.removeFromVerticalChain(R.id.wear_chip_primary_text);
    constraintSet.removeFromVerticalChain(R.id.wear_chip_secondary_text);

    constraintSet.connect(
        R.id.wear_chip_primary_text, ConstraintSet.TOP, R.id.top_guideline, ConstraintSet.TOP);
    constraintSet.connect(
        R.id.wear_chip_primary_text,
        ConstraintSet.BOTTOM,
        R.id.wear_chip_secondary_text,
        ConstraintSet.TOP);
    constraintSet.connect(
        R.id.wear_chip_secondary_text,
        ConstraintSet.TOP,
        R.id.wear_chip_primary_text,
        ConstraintSet.BOTTOM);
    constraintSet.connect(
        R.id.wear_chip_secondary_text,
        ConstraintSet.BOTTOM,
        R.id.bottom_guideline,
        ConstraintSet.BOTTOM);
    constraintSet.setVerticalChainStyle(R.id.wear_chip_primary_text, ConstraintSet.CHAIN_PACKED);
    constraintSet.applyTo(this);
  }

  private int getSizeOf(int dimensId) {
    return getResources().getDimensionPixelSize(dimensId);
  }

  private void initializeTransitions() {
    setTransitionName(WEAR_CHIP_BUTTON_TRANSITION);

    stateTransitions =
        TransitionInflater.from(getContext())
            .inflateTransition(R.transition.wear_chip_button_state);
  }

  @VisibleForTesting
  void initAccessibilityDelegates() {
    ViewCompat.setAccessibilityDelegate(
        this,
        new WearChipButtonAccessibilityDelegate(this) {

          @Override
          State onUpdateAccessibilityState() {
            View selectionControl = WearChipButton.this.selectionControl;
            return new State(
                selectionControl == null || hasEndOnClickListener()
                    ? null
                    : selectionControl.getAccessibilityClassName(),
                getPrimaryText(),
                getSecondaryText(),
                !hasEndOnClickListener());
          }
        });

    ViewCompat.setAccessibilityDelegate(
        selectionControlContainer,
        new WearChipButtonAccessibilityDelegate(this) {

          @Override
          State onUpdateAccessibilityState() {
            View selectionControl = WearChipButton.this.selectionControl;
            return new State(
                selectionControl == null ? null : selectionControl.getAccessibilityClassName(),
                getPrimaryText(),
                /* secondaryText = */ null,
                /* isCheckable = */ true);
          }
        });
  }

  @Override
  public void setChecked(boolean checked) {
    super.setChecked(checked);

    if (selectionControl != null) {
      selectionControl.setChecked(checked);
    }
  }

  /** Flag for if wear chip button has a second action */
  @Override
  protected boolean hasEndOnClickListener() {
    return hasEndOnclickListener && controlType != ControlType.NONE;
  }

  @Override
  public void setBackground(@Nullable Drawable background) {
    Drawable oldBackground = getBackground();
    if (oldBackground != background) {
      startDelayedTransitions();
      super.setBackground(background);

      updateAccentLayers(background);
      updateAccentDirection();
    }
  }

  private void updateAccentLayers(@Nullable Drawable background) {
    if (background instanceof LayerDrawable) {
      LayerDrawable layerDrawable = (LayerDrawable) background;
      startAccent = layerDrawable.findDrawableByLayerId(R.id.wear_chip_start_accent);
      startAccentRtl = layerDrawable.findDrawableByLayerId(R.id.wear_chip_start_accent_rtl);
      endAccent = layerDrawable.findDrawableByLayerId(R.id.wear_chip_end_accent);
      endAccentRtl = layerDrawable.findDrawableByLayerId(R.id.wear_chip_end_accent_rtl);
    } else {
      startAccent = null;
      startAccentRtl = null;
      endAccent = null;
      endAccentRtl = null;
    }
  }

  @Override
  public void onRtlPropertiesChanged(int layoutDirection) {
    if (resolvedLayoutDirection != layoutDirection) {
      resolvedLayoutDirection = layoutDirection;
      updateAccentDirection();
    }
  }

  /**
   * Sets the icon drawable res id.
   *
   * @param resId the drawable resource id to set as icon.
   */
  @Override
  public void setIcon(@DrawableRes int resId) {
    if (resId != 0) {
      startDelayedTransitions();
      icon.setImageResource(resId);
      icon.setVisibility(View.VISIBLE);
      icon.setDuplicateParentStateEnabled(true);
      executeContentConstraintUpdates();
    }
  }

  /**
   * Sets the icon drawable.
   *
   * @param drawable the {@link Drawable} to set as icon, or null to hide the icon.
   */
  @Override
  public void setIcon(@Nullable Drawable drawable) {
    startDelayedTransitions();
    if (drawable == null) {
      icon.setVisibility(GONE);
    } else {
      icon.setImageDrawable(drawable);
      icon.setVisibility(View.VISIBLE);
      icon.setDuplicateParentStateEnabled(true);
    }
    executeContentConstraintUpdates();
  }

  /**
   * Sets the icon size.
   *
   * @param resId the drawable resource id to set as icon.
   */
  @SuppressWarnings("dereference.of.nullable")
  public void setIconSize(@DimenRes int resId) {
    if (icon.getVisibility() == View.VISIBLE && icon.getLayoutParams() != null) {
      int sizePx = getResources().getDimensionPixelSize(resId);
      icon.getLayoutParams().height = icon.getLayoutParams().width = sizePx;
      executeContentConstraintUpdates();
    }
  }

  /**
   * Sets the primary text.
   *
   * @param text the {@link CharSequence} to be set on the primary text.
   */
  public void setPrimaryText(@Nullable CharSequence text) {
    if (primaryText == null) {
      return;
    }

    startDelayedTransitions();

    if (TextUtils.isEmpty(text)) {
      primaryText.setVisibility(View.GONE);
    } else {
      primaryText.setVisibility(View.VISIBLE);
      ContentChangeTransition.setText(primaryText, text);
      updateContentConstraints();
    }
    updateAccessibilityAttributes();
  }

  private void updateAccessibilityAttributes() {
    CharSequence contentDescriptionToSet =
        selectionControlContentDescription != null
            ? selectionControlContentDescription
            : getPrimaryText();
    int importantForAccessibilityToSet =
        hasEndOnclickListener
            ? View.IMPORTANT_FOR_ACCESSIBILITY_YES
            : View.IMPORTANT_FOR_ACCESSIBILITY_NO;

    startClickableView.setContentDescription(getPrimaryText());
    selectionControlContainer.setContentDescription(contentDescriptionToSet);
    if (controlIcon != null) {
      controlIcon.setContentDescription(contentDescriptionToSet);
      controlIcon.setImportantForAccessibility(importantForAccessibilityToSet);
    }
    if (selectionControl != null) {
      selectionControl.setContentDescription(contentDescriptionToSet);
      selectionControl.setImportantForAccessibility(importantForAccessibilityToSet);
    }
    selectionControlContainer.setImportantForAccessibility(importantForAccessibilityToSet);
  }

  /**
   * Sets {@link OnClickListener} for the left side of the button (only to be used when {@link
   * OnCheckedChangeListener} has been set and we need a second action Or to be used when we need
   * only the right side of the button which has been set with {@link OnCheckedChangeListener} to be
   * responsive to tap and nothing should happen when user taps on the right side of the button, in
   * that case, we should set an empty function to this listener.
   *
   * @param listener the {@link boolean} to be set on this view .
   */
  public void setEndOnClickListener(@Nullable OnClickListener listener) {
    if (listener == null) {
      hasEndOnclickListener = false;
      selectionControlContainer.setOnClickListener(null);
      selectionControlContainer.setBackgroundColor(Color.TRANSPARENT);
    } else {
      hasEndOnclickListener = true;
      selectionControlContainer.setBackgroundColor(
          getResources()
              .getColor(
                  R.color.wear_material_button_control_container_translucent_background,
                  getContext().getTheme()));
      updateSelectionControlContainerForeground();
      updateControlIconEnabledState();
      // In case of ICON the parent state is not duplicated to allow displaying a touch feedback on
      // the child.
      selectionControlContainer.setDuplicateParentStateEnabled(controlType != ControlType.ICON);
      selectionControlContainer.setOnClickListener(
          v -> {
            if (isCheckable() && isEnabled() && toggleOnClick) {
              toggle();
            }
            listener.onClick(v);
          });
    }
    refreshDrawableState();
    updateAccessibilityAttributes();
  }

  private void updateSelectionControlContainerForeground() {
    Drawable foreground = null;
    if (hasEndOnclickListener && controlType == ControlType.ICON) {
      // Add the touch feedback.
      TypedValue typedValue = new TypedValue();
      getContext()
          .getTheme()
          .resolveAttribute(R.attr.wearSelectableItemBackground, typedValue, true);
      foreground = getContext().getDrawable(typedValue.resourceId);
    }
    selectionControlContainer.setForeground(foreground);
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    updateControlIconEnabledState();
    startClickableView.setEnabled(enabled);
  }

  private void updateControlIconEnabledState() {
    if (controlType == ControlType.ICON) {
      // In case of ICON the parent state is not duplicated to allow displaying the touch feedback
      // so it must be manually set.
      selectionControlContainer.setEnabled(isEnabled());
      if (controlIcon != null) {
        controlIcon.setEnabled(isEnabled());
      }
    } else {
      selectionControlContainer.setEnabled(isEnabled());
    }
  }

  /**
   * Sets a click listener to use on the start section of the view (all the view except the control
   * view).
   *
   * @param listener the listener to use, {@code null} to remove a previously added listener
   */
  public void setStartOnClickListener(@Nullable OnClickListener listener) {
    startClickableView.setEnabled(isEnabled());
    startClickableView.setOnClickListener(listener);
    startClickableView.setVisibility(listener != null ? View.VISIBLE : View.GONE);
    updateAccessibilityAttributes();
  }

  /** Returns the primary text. */
  public @Nullable CharSequence getPrimaryText() {
    boolean isVisible = primaryText.getVisibility() == View.VISIBLE;
    return isVisible ? primaryText.getText() : "";
  }

  /**
   * Sets the color of the primary text.
   *
   * @param color the color to be set on the primary text.
   */
  public void setPrimaryTextColor(@ColorInt int color) {
    primaryText.setTextColor(color);
  }

  /**
   * Sets the color of the primary text.
   *
   * @param color the color state list to be set on the primary text.
   */
  public void setPrimaryTextColor(ColorStateList color) {
    primaryText.setTextColor(color);
  }

  /** Get the primary text's text color. */
  @ColorInt
  public int getPrimaryTextColor() {
    return primaryText.getCurrentTextColor();
  }

  /**
   * Sets the primary text's maximum lines when there is no secondary text.
   *
   * @param primaryTextMaxLines the maximum number of lines to be set.
   */
  public void setPrimaryTextMaxLines(int primaryTextMaxLines) {
    if (this.primaryTextMaxLines == primaryTextMaxLines) {
      return;
    }
    this.primaryTextMaxLines = primaryTextMaxLines;

    if (TextUtils.isEmpty(getSecondaryText())) {
      startDelayedTransitions();
      primaryText.setMaxLines(primaryTextMaxLines);
    }
  }

  /**
   * Sets the primary text's maximum lines when there is secondary text.
   *
   * @param primaryTextMaxLinesWithSecondary the maximum number of lines to be set.
   */
  public void setPrimaryTextMaxLinesWithSecondary(int primaryTextMaxLinesWithSecondary) {
    if (this.primaryTextMaxLinesWithSecondary == primaryTextMaxLinesWithSecondary) {
      return;
    }
    this.primaryTextMaxLinesWithSecondary = primaryTextMaxLinesWithSecondary;

    if (!TextUtils.isEmpty(getSecondaryText())) {
      startDelayedTransitions();
      primaryText.setMaxLines(primaryTextMaxLinesWithSecondary);
    }
  }

  /** Returns the maximum lines of the primary text when there is no secondary text. */
  public int getPrimaryTextMaxLines() {
    return primaryTextMaxLines;
  }

  /** Returns the maximum lines of the primary text when there is secondary text. */
  public int getPrimaryTextMaxLinesWithSecondary() {
    return primaryTextMaxLinesWithSecondary;
  }

  /**
   * Sets the primary text's horizontal position.
   *
   * @param pos the {@link int} to be set as the primary text's position. It only supports center
   *     and start (left) position.
   */
  // dereference of possibly-null reference lp
  // incompatible argument for parameter arg0 of setLayoutParams.
  @SuppressWarnings({"nullness:dereference.of.nullable", "nullness:argument"})
  public void setPrimaryTextHorizontalPosition(TextHorizontalPos pos) {
    ConstraintLayout.LayoutParams lp =
        (ConstraintLayout.LayoutParams) primaryText.getLayoutParams();
    lp.horizontalBias = pos == TextHorizontalPos.START ? 0f : 0.5f;
    primaryText.setLayoutParams(lp);

    // Accounts for text that wraps to multiple lines. In such cases, the horizontal bias does
    // nothing because the textview has already expanded to fill the chip. Setting the text
    // gravity too ensures the text is actually aligned to the center of the chip in such cases.
    primaryText.setGravity(
        pos == TextHorizontalPos.START ? Gravity.START : Gravity.CENTER_HORIZONTAL);

    // Set corresponding text alignment to handle RTL cases.  If supportsRtl is false, this will
    // follow gravity instead.
    primaryText.setTextAlignment(
        pos == TextHorizontalPos.START ? TEXT_ALIGNMENT_VIEW_START : TEXT_ALIGNMENT_CENTER);
  }

  /**
   * Sets the secondary text.
   *
   * @param text the {@link CharSequence} to be set on the secondary text.
   */
  public void setSecondaryText(@Nullable CharSequence text) {
    startDelayedTransitions();

    if (TextUtils.isEmpty(text)) {
      // Remove the visibility of secondary text view if empty text
      secondaryText.setVisibility(View.GONE);
      primaryText.setMaxLines(primaryTextMaxLines);
    } else {
      secondaryText.setVisibility(View.VISIBLE);
      ContentChangeTransition.setText(secondaryText, text);

      // Reduce the max lines of primary text since secondary text got visible
      primaryText.setMaxLines(primaryTextMaxLinesWithSecondary);
    }
    updateContentConstraints();
  }

  /** Returns the secondary text. */
  public @Nullable CharSequence getSecondaryText() {
    boolean isVisible = secondaryText.getVisibility() == View.VISIBLE;
    return isVisible ? secondaryText.getText() : "";
  }

  /**
   * Sets the color of the secondary text.
   *
   * @param color the color to be set on the secondary text.
   */
  public void setSecondaryTextColor(@ColorInt int color) {
    secondaryText.setTextColor(color);
  }

  /**
   * Sets the color of the secondary text.
   *
   * @param color the color state list to be set on the secondary text.
   */
  public void setSecondaryTextColor(ColorStateList color) {
    secondaryText.setTextColor(color);
  }

  /** Get the secondary text's text color. */
  @ColorInt
  public int getSecondaryTextColor() {
    return secondaryText.getCurrentTextColor();
  }

  /** Returns the secondary text's maximum lines. */
  public int getSecondaryTextMaxLines() {
    return secondaryTextMaxLines;
  }

  /**
   * Sets the secondary text's maximum lines.
   *
   * @param secondaryTextMaxLines the maximum number of lines to be set.
   */
  public void setSecondaryTextMaxLines(int secondaryTextMaxLines) {
    if (this.secondaryTextMaxLines == secondaryTextMaxLines) {
      return;
    }
    this.secondaryTextMaxLines = secondaryTextMaxLines;
    secondaryText.setMaxLines(secondaryTextMaxLines);
    if (!TextUtils.isEmpty(getSecondaryText())) {
      startDelayedTransitions();
    }
  }

  /**
   * Sets which control widget at the end of the button or hides all control variants if {@code
   * ControlType.NONE}.
   *
   * @param controlType the {@link ControlType} to be shown.
   */
  public void setControlType(ControlType controlType) {
    if (this.controlType == controlType) {
      return;
    }

    startDelayedTransitions();

    selectionControlContainer.removeAllViews();
    selectionControl = null;
    controlIcon = null;
    this.controlType = controlType;

    if (controlType.layoutId != 0) {
      LayoutInflater.from(getContext())
          .inflate(controlType.layoutId, selectionControlContainer, true);
      if (controlType == ControlType.ICON) {
        controlIcon = selectionControlContainer.findViewById(R.id.wear_chip_end_icon);
        controlIcon.setImageResource(controlIconResId);
        controlIcon.setImageTintList(controlIconTint);
      } else {
        selectionControl = selectionControlContainer.findViewById(R.id.wear_chip_selection_control);
      }
    }
    boolean selectionControlVisible = controlType == ControlType.ICON || selectionControl != null;
    selectionControlContainer.setVisibility(selectionControlVisible ? VISIBLE : GONE);
    updateTextsEndMargin(selectionControlVisible);
    executeContentConstraintUpdates();
    updateAccentDirection();
    updateAccessibilityAttributes();
    updateSelectionControlContainerForeground();
  }

  private void updateTextsEndMargin(boolean selectionControlVisible) {
    int textMarginEnd =
        selectionControlVisible
            ? getResources()
                .getDimensionPixelSize(R.dimen.wear_chip_button_selection_control_start_margin)
            : 0;
    updateMarginEnd(primaryText, textMarginEnd);
    updateMarginEnd(secondaryText, textMarginEnd);
  }

  private void updateMarginEnd(TextView textView, int marginEnd) {
    ViewGroup.LayoutParams layoutParams = textView.getLayoutParams();
    if (layoutParams instanceof MarginLayoutParams) {
      ((MarginLayoutParams) layoutParams).setMarginEnd(marginEnd);
    }
  }

  /**
   * Sets the icon to use when the {@code controlType} is set to {@code ControlType.ICON}.
   *
   * @param resId the resource id to use as icon.
   */
  public void setControlIcon(@DrawableRes int resId) {
    controlIconResId = resId;
    ImageView controlIcon = this.controlIcon;
    if (controlIcon != null) {
      startDelayedTransitions();
      controlIcon.setImageResource(controlIconResId);
    }
  }

  /**
   * Applies a tint to the control icon displayed when the {@code controlType} is set to {@code
   * ControlType.ICON}.
   *
   * @param tint the tint to apply, may be {@code null} to clear tint.
   */
  public void setControlIconTint(@Nullable ColorStateList tint) {
    controlIconTint = tint;
    if (controlType == ControlType.ICON && controlIcon != null) {
      controlIcon.setImageTintList(tint);
    }
  }

  /**
   * Sets the content description to use in the selection control.
   *
   * @param contentDescription the content description to use, may be {@code null} to use the
   *     primary text instead.
   */
  public void setSelectionControlContentDescription(@Nullable CharSequence contentDescription) {
    this.selectionControlContentDescription = contentDescription;
    updateAccessibilityAttributes();
  }

  /** Returns the control type currently set. */
  public ControlType getControlType() {
    return controlType;
  }

  /**
   * Starts a delayed {@link Transition}s for the state-changes that follow a call to this method.
   *
   * <p>When {@link R.attr#enableAutomaticStateTransitions app:enableAutomaticStateTransitions} is
   * set to {@code true} (its default value), delayed Transitions for state-changes are
   * automatically started. In this case, there is no need to call this method explicitly.
   *
   * <p>When this attribute is set to {@code false}, delayed Transitions for state-changes will not
   * be automatically started. Call this method instead to start one explicitly.
   *
   * <p>If this chip-button is only showing an icon, delayed-transitions will never be started, even
   * after this method is called explicitly.
   */
  public void startDelayedStateTransition() {
    if (!isReadyToBeDrawn
        || (primaryText.getVisibility() == GONE && secondaryText.getVisibility() == GONE)) {
      return;
    }

    TransitionManager.beginDelayedTransition(this, stateTransitions);
  }

  @Nullable CompoundButton getSelectionControl() {
    return selectionControl;
  }

  @Override
  @SuppressWarnings("nullness:dereference.of.nullable")
  protected void applyBackgroundColor(@Nullable ColorStateList color) {
    Drawable background = getBackground();
    if (startAccent != null
        && endAccent != null
        && startAccentRtl != null
        && endAccentRtl != null) {
      startAccent.mutate().setTintList(color);
      startAccentRtl.mutate().setTintList(color);
      endAccent.mutate().setTintList(color);
      endAccentRtl.mutate().setTintList(color);
    } else if (background != null) {
      background.mutate().setTintList(color);
    }
  }

  @SuppressWarnings("nullness:dereference.of.nullable")
  private void updateAccentDirection() {
    if (startAccent == null
        || endAccent == null
        || startAccentRtl == null
        || endAccentRtl == null) {
      return;
    }

    boolean isRtl = resolvedLayoutDirection == LAYOUT_DIRECTION_RTL;
    boolean showStart = selectionControl == null;

    startAccent.setAlpha(visibleToAlpha(showStart && !isRtl));
    startAccentRtl.setAlpha(visibleToAlpha(showStart && isRtl));
    endAccent.setAlpha(visibleToAlpha(!showStart && !isRtl));
    endAccentRtl.setAlpha(visibleToAlpha(!showStart && isRtl));
  }

  private void updateContentConstraints() {
    if (layoutIsBeingInitialized) {
      return;
    }

    constraintSet.clone(this);
    // Change the padding guidelines depending on if there is only an icon visible.
    if (isOnlyIconVisible()) {
      constraintSet.setGuidelineBegin(R.id.start_guideline, iconOnlyPadding);
      constraintSet.setGuidelineEnd(R.id.end_guideline, iconOnlyPadding);
      constraintSet.connect(
          R.id.wear_chip_icon, ConstraintSet.END, R.id.end_guideline, ConstraintSet.START);
    } else {
      constraintSet.setGuidelineBegin(R.id.start_guideline, iconAndTextPadding);
      constraintSet.setGuidelineEnd(R.id.end_guideline, iconAndTextPadding);
      constraintSet.clear(R.id.wear_chip_icon, ConstraintSet.END);
    }

    if (hasFixedLayout()) {
      // 2 line cases place primary text on top text center line, otherwise center within parent.
      int textConstraintId =
          secondaryText.getVisibility() == VISIBLE
              ? R.id.top_text_center_line
              : ConstraintSet.PARENT_ID;
      constraintSet.connect(
          R.id.wear_chip_primary_text, ConstraintSet.TOP, textConstraintId, ConstraintSet.TOP);
      constraintSet.connect(
          R.id.wear_chip_primary_text,
          ConstraintSet.BOTTOM,
          textConstraintId,
          ConstraintSet.BOTTOM);
    }
    constraintSet.applyTo(this);
  }

  private boolean isOnlyIconVisible() {
    return (icon.getVisibility() == VISIBLE
        && primaryText.getVisibility() == GONE
        && secondaryText.getVisibility() == GONE
        && controlType == ControlType.NONE);
  }

  private void updateTextMargin(View view) {
    if (layoutIsBeingInitialized) {
      return;
    }

    MarginLayoutParams layoutParams = (MarginLayoutParams) view.getLayoutParams();
    if (layoutParams == null) {
      return;
    }

    layoutParams.setMarginStart(
        icon.getVisibility() == VISIBLE
            ? getResources()
                .getDimensionPixelSize(R.dimen.wear_button_padding_between_icon_and_text)
            : 0);
    view.setLayoutParams(layoutParams);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    if (!hasFixedLayout() && adjustHorizontalGuidelines()) {
      measure(widthMeasureSpec, heightMeasureSpec);
    }
  }

  private boolean adjustHorizontalGuidelines() {
    boolean needsHorizontalGuidelines = needsHorizontalGuidelines();
    if (constraintSetNeedsHorizontalGuidelines == needsHorizontalGuidelines) {
      return false;
    }

    constraintSetNeedsHorizontalGuidelines = needsHorizontalGuidelines;

    int topPadding;
    int bottomPadding;
    if (needsHorizontalGuidelines) {
      topPadding = getSizeOf(R.dimen.wear_multiline_button_top_padding);
      bottomPadding = getSizeOf(R.dimen.wear_multiline_button_bottom_padding);
    } else {
      topPadding = 0;
      bottomPadding = 0;
    }

    constraintSet.clone(this);
    constraintSet.setGuidelineBegin(R.id.top_guideline, topPadding);
    constraintSet.setGuidelineEnd(R.id.bottom_guideline, bottomPadding);
    constraintSet.applyTo(this);
    return true;
  }

  private boolean needsHorizontalGuidelines() {
    // Horizontal guidelines should not be there if the total number of lines is two and the height
    // of the button got larger than 52dp(=minHeight).
    // Otherwise, the horizontal guidelines should be there.
    int minHeight = getMinHeight();
    int height = getMeasuredHeight();
    return (minHeight == 0
        || height < minHeight
        || (primaryText.getLineCount() + secondaryText.getLineCount() != 2));
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    getRootView().getViewTreeObserver().addOnPreDrawListener(preDrawListener);
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();

    TransitionManager.endTransitions(this);
    isReadyToBeDrawn = false;
    getRootView().getViewTreeObserver().removeOnPreDrawListener(preDrawListener);
  }

  private void startDelayedTransitions() {
    if (enableAutomaticStateTransitions && !layoutIsBeingInitialized) {
      startDelayedStateTransition();
    }
  }

  private static int visibleToAlpha(boolean visible) {
    return visible ? 255 : 0;
  }
}
