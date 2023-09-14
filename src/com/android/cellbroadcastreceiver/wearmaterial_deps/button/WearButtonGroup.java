package com.google.android.clockwork.common.wearable.wearmaterial.button;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Checkable;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Forked from {@link RadioGroup} but supports wear buttons instead. Since RadioGroup normally
 * supports adding RadioButton instances, this breaks encapsulation with WearChipButton. Performing
 * check tracking using viewId on the supplied RadioButton will also not work, as that will require
 * RadioButton Id to be set externally. Instead, WearButtonGroup provides the same functionality
 * with added benefits of being able to group check state with mix and matching of any and all
 * {@link WearButtonGroup} variations as long as it is checkable.
 *
 * <p>Note: When adding a {@link WearButton} or a {@link View} that implements {@link
 * CheckableWearButton} to a {@code WearButtonGroup}, be sure to <b>not</b> call {@link
 * View#setOnClickListener(OnClickListener)} on the button or view. Doing so will interfere with the
 * {@code WearButtonGroup}'s behavior. Instead, use a {@link OnCheckedChangeListener} listener to
 * get notified of changes in the selection in this {@code WearButtonGroup} (see {@link
 * #setOnCheckedChangeListener(OnCheckedChangeListener)}.
 */
public class WearButtonGroup extends LinearLayout {

  private final WearButtonGroupController controller = new WearButtonGroupController();

  private final PassThroughHierarchyChangeListener passThroughListener =
      new PassThroughHierarchyChangeListener();

  private @Nullable OnCheckedChangeListener onCheckedChangeListener;
  private int checkedId = View.NO_ID;

  @SuppressWarnings("nullness:method.invocation")
  public WearButtonGroup(Context context) {
    this(context, null);
  }

  @SuppressWarnings({"method.invocation", "methodref.receiver.bound"})
  public WearButtonGroup(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);

    // Retrieve selected wear button as requested by the user in the XML layout file.
    TypedArray attributes =
        context.obtainStyledAttributes(
            attrs, R.styleable.WearButtonGroup, R.attr.wearButtonGroupStyle, 0);
    int value =
        attributes.getResourceId(R.styleable.WearButtonGroup_android_checkedButton, View.NO_ID);
    final int index = attributes.getInt(R.styleable.WearButtonGroup_android_orientation, VERTICAL);
    setOrientation(index);
    attributes.recycle();

    controller.check(value);
    super.setOnHierarchyChangeListener(passThroughListener);

    Runnable notifyOnCheckedChangeListener = this::notifyOnCheckedChangeListener;
    controller.setOnCheckedChangeListener(
        (button, checked) -> {
          checkedId = checked ? button.getId() : View.NO_ID;
          removeCallbacks(notifyOnCheckedChangeListener);
          post(notifyOnCheckedChangeListener);
        });
  }

  @Override
  public void setOnHierarchyChangeListener(@Nullable OnHierarchyChangeListener listener) {
    // The user listener is delegated to our pass-through listener
    passThroughListener.onHierarchyChangeListener = listener;
  }

  /**
   * Sets the selection to the wear button whose identifier is passed in parameter. Using -1 as the
   * selection identifier clears the selection; such an operation is equivalent to invoking {@link
   * #clearCheck()}.
   *
   * @param id the unique id of the radiwearton to select in this group
   * @see #getCheckedWearButtonId()
   * @see #clearCheck()
   */
  public void check(int id) {
    controller.check(id);
  }

  /**
   * Returns the identifier of the selected wear button in this group. Upon empty selection, the
   * returned value is -1.
   *
   * @return the unique id of the selected wear button in this group
   * @see #check(int)
   * @see #clearCheck()
   */
  public int getCheckedWearButtonId() {
    return controller.getCheckedButtonId();
  }

  /**
   * Clears the selection. When the selection is cleared, no wear button in this group is selected
   * and {@link #getCheckedWearButtonId()} returns null.
   *
   * @see #check(int)
   * @see #getCheckedWearButtonId()
   */
  public void clearCheck() {
    controller.clearCheck();
  }

  /**
   * Register a callback to be invoked when the checked wear button changes in this group.
   *
   * @param listener the callback to call on checked state change
   */
  public void setOnCheckedChangeListener(@Nullable OnCheckedChangeListener listener) {
    onCheckedChangeListener = listener;
  }

  private void notifyOnCheckedChangeListener() {
    if (onCheckedChangeListener != null) {
      onCheckedChangeListener.onCheckedChanged(this, checkedId);
    }
  }

  @Override
  protected LinearLayout.LayoutParams generateDefaultLayoutParams() {
    return new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
  }

  @Override
  public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
    super.onInitializeAccessibilityEvent(event);
    event.setClassName(WearButtonGroup.class.getName());
  }

  @Override
  public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
    super.onInitializeAccessibilityNodeInfo(info);
    info.setClassName(WearButtonGroup.class.getName());
  }

  /**
   * Interface definition for a callback to be invoked when the checked wear button changed in this
   * group.
   */
  public interface OnCheckedChangeListener {
    /**
     * Called when the checked wear button has changed. When the selection is cleared, checkedId is
     * -1.
     *
     * @param group the group in which the checked wear button has changed
     * @param checkedId the unique identifier of the newly checked wear button
     */
    void onCheckedChanged(WearButtonGroup group, int checkedId);
  }

  /**
   * A pass-through listener acts upon the events and dispatches them to another listener. This
   * allows the table layout to set its own internal hierarchy change listener without preventing
   * the user to setup theirs.
   */
  private class PassThroughHierarchyChangeListener implements ViewGroup.OnHierarchyChangeListener {
    private @Nullable OnHierarchyChangeListener onHierarchyChangeListener;

    @Override
    public void onChildViewAdded(View parent, View child) {
      if (child instanceof CheckableWearButton) {
        controller.addButton((CheckableWearButton) child);
        child.setOnClickListener(this::checkIfUnchecked);
      }
      if (onHierarchyChangeListener != null) {
        onHierarchyChangeListener.onChildViewAdded(parent, child);
      }
    }

    @Override
    public void onChildViewRemoved(View parent, View child) {
      if (child instanceof CheckableWearButton) {
        child.setOnClickListener(null);
        controller.removeButton((CheckableWearButton) child);
      }
      if (onHierarchyChangeListener != null) {
        onHierarchyChangeListener.onChildViewRemoved(parent, child);
      }
    }

    private void checkIfUnchecked(View view) {
      if (view instanceof Checkable) {
        Checkable button = (Checkable) view;
        // Since the button's "toggleOnClick" is false, check it when it is clicked.
        if (!button.isChecked()) {
          button.setChecked(true);
        }
      }
    }
  }
}
