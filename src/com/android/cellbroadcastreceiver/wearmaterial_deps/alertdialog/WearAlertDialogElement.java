package com.google.android.clockwork.common.wearable.wearmaterial.alertdialog;

/** An element to be displayed in the WearAlertDialog. */
interface WearAlertDialogElement {

  /** Returns the {@link WearAlertDialogViewType} of the element. */
  @WearAlertDialogViewType
  int getViewType();
}
