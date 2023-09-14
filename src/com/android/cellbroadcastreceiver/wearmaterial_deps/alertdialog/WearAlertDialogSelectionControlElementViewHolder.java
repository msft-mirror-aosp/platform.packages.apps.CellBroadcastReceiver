package com.google.android.clockwork.common.wearable.wearmaterial.alertdialog;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import android.view.View;
import com.google.android.clockwork.common.wearable.wearmaterial.button.WearChipButton;

/** A ViewHolder for showing WearAlertDialogSelectionControlElement. */
final class WearAlertDialogSelectionControlElementViewHolder extends ViewHolder {

  private final WearChipButton wearChipButton;

  private WearAlertDialogSelectionControlElement selectionControl;

  WearAlertDialogSelectionControlElementViewHolder(
      View button, WearAlertDialogListener wearAlertDialogListener) {
    super(button);
    wearChipButton = (WearChipButton) button;
    wearChipButton.setCheckable(true);

    WearAlertDialogUtils.applyStyleBackground(
        this.wearChipButton, R.attr.wearAlertDialogCheckableChipStyle);
    WearAlertDialogUtils.applyStyleColors(
        this.wearChipButton, R.attr.wearAlertDialogCheckableChipStyle);

    this.wearChipButton.setOnClickListener(
        v -> {
          wearChipButton.setChecked(!wearChipButton.isChecked());
          wearAlertDialogListener.onActionButtonClicked(
              checkNotNull(selectionControl).getSelectionControlId());
        });

    this.wearChipButton.addOnCheckedChangeListener(
        (b, checked) -> {
          checkNotNull(selectionControl).setChecked(checked);
          wearAlertDialogListener.onSelectionControlChanged(
              selectionControl.getSelectionControlId(), checked);
        });
  }

  void setWearAlertDialogSelectionControlElement(
      WearAlertDialogSelectionControlElement wearAlertDialogSelectionControlElement) {
    selectionControl = wearAlertDialogSelectionControlElement;
    wearChipButton.setIcon(wearAlertDialogSelectionControlElement.getSelectionControlIconId());
    wearChipButton.setPrimaryText(
        wearAlertDialogSelectionControlElement.getSelectionControlString());
    wearChipButton.setControlType(wearAlertDialogSelectionControlElement.getControlType());
    wearChipButton.setChecked(wearAlertDialogSelectionControlElement.isChecked());
    wearChipButton.setToggleOnClick(wearAlertDialogSelectionControlElement.getToggleOnClick());
  }
}
