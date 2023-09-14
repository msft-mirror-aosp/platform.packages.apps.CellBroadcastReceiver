package com.google.android.clockwork.common.wearable.wearmaterial.util;

import android.view.View;
import androidx.annotation.ColorInt;
import androidx.wear.widget.CurvedTextView;
import org.checkerframework.checker.nullness.qual.Nullable;

final class CurvedTextViewWrapper implements TextViewWrapper {

  private final CurvedTextView textView;

  CurvedTextViewWrapper(CurvedTextView textView) {
    this.textView = textView;
  }

  @Override
  public @Nullable CharSequence getText() {
    return textView.getText();
  }

  @Override
  public void setText(@Nullable CharSequence text) {
    textView.setText(text != null ? text.toString() : "");
  }

  @Override
  @ColorInt
  public int getTextColor() {
    return textView.getTextColor();
  }

  @Override
  public void setTextColor(int color) {
    textView.setTextColor(color);
  }

  @Override
  public View getView() {
    return textView;
  }
}
