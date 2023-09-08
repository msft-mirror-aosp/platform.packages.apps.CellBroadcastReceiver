package com.google.android.clockwork.common.wearable.wearmaterial.alertdialog;

/** A Pojo for holding data for WearAlertDialogIconElement. */
class WearAlertDialogIconElement implements WearAlertDialogElement {

  private final int iconResId;

  public WearAlertDialogIconElement(int iconResId) {
    this.iconResId = iconResId;
  }

  public int getIconResId() {
    return iconResId;
  }

  @Override
  public final int getViewType() {
    return WearAlertDialogViewType.ICON;
  }
}
