package com.google.android.clockwork.common.wearable.wearmaterial.alertdialog;

import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import android.view.View;
import com.google.android.clockwork.common.wearable.wearmaterial.button.WearChipButton;

/** A ViewHolder for showing WearAlertDialogChipButtonElement. */
final class WearAlertDialogChipButtonElementViewHolder extends ViewHolder {

  private final WearChipButton chipButton;

  private int buttonId;

  WearAlertDialogChipButtonElementViewHolder(
      View chipButton, WearAlertDialogListener wearAlertDialogListener) {
    super(chipButton);
    this.chipButton = (WearChipButton) chipButton;
    this.chipButton.setOnClickListener(
        v -> wearAlertDialogListener.onActionButtonClicked(this.buttonId));
  }

  void setWearAlertDialogChipButtonElement(
      WearAlertDialogChipButtonElement wearAlertDialogChipButtonElement) {
    this.buttonId = wearAlertDialogChipButtonElement.getChipButtonId();
    chipButton.setIcon(wearAlertDialogChipButtonElement.getChipButtonIconId());
    chipButton.setPrimaryText(wearAlertDialogChipButtonElement.getChipButtonString());
    chipButton.setContentDescription(
        wearAlertDialogChipButtonElement.getChipButtonContentDescription());
  }
}
