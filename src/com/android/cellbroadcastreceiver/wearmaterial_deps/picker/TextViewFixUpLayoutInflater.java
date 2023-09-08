package com.google.android.clockwork.common.wearable.wearmaterial.picker;

import android.content.Context;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

/**
 * This is a {@link LayoutInflater} that fixes up all {@link TextView}s in a layout so that their
 * text-rendering still will look good when it is drawn onto a scaled canvas.
 */
public final class TextViewFixUpLayoutInflater extends LayoutInflater {

  /**
   * Wraps and returns a {@link TextViewFixUpLayoutInflater} around the default {@link
   * LayoutInflater} of the given {@code context}.
   */
  public static LayoutInflater wrap(Context context) {
    return new TextViewFixUpLayoutInflater(LayoutInflater.from(context), context);
  }

  public TextViewFixUpLayoutInflater(Context context) {
    super(context);
  }

  public TextViewFixUpLayoutInflater(LayoutInflater originalInflater, Context newContext) {
    super(originalInflater, newContext);
  }

  @Override
  public LayoutInflater cloneInContext(Context newContext) {
    return new TextViewFixUpLayoutInflater(newContext);
  }

  @Override
  protected View onCreateView(String name, AttributeSet attrs) throws ClassNotFoundException {
    View view = super.onCreateView(name, attrs);
    if (view instanceof TextView) {
      Paint paint = ((TextView) view).getPaint();
      paint.setLinearText(true);
      paint.setSubpixelText(true);
      paint.setAntiAlias(true);
    }
    return view;
  }
}
