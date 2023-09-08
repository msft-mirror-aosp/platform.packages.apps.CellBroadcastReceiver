package com.google.android.clockwork.common.wearable.wearmaterial.alertdialog;

final class WearAlertDialogActionButtonsElement implements WearAlertDialogElement {

  private final int positiveButtonIconId;
  private final int negativeButtonIconId;
  private final CharSequence positiveButtonContentDescription;
  private final CharSequence negativeButtonContentDescription;

  WearAlertDialogActionButtonsElement(
      int positiveButtonIconId,
      int negativeButtonIconId,
      CharSequence positiveButtonContentDescription,
      CharSequence negativeButtonContentDescription) {
    this.positiveButtonIconId = positiveButtonIconId;
    this.negativeButtonIconId = negativeButtonIconId;
    this.positiveButtonContentDescription = positiveButtonContentDescription;
    this.negativeButtonContentDescription = negativeButtonContentDescription;
  }

  int getPositiveButtonIconId() {
    return positiveButtonIconId;
  }

  int getNegativeButtonIconId() {
    return negativeButtonIconId;
  }

  CharSequence getPositiveButtonContentDescription() {
    return positiveButtonContentDescription;
  }

  CharSequence getNegativeButtonContentDescription() {
    return negativeButtonContentDescription;
  }

  @Override
  public int getViewType() {
    return WearAlertDialogViewType.ACTION_BUTTONS;
  }
}
