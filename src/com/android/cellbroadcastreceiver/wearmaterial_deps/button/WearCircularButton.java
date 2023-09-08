package com.google.android.clockwork.common.wearable.wearmaterial.button;

import static androidx.annotation.VisibleForTesting.PROTECTED;
import static com.google.android.clockwork.common.wearable.wearmaterial.util.MathUtils.clamp;
import static java.lang.Math.min;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.VisibleForTesting;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.google.android.clockwork.common.wearable.wearmaterial.util.ThemeUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A button widget to support the Wear 3 material image styled button. This button in its full form
 * will only have an icon thats centrally placed in the container. This button will have fixed width
 * and height with a circular shape.
 *
 * <p>All attributes from {@link R.styleable#WearCircularButton} are supported.
 *
 * <p>If you want to override the stateful colors for the background and the icon, these are the
 * default ones for a {@code WearCircularButton}:
 *
 * <ul>
 *   <li>Background Color
 *       <ul>
 *         <li>Enabled and Checked<br>
 *             {@code ?attr/colorPrimary}
 *         <li>Otherwise<br>
 *             {@code ?attr/colorSurface}
 *       </ul>
 *   <li>Icon Tint
 *       <ul>
 *         <li>Enabled and Checked<br>
 *             {@code ?attr/colorOnPrimary}
 *         <li>Otherwise<br>
 *             {@code ?attr/colorPrimaryVariant}
 *       </ul>
 * </ul>
 */
public class WearCircularButton extends WearButton {

  /** An enum for all the fixed sizes supported by the button. */
  @VisibleForTesting(otherwise = PROTECTED)
  public enum ButtonSize {
    XSMALL(
        R.dimen.wear_circular_button_diameter_xsmall,
        R.dimen.wear_circular_button_icon_size_xsmall),
    SMALL(
        R.dimen.wear_circular_button_diameter_small, R.dimen.wear_circular_button_icon_size_small),
    STANDARD(
        R.dimen.wear_circular_button_diameter_standard,
        R.dimen.wear_circular_button_icon_size_standard),
    LARGE(
        R.dimen.wear_circular_button_diameter_large, R.dimen.wear_circular_button_icon_size_large);

    @DimenRes public final int diameterResId;
    @DimenRes public final int iconSizeResId;

    ButtonSize(@DimenRes int diameterResId, @DimenRes int iconSizeResId) {
      this.diameterResId = diameterResId;
      this.iconSizeResId = iconSizeResId;
    }
  }

  @DimenRes private int diameterResId = ButtonSize.STANDARD.diameterResId;
  @DimenRes private int iconSizeResId = ButtonSize.STANDARD.iconSizeResId;

  public WearCircularButton(Context context) {
    this(context, null);
  }

  public WearCircularButton(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, R.attr.wearCircularButtonStyle);
  }

  @SuppressWarnings({"nullness:argument", "nullness:method.invocation"})
  public WearCircularButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    // Set the default tinting for a circular-button to 'colorOnPrimary'/'colorOnPrimaryVariant'.
    int colorOnPrimary = ThemeUtils.getThemeAttrColor(context, R.attr.colorPrimary);
    int colorOnPrimaryVariant = ThemeUtils.getThemeAttrColor(context, R.attr.colorOnPrimary);
    defaultIconColors = createLegacyColorStateList(colorOnPrimary, colorOnPrimaryVariant);

    createViews();
    initFromAttributes(attrs, defStyleAttr);
  }

  @Override
  protected void initFromAttributes(@Nullable AttributeSet attrs, int defStyleAttr) {
    super.initFromAttributes(attrs, defStyleAttr);

    TypedArray a =
        getContext()
            .getTheme()
            .obtainStyledAttributes(
                attrs,
                R.styleable.WearCircularButton,
                defStyleAttr,
                R.style.WearCircularButtonDefault);

    try {
      int sizeIndex = a.getInt(R.styleable.WearCircularButton_size, ButtonSize.STANDARD.ordinal());
      setSize(ButtonSize.values()[clamp(sizeIndex, 0, ButtonSize.values().length - 1)]);
    } finally {
      a.recycle();
    }
  }

  /*
   * Implements the abstract function from base class WearButton, to inflate a chip styled layout
   */
  protected void createViews() {
    // This method must be protected (instead of private) since it is overridden by
    // LithiumBatteryButtonView.
    LayoutInflater.from(getContext()).inflate(R.layout.wear_circular_button_layout, this, true);
    icon = findViewById(R.id.icon);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int diameter = getResources().getDimensionPixelSize(this.diameterResId);
    int resolvedWidth = resolveDimension(diameter, widthMeasureSpec);
    int resolvedHeight = resolveDimension(diameter, heightMeasureSpec);
    int resolvedDiameter = min(resolvedWidth, resolvedHeight);
    final int desiredHSpec = MeasureSpec.makeMeasureSpec(resolvedDiameter, MeasureSpec.EXACTLY);
    final int desiredWSpec = MeasureSpec.makeMeasureSpec(resolvedDiameter, MeasureSpec.EXACTLY);

    super.onMeasure(desiredWSpec, desiredHSpec);
  }

  /** @deprecated Use the {@link #icon} field instead. */
  @Deprecated
  protected void setIcon(ImageView icon) {
    // This method must be protected (instead of private) since it is used by
    // LithiumBatteryButtonView.
    this.icon = icon;
  }

  /** @deprecated Use {@link #setIcon(int)} instead. */
  @Deprecated
  public void setIconResource(@DrawableRes int resId) {
    if (resId != 0) {
      icon.setImageResource(resId);
    }
  }

  /** @deprecated Use {@link #setIcon(Drawable)} instead. */
  @Deprecated
  public void setIconDrawable(@Nullable Drawable drawable) {
    icon.setImageDrawable(drawable);
  }

  @Override
  public void setIcon(int resId) {
    if (resId != 0) {
      icon.setImageResource(resId);
    }
  }

  @Override
  public void setIcon(@Nullable Drawable drawable) {
    icon.setImageDrawable(drawable);
  }

  public @Nullable Drawable getIconDrawable() {
    return icon.getDrawable();
  }

  /** Returns the button size configured on this button. */
  public void setSize(ButtonSize size) {
    setSize(size.diameterResId, size.iconSizeResId);
  }

  /** Sets a custom diameter and icon size for this button. */
  public void setSize(@DimenRes int diameterResId, @DimenRes int iconSizeResId) {
    this.diameterResId = diameterResId;
    this.iconSizeResId = iconSizeResId;
    updateSize();
  }

  /** Gets the diameter for this button */
  @DimenRes
  public int getDiameterResId() {
    return diameterResId;
  }

  /** Gets the icon size for this button */
  @DimenRes
  public int getIconSizeResId() {
    return iconSizeResId;
  }

  // dereference of possibly-null reference lp
  // incompatible argument for parameter arg0 of setLayoutParams.
  @SuppressWarnings({"nullness:dereference.of.nullable", "nullness:argument"})
  protected void updateSize() {
    int iconSize = getResources().getDimensionPixelSize(this.iconSizeResId);
    ConstraintLayout.LayoutParams lp = (LayoutParams) icon.getLayoutParams();
    lp.width = iconSize;
    lp.height = iconSize;
    icon.setLayoutParams(lp);
    requestLayout();
  }

  private int resolveDimension(int desiredSize, int measureSpec) {
    int mode = MeasureSpec.getMode(measureSpec);
    int specSize = MeasureSpec.getSize(measureSpec);
    switch (mode) {
      case MeasureSpec.EXACTLY:
        return specSize;
      case MeasureSpec.AT_MOST:
        return min(specSize, desiredSize);
      case MeasureSpec.UNSPECIFIED:
      default:
        return desiredSize;
    }
  }
}
