package com.google.android.clockwork.common.wearable.wearmaterial.alertdialog;

import androidx.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Intdef for different items in the WearAlertDialogElements. */
@Retention(RetentionPolicy.SOURCE)
@IntDef({
  WearAlertDialogViewType.ICON,
  WearAlertDialogViewType.TITLE,
  WearAlertDialogViewType.MESSAGE_TEXT,
  WearAlertDialogViewType.CHIP_BUTTONS,
  WearAlertDialogViewType.SELECTION_CONTROL,
  WearAlertDialogViewType.POSITIVE_CHIP,
  WearAlertDialogViewType.NEGATIVE_CHIP,
  WearAlertDialogViewType.ACTION_BUTTONS
})
public @interface WearAlertDialogViewType {
  int ICON = 0;
  int TITLE = 1;
  int MESSAGE_TEXT = 2;
  int CHIP_BUTTONS = 3;
  int SELECTION_CONTROL = 4;
  int POSITIVE_CHIP = 5;
  int NEGATIVE_CHIP = 6;
  int ACTION_BUTTONS = 7;
}
