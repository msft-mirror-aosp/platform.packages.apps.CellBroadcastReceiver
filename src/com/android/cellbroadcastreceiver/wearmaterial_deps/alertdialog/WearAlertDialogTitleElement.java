package com.google.android.clockwork.common.wearable.wearmaterial.alertdialog;

/** A Pojo for holding data for WearAlertDialogTitleElement. */
class WearAlertDialogTitleElement implements WearAlertDialogElement {

  private final CharSequence title;

  public WearAlertDialogTitleElement(CharSequence title) {
    this.title = title;
  }

  public CharSequence getTitle() {
    return title;
  }

  @Override
  public final int getViewType() {
    return WearAlertDialogViewType.TITLE;
  }
}
