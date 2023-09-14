package com.google.android.clockwork.common.wearable.wearmaterial.alertdialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.widget.TextView;
import androidx.annotation.AttrRes;
import androidx.annotation.StyleRes;
import com.google.android.clockwork.common.wearable.wearmaterial.button.WearChipButton;

/** Helper methods for applying style to WearAlertDialog elements. */
final class WearAlertDialogUtils {

  /** Max lines allowed while view is centered */
  private static final int LINE_COUNT_THRESHOLD = 3;

  private WearAlertDialogUtils() {}

  @SuppressLint("ResourceType")
  static void applyStyleBackground(WearChipButton chipButton, @AttrRes int styleAttr) {
    int[] backgroundAttrs = {android.R.attr.background};

    Context context = chipButton.getContext();
    @StyleRes int style = getStyle(context, styleAttr);

    TypedArray a = context.obtainStyledAttributes(style, backgroundAttrs);
    if (a.hasValue(0)) {
      chipButton.setBackground(a.getDrawable(0));
    }
    a.recycle();
  }

  @SuppressLint("ResourceType")
  static void applyStyleColors(WearChipButton chipButton, @AttrRes int styleAttr) {
    int[] colorAttrs = {R.attr.buttonBackgroundColor, R.attr.primaryTextColor};

    Context context = chipButton.getContext();
    @StyleRes int style = getStyle(context, styleAttr);

    TypedArray a = context.obtainStyledAttributes(style, colorAttrs);
    if (a.hasValue(0)) {
      chipButton.setBackgroundColor(a.getColorStateList(0));
    }
    if (a.hasValue(1)) {
      chipButton.setPrimaryTextColor(a.getColor(1, 0));
    }
    a.recycle();
  }

  @StyleRes
  static int getStyle(Context context, @AttrRes int styleAttr) {
    Theme theme = context.getTheme();
    TypedValue tv = new TypedValue();
    theme.resolveAttribute(styleAttr, tv, true);
    return tv.resourceId;
  }

  /**
   * Assigns the correct {@link Gravity} to {@code messageView} based on the number of lines it
   * shows.
   */
  static void assignMessageViewGravity(TextView messageView) {
    messageView
        .getViewTreeObserver()
        .addOnPreDrawListener(
            new OnPreDrawListener() {
              @Override
              public boolean onPreDraw() {
                if (messageView.getLineCount() <= LINE_COUNT_THRESHOLD) {
                  messageView.setGravity(Gravity.CENTER_HORIZONTAL);
                } else {
                  messageView.setGravity(Gravity.START);
                }
                messageView.getViewTreeObserver().removeOnPreDrawListener(this);
                return false;
              }
            });
  }
}
