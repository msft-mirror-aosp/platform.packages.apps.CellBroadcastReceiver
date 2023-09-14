package com.google.android.clockwork.common.wearable.wearmaterial.preference;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.TwoStatePreference;
import com.google.android.clockwork.common.wearable.wearmaterial.button.WearChipButton;

/**
 * Handles binding data to a {@link WearChipButton}. We need this as we need types to look like
 * their AndroidX parents, so this lets us keep some functionality some common to {@link
 * WearPreference} subclasses.
 */
public class WearPreferenceViewBinder {
  private final Preference preference;

  public WearPreferenceViewBinder(Preference preference) {
    this.preference = preference;
  }

  public void onBindViewHolder(PreferenceViewHolder holder) {
    if (!(holder.itemView instanceof WearChipButton)) {
      // Ignore usage of non-WearMaterial theme, as it'll be handled by the parent implementation.
      return;
    }

    WearChipButton button = (WearChipButton) holder.itemView;

    button.setPrimaryText(preference.getTitle());
    button.setSecondaryText(preference.getSummary());
    button.setIcon(preference.getIcon());

    // Special handling for TwoStatePreference
    if (preference instanceof TwoStatePreference) {
      boolean isChecked = ((TwoStatePreference) preference).isChecked();
      button.setChecked(isChecked);
    }
  }
}
