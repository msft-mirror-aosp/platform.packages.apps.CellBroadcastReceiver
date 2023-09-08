package com.google.android.clockwork.common.wearable.wearmaterial.color;

import static androidx.core.graphics.ColorUtils.HSLToColor;
import static androidx.core.graphics.ColorUtils.LABToColor;
import static androidx.core.graphics.ColorUtils.calculateContrast;
import static androidx.core.graphics.ColorUtils.calculateLuminance;
import static androidx.core.graphics.ColorUtils.colorToHSL;
import static androidx.core.graphics.ColorUtils.colorToLAB;
import static androidx.core.graphics.ColorUtils.compositeColors;
import static androidx.core.graphics.ColorUtils.distanceEuclidean;

import android.app.Notification;
import android.content.Context;
import android.graphics.Color;
import androidx.annotation.ColorInt;
import androidx.annotation.VisibleForTesting;
import com.google.android.clockwork.common.suppliers.LazyContextSupplier;
import java.util.ArrayList;
import java.util.List;

/**
 * Utils class that contains helpful methods to help apps better utilize the Wear Material preset
 * colors.
 */
public final class WearColorUtils {

  private static final String TAG = "WearColorUtils";

  private static final int BLACK = 0xFF000000;
  private static final float LUMINANCE_THRESHOLD = 0.5f;
  private static final double CONTRAST_THRESHOLD = 4.5;
  private static final int CONTRAST_ITERATION_DEPTH_MAX = 15;
  private static final double CONTRAST_RANGE_MINIMUM = 0.00001;

  private static final int[] ALL_SYSTEM_COLOR_RESOURCES = {
    R.color.wear_material_system_white,
    R.color.wear_material_system_white_mid,
    R.color.wear_material_system_red,
    R.color.wear_material_system_red_mid,
    R.color.wear_material_system_green,
    R.color.wear_material_system_green_mid,
    R.color.wear_material_system_blue,
    R.color.wear_material_system_blue_mid,
    R.color.wear_material_system_purple,
    R.color.wear_material_system_purple_mid,
    R.color.wear_material_system_orange,
    R.color.wear_material_system_orange_mid,
    R.color.wear_material_system_pink,
    R.color.wear_material_system_pink_mid,
    R.color.wear_material_system_yellow,
    R.color.wear_material_system_yellow_mid,
    R.color.wear_material_system_cyan,
    R.color.wear_material_system_cyan_mid,
  };

  private final List<double[]> systemColorsLAB;

  public static final LazyContextSupplier<WearColorUtils> INSTANCE =
      new LazyContextSupplier<>(WearColorUtils::new, "WearColorUtils");

  /** Returns true only if the provided {@code color}'s luminance is light/bright. */
  public static boolean isColorLight(@ColorInt int color) {
    return calculateLuminance(compositeColors(color, BLACK)) >= LUMINANCE_THRESHOLD;
  }

  /**
   * Returns an updated {@code state} that contains a drawable state indicating whether the provided
   * {@code color} is light/bright or not.
   */
  public static int[] mergeIsColorLightState(int[] state, @ColorInt int color) {
    return mergeIsColorLightState(state, isColorLight(color));
  }

  /**
   * Returns an updated {@code state} that contains a drawable state reflecting the value of the
   * provided {@code isLight}.
   */
  public static int[] mergeIsColorLightState(int[] state, boolean isLight) {
    int length = state.length;

    int i = length - 1;
    while (i >= 0 && state[i] == 0) {
      i--;
    }

    int[] dstState;
    if (i == length - 1) {
      // No empty spot found. Create a larger array and copy over the current contents.
      dstState = new int[length + 1];
      System.arraycopy(state, 0, dstState, 0, length);
    } else {
      dstState = state;
    }

    int stateLight = isLight ? R.attr.state_light : -R.attr.state_light;
    dstState[i + 1] = stateLight;

    return dstState;
  }

  /**
   * Returns the foreground color that has enough contrast with the background color.
   *
   * <p>If there is enough contrast already, we return {@code foregroundColor}. Otherwise, we tune
   * the color until there is enough contrast.
   */
  @ColorInt
  public static int getContrastedColor(
      @ColorInt int foregroundColor, @ColorInt int backgroundColor) {
    if (Color.alpha(foregroundColor) < 255) {
      return foregroundColor;
    }
    if (hasEnoughContrast(foregroundColor, backgroundColor)) {
      return foregroundColor;
    }
    float[] hsl = new float[3];
    colorToHSL(foregroundColor, hsl);
    // This is basically a light grey, pushing the color will only distort it.
    // Best thing to do in here is to fallback to the default color.
    if (hsl[1] < 0.2f) {
      return Notification.COLOR_DEFAULT;
    }
    boolean isBackgroundDark = !isColorLight(backgroundColor);
    return findContrastColor(foregroundColor, backgroundColor, isBackgroundDark);
  }

  @VisibleForTesting
  WearColorUtils(Context context) {
    systemColorsLAB = loadColorLABFromResources(context, ALL_SYSTEM_COLOR_RESOURCES);
  }

  private static List<double[]> loadColorLABFromResources(Context context, int[] colorResources) {
    List<double[]> colorsLAB = new ArrayList<>();
    for (int colorRes : colorResources) {
      int color = context.getColor(colorRes);
      double[] colorLAB = {0, 0, 0};
      colorToLAB(color, colorLAB);
      colorsLAB.add(colorLAB);
    }
    return colorsLAB;
  }

  /** Finds the closest color in the Wear Material system colors given the input color. */
  @ColorInt
  public int findClosestSystemColor(@ColorInt int color) {
    double[] colorLAB = {0, 0, 0};
    colorToLAB(color, colorLAB);

    double minDistance = Double.MAX_VALUE;
    double[] closestColorLAB = {0, 0, 0};
    for (double[] candidateColorLAB : systemColorsLAB) {
      double distance = distanceEuclidean(colorLAB, candidateColorLAB);
      if (distance < minDistance) {
        minDistance = distance;
        closestColorLAB = candidateColorLAB;
      }
    }

    return LABToColor(closestColorLAB[0], closestColorLAB[1], closestColorLAB[2]);
  }

  private static int findContrastColor(
      @ColorInt int foregroundColor, @ColorInt int backgroundColor, boolean isBackgroundDark) {
    return isBackgroundDark
        ? findContrastAgainstDarkBackground(foregroundColor, backgroundColor)
        : findContrastAgainstLightBackground(foregroundColor, backgroundColor);
  }

  private static int findContrastAgainstDarkBackground(
      @ColorInt int foregroundColor, @ColorInt int backgroundColor) {
    float[] hsl = new float[3];
    colorToHSL(foregroundColor, hsl);

    float low = hsl[2];
    float high = 1;

    for (int i = 0; i < CONTRAST_ITERATION_DEPTH_MAX && high - low > CONTRAST_RANGE_MINIMUM; i++) {
      float l = (low + high) / 2;
      hsl[2] = l;
      foregroundColor = HSLToColor(hsl);
      if (calculateContrast(foregroundColor, backgroundColor) > CONTRAST_THRESHOLD) {
        high = l;
      } else {
        low = l;
      }
    }
    return foregroundColor;
  }

  private static int findContrastAgainstLightBackground(
      @ColorInt int foregroundColor, @ColorInt int backgroundColor) {
    double[] lab = new double[3];
    colorToLAB(foregroundColor, lab);

    double low = 0;
    double high = lab[0];
    double a = lab[1];
    double b = lab[2];

    for (int i = 0; i < CONTRAST_ITERATION_DEPTH_MAX && high - low > CONTRAST_RANGE_MINIMUM; i++) {
      double l = (low + high) / 2;
      foregroundColor = LABToColor(l, a, b);
      if (calculateContrast(foregroundColor, backgroundColor) > CONTRAST_THRESHOLD) {
        low = l;
      } else {
        high = l;
      }
    }
    return LABToColor(low, a, b);
  }

  private static boolean hasEnoughContrast(
      @ColorInt int foregroundColor, @ColorInt int backgroundColor) {
    return calculateContrast(foregroundColor, backgroundColor) >= CONTRAST_THRESHOLD;
  }
}
