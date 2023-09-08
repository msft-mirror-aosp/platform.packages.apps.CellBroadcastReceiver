package com.google.android.clockwork.common.wearable.wearmaterial.alertdialog;

/** Callbacks that triggers when action buttons or selection controls are clicked. */
interface WearAlertDialogListener {

  void onActionButtonClicked(int buttonId);

  void onSelectionControlChanged(int buttonId, boolean isChecked);
}
