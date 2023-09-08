package com.google.android.clockwork.common.wearable.wearmaterial.color;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import com.google.android.clockwork.common.wearable.wearmaterial.color.ColorOverlays.ThemeOverlaySupplier;

/**
 * Implementation of {@link ThemeOverlaySupplier} that returns the dynamic theme from the device,
 * when dynamic colors are available.
 */
public class DynamicThemeOverlaySupplier implements ThemeOverlaySupplier {

  @Override
  public int getThemeOverlay(Context themedContext) {
    if (VERSION.SDK_INT < VERSION_CODES.TIRAMISU) {
      // Wear only supports dynamic colors starting in T, compared to S on phones.
      return 0;
    }
    return getDynamicColorThemeOverlay(themedContext);
  }

  private static int getDynamicColorThemeOverlay(Context context) {
    TypedArray dynamicColorAttributes =
        context.obtainStyledAttributes(new int[] {R.attr.dynamicColorThemeOverlay});
    final int theme = dynamicColorAttributes.getResourceId(0, 0);
    dynamicColorAttributes.recycle();
    return theme;
  }
}
