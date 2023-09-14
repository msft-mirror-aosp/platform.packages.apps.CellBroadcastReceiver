package com.google.android.clockwork.common.wearable.wearmaterial.drawable;

import static java.lang.Math.min;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import androidx.annotation.Keep;
import androidx.annotation.RequiresApi;
import java.io.IOException;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Drawable that draws background glow gradient such that it is scaled to the smaller side of the
 * canvas and is centered in view.
 */
@Keep
public class BackgroundGlowDrawable extends Drawable {

  private Drawable gradient;

  @Override
  public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs, @Nullable Theme theme)
      throws IOException, XmlPullParserException {
    super.inflate(r, parser, attrs, theme);
    gradient = r.getDrawable(R.drawable.wear_blurred_background_glow_gradient, null);
  }

  @Override
  public void draw(Canvas canvas) {
    int smallerSize = min(getBounds().width(), getBounds().height());
    int gradientLeft = (getBounds().width() - smallerSize) / 2;
    int gradientTop = (getBounds().height() - smallerSize) / 2;
    gradient.setBounds(
        gradientLeft, gradientTop, gradientLeft + smallerSize, gradientTop + smallerSize);
    gradient.draw(canvas);
  }

  @Override
  public void setAlpha(int i) {
    gradient.setAlpha(i);
  }

  @Override
  public int getAlpha() {
    return gradient.getAlpha();
  }

  @Override
  public void setColorFilter(@Nullable ColorFilter colorFilter) {
    if (gradient != null) {
      gradient.setColorFilter(colorFilter);
    }
  }

  @Override
  public @Nullable ColorFilter getColorFilter() {
    if (gradient != null) {
      return gradient.getColorFilter();
    }
    return super.getColorFilter();
  }

  @Override
  public void setTintList(@Nullable ColorStateList tint) {
    if (gradient != null) {
      gradient.setTintList(tint);
    }
  }

  @RequiresApi(29)
  @Override
  public void setTintBlendMode(@Nullable BlendMode blendMode) {
    if (gradient != null) {
      gradient.setTintBlendMode(blendMode);
    }
  }

  @Override
  public int getOpacity() {
    return gradient.getOpacity();
  }
}
