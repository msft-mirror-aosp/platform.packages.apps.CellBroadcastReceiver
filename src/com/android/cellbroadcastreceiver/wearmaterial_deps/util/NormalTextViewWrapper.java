package com.google.android.clockwork.common.wearable.wearmaterial.util;

import android.view.View;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import org.checkerframework.checker.nullness.qual.Nullable;

final class NormalTextViewWrapper implements TextViewWrapper {

  private final TextView textView;

  NormalTextViewWrapper(TextView textView) {
    this.textView = textView;
  }

  @Override
  public @Nullable CharSequence getText() {
    return textView.getText();
  }

  @Override
  public void setText(@Nullable CharSequence text) {
    textView.setText(text);
  }

  @Override
  @ColorInt
  public int getTextColor() {
    return textView.getCurrentTextColor();
  }

  @Override
  public void setTextColor(@ColorInt int color) {
    textView.setTextColor(color);
  }

  @Override
  public View getView() {
    return textView;
  }
}
