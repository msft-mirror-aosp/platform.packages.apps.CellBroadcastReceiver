package com.google.android.clockwork.common.wearable.wearmaterial.button;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import androidx.annotation.CallSuper;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.VisibleForTesting;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.clockwork.common.wearable.wearmaterial.color.WearColorUtils;
import com.google.android.clockwork.common.wearable.wearmaterial.util.ThemeUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Abstract class for Wear button custom view. This class supports the common behavior for all the
 * Wear 3 Material Button styles.
 *
 * <p>This class does not provide any layout. Derived classed should provide their layout by
 * implementing createViews.
 *
 * <p>All attributes from {@link R.styleable#WearButton} are supported.
 *
 * <ul>
 *   <li>{@link R.styleable#WearButton_icon app:icon} - The icon to be shown in this {@code
 *       WearButton}.
 *   <li>{@link android.R.attr#checkable android:checkable} - If true, the button can be checked. If
 *       false, the button acts like a normal button.
 *   <li>{@link R.styleable#WearButton_buttonBackgroundColor app:buttonBackgroundColor} - Stateful
 *       background color of the button.
 *   <li>{@link R.styleable#WearButton_buttonIconTint app:buttonIconTint} - Stateful tint-color of
 *       icon of the button.
 *   <li>{@link R.styleable#WearButton_cornerRadius app:cornerRadius} - To override the default
 *       corner radius of the button background rectangle shape.
 *   <li>{@link android.R.attr#enabled android:enabled} - To enable/disable the button.
 *   <li>{@link R.styleable#WearButton_toggleOnClick app:toggleOnClick} - If true, a click on this
 *       {@code WearButton} will automatically toggle its {@code checked} state.
 * </ul>
 *
 * <p>You can register a listener on the button with {@link
 * #setOnCheckedChangeListener(OnCheckedChangeListener)}
 *
 * <p>If you want to override the stateful colors for the background and the icon, these are the
 * default ones for a {@code WearButton}:
 *
 * <ul>
 *   <li>Background Color
 *       <ul>
 *         <li>Enabled and Checked<br>
 *             {@code ?attr/colorSurface}
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
public abstract class WearButton extends ConstraintLayout implements CheckableWearButton {

  protected static final int NO_COLOR = 0;

  // state sets for tint color state list
  @VisibleForTesting
  static final int[] DISABLED_STATE_SET = new int[] {-android.R.attr.state_enabled};

  @VisibleForTesting
  static final int[] ENABLED_AND_UNCHECKED_STATE_SET =
      new int[] {android.R.attr.state_enabled, -android.R.attr.state_checked};

  @VisibleForTesting
  static final int[] ENABLED_AND_CHECKED_STATE_SET =
      new int[] {android.R.attr.state_enabled, android.R.attr.state_checked};

  private static final int[] CHECKED_STATE_SET = {android.R.attr.state_checked};

  protected @Nullable ColorStateList defaultIconColors;
  protected @Nullable ColorStateList defaultBackgroundColors;

  private @Nullable ColorStateList iconColorStateList;
  private @Nullable ColorStateList backgroundColorStateList;

  protected ImageView icon;

  /** Current status of checked */
  protected boolean checked = false;

  /** Flag to decide if the button can be toggled */
  protected boolean checkable;

  protected int cornerRadius;

  private @Nullable OnCheckedChangeListener legacyOnCheckedChangeListener;
  private final Set<OnWearCheckedChangeListener<CheckableWearButton>> onCheckedChangeListeners =
      new HashSet<>();

  /**
   * Will be used when we want to block the button from getting immediately toggled on click and
   * instead set the state asynchronously later
   */
  protected boolean toggleOnClick;

  /**
   * Interface definition for a callback to be invoked when the checked state of this Button is
   * changed.
   */
  public interface OnCheckedChangeListener extends OnWearCheckedChangeListener<WearButton> {}

  public WearButton(Context context) {
    this(context, null);
  }

  public WearButton(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, R.attr.wearButtonStyle);
  }

  @SuppressWarnings({"method.invocation", "methodref.receiver.bound"})
  public WearButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    // Icons are not tinted by default.
    defaultIconColors = null;

    // Default background color is default color of 'colorSurface'.
    int colorSurface = ThemeUtils.getThemeAttrColor(context, R.attr.colorOnPrimary);
    defaultBackgroundColors = createLegacyColorStateList(colorSurface, colorSurface);

    addOnCheckedChangeListener((OnCheckedChangeListener) this::notifyLegacyOnCheckedChangeListener);

    initAccessibilityDelegate();
  }

  @CallSuper
  protected void initFromAttributes(@Nullable AttributeSet attrs, int defStyleAttr) {
    TypedArray a =
        getContext()
            .getTheme()
            .obtainStyledAttributes(
                attrs, R.styleable.WearButton, defStyleAttr, R.style.WearButtonDefault);

    try {
      boolean useDeprecatedColorAttributes =
          a.hasValue(R.styleable.WearButton_buttonActiveIconTint)
              || a.hasValue(R.styleable.WearButton_buttonInactiveIconTint)
              || a.hasValue(R.styleable.WearButton_buttonActiveBackgroundColor)
              || a.hasValue(R.styleable.WearButton_buttonInactiveBackgroundColor);

      if (a.hasValue(R.styleable.WearButton_icon)) {
        setIcon(a.getResourceId(R.styleable.WearButton_icon, 0));
      }

      if (a.hasValueOrEmpty(R.styleable.WearButton_buttonIconTint)) {
        iconColorStateList = a.getColorStateList(R.styleable.WearButton_buttonIconTint);
      } else {
        iconColorStateList = defaultIconColors;
      }

      if (a.hasValueOrEmpty(R.styleable.WearButton_buttonBackgroundColor)) {
        backgroundColorStateList =
            a.getColorStateList(R.styleable.WearButton_buttonBackgroundColor);
      } else {
        backgroundColorStateList = defaultBackgroundColors;
      }

      if (useDeprecatedColorAttributes) {
        int activeIconTintColor =
            a.getColor(
                R.styleable.WearButton_buttonActiveIconTint,
                getLegacyActiveStateColor(iconColorStateList));

        int inactiveIconTintColor =
            a.getColor(
                R.styleable.WearButton_buttonInactiveIconTint,
                getLegacyInactiveStateColor(iconColorStateList));

        iconColorStateList = createLegacyIconTintList(activeIconTintColor, inactiveIconTintColor);

        int activeBackgroundColor =
            a.getColor(
                R.styleable.WearButton_buttonActiveBackgroundColor,
                getLegacyActiveStateColor(backgroundColorStateList));

        int inactiveBackgroundColor =
            a.getColor(
                R.styleable.WearButton_buttonInactiveBackgroundColor,
                getLegacyInactiveStateColor(backgroundColorStateList));

        backgroundColorStateList =
            createLegacyColorStateList(activeBackgroundColor, inactiveBackgroundColor);
      }

      icon.setImageTintList(iconColorStateList);
      applyBackgroundColor(backgroundColorStateList);

      cornerRadius = (int) a.getDimension(R.styleable.WearButton_cornerRadius, 0);
      setButtonCornerRadius(cornerRadius);

      setEnabled(a.getBoolean(R.styleable.WearButton_android_enabled, true));
      setCheckable(a.getBoolean(R.styleable.WearButton_android_checkable, false));
      setChecked(a.getBoolean(R.styleable.WearButton_android_checked, false));
      setToggleOnClick(a.getBoolean(R.styleable.WearButton_toggleOnClick, false));

      setClipToOutline(true);
    } finally {
      a.recycle();
    }
  }

  private void initAccessibilityDelegate() {
    ViewCompat.setAccessibilityDelegate(
        this,
        new AccessibilityDelegateCompat() {
          @Override
          public void onInitializeAccessibilityEvent(View view, AccessibilityEvent event) {
            super.onInitializeAccessibilityEvent(view, event);
            event.setClassName(getA11yClassName());
            event.setChecked(isChecked());
          }

          @Override
          public void onInitializeAccessibilityNodeInfo(
              View view, AccessibilityNodeInfoCompat info) {
            super.onInitializeAccessibilityNodeInfo(view, info);
            info.setClassName(getA11yClassName());
            info.setCheckable(isCheckable());
            info.setChecked(isChecked());
          }
        });
  }

  private String getA11yClassName() {
    // Use the platform widget classes so Talkback can recognize this as a button.
    return (isCheckable() ? CompoundButton.class : Button.class).getName();
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();

    if (getParent() instanceof WearButtonGroup) {
      // ensure buttons within group is always checkable to avoid breaking check behavior.
      setCheckable(true);
    }
  }

  protected boolean hasEndOnClickListener() {
    return false;
  }

  public void setButtonCornerRadius(int radius) {
    Drawable drawable = getBackground();

    if (drawable instanceof LayerDrawable) {
      drawable = ((LayerDrawable) drawable).findDrawableByLayerId(android.R.id.background);
    }
    if (drawable instanceof GradientDrawable) {
      ((GradientDrawable) drawable).setCornerRadius(radius);
    }
  }

  public boolean isCheckable() {
    return checkable;
  }

  public void setCheckable(boolean checkable) {
    if (getParent() instanceof WearButtonGroup) {
      checkable = true;
    }
    this.checkable = checkable;
    refreshDrawableState();
  }

  @Override
  public boolean isChecked() {
    return checked;
  }

  @Override
  public void setChecked(boolean b) {
    if (checkable && b != checked) {
      checked = b;
      refreshDrawableState();

      // Make a copy to avoid any ConcurrentModificationException.
      Set<OnWearCheckedChangeListener<CheckableWearButton>> listeners =
          Collections.unmodifiableSet(onCheckedChangeListeners);

      for (OnWearCheckedChangeListener<CheckableWearButton> listener : listeners) {
        listener.onCheckedChanged(this, checked);
      }
    }
  }

  @Override
  public void toggle() {
    setChecked(!checked);
  }

  // Checkable state by default not part of drawable state
  @Override
  public int[] onCreateDrawableState(int extraSpace) {
    final int[] drawableState = super.onCreateDrawableState(extraSpace + 2);
    if (!hasEndOnClickListener() && isChecked()) {
      mergeDrawableStates(drawableState, CHECKED_STATE_SET);
    }

    ColorStateList backgroundColor = getBackgroundColor();
    int color =
        backgroundColor == null
            ? NO_COLOR
            : backgroundColor.getColorForState(drawableState, NO_COLOR);

    return WearColorUtils.mergeIsColorLightState(drawableState, color);
  }

  /**
   * Register a callback to be invoked when the checked state of this button changes.
   *
   * @deprecated Use {@link #addOnCheckedChangeListener(OnWearCheckedChangeListener)} and {@link
   *     #removeOnCheckedChangeListener(OnWearCheckedChangeListener)} instead.
   * @param listener the callback to call on checked state change
   */
  @Deprecated
  public void setOnCheckedChangeListener(@Nullable OnCheckedChangeListener listener) {
    legacyOnCheckedChangeListener = listener;
  }

  private void notifyLegacyOnCheckedChangeListener(WearButton button, boolean isChecked) {
    if (legacyOnCheckedChangeListener != null) {
      legacyOnCheckedChangeListener.onCheckedChanged(button, isChecked);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public void addOnCheckedChangeListener(
      OnWearCheckedChangeListener<? extends CheckableWearButton> listener) {
    onCheckedChangeListeners.add((OnWearCheckedChangeListener<CheckableWearButton>) listener);
  }

  @Override
  public void removeOnCheckedChangeListener(
      OnWearCheckedChangeListener<? extends CheckableWearButton> listener) {
    onCheckedChangeListeners.remove(listener);
  }

  
  @Override
  public boolean performClick() {
    if (isCheckable() && isEnabled() && toggleOnClick && !hasEndOnClickListener()) {
      toggle();
    }
    return super.performClick();
  }

  /** Sets the icon to show the provided drawable {@code resId}. */
  public abstract void setIcon(@DrawableRes int resId);

  /** Sets the icon to show the provided {@code drawable}. */
  public abstract void setIcon(@Nullable Drawable drawable);

  /** Gets the drawable shown in icon. */
  public @Nullable Drawable getIcon() {
    return icon.getDrawable();
  }

  @Override
  public void setBackground(@Nullable Drawable background) {
    if (background != null) {
      background.mutate().setTintMode(Mode.SRC_ATOP);
    }

    super.setBackground(background);
    applyBackgroundColor(backgroundColorStateList);
  }

  /**
   * Sets the icon tint color when button is in active state.
   *
   * @deprecated Use {@link #setIconTintColor(ColorStateList)} instead.
   * @param activeIconTintColor the color to tint the icon when button is in active state.
   */
  @Deprecated
  public void setActiveIconTintColor(int activeIconTintColor) {
    iconColorStateList = createLegacyIconTintList(activeIconTintColor, getInactiveIconTintColor());
    icon.setImageTintList(iconColorStateList);
  }

  /**
   * Returns the icon tint color for an active/checked button.
   *
   * @deprecated Use {@link #getIconTintColor()} instead.
   */
  @Deprecated
  public int getActiveIconTintColor() {
    return getLegacyActiveStateColor(iconColorStateList);
  }

  /**
   * Sets the icon tint color when button is in inactive state.
   *
   * @deprecated Use {@link #setIconTintColor(ColorStateList)} instead.
   * @param inactiveIconTintColor the color to tint the icon when button is in inactive state.
   */
  @Deprecated
  public void setInactiveIconTintColor(int inactiveIconTintColor) {
    iconColorStateList = createLegacyIconTintList(getActiveIconTintColor(), inactiveIconTintColor);
    icon.setImageTintList(iconColorStateList);
  }

  /**
   * Returns the icon tint color for an inactive/un-checked button.
   *
   * @deprecated Use {@link #getIconTintColor()} instead.
   */
  @Deprecated
  public int getInactiveIconTintColor() {
    return getLegacyInactiveStateColor(iconColorStateList);
  }

  /** Sets the stateful icon tint color to the provided {@code color}. */
  public void setIconTintColor(@Nullable ColorStateList color) {
    iconColorStateList = color;
    icon.setImageTintList(iconColorStateList);
  }

  /** Returns the icon tint color. */
  public @Nullable ColorStateList getIconTintColor() {
    return iconColorStateList;
  }

  /**
   * Gets the icon's global visible rect.
   *
   * @param r the {@link Rect} to populate the visible rect bounds.
   */
  public void getIconGlobalVisibleRect(Rect r) {
    icon.getGlobalVisibleRect(r);
  }

  /**
   * Set the background tint color when button is active (checked and enabled, or noncheckable and
   * enabled).
   *
   * @deprecated Use {@link #setBackgroundColor(ColorStateList)} instead.
   * @param color the background color.
   */
  @Deprecated
  public void setActiveBackgroundColor(int color) {
    backgroundColorStateList = createLegacyColorStateList(color, getInactiveBackgroundColor());
    applyBackgroundColor(backgroundColorStateList);
    refreshDrawableState();
  }

  /**
   * Set the background tint color when button is inactive (unchecked and enabled, or noncheckable
   * and disabled).
   *
   * @deprecated Use {@link #setBackgroundColor(ColorStateList)} instead.
   * @param color the background color.
   */
  @Deprecated
  public void setInactiveBackgroundColor(int color) {
    backgroundColorStateList = createLegacyColorStateList(getActiveBackgroundColor(), color);
    applyBackgroundColor(backgroundColorStateList);
    refreshDrawableState();
  }

  /** Sets the background tint color to the stateful {@code color} list. */
  public void setBackgroundColor(@Nullable ColorStateList color) {
    backgroundColorStateList = color;
    applyBackgroundColor(backgroundColorStateList);
    refreshDrawableState();
  }

  /** Sets the background tint color to the provided {@code color} ARGB value. */
  @Override
  public void setBackgroundColor(@ColorInt int color) {
    setBackgroundColor(ColorStateList.valueOf(color));
  }

  /**
   * Returns the background tint color when button is active/checked (and enabled).
   *
   * @deprecated Use {@link #getBackgroundColor()} instead.
   */
  @Deprecated
  public int getActiveBackgroundColor() {
    return getLegacyActiveStateColor(backgroundColorStateList);
  }

  /**
   * Returns the background tint color when button is inactive/un-checked (and enabled).
   *
   * @deprecated Use {@link #getBackgroundColor()} instead.
   */
  @Deprecated
  public int getInactiveBackgroundColor() {
    return getLegacyInactiveStateColor(backgroundColorStateList);
  }

  /** Returns the background tint color. */
  public @Nullable ColorStateList getBackgroundColor() {
    return backgroundColorStateList;
  }

  /**
   * Sets whether button should toggle checkable state on click.
   *
   * @param toggleOnClick whether check state should toggle on click.
   */
  @Override
  public void setToggleOnClick(boolean toggleOnClick) {
    this.toggleOnClick = toggleOnClick;
  }

  public boolean getToggleOnClick() {
    return this.toggleOnClick;
  }

  protected void applyBackgroundColor(@Nullable ColorStateList color) {
    Drawable drawable = this.getBackground();
    if (drawable != null) {
      drawable.mutate().setTintList(color);
    }
    refreshDrawableState();
  }

  private static int getLegacyActiveStateColor(@Nullable ColorStateList colorStateList) {
    return colorStateList == null
        ? NO_COLOR
        : colorStateList.getColorForState(ENABLED_AND_CHECKED_STATE_SET, NO_COLOR);
  }

  private static int getLegacyInactiveStateColor(@Nullable ColorStateList colorStateList) {
    return colorStateList == null
        ? NO_COLOR
        : colorStateList.getColorForState(ENABLED_AND_UNCHECKED_STATE_SET, NO_COLOR);
  }

  private static @Nullable ColorStateList createLegacyIconTintList(
      int activeColor, int inactiveColor) {
    if (activeColor == NO_COLOR && inactiveColor == NO_COLOR) {
      return null;
    }
    return createLegacyColorStateList(activeColor, inactiveColor);
  }

  static ColorStateList createLegacyColorStateList(int activeColor, int inactiveColor) {
    return new ColorStateList(
        new int[][] {
          ENABLED_AND_UNCHECKED_STATE_SET, ENABLED_AND_CHECKED_STATE_SET, DISABLED_STATE_SET
        },
        new int[] {inactiveColor, activeColor, inactiveColor});
  }
}
