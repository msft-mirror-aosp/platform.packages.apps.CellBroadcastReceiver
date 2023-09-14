package com.google.android.clockwork.common.wearable.wearmaterial.preference;

import static java.lang.Math.min;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.fragment.app.FragmentManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceViewHolder;
import com.google.android.clockwork.common.wearable.wearmaterial.alertdialog.WearAlertDialog;
import com.google.android.clockwork.common.wearable.wearmaterial.alertdialog.WearAlertDialog.OnActionClickListener;
import com.google.android.clockwork.common.wearable.wearmaterial.alertdialog.WearAlertDialog.OnDismissListener;
import com.google.android.clockwork.common.wearable.wearmaterial.button.WearChipButton.ControlType;
import com.google.android.clockwork.common.wearable.wearmaterial.util.TypedArrayUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Wear-specific implementation of {@link ListPreference}. */
public class WearListPreference extends ListPreference
    implements OnActionClickListener, OnDismissListener {

  public static final int MIN_CONTROL_ID = 1;
  private boolean dismissDialogOnClick;
  private int dialogIconResId;

  // Suppress as this is only used after initialization.
  @SuppressWarnings({"nullness:assignment", "nullness:argument"})
  private WearPreferenceViewBinder viewBinder = new WearPreferenceViewBinder(this);

  private @Nullable WearAlertDialog alertDialog;

  private boolean entrySelected;
  private int selectedIndex;

  public WearListPreference(Context context) {
    this(context, null);
  }

  // AttributeSet can actually be null
  @SuppressWarnings("nullness:argument")
  public WearListPreference(Context context, @Nullable AttributeSet attrs) {
    this(
        context,
        attrs,
        TypedArrayUtils.getAttr(
            context, R.attr.dialogPreferenceStyle, android.R.attr.dialogPreferenceStyle));
  }

  public WearListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
    this(context, attrs, defStyleAttr, 0);
  }

  public WearListPreference(
      Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);

    // Get the dialog icon resource id instead of the drawable from the base class, as the dialog
    // implementation sends this over a bundle
    TypedArray array =
        context.obtainStyledAttributes(
            attrs, R.styleable.DialogPreference, defStyleAttr, defStyleRes);
    dialogIconResId =
        TypedArrayUtils.getResourceId(
            array,
            R.styleable.DialogPreference_android_dialogIcon,
            R.styleable.DialogPreference_dialogIcon,
            0);
    TypedArray wearListPreferenceArray =
        context.obtainStyledAttributes(
            attrs, R.styleable.WearListPreference, defStyleAttr, defStyleRes);
    dismissDialogOnClick =
        wearListPreferenceArray.getBoolean(
            R.styleable.WearListPreference_dismissDialogOnClick, false);
    array.recycle();
    wearListPreferenceArray.recycle();
  }

  @Override
  public void onBindViewHolder(PreferenceViewHolder holder) {
    super.onBindViewHolder(holder);

    viewBinder.onBindViewHolder(holder);
  }

  @Override
  protected void onPrepareForRemoval() {
    super.onPrepareForRemoval();
    if (alertDialog != null) {
      alertDialog.dismiss();
      alertDialog = null;
    }
  }

  public int getDialogIconResId() {
    return dialogIconResId;
  }

  public void setDialogIconResId(int dialogIconResId) {
    this.dialogIconResId = dialogIconResId;
  }

  public void setDismissDialogOnClick(boolean dismissDialogOnClick) {
    this.dismissDialogOnClick = dismissDialogOnClick;
  }

  /** Returns a dialog builder for this preference. */
  protected WearAlertDialog.Builder getAlertDialogBuilder() {
    // incompatible argument for parameter title of setTitle.
    // incompatible argument for parameter message of setMessage.
    @SuppressWarnings("nullness:argument")
    WearAlertDialog.Builder dialogBuilder =
        new WearAlertDialog.Builder(getContext())
            .setTitle(getDialogTitle())
            .setMessage(getDialogMessage())
            .setIcon(dialogIconResId)
            .setOnActionClickedListener(this)
            .setOnDismissListener(this);

    int controlId = WearAlertDialog.SELECTION_CONTROL_MIN_ID;
    int length = min(getEntries().length, getEntryValues().length);
    for (int i = 0; i < length; i++) {
      dialogBuilder.addSelectionControl(
          controlId++,
          /* selectionControlIconId= */ 0,
          getEntries()[i],
          ControlType.RADIO,
          TextUtils.equals(getValue(), getEntryValues()[i]));
    }
    return dialogBuilder;
  }

  /** Shows the dialog based on this preference's state. */
  public void show(FragmentManager fragmentManager) {
    entrySelected = false;
    selectedIndex = 0;
    alertDialog = getAlertDialogBuilder().create();
    alertDialog.showNow(fragmentManager, getClass().getSimpleName());
  }

  @VisibleForTesting
  @Nullable
  WearAlertDialog getAlertDialog() {
    return alertDialog;
  }

  @VisibleForTesting
  void setViewBinder(WearPreferenceViewBinder viewBinder) {
    this.viewBinder = viewBinder;
  }

  @Override
  public void onActionClicked(WearAlertDialog dialog, int buttonID) {
    int buttonIndex = buttonID - MIN_CONTROL_ID;
    if (buttonIndex >= 0
        && buttonIndex < getEntryValues().length
        && !TextUtils.equals(getValue(), getEntryValues()[buttonIndex])) {
      entrySelected = true;
      selectedIndex = buttonIndex;
    }
    if (alertDialog != null && dismissDialogOnClick) {
      alertDialog.dismiss();
    } else if (!dismissDialogOnClick) {
      updateValue();
    }
  }

  private void updateValue() {
    if (entrySelected && getEntryValues() != null) {
      String value = getEntryValues()[selectedIndex].toString();
      if (callChangeListener(value)) {
        setValue(value);
      }
    }
  }

  @Override
  public void onDismissed(WearAlertDialog alertDialog) {
    updateValue();
  }
}
