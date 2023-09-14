package com.google.android.clockwork.common.wearable.wearmaterial.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.TypedValue;
import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.StyleRes;

/**
 * Shared utility functions dealing with {@link Context}s and {@link
 * android.content.res.Resources.Theme}s
 */
public final class ThemeUtils {

  /**
   * Applies a the attributes from a theme overlay to the given theme.
   *
   * @param context the context to apply the theme to.
   * @param theme the resource ID of the theme being applied.
   */
  public static void applyThemeOverlay(Context context, @StyleRes int theme) {
    context.getTheme().applyStyle(theme, /* force= */ true);
  }

  /**
   * Applies an Theme-overlay defined by the {@code android:theme} attribute to the provided context
   * and returns the modified context.
   *
   * @param context The context to which the {@code android:theme} will be applied
   * @param defStyleAttr The attribute referring to the style from which to extract the {@code
   *     android:theme} attribute
   * @param defStyleRes The style from which to extract the {@code android:theme} attribute, if it
   *     couldn't be found through the {@code defStyleAttr}
   * @return The context to which the theme is applied
   */
  @SuppressLint("ResourceType")
  public static Context applyThemeOverlay(
      Context context, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {

    int[] attrs = {android.R.attr.theme};
    TypedArray a = context.obtainStyledAttributes(null, attrs, defStyleAttr, defStyleRes);
    try {
      int themeOverlay = a.getResourceId(0, 0);
      if (themeOverlay != 0) {
        context.getTheme().applyStyle(themeOverlay, false);
      }
    } finally {
      a.recycle();
    }

    return context;
  }

  /**
   * Returns the color associated with the specified attribute in the context's theme.
   *
   * @param context The context to which the {@code android:theme} will be applied
   * @param colorAttr The color attribute referring to the style from which to extract the {@code
   *     android:theme} attribute
   */
  @ColorInt
  public static int getThemeAttrColor(Context context, @AttrRes int colorAttr) {
    final TypedValue typedValue = new TypedValue();
    final TypedArray array = context.obtainStyledAttributes(typedValue.data, new int[] {colorAttr});
    final int color = array.getColor(0, 0);
    array.recycle();
    return color;
  }

  private ThemeUtils() {}
}
