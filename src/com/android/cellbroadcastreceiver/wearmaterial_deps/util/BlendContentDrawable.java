package com.google.android.clockwork.common.wearable.wearmaterial.util;

import static android.graphics.PorterDuff.Mode.DST_OUT;
import static android.graphics.PorterDuff.Mode.SRC_ATOP;
import static android.graphics.PorterDuff.Mode.SRC_OVER;

import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableWrapper;
import android.util.AttributeSet;
import androidx.annotation.Keep;
import androidx.core.util.Consumer;
import java.io.IOException;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * A {@link DrawableWrapper} that blends the inner {@link #getDrawable()} with content drawn by an
 * external provider (see {@link #drawWithAlphaBlending(Canvas, Consumer)})
 */
@Keep
public final class BlendContentDrawable extends DrawableWrapper {

  /** The type of blending mode to use when blending the drawable and the content. */
  public enum BlendMode {
    NONE(SRC_ATOP),
    COLOR(SRC_ATOP),
    ALPHA(DST_OUT);

    private final PorterDuff.Mode mode;

    BlendMode(PorterDuff.Mode mode) {
      this.mode = mode;
    }
  }

  /**
   * Returns a new {@link BlendContentDrawable} that blends the provided {@code drawable} with the
   * content rendered by {@link #draw(Canvas, Consumer)}.
   *
   * <p>The {@code blendMode} specifies how to blend the drawable and the content.
   */
  public static BlendContentDrawable create(Drawable drawable, BlendMode blendMode) {
    BlendContentDrawable blendContentDrawable = new BlendContentDrawable();
    blendContentDrawable.initialize(drawable, blendMode);
    return blendContentDrawable;
  }

  /**
   * Returns a new {@link BlendContentDrawable} that blends the provided {@code drawable} with the
   * content rendered by {@link #draw(Canvas, Consumer)}.
   */
  public static BlendContentDrawable create(Drawable drawable) {
    return create(drawable, BlendMode.NONE);
  }

  /** Paint configuring the blend-mode for drawing content. */
  private final Paint contentPaint = new Paint();

  /** Paint configuring the blend-mode for drawing the drawable. */
  private final Paint drawablePaint = new Paint();

  /** This object is here to avoid too many allocations during drawing. */
  private final RectF tmpRectF = new RectF();

  private BlendMode blendMode = BlendMode.NONE;

  private @Nullable Consumer<Canvas> contentProvider;

  public BlendContentDrawable() {
    super(null);

    contentPaint.setXfermode(new PorterDuffXfermode(SRC_OVER));
    drawablePaint.setXfermode(new PorterDuffXfermode(SRC_ATOP));
  }

  private void initialize(Drawable drawable, BlendMode blendMode) {
    setDrawable(drawable);
    setBlendMode(blendMode);
  }

  @Override
  public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs, @Nullable Theme theme)
      throws IOException, XmlPullParserException {
    super.inflate(r, parser, attrs, theme);

    TypedArray a;
    if (theme != null) {
      a = theme.obtainStyledAttributes(attrs, R.styleable.BlendDrawable, 0, 0);
    } else {
      a = r.obtainAttributes(attrs, R.styleable.BlendDrawable);
    }

    setDrawable(a.getDrawable(R.styleable.BlendDrawable_android_drawable));

    BlendMode mode;
    switch (a.getInt(R.styleable.BlendDrawable_blendMode, 0)) {
      case 1:
        mode = BlendMode.COLOR;
        break;
      case 2:
        mode = BlendMode.ALPHA;
        break;
      default:
        mode = BlendMode.NONE;
        break;
    }
    setBlendMode(mode);

    a.recycle();
  }

  /**
   * Sets the callback that provides the drawing content which will be blended by this drawable.
   *
   * <p>If the content-provider is not set or cleared, this {@code BlendContentDrawable} will behave
   * like a regular {@link DrawableWrapper}.
   *
   * <p>The {@code contentProvider} is a function from the client that provides and draws the actual
   * content (e.g child-views). This content will be blended with the drawable returned by {@link
   * #getDrawable()}.
   *
   * <p>Be sure the client clears this provider by calling {@code setContentProvider(null)} when the
   * client is done with this drawable.
   */
  public void setContentProvider(@Nullable Consumer<Canvas> contentProvider) {
    if (this.contentProvider == null) {
      this.contentProvider = contentProvider;
    }
  }

  public void setBlendMode(BlendMode blendMode) {
    this.blendMode = blendMode;
    drawablePaint.setXfermode(new PorterDuffXfermode(blendMode.mode));
  }

  @Override
  public int getOpacity() {
    return useAlphaChannelBlending() ? PixelFormat.TRANSLUCENT : super.getOpacity();
  }

  @Override
  public void draw(Canvas canvas) {
    Consumer<Canvas> contentProvider = this.contentProvider;
    if (contentProvider != null) {
      draw(canvas, contentProvider);
    } else {
      super.draw(canvas);
    }
  }

  /**
   * Draws this {@code Drawable} onto the canvas and optionally blends it with the drawing of any
   * content, based on the value of {@link #blendMode}.
   */
  private void draw(Canvas canvas, Consumer<Canvas> contentProvider) {
    if (useAlphaChannelBlending()) {
      drawWithAlphaBlending(canvas, contentProvider);
    } else {
      contentProvider.accept(canvas);
      super.draw(canvas);
    }
  }

  private boolean useAlphaChannelBlending() {
    return blendMode != BlendMode.NONE;
  }

  private void drawWithAlphaBlending(Canvas canvas, Consumer<Canvas> contentProvider) {
    tmpRectF.set(getBounds());

    int contentLayerId = canvas.saveLayer(tmpRectF, contentPaint);
    contentProvider.accept(canvas);
    int drawableLayerId = canvas.saveLayer(tmpRectF, drawablePaint);
    super.draw(canvas);
    canvas.restoreToCount(drawableLayerId);
    canvas.restoreToCount(contentLayerId);
  }
}
