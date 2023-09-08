package com.google.android.clockwork.common.wearable.wearmaterial.alertdialog;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.widget.ImageView;

/** A ViewHolder for showing WearAlertDialogIconElement. */
final class WearAlertDialogIconElementViewHolder extends ViewHolder {

  private final ImageView wearAlertDialogIconView;

  WearAlertDialogIconElementViewHolder(View icon) {
    super(icon);
    wearAlertDialogIconView = (ImageView) icon.findViewById(R.id.wear_alertdialog_icon);
  }

  void setWearAlertDialogIconElement(WearAlertDialogIconElement wearAlertDialogIconElement) {
    Context context = wearAlertDialogIconView.getContext();
    int iconResId = wearAlertDialogIconElement.getIconResId();
    if (iconResId != 0) {
      wearAlertDialogIconView.setVisibility(View.VISIBLE);
      wearAlertDialogIconView.setImageDrawable(context.getDrawable(iconResId));
    } else {
      wearAlertDialogIconView.setVisibility(View.GONE);
    }
  }
}
