package com.google.android.clockwork.common.wearable.wearmaterial.button;

import android.view.View;
import android.widget.Checkable;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;
import com.google.android.clockwork.common.wearable.wearmaterial.button.CheckableWearButton.OnWearCheckedChangeListener;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Controls the behavior of a group of {@link CheckableWearButton}s.
 *
 * <p>It keeps track of a collection of {@link CheckableWearButton}s and at most one of them can be
 * checked at any given time. If another is checked, the current one will become unchecked.
 *
 * <p>A {@code WearButtonGroupController} allows for buttons, that are not siblings of each other,
 * to behave like a radio-group.
 *
 * <p>A {@code WearButtonGroupController} uses {@link CheckableWearButton}s instead of checkable
 * {@link View}s. This allows it to manage the checked-state of POJOs, e.g. a list of data
 * representing radio-buttons inside a recycler-view.
 */
public class WearButtonGroupController {

  private final Map<Integer, CheckableWearButton> groupedButtons = new ArrayMap<>();

  @SuppressWarnings("nullness:method.invocation")
  private final OnWearCheckedChangeListener<CheckableWearButton> checkedStateTracker =
      (button, checked) -> ensureButtonIsChecked(button.getId(), checked);

  private final Set<Integer> idsOfButtonsToSkipSetChecked = new ArraySet<>();

  private int checkedId = View.NO_ID;
  private @Nullable OnWearCheckedChangeListener<CheckableWearButton> onCheckedChangeListener;

  /** Adds the {@link CheckableWearButton button} to this group-controller. */
  public void addButton(CheckableWearButton button) {
    int id = button.getId();
    if (id == View.NO_ID) {
      id = View.generateViewId();
    }

    if (groupedButtons.containsKey(id)) {
      return;
    }

    button.setId(id);
    groupedButtons.put(id, button);

    boolean isChecked = button.isChecked();
    if (isChecked || id == checkedId) {
      ensureButtonIsChecked(id, true);
      if (!isChecked && button.isChecked()) {
        notifyListener(id, true);
      }
    }
    button.setToggleOnClick(false);
    button.addOnCheckedChangeListener(checkedStateTracker);
  }

  /** Removes the {@link CheckableWearButton button} from this group-controller. */
  public void removeButton(CheckableWearButton button) {
    int id = button.getId();
    if (!groupedButtons.containsKey(id)) {
      return;
    }

    button.removeOnCheckedChangeListener(checkedStateTracker);
    button.setToggleOnClick(true);
    groupedButtons.remove(id);
    ensureButtonIsChecked();
  }

  /** Removes all the {@link CheckableWearButton buttons} from this group-controller. */
  public void removeAllButtons() {
    for (CheckableWearButton button : groupedButtons.values()) {
      button.removeOnCheckedChangeListener(checkedStateTracker);
      button.setToggleOnClick(true);
    }
    groupedButtons.clear();
    ensureButtonIsChecked();
  }

  /**
   * Registers a {@code listener} to be invoked when the checked {@link CheckableWearButton} changes
   * in this group. Unregister the listener by providing the value {@code null} for the {@code
   * listener}.
   */
  @SuppressWarnings("unchecked")
  public void setOnCheckedChangeListener(
      @Nullable OnWearCheckedChangeListener<? extends CheckableWearButton> listener) {
    onCheckedChangeListener = (OnWearCheckedChangeListener<CheckableWearButton>) listener;
  }

  public int getCheckedButtonId() {
    return checkedId;
  }

  /**
   * Checks the {@link CheckableWearButton} whose id is equal to the provided {@code id}. Using
   * {@link View#NO_ID} as the {@code id} clears the selection; such an operation is equivalent to
   * invoking {@link #clearCheck()}.
   *
   * @see #getCheckedButtonId()
   * @see #clearCheck()
   */
  public void check(int id) {
    if (id != View.NO_ID && (id == checkedId)) {
      return;
    }
    if (checkedId != View.NO_ID) {
      setCheckedStateForButton(checkedId, false);
    }
    if (id != View.NO_ID) {
      setCheckedStateForButton(id, true);
    }
    changeCheckedIdAndNotify(id);
  }

  /** Unchecks all {@link CheckableWearButton}s managed by this group-controller. */
  public void clearCheck() {
    check(View.NO_ID);
  }

  private void ensureButtonIsChecked() {
    if (groupedButtons.containsKey(checkedId)) {
      ensureButtonIsChecked(checkedId, true);
    } else {
      ensureButtonIsChecked(View.NO_ID, false);
    }
  }

  private void ensureButtonIsChecked(int id, boolean isChecked) {
    boolean isCurrentButton = this.checkedId == id;
    if (groupedButtons.containsKey(id)) {
      // Ensure the current button's checked-state, or that the other button becomes unchecked.
      setCheckedStateForButton(this.checkedId, isCurrentButton && isChecked);
    }
    // Either the other button becomes checked or not, or the current button becomes unchecked.
    changeCheckedIdAndNotify(!isCurrentButton || isChecked ? id : View.NO_ID);
  }

  /** Sets the {@code checked}-state of the button with the given {@code id}. */
  private void setCheckedStateForButton(int id, boolean checked) {
    if (idsOfButtonsToSkipSetChecked.contains(id)) {
      return;
    }

    idsOfButtonsToSkipSetChecked.add(id);
    Checkable button = groupedButtons.get(id);
    if (button != null) {
      button.setChecked(checked);
    }
    idsOfButtonsToSkipSetChecked.remove(id);
  }

  /** Sets the {@link #checkedId} and notifies the registered {@link #onCheckedChangeListener}. */
  private void changeCheckedIdAndNotify(int id) {
    if (checkedId == id) {
      return;
    }

    notifyListener(checkedId, false);
    checkedId = id;
    notifyListener(checkedId, true);
  }

  private void notifyListener(int id, boolean isChecked) {
    if (onCheckedChangeListener != null) {
      CheckableWearButton button = groupedButtons.get(id);
      if (button != null) {
        onCheckedChangeListener.onCheckedChanged(button, isChecked);
      }
    }
  }
}
