package com.google.android.clockwork.common.wearable.wearmaterial.card;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import androidx.annotation.ColorInt;
import androidx.annotation.VisibleForTesting;
import androidx.core.graphics.ColorUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Custom card widget that supports Wear 3 Material styles. This card is just a container that has
 * the rounded edges, gradient background, and shadows. The inside layout of the card should be
 * defined in the actual application.
 */
public final class WearCard extends FrameLayout {

  /** Alpha of the starting color of the background gradient. */
  @VisibleForTesting float backgroundGradientStartAlpha;

  @VisibleForTesting float backgroundGradientEndAlpha;

  public WearCard(Context context) {
    this(context, null);
  }

  public WearCard(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, R.attr.wearCardStyle);
  }

  public WearCard(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    this(context, attrs, defStyleAttr, R.style.WearCardDefault);
  }

  public WearCard(
      final Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr);

    Resources resources = context.getResources();
    backgroundGradientStartAlpha =
        resources.getFraction(
            R.fraction.wear_card_gradient_start_alpha, /* base= */ 1, /* pbase= */ 1);
    backgroundGradientEndAlpha =
        resources.getFraction(
            R.fraction.wear_card_gradient_end_alpha, /* base= */ 1, /* pbase= */ 1);

    applyAttributes(context, attrs, defStyleAttr, defStyleRes);
  }

  /** Applies the attributes to the card component. */
  private void applyAttributes(
      final Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    TypedArray a =
        context.obtainStyledAttributes(attrs, R.styleable.WearCard, defStyleAttr, defStyleRes);

    int backgroundGradientStartColor = a.getColor(R.styleable.WearCard_backgroundGradientStart, 0);
    int backgroundGradientEndColor = a.getColor(R.styleable.WearCard_backgroundGradientEnd, 0);
    setCardBackgroundGradient(backgroundGradientStartColor, backgroundGradientEndColor);

    setCardCornerRadius((int) a.getDimension(R.styleable.WearCard_cornerRadius, 0));
    setClipToOutline(true);

    a.recycle();
  }

  /**
   * Sets the gradient colors of the Card component background. If the background of the card is not
   * a {@link GradientDrawable}, this method does not do anything.
   */
  @VisibleForTesting
  void setCardBackgroundGradient(@ColorInt int startColor, @ColorInt int endColor) {
    Drawable drawable = getBackground();

    if (drawable instanceof GradientDrawable) {
      int startColorBlended =
          ColorUtils.blendARGB(Color.BLACK, startColor, backgroundGradientStartAlpha);
      int endColorBlended = ColorUtils.blendARGB(Color.BLACK, endColor, backgroundGradientEndAlpha);
      ((GradientDrawable) drawable.mutate())
          .setColors(new int[] {startColorBlended, endColorBlended});
    }
  }

  /** Sets the corner radius of the Card component. */
  public void setCardCornerRadius(int radius) {
    setOutlineProvider(
        new ViewOutlineProvider() {
          @Override
          public void getOutline(View view, Outline outline) {
            outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
          }
        });
  }
}
