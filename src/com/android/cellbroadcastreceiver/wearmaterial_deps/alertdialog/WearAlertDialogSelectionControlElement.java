package com.google.android.clockwork.common.wearable.wearmaterial.alertdialog;

import android.util.ArraySet;
import android.view.View;
import com.google.android.clockwork.common.wearable.wearmaterial.button.CheckableWearButton;
import com.google.android.clockwork.common.wearable.wearmaterial.button.WearChipButton.ControlType;
import com.google.common.collect.ImmutableSet;
import java.util.Set;

/**
 * A Pojo for holding data for WearAlertDialogSelectionControlElement. This class can't be an
 * immutable AutoValue. It must be a mutable class because listeners need to be added or removed
 * from it.
 */
final class WearAlertDialogSelectionControlElement
    implements WearAlertDialogElement, CheckableWearButton {

  private final int selectionControlId;
  private final ControlType controlType;
  private final int selectionControlIconId;
  private final CharSequence selectionControlString;
  private final Set<OnWearCheckedChangeListener<CheckableWearButton>> onCheckedChangeListeners =
      new ArraySet<>();

  private int id = View.NO_ID;
  private boolean isChecked;
  private boolean toggleOnClick;

  WearAlertDialogSelectionControlElement(
      int selectionControlId,
      ControlType controlType,
      int selectionControlIconId,
      CharSequence selectionControlString) {
    this.selectionControlId = selectionControlId;
    this.controlType = controlType;
    this.selectionControlIconId = selectionControlIconId;
    this.selectionControlString = selectionControlString;
  }

  int getSelectionControlId() {
    return selectionControlId;
  }

  ControlType getControlType() {
    return controlType;
  }

  int getSelectionControlIconId() {
    return selectionControlIconId;
  }

  CharSequence getSelectionControlString() {
    return selectionControlString;
  }

  @Override
  public int getViewType() {
    return WearAlertDialogViewType.SELECTION_CONTROL;
  }

  @Override
  public int getId() {
    return id;
  }

  @Override
  public void setId(int id) {
    this.id = id;
  }

  @Override
  public void setToggleOnClick(boolean isToggleable) {
    this.toggleOnClick = isToggleable;
  }

  boolean getToggleOnClick() {
    return toggleOnClick;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void addOnCheckedChangeListener(
      OnWearCheckedChangeListener<? extends CheckableWearButton> listener) {
    onCheckedChangeListeners.add((OnWearCheckedChangeListener<CheckableWearButton>) listener);
  }

  @Override
  public void removeOnCheckedChangeListener(
      OnWearCheckedChangeListener<? extends CheckableWearButton> listener) {
    onCheckedChangeListeners.remove(listener);
  }

  @Override
  public void setChecked(boolean checked) {
    if (this.isChecked == checked) {
      return;
    }
    this.isChecked = checked;
    // Make a copy to avoid any ConcurrentModificationException.
    Set<OnWearCheckedChangeListener<CheckableWearButton>> listeners =
        ImmutableSet.copyOf(onCheckedChangeListeners);

    for (OnWearCheckedChangeListener<CheckableWearButton> listener : listeners) {
      listener.onCheckedChanged(this, isChecked);
    }
  }

  @Override
  public boolean isChecked() {
    return isChecked;
  }

  @Override
  public void toggle() {
    setChecked(!isChecked);
  }
}
