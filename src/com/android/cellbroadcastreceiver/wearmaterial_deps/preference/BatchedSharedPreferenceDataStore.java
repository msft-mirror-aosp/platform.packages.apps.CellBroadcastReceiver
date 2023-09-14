package com.google.android.clockwork.common.wearable.wearmaterial.preference;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import androidx.preference.PreferenceDataStore;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A wrapper around shared preferences, which does not call {@link Editor#apply()} until a call to
 * {@link #apply()}. This prevents issues during initialization where apply() is for each preference
 * with an initial value set on the first load of preferences, and exists to maintain parity between
 * the AndroidX {@link androidx.preference.PreferenceManager}'s implementation that calls blocks
 * commits during inflation.
 */
class BatchedSharedPreferenceDataStore extends PreferenceDataStore {

  private final SharedPreferences preferences;
  private @Nullable Editor editor;

  public BatchedSharedPreferenceDataStore(SharedPreferences preferences) {
    this.preferences = preferences;
  }

  @Override
  public void putString(String key, @Nullable String value) {
    getEditor().putString(key, value);
  }

  @Override
  public void putStringSet(String key, @Nullable Set<String> values) {
    getEditor().putStringSet(key, values);
  }

  @Override
  public void putInt(String key, int value) {
    getEditor().putInt(key, value);
  }

  @Override
  public void putLong(String key, long value) {
    getEditor().putLong(key, value);
  }

  @Override
  public void putFloat(String key, float value) {
    getEditor().putFloat(key, value);
  }

  @Override
  public void putBoolean(String key, boolean value) {
    getEditor().putBoolean(key, value);
  }

  @Override
  public @Nullable String getString(String key, @Nullable String defValue) {
    return preferences.getString(key, defValue);
  }

  @Override
  public @Nullable Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
    return preferences.getStringSet(key, defValues);
  }

  @Override
  public int getInt(String key, int defValue) {
    return preferences.getInt(key, defValue);
  }

  @Override
  public long getLong(String key, long defValue) {
    return preferences.getLong(key, defValue);
  }

  @Override
  public float getFloat(String key, float defValue) {
    return preferences.getFloat(key, defValue);
  }

  @Override
  public boolean getBoolean(String key, boolean defValue) {
    return preferences.getBoolean(key, defValue);
  }

  public void apply() {
    if (editor != null) {
      editor.apply();
    }
  }

  private Editor getEditor() {
    if (editor == null) {
      editor = preferences.edit();
    }
    return editor;
  }
}
