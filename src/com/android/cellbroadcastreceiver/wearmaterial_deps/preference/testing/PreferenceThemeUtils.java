package com.google.android.clockwork.common.wearable.wearmaterial.preference;

import android.content.Context;
import android.util.TypedValue;

/** Util class for testing AndroidX preference themes. */
public class PreferenceThemeUtils {

  private PreferenceThemeUtils() {}

  /**
   * Mimics theme initialization in PreferenceFragmentCompat by applying the {@code preferenceTheme}
   * attribute to the given context.
   */
  public static void applyPreferenceTheme(Context context) {
    TypedValue tv = new TypedValue();
    context.getTheme().resolveAttribute(R.attr.preferenceTheme, tv, true);
    int theme = tv.resourceId;
    if (theme == 0) {
      // Fallback to default theme.
      theme = R.style.PreferenceThemeOverlay;
    }
    context.getTheme().applyStyle(theme, /* force= */ false);
  }
}
