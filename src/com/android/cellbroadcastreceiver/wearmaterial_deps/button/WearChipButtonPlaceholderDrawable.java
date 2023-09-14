package com.google.android.clockwork.common.wearable.wearmaterial.button;

import android.content.Context;
import android.content.res.Resources.Theme;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.VisibleForTesting;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A Placeholder {@link Drawable} for a {@link WearChipButton}. It draws a greyed-out outline of a
 * one- or two-line {@code WearChipButton}, with or without an icon.
 */
public final class WearChipButtonPlaceholderDrawable extends Drawable {

  private final Drawable background;
  private final @Nullable Drawable iconPlaceholder;
  private final @Nullable Drawable primaryTextPlaceholder;
  private final @Nullable Drawable secondaryTextPlaceholder;
  private final int buttonDefaultHeight;
  private final int defaultTextHeight;
  private final int horizontalPadding;
  private final int iconTextPadding;
  private final int verticalPadding;

  private final Rect tmpRect = new Rect();
  private final Paint paint = new Paint();

  /**
   * Creates a new {@link WearChipButtonPlaceholderDrawable} for a given {@link WearChipButton}
   * {@code view}.
   *
   * @param view The {@link WearChipButton} for which this Placeholder Drawable will be created.
   * @param showIcon If true, show the outline of an icon.
   * @param showPrimaryText If true, show the outline of the primary text area.
   * @param showSecondaryText If true, show the outline of the secondary text area.
   */
  public WearChipButtonPlaceholderDrawable(
      WearChipButton view, boolean showIcon, boolean showPrimaryText, boolean showSecondaryText) {
    Context context = view.getContext();

    this.iconPlaceholder =
        showIcon ? getMutatedDrawable(context, R.drawable.wear_chip_button_icon_placeholder) : null;
    this.primaryTextPlaceholder =
        showPrimaryText
            ? getMutatedDrawable(context, R.drawable.wear_chip_button_text_placeholder)
            : null;
    this.secondaryTextPlaceholder =
        showPrimaryText && showSecondaryText
            ? getMutatedDrawable(context, R.drawable.wear_chip_button_text_placeholder)
            : null;

    this.buttonDefaultHeight = getSize(context, R.dimen.wear_chip_button_default_min_height);
    this.defaultTextHeight = getSize(context, R.dimen.wear_placeholder_chip_button_text_height);
    this.horizontalPadding = getSize(context, R.dimen.wear_button_start_padding);
    this.verticalPadding = getSize(context, R.dimen.wear_placeholder_chip_button_vertical_padding);
    this.iconTextPadding = getSize(context, R.dimen.wear_button_padding_between_icon_and_text);

    paint.setColor(context.getColor(R.color.wear_chip_button_placeholder_skeleton));
    paint.setStyle(Style.FILL_AND_STROKE);

    Drawable background = view.getBackground();
    if (background == null) {
      background = new ColorDrawable(getDefaultBackgroundColor(context));
    } else {
      ConstantState state = background.getConstantState();
      background =
          state == null
              ? background
              : state.newDrawable(context.getResources(), context.getTheme());
    }
    this.background = background;
  }

  @Override
  protected void onBoundsChange(Rect bounds) {
    super.onBoundsChange(bounds);
    layout(bounds);
  }

  @Override
  public void draw(Canvas canvas) {
    background.draw(canvas);

    if (iconPlaceholder != null) {
      iconPlaceholder.draw(canvas);
    }

    if (primaryTextPlaceholder != null) {
      primaryTextPlaceholder.draw(canvas);
    }

    if (secondaryTextPlaceholder != null) {
      secondaryTextPlaceholder.draw(canvas);
    }
  }

  @Override
  public int getAlpha() {
    return paint.getAlpha();
  }

  @Override
  public void setAlpha(int alpha) {
    paint.setAlpha(alpha);
  }

  @Override
  public @Nullable ColorFilter getColorFilter() {
    return paint.getColorFilter();
  }

  @Override
  public void setColorFilter(@Nullable ColorFilter colorFilter) {
    paint.setColorFilter(colorFilter);
  }

  @Override
  public int getOpacity() {
    return PixelFormat.TRANSLUCENT;
  }

  private void layout(Rect bounds) {
    background.setBounds(bounds);

    int left = bounds.left + horizontalPadding;
    int right = bounds.right - horizontalPadding;
    int top = bounds.top + verticalPadding;
    int bottom = bounds.bottom - verticalPadding;

    if (iconPlaceholder != null) {
      tmpRect.set(0, 0, iconPlaceholder.getIntrinsicWidth(), iconPlaceholder.getIntrinsicHeight());
      tmpRect.offset(
          left, bounds.top + ((bounds.height() - iconPlaceholder.getIntrinsicHeight()) / 2));
      iconPlaceholder.setBounds(tmpRect);

      left += tmpRect.width() + iconTextPadding;
    }

    if (secondaryTextPlaceholder != null) {
      tmpRect.set(left, bottom - defaultTextHeight, right, bottom);
      secondaryTextPlaceholder.setBounds(tmpRect);

      int spaceBetweenTexts = buttonDefaultHeight - 2 * (defaultTextHeight + verticalPadding);
      bottom = tmpRect.top - spaceBetweenTexts;
    } else {
      top = bounds.top + (bounds.height() - defaultTextHeight) / 2;
      bottom = top + defaultTextHeight;
    }

    if (primaryTextPlaceholder != null) {
      tmpRect.set(left, top, right, bottom);
      primaryTextPlaceholder.setBounds(tmpRect);
    }
  }

  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  Drawable getBackground() {
    return background;
  }

  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  @Nullable Drawable getIconPlaceholder() {
    return iconPlaceholder;
  }

  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  @Nullable Drawable getPrimaryTextPlaceholder() {
    return primaryTextPlaceholder;
  }

  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  @Nullable Drawable getSecondaryTextPlaceholder() {
    return secondaryTextPlaceholder;
  }

  private static @Nullable Drawable getMutatedDrawable(
      Context context, @DrawableRes int drawableId) {
    Drawable drawable = context.getDrawable(drawableId);
    return drawable == null ? null : drawable.mutate();
  }

  private static int getDefaultBackgroundColor(Context context) {
    Theme theme = context.getTheme();
    TypedValue tv = new TypedValue();
    if (theme.resolveAttribute(R.attr.colorSurface, tv, true)
        && TypedValue.TYPE_FIRST_COLOR_INT <= tv.type
        && tv.type <= TypedValue.TYPE_LAST_COLOR_INT) {
      return tv.data;
    } else {
      // This generally shouldn't be hit, because WearChipButton doesn't look correct without a
      // background.
      throw new IllegalStateException(
          "Chip button placeholder cannot be created without valid theme.");
    }
  }

  private static int getSize(Context context, @DimenRes int dimenId) {
    return context.getResources().getDimensionPixelSize(dimenId);
  }
}
