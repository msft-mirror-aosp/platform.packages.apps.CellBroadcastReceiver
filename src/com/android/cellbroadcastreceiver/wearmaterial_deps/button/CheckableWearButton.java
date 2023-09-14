package com.google.android.clockwork.common.wearable.wearmaterial.button;

import android.widget.Checkable;

/**
 * An interface that extends {@link Checkable} to add the ability to get and set a View's id and
 * requires its implementation to be able to handle more than one registered checked-state change
 * listener.
 *
 * <p>These requirements are necessary for any implementation that would like to be able to be
 * controlled by a {@link WearButtonGroupController}.
 */
public interface CheckableWearButton extends Checkable {

  /** Returns the id of this button. */
  int getId();

  /** Assigns an {@code id} to this button. */
  void setId(int id);

  /**
   * Determines if this button is toggleable when clicked.
   *
   * @param toggleOnClick boolean that determines if button is toggleable when a click is performed.
   */
  void setToggleOnClick(boolean toggleOnClick);

  /** Registers a {@code listener} that is invoked when the checked state of this button changes. */
  void addOnCheckedChangeListener(
      OnWearCheckedChangeListener<? extends CheckableWearButton> listener);

  /** Removes a previously registered callback {@code listener}. */
  void removeOnCheckedChangeListener(
      OnWearCheckedChangeListener<? extends CheckableWearButton> listener);

  /**
   * Interface definition for a callback to be invoked when the checked {@link CheckableWearButton}
   * changed in this group-controller.
   */
  interface OnWearCheckedChangeListener<T extends CheckableWearButton> {

    /**
     * Called when the checked state of the {@link CheckableWearButton} has changed.
     *
     * @param button The button whose state has changed
     * @param isChecked The new checked state of {@link CheckableWearButton}
     */
    void onCheckedChanged(T button, boolean isChecked);
  }
}
