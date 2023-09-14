package com.google.android.clockwork.common.wearable.wearmaterial.preference;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.fragment.app.FragmentManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import androidx.annotation.VisibleForTesting;
import androidx.preference.DialogPreference;
import androidx.preference.PreferenceViewHolder;
import com.google.android.clockwork.common.wearable.wearmaterial.alertdialog.WearAlertDialog;
import com.google.android.clockwork.common.wearable.wearmaterial.alertdialog.WearAlertDialog.OnActionClickListener;
import com.google.android.clockwork.common.wearable.wearmaterial.alertdialog.WearAlertDialog.OnDismissListener;
import com.google.android.clockwork.common.wearable.wearmaterial.util.TypedArrayUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Wear-specific implementation of {@link DialogPreference}. Note that unlike its parent, this a
 * concrete implementation, similar to code the support-lib variant {@code WearableDialogPreference}
 * is implemented.
 */
public class WearDialogPreference extends DialogPreference {

  private @Nullable OnActionClickListener onActionClickListener;
  private @Nullable OnDismissListener onDismissListener;

  private int dialogIconResId;
  private int positiveIconResId;
  private int negativeIconResId;

  // Suppress as this is only used after initialization.
  @SuppressWarnings({"nullness:assignment", "nullness:argument"})
  private WearPreferenceViewBinder viewBinder = new WearPreferenceViewBinder(this);

  private @Nullable WearAlertDialog alertDialog;

  public WearDialogPreference(Context context) {
    this(context, null);
  }

  // AttributeSet can actually be null
  @SuppressWarnings("nullness:argument")
  public WearDialogPreference(Context context, @Nullable AttributeSet attrs) {
    this(
        context,
        attrs,
        TypedArrayUtils.getAttr(
            context, R.attr.dialogPreferenceStyle, android.R.attr.dialogPreferenceStyle));
  }

  public WearDialogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
    this(context, attrs, defStyleAttr, 0);
  }

  public WearDialogPreference(
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
    array.recycle();
    // Get wear-specific dialog attributes
    array =
        context.obtainStyledAttributes(attrs, R.styleable.WearDialogPreference, defStyleAttr, 0);
    positiveIconResId = array.getResourceId(R.styleable.WearDialogPreference_positiveIcon, 0);
    negativeIconResId = array.getResourceId(R.styleable.WearDialogPreference_negativeIcon, 0);
    array.recycle();
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

  public @Nullable OnActionClickListener getOnActionClickListener() {
    return onActionClickListener;
  }

  public @Nullable OnDismissListener getOnDismissListener() {
    return onDismissListener;
  }

  public int getDialogIconResId() {
    return dialogIconResId;
  }

  public int getPositiveIconResId() {
    return positiveIconResId;
  }

  public int getNegativeIconResId() {
    return negativeIconResId;
  }

  public void setOnActionClickListener(@Nullable OnActionClickListener onActionClickListener) {
    this.onActionClickListener = onActionClickListener;
  }

  public void setOnDismissListener(@Nullable OnDismissListener onDismissListener) {
    this.onDismissListener = onDismissListener;
  }

  public void setDialogIconResId(int dialogIconResId) {
    this.dialogIconResId = dialogIconResId;
  }

  public void setPositiveIconResId(int positiveIconResId) {
    this.positiveIconResId = positiveIconResId;
  }

  public void setNegativeIconResId(int negativeIconResId) {
    this.negativeIconResId = negativeIconResId;
  }

  /** Returns a dialog builder for this preference. */
  // incompatible argument for parameter positiveChipText of setActionChips.
  // incompatible argument for parameter negativeChipText of setActionChips.
  @SuppressWarnings("nullness:argument")
  protected WearAlertDialog.Builder getAlertDialogBuilder() {
    // incompatible argument for parameter title of setTitle.
    // incompatible argument for parameter message of setMessage.
    @SuppressWarnings("nullness:argument")
    WearAlertDialog.Builder dialogBuilder =
        new WearAlertDialog.Builder(getContext())
            .setTitle(getDialogTitle())
            .setMessage(getDialogMessage())
            .setIcon(dialogIconResId);
    if (onActionClickListener != null) {
      dialogBuilder.setOnActionClickedListener(onActionClickListener);
    }
    if (onDismissListener != null) {
      dialogBuilder.setOnDismissListener(onDismissListener);
    }
    if (!TextUtils.isEmpty(getPositiveButtonText())
        || !TextUtils.isEmpty(getNegativeButtonText())) {
      dialogBuilder.setActionChips(getPositiveButtonText(), getNegativeButtonText());
    } else {
      dialogBuilder.setActionButtons(positiveIconResId, negativeIconResId);
    }
    return dialogBuilder;
  }

  /** Shows the dialog based on this preference's state. */
  public void show(FragmentManager fragmentManager) {
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
}
