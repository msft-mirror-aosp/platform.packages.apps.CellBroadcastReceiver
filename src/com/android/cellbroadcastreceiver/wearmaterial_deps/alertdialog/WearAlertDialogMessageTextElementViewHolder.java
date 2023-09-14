package com.google.android.clockwork.common.wearable.wearmaterial.alertdialog;

import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

/** A ViewHolder for showing WearAlertDialogMessageTextElement. */
final class WearAlertDialogMessageTextElementViewHolder extends ViewHolder {

  private final TextView wearAlertDialogMessageTextView;

  WearAlertDialogMessageTextElementViewHolder(View wearAlertDialogMessageTextView) {
    super(wearAlertDialogMessageTextView);
    this.wearAlertDialogMessageTextView = (TextView) wearAlertDialogMessageTextView;
  }

  void setWearAlertDialogMessageTextElement(
      WearAlertDialogMessageTextElement wearAlertDialogMessageTextElement) {
    CharSequence message = wearAlertDialogMessageTextElement.getMessageText();
    if (TextUtils.isEmpty(message)) {
      wearAlertDialogMessageTextView.setVisibility(View.GONE);
    } else {
      wearAlertDialogMessageTextView.setVisibility(View.VISIBLE);
      wearAlertDialogMessageTextView.setText(message);
    }

    if (!TextUtils.isEmpty(message)) {
      WearAlertDialogUtils.assignMessageViewGravity(wearAlertDialogMessageTextView);
    }
  }
}
