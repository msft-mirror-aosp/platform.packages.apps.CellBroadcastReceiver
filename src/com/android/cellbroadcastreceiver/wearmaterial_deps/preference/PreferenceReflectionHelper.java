package com.google.android.clockwork.common.wearable.wearmaterial.preference;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Helper for calling protected/restricted methods in {@link Preference}. */
class PreferenceReflectionHelper {

  private final Preference preference;

  private final Method onAttachedToHierarchyMethod;

  public PreferenceReflectionHelper(Preference preference) {
    this.preference = preference;

    onAttachedToHierarchyMethod = getOnAttachedToHierarchyMethod();
  }

  public void onAttachedToHierarchy(PreferenceManager preferenceManager) {
    try {
      onAttachedToHierarchyMethod.invoke(preference, preferenceManager);
    } catch (IllegalAccessException ignore) {
      // This shouldn't happen, since we made it accessible previously.
    } catch (InvocationTargetException e) {
      // Rethrow this as onAttachedToHierarchy has no checked exceptions.
      throw new AssertionError(e);
    }
  }

  private static Method getOnAttachedToHierarchyMethod() {
    try {
      Class<?> preferenceClass = Class.forName("androidx.preference.Preference");
      Method method =
          preferenceClass.getDeclaredMethod("onAttachedToHierarchy", PreferenceManager.class);
      method.setAccessible(true);
      return method;
    } catch (Exception e) {
      // If the method is not available, we won't be able to do the custom inflation of preferences
      // anyway so we may as well crash.
      throw new UnsupportedOperationException("onAttachedToHierarchy not available", e);
    }
  }
}
