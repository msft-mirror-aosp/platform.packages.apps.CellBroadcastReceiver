package com.google.android.clockwork.common.wearable.wearmaterial.alertdialog;

/** A Pojo for holding data for WearAlertDialogNegativeChipElement. */
class WearAlertDialogNegativeChipElement implements WearAlertDialogElement {

  private final CharSequence negativeChipText;

  public WearAlertDialogNegativeChipElement(CharSequence negativeChipText) {
    this.negativeChipText = negativeChipText;
  }

  public CharSequence getNegativeChipText() {
    return negativeChipText;
  }

  @Override
  public final int getViewType() {
    return WearAlertDialogViewType.NEGATIVE_CHIP;
  }
}
