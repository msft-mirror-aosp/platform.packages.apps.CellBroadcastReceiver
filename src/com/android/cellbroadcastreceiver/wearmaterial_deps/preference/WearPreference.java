package com.google.android.clockwork.common.wearable.wearmaterial.preference;

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Wear-specific implementation of {@link Preference}. */
public class WearPreference extends Preference {

  // Suppress as this is only used after initialization.
  @SuppressWarnings({"nullness:assignment", "nullness:argument"})
  private WearPreferenceViewBinder viewBinder = new WearPreferenceViewBinder(this);

  public WearPreference(Context context) {
    this(context, null);
  }

  // AttributeSet can actually be null
  @SuppressWarnings("nullness:argument")
  public WearPreference(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public void onBindViewHolder(PreferenceViewHolder holder) {
    super.onBindViewHolder(holder);

    viewBinder.onBindViewHolder(holder);
  }

  @VisibleForTesting
  void setViewBinder(WearPreferenceViewBinder viewBinder) {
    this.viewBinder = viewBinder;
  }
}
