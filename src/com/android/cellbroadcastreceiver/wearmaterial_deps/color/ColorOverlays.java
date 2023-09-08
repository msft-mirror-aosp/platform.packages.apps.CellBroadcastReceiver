package com.google.android.clockwork.common.wearable.wearmaterial.color;

import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.content.Context;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import com.google.android.clockwork.common.wearable.wearmaterial.util.ThemeUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This is approximately based on {@code com.google.android.material.color.DynamicColors}, but with
 * different conditions for checking if Material You is available due to implementation differences.
 *
 * <p>This also allows more generally for applying color overlays.
 */
public class ColorOverlays {
  /**
   * Applies the dynamic color overlay for the application's activities, if available on the device.
   *
   * @param application the application to apply the theme to
   */
  public static void applyDynamicThemeToApplicationIfSupported(Application application) {
    applyThemeToApplication(application, new DynamicThemeOverlaySupplier());
  }

  /**
   * Applies color overlay from the given theme supplier to the application's activities.
   *
   * @param application the application to apply the theme to
   * @param themeOverlaySupplier the supplier for the theme to apply
   */
  public static void applyThemeToApplication(
      Application application, ThemeOverlaySupplier themeOverlaySupplier) {
    if (VERSION.SDK_INT >= VERSION_CODES.Q) {
      // onActivityPreCreated is only available in Q or above.
      application.registerActivityLifecycleCallbacks(
          new DynamicColorsActivityLifecycleCallbacks(themeOverlaySupplier));
    }
  }

  /**
   * Applies the dynamic color overlay for the given Activity's theme, if available on the device.
   *
   * @param activity the activity to apply the theme to
   */
  public static void applyDynamicThemeToActivityIfSupported(Activity activity) {
    applyThemeToActivity(activity, new DynamicThemeOverlaySupplier());
  }

  /**
   * Applies color overlay from given theme supplier to the activity.
   *
   * @param activity the activity to apply the theme to
   * @param themeOverlaySupplier the supplier for the theme to apply
   */
  public static void applyThemeToActivity(
      Activity activity, ThemeOverlaySupplier themeOverlaySupplier) {
    int theme = themeOverlaySupplier.getThemeOverlay(activity);
    if (theme != 0) {
      ThemeUtils.applyThemeOverlay(activity, theme);
    }
  }

  /**
   * Applies the dynamic color overlay for the given Context's theme, if available on the device.
   *
   * @param context the context to apply the theme to
   */
  public static Context wrapDynamicThemeIfSupported(Context context) {
    return wrapTheme(context, new DynamicThemeOverlaySupplier());
  }

  /**
   * Applies the color overlay set from given theme to the context.
   *
   * @param context the context to apply the theme to
   * @param themeOverlaySupplier the supplier for the theme to apply
   */
  public static Context wrapTheme(Context context, ThemeOverlaySupplier themeOverlaySupplier) {
    int theme = themeOverlaySupplier.getThemeOverlay(context);
    return theme == 0 ? context : new ContextThemeWrapper(context, theme);
  }

  private static class DynamicColorsActivityLifecycleCallbacks
      implements ActivityLifecycleCallbacks {

    private final ThemeOverlaySupplier themeOverlaySupplier;

    DynamicColorsActivityLifecycleCallbacks(ThemeOverlaySupplier themeOverlaySupplier) {
      this.themeOverlaySupplier = themeOverlaySupplier;
    }

    @Override
    public void onActivityPreCreated(Activity activity, @Nullable Bundle savedInstanceState) {
      applyThemeToActivity(activity, themeOverlaySupplier);
    }

    @Override
    public void onActivityCreated(Activity activity, @Nullable Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(Activity activity) {}

    @Override
    public void onActivityResumed(Activity activity) {}

    @Override
    public void onActivityPaused(Activity activity) {}

    @Override
    public void onActivityStopped(Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

    @Override
    public void onActivityDestroyed(Activity activity) {}
  }

  /** Interface for supplying the theme to be used. */
  public interface ThemeOverlaySupplier {
    /**
     * Returns the theme that should be applied on the given activity with the given theme overlay,
     * or 0 if it should not be applied.
     */
    int getThemeOverlay(Context themedContext);
  }

  private ColorOverlays() {}
}
