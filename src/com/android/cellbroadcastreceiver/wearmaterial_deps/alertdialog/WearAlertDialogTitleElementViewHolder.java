package com.google.android.clockwork.common.wearable.wearmaterial.alertdialog;

import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

/** A ViewHolder for showing WearAlertDialogTitleElement. */
final class WearAlertDialogTitleElementViewHolder extends ViewHolder {

  private final TextView wearAlertDialogTitleView;

  WearAlertDialogTitleElementViewHolder(View wearAlertDialogTitle) {
    super(wearAlertDialogTitle);
    this.wearAlertDialogTitleView =
        (TextView) wearAlertDialogTitle.findViewById(R.id.wear_alertdialog_title_text);
  }

  void setWearAlertDialogTitleElement(WearAlertDialogTitleElement wearAlertDialogTitleElement) {
    CharSequence title = wearAlertDialogTitleElement.getTitle();
    if (TextUtils.isEmpty(wearAlertDialogTitleElement.getTitle())) {
      wearAlertDialogTitleView.setVisibility(View.GONE);
    } else {
      wearAlertDialogTitleView.setVisibility(View.VISIBLE);
      wearAlertDialogTitleView.setText(title);
    }
  }
}
