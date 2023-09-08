package com.google.android.clockwork.common.wearable.wearmaterial.util;

import android.view.View;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.wear.widget.CurvedTextView;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Wrapper to contain either {@link android.widget.TextView} or {@link
 * androidx.wear.widget.CurvedTextView} which does not inherit from TextView. Simplifying code that
 * requires tracking both possibilities of a normal TextView and WearCurvedTextViews depending on
 * screen shape where normally both needs to be kept and api calls need to be made with null checks
 * etc.
 */
public interface TextViewWrapper {

  static TextViewWrapper wrap(View view) {
    if (view instanceof TextView) {
      return new NormalTextViewWrapper((TextView) view);
    } else if (view instanceof CurvedTextView) {
      return new CurvedTextViewWrapper((CurvedTextView) view);
    }
    throw new IllegalArgumentException("Parameter must be of type TextView or CurvedTextView");
  }

  @Nullable CharSequence getText();

  void setText(@Nullable CharSequence text);

  @ColorInt
  int getTextColor();

  void setTextColor(@ColorInt int color);

  View getView();
}
