package com.google.android.clockwork.common.wearable.wearmaterial.selectioncontrol;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableWrapper;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ToggleButton;
import org.checkerframework.checker.nullness.qual.Nullable;

/** WearToggle - Wear OS styled toggle. */
public final class WearToggle extends ToggleButton {

  public WearToggle(Context context) {
    this(context, null);
  }

  public WearToggle(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, R.attr.wearToggleStyle);
  }

  @SuppressWarnings({"nullness:argument", "nullness:method.invocation"})
  public WearToggle(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override
  public @Nullable Drawable getButtonDrawable() {
    Drawable drawable = super.getButtonDrawable();

    if (drawable instanceof FlipHorizontalOnRtlDrawable) {
      return ((FlipHorizontalOnRtlDrawable) drawable).getDrawable();
    }
    return drawable;
  }

  @Override
  public void setButtonDrawable(@Nullable Drawable drawable) {
    super.setButtonDrawable(new FlipHorizontalOnRtlDrawable(drawable));
  }

  @Override
  public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
    if (VERSION.SDK_INT >= VERSION_CODES.R
        && event.getEventType() == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
      CharSequence stateDescription = getStateDescription();
      if (stateDescription != null) {
        announceForAccessibility(stateDescription.toString());
      }
    }
    super.onPopulateAccessibilityEvent(event);
  }

  /** Drawable whose wrapped drawable gets flipped horizontally on RTL layout. */
  private static class FlipHorizontalOnRtlDrawable extends DrawableWrapper {

    FlipHorizontalOnRtlDrawable(@Nullable Drawable dr) {
      super(dr);
    }

    @Override
    public void draw(Canvas canvas) {
      if (getLayoutDirection() == LAYOUT_DIRECTION_LTR) {
        super.draw(canvas);
        return;
      }

      int save = canvas.save();
      canvas.scale(-1, 1, getBounds().centerX(), 0);
      super.draw(canvas);
      canvas.restoreToCount(save);
    }
  }
}
