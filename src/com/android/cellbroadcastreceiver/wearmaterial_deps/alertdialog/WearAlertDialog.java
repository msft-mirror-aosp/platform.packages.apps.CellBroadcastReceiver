package com.google.android.clockwork.common.wearable.wearmaterial.alertdialog;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import com.google.android.clockwork.common.wearable.wearmaterial.button.WearChipButton.ControlType;
import com.google.android.clockwork.common.wearable.wearmaterial.list.FadingWearableRecyclerView;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.ArrayList;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A confirmation overlay which is a special use case of an alert/dialog where user input is not
 * required. WearAlertDialog uses two different layouts, it checks if positive or negative button
 * icon are not empty then it use wear_alertdialog_simple_layout else use wear_alertdialog.
 */
public final class WearAlertDialog extends AlertDialogFragment {

  /** Interface for a callback to be invoked when the alert's action button is clicked. */
  public interface OnAlertActionClickListener<T> {
    /** Called when {@code alert}'s action button clicked. */
    void onActionClicked(T alert, int buttonId);
  }

  /** Interface for a callback to be invoked when a alert's selection chip selection has changed. */
  public interface OnAlertSelectionChangeListener<T> {
    /** Called when {@code alert}'s selection chip button's selection has changed. */
    void onSelectionChanged(T alert, int buttonId, boolean isSelected);
  }

  /**
   * Interface for a callback to be invoked when the dialog's action button is clicked. This is
   * needed to maintain backwards compatibility, so that current users/callers of the
   * WearAlertDialog can keep using OnActionClickListener without any compilation issues
   */
  public interface OnActionClickListener extends OnAlertActionClickListener<WearAlertDialog> {}

  /**
   * Interface for a callback to be invoked when a dialog's selection chip selection has
   * changed.This is needed to maintain backwards compatibility, so that current users/callers of
   * the WearAlertDialog can keep using OnActionClickListener without any compilation issues
   */
  public interface OnSelectionChangeListener
      extends OnAlertSelectionChangeListener<WearAlertDialog> {}

  /** Interface for a callback to be invoked when the dialog is dismissed. */
  public interface OnDismissListener {
    /** Called when {@link WearAlertDialog} is dismissed. */
    void onDismissed(WearAlertDialog alertDialog);
  }

  /** Interface for a callback to be invoked when the dialog is cancelled. */
  public interface OnCancelListener {
    /** Called when {@link WearAlertDialog} is cancelled. */
    void onCancel(WearAlertDialog alertDialog);
  }

  /**
   * ID that is passed when calling onActionClickListener onActionClicked for a positive or negative
   * button
   */
  public static final int NEGATIVE_ACTION_BUTTON_ID = -1;

  /** The minimum that can be set on selection controls. */
  public static final int SELECTION_CONTROL_MIN_ID = 1;

  public static final int POSITIVE_ACTION_BUTTON_ID = -2;
  public static final int NEGATIVE_ACTION_CHIP_ID = -3;
  public static final int POSITIVE_ACTION_CHIP_ID = -4;

  private static final String ICON_KEY = "icon";
  private static final String MESSAGE_KEY = "message";
  private static final String NEGATIVE_ACTION_BUTTON_ICON = "negativeButtonIcon";
  private static final String POSITIVE_ACTION_BUTTON_ICON = "positiveButtonIcon";
  private static final String NEGATIVE_ACTION_BUTTON_CONTENT_DESCRIPTION =
      "negativeButtonContentDescription";
  private static final String POSITIVE_ACTION_BUTTON_CONTENT_DESCRIPTION =
      "positiveButtonContentDescription";
  private static final String TITLE_KEY = "title";
  private static final String CHIP_ARRAYLIST_ID_KEY = "chipArrayIdList";
  private static final String CHIP_ARRAYLIST_ICON_ID_KEY = "chipArrayIconIdList";
  private static final String CHIP_ARRAYLIST_STRING_KEY = "chipArrayStringList";
  private static final String CHIP_ARRAYLIST_CONTENT_DESCRIPTION_KEY =
      "chipArrayContentDescriptionList";
  private static final String NEGATIVE_ACTION_CHIP_TEXT = "negativeChipText";
  private static final String POSITIVE_ACTION_CHIP_TEXT = "positiveChipText";
  private static final String SELECTION_CONTROL_ARRAYLIST_ID_KEY = "controlArrayIdList";
  private static final String SELECTION_CONTROL_ARRAYLIST_ICON_ID_KEY = "controlArrayIconIdList";
  private static final String SELECTION_CONTROL_ARRAYLIST_STRING_KEY = "controlArrayStringList";
  private static final String SELECTION_CONTROL_ARRAYLIST_TYPE_KEY = "controlArrayTypeList";
  private static final String SELECTION_CONTROL_SELECTED_ID_KEY = "controlSelectedId";

  private final WearAlertDialogViewController<? super WearAlertDialog>
      wearAlertDialogViewController =
          new WearAlertDialogViewController<WearAlertDialog>() {

            // call to requireContext() not allowed on the given receiver.
            @SuppressWarnings("nullness:method.invocation")
            @Override
            Context requireContext() {
              return WearAlertDialog.this.requireContext();
            }
          };

  private @MonotonicNonNull OnDismissListener dismissListener;
  private @MonotonicNonNull OnCancelListener cancelListener;

  @Override
  protected int getContentLayoutId() {
    // Check if positive or negative button icon are not empty then use
    // wear_alertdialog_simple_layout else use wear_alertdialog.
    return wearAlertDialogViewController.getContentLayoutId();
  }

  @Override
  public void onCreate(@Nullable Bundle bundle) {
    super.onCreate(bundle);
    wearAlertDialogViewController.setOwner(this);
    wearAlertDialogViewController.setArguments(requireArguments());
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    wearAlertDialogViewController.configureView(view);
    requireDialog().setTitle(wearAlertDialogViewController.getDialogTitle());
  }

  void setOnActionClickListener(OnAlertActionClickListener<WearAlertDialog> onActionClickListener) {
    wearAlertDialogViewController.setOnActionClickListener(onActionClickListener);
  }

  void setOnSelectionChangeListener(
      OnAlertSelectionChangeListener<WearAlertDialog> onSelectionChangeListener) {
    wearAlertDialogViewController.setOnSelectionChangeListener(onSelectionChangeListener);
  }

  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  FadingWearableRecyclerView getRecyclerView() {
    return wearAlertDialogViewController.getRecyclerView();
  }

  @SuppressWarnings("unchecked")
  @Nullable OnAlertActionClickListener<WearAlertDialog> getOnActionClickListener() {
    return (OnAlertActionClickListener<WearAlertDialog>)
        wearAlertDialogViewController.getOnActionClickListener();
  }

  @VisibleForTesting
  public void callOnActionClickListener(int buttonId) {
    wearAlertDialogViewController.callOnActionClickListener(buttonId);
  }

  @VisibleForTesting
  public void callOnSelectionChangedListener(int buttonId, boolean isSelected) {
    wearAlertDialogViewController.callOnSelectionChangedListener(buttonId, isSelected);
  }

  @Override
  public void onDismiss(DialogInterface dialogInterface) {
    super.onDismiss(dialogInterface);

    if (dismissListener != null) {
      dismissListener.onDismissed(this);
    }
  }

  @Override
  public void onCancel(DialogInterface dialogInterface) {
    super.onCancel(dialogInterface);

    if (cancelListener != null) {
      cancelListener.onCancel(this);
    }
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
  }

  /** Builder to be used for the creation of {@link WearAlertDialog} */
  public static class Builder extends BuilderBase<Builder> {

    private final Bundle args;
    private final Context context;
    private final ArrayList<Integer> actionChipIdArrayList = new ArrayList<>();
    private final ArrayList<Integer> actionChipIconIdArrayList = new ArrayList<>();
    private final ArrayList<Integer> actionChipStringResourceArrayList = new ArrayList<>();
    private final ArrayList<Integer> actionChipContentDescriptionResourceArrayList =
        new ArrayList<>();
    private final ArrayList<Integer> selectionControlIdArrayList = new ArrayList<>();
    private final ArrayList<Integer> selectionControlIconIdArrayList = new ArrayList<>();
    private final ArrayList<CharSequence> selectionControlStringList = new ArrayList<>();
    private final ArrayList<Integer> selectionControlTypeArrayList = new ArrayList<>();
    private OnAlertActionClickListener<?> onActionClickListener;
    private OnAlertSelectionChangeListener<?> onSelectionChangeListener;
    private OnDismissListener onDismissListener;
    private OnCancelListener onCancelListener;
    private CharSequence alertDialogTitle = "";
    private CharSequence alertDialogMessage = "";
    private int positiveButtonIconId = 0;
    private int negativeButtonIconId = 0;
    private CharSequence positiveButtonContentDescription = "";
    private CharSequence negativeButtonContentDescription = "";
    private CharSequence positiveChipText = "";
    private CharSequence negativeChipText = "";
    private int selectionControlSelectedId = SELECTION_CONTROL_MIN_ID - 1;
    private int iconResId = 0;

    /**
     * Creates a builder for an alert dialog
     *
     * @param context the parent context
     */
    public Builder(Context context) {
      args = new Bundle();
      this.context = context;
    }

    /** Sets the title of an AlertDialog - uses a StringRes Param */
    @CanIgnoreReturnValue
    public Builder setTitle(@StringRes int titleResId) {
      this.alertDialogTitle = context.getResources().getString(titleResId);
      return this;
    }

    /** Sets the title of an AlertDialog - uses a CharSequence Param */
    @CanIgnoreReturnValue
    public Builder setTitle(CharSequence title) {
      this.alertDialogTitle = title;
      return this;
    }

    /** Sets the message of an AlertDialog - uses a CharSequence Param */
    @CanIgnoreReturnValue
    public Builder setMessage(@StringRes int titleResId) {
      this.alertDialogMessage = context.getResources().getString(titleResId);
      return this;
    }

    /** Sets the message of an AlertDialog - uses a CharSequence Param */
    @CanIgnoreReturnValue
    public Builder setMessage(CharSequence message) {
      this.alertDialogMessage = message;
      return this;
    }

    /** Sets the icon drawable of an Alert Dialog - uses a DrawableRes param */
    @CanIgnoreReturnValue
    public Builder setIcon(@DrawableRes int iconResId) {
      this.iconResId = iconResId;
      return this;
    }

    /** Set the Action Button icons for the Positive and Negative Buttons of an Alert Dialog */
    @CanIgnoreReturnValue
    public Builder setActionButtons(
        @DrawableRes int positiveButtonIconId, @DrawableRes int negativeButtonIconId) {
      this.positiveButtonIconId = positiveButtonIconId;
      this.negativeButtonIconId = negativeButtonIconId;
      return this;
    }

    /** Set the content descriptions for the Positive and Negative Buttons of an Alert Dialog */
    @CanIgnoreReturnValue
    public Builder setActionButtonContentDescriptions(
        CharSequence positiveButtonContentDescription,
        CharSequence negativeButtonContentDescription) {
      this.positiveButtonContentDescription = positiveButtonContentDescription;
      this.negativeButtonContentDescription = negativeButtonContentDescription;
      return this;
    }

    /**
     * Set the Action Chip strings for the positive and Negative chips of an Alert Dialog. To hide
     * either of the buttons, pass 0 as an argument respectively.
     */
    @CanIgnoreReturnValue
    public Builder setActionChips(
        @StringRes int positiveChipStringId, @StringRes int negativeChipStringId) {
      this.positiveChipText =
          positiveChipStringId == 0 ? "" : context.getString(positiveChipStringId);
      this.negativeChipText =
          negativeChipStringId == 0 ? "" : context.getString(negativeChipStringId);
      return this;
    }

    /** Set the Action Chip strings for the positive and Negative chips of an Alert Dialog */
    @CanIgnoreReturnValue
    public Builder setActionChips(CharSequence positiveChipText, CharSequence negativeChipText) {
      this.positiveChipText = positiveChipText;
      this.negativeChipText = negativeChipText;
      return this;
    }

    /**
     * Sets a listener to be called when an action button or chip has been clicked.
     *
     * @deprecated Please use {@link #setOnAlertActionClickedListener(OnAlertActionClickListener)}
     *     instead.
     */
    @CanIgnoreReturnValue
    @Deprecated
    public Builder setOnActionClickedListener(OnActionClickListener onActionClickedListener) {
      this.onActionClickListener = onActionClickedListener;
      return this;
    }

    /**
     * Sets a listener to be called when a selection chip button's selection has changed.
     *
     * @deprecated Please use {@link
     *     #setOnAlertSelectionChangedListener(OnAlertSelectionChangeListener)} instead.
     */
    @CanIgnoreReturnValue
    @Deprecated
    public Builder setOnSelectionChangedListener(
        OnSelectionChangeListener onSelectionChangedListener) {
      this.onSelectionChangeListener = onSelectionChangedListener;
      return this;
    }

    /** Sets a listener to be called when an action button or chip has been clicked. */
    @CanIgnoreReturnValue
    public <T> Builder setOnAlertActionClickedListener(
        OnAlertActionClickListener<T> onActionClickedListener) {
      this.onActionClickListener = onActionClickedListener;
      return this;
    }

    /** Sets a listener to be called when a selection chip button's selection has changed */
    @CanIgnoreReturnValue
    public <T> Builder setOnAlertSelectionChangedListener(
        OnAlertSelectionChangeListener<T> onSelectionChangedListener) {
      this.onSelectionChangeListener = onSelectionChangedListener;
      return this;
    }

    /**
     * Sets a listener to be called with this alert dialog is dismissed. It can only be set when a
     * WearAlertDialog is being constructed (see {@link #create()}).
     */
    @CanIgnoreReturnValue
    public Builder setOnDismissListener(OnDismissListener onDismissListener) {
      this.onDismissListener = onDismissListener;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setOnCancelListener(OnCancelListener onCancelListener) {
      this.onCancelListener = onCancelListener;
      return this;
    }

    /**
     * Adds action chip params necessary to create WearActionChips to an Arraylist.
     *
     * @param actionChipId must be positive to distinguish from actionButtons
     * @param actionChipIconId for the icon drawable of the WearChipButton
     * @param actionChipStringResourceID for the string resource of the WearChipButton
     */
    @CanIgnoreReturnValue
    public Builder addActionChip(
        int actionChipId,
        @DrawableRes int actionChipIconId,
        @StringRes int actionChipStringResourceID) {
      if (actionChipId < SELECTION_CONTROL_MIN_ID) {
        throw new IllegalArgumentException("The actionChipId must be a positive integer.");
      }
      this.actionChipIdArrayList.add(actionChipId);
      this.actionChipIconIdArrayList.add(actionChipIconId);
      this.actionChipStringResourceArrayList.add(actionChipStringResourceID);
      this.actionChipContentDescriptionResourceArrayList.add(0);
      return this;
    }

    /**
     * Adds action chip params necessary to create WearActionChips to an Arraylist.
     *
     * @param actionChipId must be positive to distinguish from actionButtons
     * @param actionChipIconId for the icon drawable of the WearChipButton
     * @param actionChipStringResourceId for the string resource of the WearChipButton
     * @param actionChipContentDescriptionResourceId for the content description resource of the
     *     WearChipButton
     */
    @CanIgnoreReturnValue
    public Builder addActionChip(
        int actionChipId,
        @DrawableRes int actionChipIconId,
        @StringRes int actionChipStringResourceId,
        @StringRes int actionChipContentDescriptionResourceId) {
      if (actionChipId < SELECTION_CONTROL_MIN_ID) {
        throw new IllegalArgumentException("The actionChipId must be a positive integer.");
      }
      this.actionChipIdArrayList.add(actionChipId);
      this.actionChipIconIdArrayList.add(actionChipIconId);
      this.actionChipStringResourceArrayList.add(actionChipStringResourceId);
      this.actionChipContentDescriptionResourceArrayList.add(
          actionChipContentDescriptionResourceId);
      return this;
    }

    /**
     * Add action chip params necessary to create WearActionChips with selection controls with
     * Arraylists.
     *
     * @param selectionControlId must be positive to distinguish from actionButtons
     * @param selectionControlIconId for the icon drawable of the WearChipButton
     * @param selectionControlStringResourceID for the string resource of the WearChipButton
     * @param controlType control type to be used, cannot be {@code ControlType.NONE}
     */
    @CanIgnoreReturnValue
    public Builder addSelectionControl(
        int selectionControlId,
        @DrawableRes int selectionControlIconId,
        @StringRes int selectionControlStringResourceID,
        ControlType controlType) {
      return addSelectionControl(
          selectionControlId,
          selectionControlIconId,
          context.getString(selectionControlStringResourceID),
          controlType);
    }

    /**
     * Add action chip params necessary to create WearActionChips with selection controls with
     * Arraylists.
     *
     * @param selectionControlId must be positive to distinguish from actionButtons
     * @param selectionControlIconId for the icon drawable of the WearChipButton
     * @param selectionControlString for the string shown on the WearChipButton
     * @param controlType control type to be used, cannot be {@code ControlType.NONE}
     */
    @CanIgnoreReturnValue
    public Builder addSelectionControl(
        int selectionControlId,
        @DrawableRes int selectionControlIconId,
        CharSequence selectionControlString,
        ControlType controlType) {
      return addSelectionControl(
          selectionControlId,
          selectionControlIconId,
          selectionControlString,
          controlType,
          /* selected= */ false);
    }

    /**
     * Add action chip params necessary to create WearActionChips with selection controls with
     * Arraylists.
     *
     * @param selectionControlId must be positive to distinguish from actionButtons
     * @param selectionControlIconId for the icon drawable of the WearChipButton
     * @param selectionControlStringResourceID for the string resource of the WearChipButton
     * @param controlType control type to be used, cannot be {@code ControlType.NONE}
     * @param selected if this control type is selected. Only one control type can be selected at a
     *     time, and multiple selected control types will lead to the last added one being selected.
     */
    @CanIgnoreReturnValue
    public Builder addSelectionControl(
        int selectionControlId,
        @DrawableRes int selectionControlIconId,
        @StringRes int selectionControlStringResourceID,
        ControlType controlType,
        boolean selected) {
      return addSelectionControl(
          selectionControlId,
          selectionControlIconId,
          context.getString(selectionControlStringResourceID),
          controlType,
          selected);
    }

    /**
     * Add action chip params necessary to create WearActionChips with selection controls with
     * Arraylists.
     *
     * @param selectionControlId must be positive to distinguish from actionButtons
     * @param selectionControlIconId for the icon drawable of the WearChipButton
     * @param selectionControlString for the string shown on the WearChipButton
     * @param controlType control type to be used, cannot be {@code ControlType.NONE}
     * @param selected if this control type is selected. Only one control type can be selected at a
     *     time, and multiple selected control types will lead to the last added one being selected.
     */
    @CanIgnoreReturnValue
    public Builder addSelectionControl(
        int selectionControlId,
        @DrawableRes int selectionControlIconId,
        CharSequence selectionControlString,
        ControlType controlType,
        boolean selected) {
      if (selectionControlId < SELECTION_CONTROL_MIN_ID) {
        throw new IllegalArgumentException("The selectionControlId must be a positive integer.");
      }
      if (controlType.equals(ControlType.NONE)) {
        throw new IllegalArgumentException(
            "The controlType must not be none, use addActionChip(int, int, int) instead.");
      }
      this.selectionControlIdArrayList.add(selectionControlId);
      this.selectionControlIconIdArrayList.add(selectionControlIconId);
      this.selectionControlStringList.add(selectionControlString);
      this.selectionControlTypeArrayList.add(controlType.ordinal());
      if (selected) {
        this.selectionControlSelectedId = selectionControlId;
      }
      return this;
    }

    /**
     * Creates an Alert Dialog @throws IllegalStateException cannot have both action buttons and
     * action chips
     */
    @SuppressWarnings("unchecked")
    public WearAlertDialog create() {
      Bundle args = getVerifiedArguments();
      WearAlertDialog alertDialog = new WearAlertDialog();
      alertDialog.setArguments(args);
      alertDialog.setOnActionClickListener(
          (OnAlertActionClickListener<WearAlertDialog>) onActionClickListener);
      alertDialog.setOnSelectionChangeListener(
          (OnAlertSelectionChangeListener<WearAlertDialog>) onSelectionChangeListener);
      alertDialog.dismissListener = onDismissListener;
      alertDialog.cancelListener = onCancelListener;
      return alertDialog;
    }

    @VisibleForTesting
    @SuppressWarnings("unchecked")
    View createView(
        @Nullable ViewGroup parent,
        WearAlertDialogViewController<View> wearAlertDialogViewController) {
      if (onDismissListener != null) {
        throw new IllegalStateException("onDismissListener can't be called on view");
      }
      Bundle args = getVerifiedArguments();
      wearAlertDialogViewController.setArguments(args);
      wearAlertDialogViewController.setOnActionClickListener(
          (OnAlertActionClickListener<View>) onActionClickListener);
      wearAlertDialogViewController.setOnSelectionChangeListener(
          (OnAlertSelectionChangeListener<View>) onSelectionChangeListener);
      Context viewContext =
          contextTheme == 0 ? context : new ContextThemeWrapper(context, contextTheme);
      LayoutInflater layoutInflater = LayoutInflater.from(viewContext);
      ViewGroup wearLayout =
          (ViewGroup) layoutInflater.inflate(R.layout.wear_alert_dialog_content, parent, false);
      View view =
          layoutInflater.inflate(
              wearAlertDialogViewController.getContentLayoutId(), wearLayout, true);
      wearAlertDialogViewController.setOwner(view);
      wearAlertDialogViewController.configureView(view);
      if (Build.VERSION.SDK_INT >= VERSION_CODES.P) {
        view.setAccessibilityPaneTitle(args.getCharSequence(TITLE_KEY));
      }
      return view;
    }

    /**
     * Creates an Alert View @throws IllegalStateException cannot have both action buttons and
     * action chips
     */
    @VisibleForTesting
    public View createView(@Nullable ViewGroup parent) {
      return createView(
          parent,
          new WearAlertDialogViewController<View>() {
            @Override
            Context requireContext() {
              return Builder.this.context;
            }
          });
    }

    private Bundle getVerifiedArguments() {
      if (!actionChipIdArrayList.isEmpty()
          && (positiveButtonIconId != 0 || negativeButtonIconId != 0)) {
        throw new IllegalStateException("Can't have both chips and action buttons");
      }
      if ((!TextUtils.isEmpty(positiveChipText) || !TextUtils.isEmpty(negativeChipText))
          && (positiveButtonIconId != 0 || negativeButtonIconId != 0)) {
        throw new IllegalStateException("Can't have both action chips and action buttons");
      }
      if (!actionChipIdArrayList.isEmpty() && !selectionControlIdArrayList.isEmpty()) {
        throw new IllegalStateException("Can't have both chips and selection controls");
      }
      if (selectionControlIdArrayList.size() > 1
          && (positiveButtonIconId != 0 || negativeButtonIconId != 0)) {
        throw new IllegalStateException(
            "Can't have more than one selection controls with action buttons");
      }
      args.putAll(createThemeArguments());
      args.putInt(ICON_KEY, iconResId);
      args.putCharSequence(MESSAGE_KEY, alertDialogMessage);
      args.putCharSequence(TITLE_KEY, alertDialogTitle);
      args.putInt(POSITIVE_ACTION_BUTTON_ICON, positiveButtonIconId);
      args.putInt(NEGATIVE_ACTION_BUTTON_ICON, negativeButtonIconId);
      args.putCharSequence(
          POSITIVE_ACTION_BUTTON_CONTENT_DESCRIPTION, positiveButtonContentDescription);
      args.putCharSequence(
          NEGATIVE_ACTION_BUTTON_CONTENT_DESCRIPTION, negativeButtonContentDescription);
      args.putCharSequence(POSITIVE_ACTION_CHIP_TEXT, positiveChipText);
      args.putCharSequence(NEGATIVE_ACTION_CHIP_TEXT, negativeChipText);
      args.putIntegerArrayList(CHIP_ARRAYLIST_ID_KEY, actionChipIdArrayList);
      args.putIntegerArrayList(CHIP_ARRAYLIST_ICON_ID_KEY, actionChipIconIdArrayList);
      args.putIntegerArrayList(CHIP_ARRAYLIST_STRING_KEY, actionChipStringResourceArrayList);
      args.putIntegerArrayList(
          CHIP_ARRAYLIST_CONTENT_DESCRIPTION_KEY, actionChipContentDescriptionResourceArrayList);
      args.putIntegerArrayList(SELECTION_CONTROL_ARRAYLIST_ID_KEY, selectionControlIdArrayList);
      args.putIntegerArrayList(
          SELECTION_CONTROL_ARRAYLIST_ICON_ID_KEY, selectionControlIconIdArrayList);
      args.putCharSequenceArrayList(
          SELECTION_CONTROL_ARRAYLIST_STRING_KEY, selectionControlStringList);
      args.putIntegerArrayList(SELECTION_CONTROL_ARRAYLIST_TYPE_KEY, selectionControlTypeArrayList);
      args.putInt(SELECTION_CONTROL_SELECTED_ID_KEY, selectionControlSelectedId);
      return args;
    }
  }
}
