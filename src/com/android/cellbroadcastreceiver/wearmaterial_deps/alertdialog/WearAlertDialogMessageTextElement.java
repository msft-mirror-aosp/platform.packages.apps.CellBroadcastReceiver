package com.google.android.clockwork.common.wearable.wearmaterial.alertdialog;

/** A Pojo for holding data for WearAlertDialogMessageTextElement. */
class WearAlertDialogMessageTextElement implements WearAlertDialogElement {

  private final CharSequence messageText;

  public WearAlertDialogMessageTextElement(CharSequence messageText) {
    this.messageText = messageText;
  }

  public CharSequence getMessageText() {
    return messageText;
  }

  @Override
  public final int getViewType() {
    return WearAlertDialogViewType.MESSAGE_TEXT;
  }
}
