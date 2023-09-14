package com.google.android.clockwork.common.wearable.wearmaterial.alertdialog;

/** A Pojo for holding data for WearAlertDialogPositiveChipElement. */
class WearAlertDialogPositiveChipElement implements WearAlertDialogElement {

  private final CharSequence positiveChipText;

  public WearAlertDialogPositiveChipElement(CharSequence positiveChipText) {
    this.positiveChipText = positiveChipText;
  }

  public CharSequence getPositiveChipText() {
    return positiveChipText;
  }

  @Override
  public final int getViewType() {
    return WearAlertDialogViewType.POSITIVE_CHIP;
  }
}
