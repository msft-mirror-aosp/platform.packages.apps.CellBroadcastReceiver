package com.google.android.clockwork.common.wearable.wearmaterial.preference;

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreference;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Wear-specific implementation of {@link WearSwitchPreference}. */
public class WearSwitchPreference extends SwitchPreference {

  // Suppress as this is only used after initialization.
  @SuppressWarnings({"nullness:assignment", "nullness:argument"})
  private WearPreferenceViewBinder viewBinder = new WearPreferenceViewBinder(this);

  public WearSwitchPreference(Context context) {
    this(context, null);
  }

  // AttributeSet can actually be null
  @SuppressWarnings("nullness:argument")
  public WearSwitchPreference(Context context, @Nullable AttributeSet attrs) {
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
