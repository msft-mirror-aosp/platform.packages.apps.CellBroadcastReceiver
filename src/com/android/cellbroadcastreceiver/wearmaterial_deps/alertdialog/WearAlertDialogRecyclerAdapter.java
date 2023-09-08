package com.google.android.clockwork.common.wearable.wearmaterial.alertdialog;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.common.collect.ImmutableList;
import java.util.List;

/** An adapter for the WearAlertDialog. */
final class WearAlertDialogRecyclerAdapter extends RecyclerView.Adapter<ViewHolder> {

  private final ImmutableList<WearAlertDialogElement> elements;
  private final WearAlertDialogListener wearAlertDialogListener;

  WearAlertDialogRecyclerAdapter(
      List<WearAlertDialogElement> wearAlertDialogElements,
      WearAlertDialogListener wearAlertDialogListener) {
    elements = ImmutableList.copyOf(wearAlertDialogElements);
    this.wearAlertDialogListener = wearAlertDialogListener;
  }

  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    switch (viewType) {
      case WearAlertDialogViewType.ICON:
        View icon = inflater.inflate(R.layout.wear_alert_dialog_icon, parent, false);
        return new WearAlertDialogIconElementViewHolder(icon);
      case WearAlertDialogViewType.TITLE:
        View title = inflater.inflate(R.layout.wear_alert_dialog_title, parent, false);
        return new WearAlertDialogTitleElementViewHolder(title);
      case WearAlertDialogViewType.MESSAGE_TEXT:
        View messageText = inflater.inflate(R.layout.wear_alert_dialog_message_text, parent, false);
        return new WearAlertDialogMessageTextElementViewHolder(messageText);
      case WearAlertDialogViewType.CHIP_BUTTONS:
        View chipButton = inflater.inflate(R.layout.wear_alert_dialog_chip_button, parent, false);
        return new WearAlertDialogChipButtonElementViewHolder(chipButton, wearAlertDialogListener);
      case WearAlertDialogViewType.SELECTION_CONTROL:
        View selectionControl =
            inflater.inflate(R.layout.wear_alert_dialog_chip_button, parent, false);
        return new WearAlertDialogSelectionControlElementViewHolder(
            selectionControl, wearAlertDialogListener);
      case WearAlertDialogViewType.POSITIVE_CHIP:
        View positiveChipButton =
            inflater.inflate(R.layout.wear_alert_dialog_chip_button, parent, false);
        return new WearAlertDialogPositiveChipElementViewHolder(
            positiveChipButton, wearAlertDialogListener);
      case WearAlertDialogViewType.NEGATIVE_CHIP:
        View negativeChipButton =
            inflater.inflate(R.layout.wear_alert_dialog_chip_button, parent, false);
        return new WearAlertDialogNegativeChipElementViewHolder(
            negativeChipButton, wearAlertDialogListener);
      case WearAlertDialogViewType.ACTION_BUTTONS:
        View actionsButtons =
            inflater.inflate(R.layout.wear_alert_dialog_action_buttons, parent, false);
        return new WearAlertDialogActionButtonsElementViewHolder(
            actionsButtons, wearAlertDialogListener);
      default:
        throw new UnsupportedOperationException(
            "#onCreateViewHolder: unsupported viewType: " + viewType);
    }
  }

  @Override
  public void onBindViewHolder(ViewHolder viewHolder, int position) {
    if (viewHolder instanceof WearAlertDialogIconElementViewHolder) {
      WearAlertDialogIconElement wearAlertDialogIconElement =
          (WearAlertDialogIconElement) elements.get(position);
      WearAlertDialogIconElementViewHolder wearAlertDialogIconElementViewHolder =
          (WearAlertDialogIconElementViewHolder) viewHolder;
      wearAlertDialogIconElementViewHolder.setWearAlertDialogIconElement(
          wearAlertDialogIconElement);
    } else if (viewHolder instanceof WearAlertDialogTitleElementViewHolder) {
      WearAlertDialogTitleElement wearAlertDialogTitleElement =
          (WearAlertDialogTitleElement) elements.get(position);
      WearAlertDialogTitleElementViewHolder wearAlertDialogTitleElementViewHolder =
          (WearAlertDialogTitleElementViewHolder) viewHolder;
      wearAlertDialogTitleElementViewHolder.setWearAlertDialogTitleElement(
          wearAlertDialogTitleElement);
    } else if (viewHolder instanceof WearAlertDialogMessageTextElementViewHolder) {
      WearAlertDialogMessageTextElement wearAlertDialogMessageTextElement =
          (WearAlertDialogMessageTextElement) elements.get(position);
      WearAlertDialogMessageTextElementViewHolder wearAlertDialogMessageTextElementViewHolder =
          (WearAlertDialogMessageTextElementViewHolder) viewHolder;
      wearAlertDialogMessageTextElementViewHolder.setWearAlertDialogMessageTextElement(
          wearAlertDialogMessageTextElement);
    } else if (viewHolder instanceof WearAlertDialogChipButtonElementViewHolder) {
      WearAlertDialogChipButtonElement wearAlertDialogChipButtonElement =
          (WearAlertDialogChipButtonElement) elements.get(position);
      WearAlertDialogChipButtonElementViewHolder wearAlertDialogChipButtonElementViewHolder =
          (WearAlertDialogChipButtonElementViewHolder) viewHolder;
      wearAlertDialogChipButtonElementViewHolder.setWearAlertDialogChipButtonElement(
          wearAlertDialogChipButtonElement);
    } else if (viewHolder instanceof WearAlertDialogSelectionControlElementViewHolder) {
      WearAlertDialogSelectionControlElement wearAlertDialogSelectionControlElement =
          (WearAlertDialogSelectionControlElement) elements.get(position);
      WearAlertDialogSelectionControlElementViewHolder
          wearAlertDialogSelectionControlElementViewHolder =
              (WearAlertDialogSelectionControlElementViewHolder) viewHolder;
      wearAlertDialogSelectionControlElementViewHolder.setWearAlertDialogSelectionControlElement(
          wearAlertDialogSelectionControlElement);
    } else if (viewHolder instanceof WearAlertDialogPositiveChipElementViewHolder) {
      WearAlertDialogPositiveChipElement wearAlertDialogPositiveChipElement =
          (WearAlertDialogPositiveChipElement) elements.get(position);
      WearAlertDialogPositiveChipElementViewHolder wearAlertDialogPositiveChipElementViewHolder =
          (WearAlertDialogPositiveChipElementViewHolder) viewHolder;
      wearAlertDialogPositiveChipElementViewHolder.setWearAlertDialogPositiveChipElement(
          wearAlertDialogPositiveChipElement);
    } else if (viewHolder instanceof WearAlertDialogNegativeChipElementViewHolder) {
      WearAlertDialogNegativeChipElement wearAlertDialogNegativeChipElement =
          (WearAlertDialogNegativeChipElement) elements.get(position);
      WearAlertDialogNegativeChipElementViewHolder wearAlertDialogNegativeChipElementViewHolder =
          (WearAlertDialogNegativeChipElementViewHolder) viewHolder;
      wearAlertDialogNegativeChipElementViewHolder.setWearAlertDialogNegativeChipElement(
          wearAlertDialogNegativeChipElement);
    } else if (viewHolder instanceof WearAlertDialogActionButtonsElementViewHolder) {
      WearAlertDialogActionButtonsElement wearAlertDialogActionButtonsElement =
          (WearAlertDialogActionButtonsElement) elements.get(position);
      WearAlertDialogActionButtonsElementViewHolder wearAlertDialogActionButtonsElementViewHolder =
          (WearAlertDialogActionButtonsElementViewHolder) viewHolder;
      wearAlertDialogActionButtonsElementViewHolder.setWearAlertDialogActionButtonsElement(
          wearAlertDialogActionButtonsElement);
    }
  }

  @Override
  public int getItemCount() {
    return elements.size();
  }

  @Override
  public int getItemViewType(int position) {
    return elements.get(position).getViewType();
  }

  ImmutableList<WearAlertDialogElement> getElements() {
    return elements;
  }
}
