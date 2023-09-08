package com.google.android.clockwork.common.wearable.wearmaterial.alertdialog;

import static com.google.android.clockwork.common.wearable.wearmaterial.alertdialog.WearAlertDialog.NEGATIVE_ACTION_CHIP_ID;

import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import android.view.View;
import com.google.android.clockwork.common.wearable.wearmaterial.button.WearChipButton;
import com.google.android.clockwork.common.wearable.wearmaterial.button.WearChipButton.TextHorizontalPos;

/** A ViewHolder for showing WearAlertDialogNegativeChipElement. */
final class WearAlertDialogNegativeChipElementViewHolder extends ViewHolder {

  private final WearChipButton negativeChipButton;

  private int negativeChipButtonId;

  WearAlertDialogNegativeChipElementViewHolder(
      View negativeChipButton, WearAlertDialogListener wearAlertDialogListener) {
    super(negativeChipButton);
    this.negativeChipButton = (WearChipButton) negativeChipButton;

    WearAlertDialogUtils.applyStyleColors(
        this.negativeChipButton, R.attr.wearAlertDialogButtonNegativeStyle);

    this.negativeChipButton.setOnClickListener(
        v -> wearAlertDialogListener.onActionButtonClicked(negativeChipButtonId));
  }

  void setWearAlertDialogNegativeChipElement(
      WearAlertDialogNegativeChipElement wearAlertDialogNegativeChipElement) {
    this.negativeChipButtonId = NEGATIVE_ACTION_CHIP_ID;
    negativeChipButton.setPrimaryText(wearAlertDialogNegativeChipElement.getNegativeChipText());
    negativeChipButton.setPrimaryTextHorizontalPosition(TextHorizontalPos.CENTER);
  }
}
