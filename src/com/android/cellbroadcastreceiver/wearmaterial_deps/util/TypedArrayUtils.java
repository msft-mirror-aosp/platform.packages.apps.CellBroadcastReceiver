package com.google.android.clockwork.common.wearable.wearmaterial.util;

import android.content.Context;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.util.TypedValue;
import androidx.annotation.AnyRes;
import androidx.annotation.AttrRes;
import androidx.annotation.StyleableRes;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Forked version of {@link androidx.core.content.res.TypedArrayUtils}, which is has restricted
 * visibility.
 */
public class TypedArrayUtils {

  private TypedArrayUtils() {}

  /**
   * Returns the attr specified by {@code attr}, if it is declared int he context theme. If it does
   * not exist, returns {@code fallbackAttr}.
   */
  public static int getAttr(Context context, int attr, int fallbackAttr) {
    TypedValue value = new TypedValue();
    context.getTheme().resolveAttribute(attr, value, /* resolveRefs= */ true);
    if (value.type != TypedValue.TYPE_NULL) {
      return attr;
    }
    return fallbackAttr;
  }

  /**
   * Returns resource ID value of {@code index}. If it does not exist, a resource ID value of {@code
   * fallbackIndex}. If it still does not exist, {@code defaultValue}.
   */
  @AnyRes
  public static int getResourceId(
      TypedArray a,
      @StyleableRes int index,
      @StyleableRes int fallbackIndex,
      @AnyRes int defaultValue) {
    int val = a.getResourceId(fallbackIndex, defaultValue);
    return a.getResourceId(index, val);
  }

  /**
   * Returns the {@link CharSequence} that is the value of the themed attribute {@code attr}. If the
   * {@code theme} is null or the attribute does not exist, it returns null.
   */
  public static @Nullable CharSequence getStringAttr(@Nullable Theme theme, @AttrRes int attr) {
    if (theme == null) {
      return null;
    }

    TypedValue value = new TypedValue();
    theme.resolveAttribute(attr, value, /* resolveRefs= */ true);
    return value.string;
  }
}
