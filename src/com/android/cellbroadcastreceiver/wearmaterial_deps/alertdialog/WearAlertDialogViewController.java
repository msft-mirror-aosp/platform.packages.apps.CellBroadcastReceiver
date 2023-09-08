package com.google.android.clockwork.common.wearable.wearmaterial.alertdialog;

import static com.google.android.clockwork.common.wearable.wearmaterial.alertdialog.WearAlertDialog.NEGATIVE_ACTION_BUTTON_ID;
import static com.google.android.clockwork.common.wearable.wearmaterial.alertdialog.WearAlertDialog.POSITIVE_ACTION_BUTTON_ID;
import static com.google.android.clockwork.common.wearable.wearmaterial.alertdialog.WearAlertDialog.SELECTION_CONTROL_MIN_ID;
import static com.google.common.base.Preconditions.checkNotNull;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.FractionRes;
import androidx.annotation.VisibleForTesting;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;
import com.google.android.clockwork.common.wearable.wearmaterial.alertdialog.WearAlertDialog.OnAlertActionClickListener;
import com.google.android.clockwork.common.wearable.wearmaterial.alertdialog.WearAlertDialog.OnAlertSelectionChangeListener;
import com.google.android.clockwork.common.wearable.wearmaterial.button.WearButtonGroupController;
import com.google.android.clockwork.common.wearable.wearmaterial.button.WearChipButton.ControlType;
import com.google.android.clockwork.common.wearable.wearmaterial.button.WearCircularButton;
import com.google.android.clockwork.common.wearable.wearmaterial.list.FadingWearableRecyclerView;
import com.google.android.clockwork.common.wearable.wearmaterial.list.ViewGroupFader;
import com.google.android.clockwork.common.wearable.wearmaterial.list.ViewGroupFader.AnimationCallback;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Abstract class to control WearAlertDialog */
abstract class WearAlertDialogViewController<T> {

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

  private final List<WearAlertDialogElement> wearAlertDialogElementlist = new ArrayList<>();
  private final WearAlertDialogListener wearAlertDialogListener =
      new WearAlertDialogListener() {
        @SuppressWarnings({
          "nullness:initialization.fields.uninitialized",
          "nullness:method.invocation"
        })
        @Override
        public void onActionButtonClicked(int buttonId) {
          callOnActionClickListener(buttonId);
        }

        @SuppressWarnings({
          "nullness:initialization.fields.uninitialized",
          "nullness:method.invocation"
        })
        @Override
        public void onSelectionControlChanged(int buttonId, boolean isChecked) {
          callOnSelectionChangedListener(buttonId, isChecked);
        }
      };

  private @MonotonicNonNull OnAlertActionClickListener<T> onActionClickListener;
  private @MonotonicNonNull OnAlertSelectionChangeListener<T> onSelectionChangeListener;
  private FadingWearableRecyclerView wearRecyclerView;
  private Bundle arguments;
  private T owner;
  private Resources resources;

  abstract Context requireContext();

  void setOwner(T owner) {
    this.owner = owner;
  }

  void setArguments(Bundle bundle) {
    arguments = bundle;
  }

  @SuppressWarnings("unchecked")
  void setOnActionClickListener(OnAlertActionClickListener<? extends T> onActionClickListener) {
    this.onActionClickListener = (OnAlertActionClickListener<T>) onActionClickListener;
  }

  @SuppressWarnings("unchecked")
  void setOnSelectionChangeListener(
      OnAlertSelectionChangeListener<? extends T> onSelectionChangeListener) {
    this.onSelectionChangeListener = (OnAlertSelectionChangeListener<T>) onSelectionChangeListener;
  }

  int getContentLayoutId() {
    return argumentsHaveActionButtonAndSimpleContent()
        ? R.layout.wear_alertdialog_action_button_layout
        : R.layout.wear_alertdialog;
  }

  /**
   * This checks if the arguments have action buttons, icons, text, message, chip and selection
   * control.
   *
   * <p>If returns true, then simple action button layout will inflate else alert dialog with
   * RecyclerView will inflate.
   */
  @SuppressWarnings("dereference.of.nullable") // The 2 getIntegerArrayList calls never return null.
  private boolean argumentsHaveActionButtonAndSimpleContent() {
    return (arguments.getInt(POSITIVE_ACTION_BUTTON_ICON) != 0
            || arguments.getInt(NEGATIVE_ACTION_BUTTON_ICON) != 0)
        && (arguments.getInt(ICON_KEY) != 0
            || !TextUtils.isEmpty(arguments.getCharSequence(TITLE_KEY))
            || !TextUtils.isEmpty(arguments.getCharSequence(MESSAGE_KEY)))
        && arguments.getIntegerArrayList(CHIP_ARRAYLIST_ID_KEY).isEmpty()
        && arguments.getIntegerArrayList(SELECTION_CONTROL_ARRAYLIST_ID_KEY).isEmpty();
  }

  /**
   * Returns the title to be set on the {@link android.app.Dialog} of the {@link
   * AlertDialogFragment}.
   *
   * <p>When the dialog's contents are shown through a RecyclerView, this method returns the title
   * from the {@link #arguments} (see {@link #TITLE_KEY}), otherwise it returns null.
   */
  @Nullable CharSequence getDialogTitle() {
    return mustSetDialogTitle() ? arguments.getCharSequence(TITLE_KEY) : null;
  }

  /**
   * Returns true if the {@code Dialog}'s Title itself needs to be set, because Talkback's initial
   * navigation may skip the top focusable item (the Title TextView). The Dialog-{@code Window}'s
   * Title will be announced instead.
   *
   * <p>Returns false if the {@code Dialog}'s title should not be set, because the {@code
   * WearAlertDialog}'s layout will not cause Talkback's initial navigation to skip the top
   * focusable item (the Title TextView).
   *
   * <p>At this moment, this method will always return true.
   */
  private boolean mustSetDialogTitle() {
    // Return true if the layout contains a RecyclerView
    // Return false if the layout just contains a11y-focusable children.
    return !argumentsHaveActionButtonAndSimpleContent();
  }

  void configureView(View view) {
    resources = requireContext().getResources();
    if (argumentsHaveActionButtonAndSimpleContent()) {
      configureWearAlertDialogSimpleLayout(view);
    } else {
      configureWearAlertDialogLayout(view);
    }
  }

  private void configureWearAlertDialogSimpleLayout(View view) {
    ScrollView scrollView = view.findViewById(R.id.wear_alertdialog_scroll_view);
    ConstraintLayout constraintLayout = view.findViewById(R.id.wear_alertdialog_constraint_layout);
    TextView titleView = view.findViewById(R.id.wear_alertdialog_title_text);
    TextView messageView = view.findViewById(R.id.wear_alertdialog_message_text);
    ImageView icon = view.findViewById(R.id.wear_alertdialog_icon);
    WearCircularButton positiveButton = view.findViewById(R.id.wear_alertdialog_positive_button);
    WearCircularButton negativeButton = view.findViewById(R.id.wear_alertdialog_negative_button);

    DisplayMetrics displayMetrics = resources.getDisplayMetrics();
    int topMargin;

    if (arguments.getInt(ICON_KEY) == 0
        && TextUtils.isEmpty(arguments.getCharSequence(TITLE_KEY))) {
      topMargin =
          getTopMargin(R.fraction.wear_alertdialog_top_padding_message_fraction, displayMetrics);
    } else {
      topMargin = getTopMargin(R.fraction.wear_alertdialog_guideline_top, displayMetrics);
    }

    int bottomMargin =
        (int)
            requireContext()
                .getResources()
                .getFraction(
                    R.fraction.wear_alertdialog_padding_bottom,
                    displayMetrics.heightPixels,
                    /* pbase= */ 1);
    constraintLayout.setPadding(
        constraintLayout.getPaddingLeft(),
        topMargin,
        constraintLayout.getPaddingRight(),
        bottomMargin);

    view.getViewTreeObserver()
        .addOnPreDrawListener(
            new OnPreDrawListener() {
              @Override
              public boolean onPreDraw() {
                view.getViewTreeObserver().removeOnPreDrawListener(this);
                handleScrollPadding(scrollView, constraintLayout, displayMetrics, topMargin);
                return true;
              }
            });

    ViewGroupFader fader =
        new ViewGroupFader(
            constraintLayout,
            new AnimationCallback() {
              @Override
              public boolean shouldFadeFromTop(View view) {
                return true;
              }

              @Override
              public boolean shouldFadeFromBottom(View view) {
                return true;
              }

              @Override
              public void viewHasBecomeFullSize(View view) {}
            },
            new ViewGroupFader.GlobalVisibleViewBoundsProvider());
    scrollView.setOnScrollChangeListener(
        (v, scrollX, scrollY, oldScrollX, oldScrollY) -> fader.updateFade());

    CharSequence title = arguments.getCharSequence(TITLE_KEY);
    if (TextUtils.isEmpty(title)) {
      titleView.setVisibility(View.GONE);
    } else {
      titleView.setText(title);
    }

    CharSequence message = arguments.getCharSequence(MESSAGE_KEY);
    if (TextUtils.isEmpty(message)) {
      messageView.setVisibility(View.GONE);
    } else {
      messageView.setText(message);
      WearAlertDialogUtils.assignMessageViewGravity(messageView);
    }

    int iconResId = arguments.getInt(ICON_KEY);
    if (iconResId != 0) {
      icon.setImageDrawable(requireContext().getDrawable(iconResId));
    } else {
      icon.setVisibility(View.GONE);
    }

    initPositiveNegativeButtonsSimpleLayout(positiveButton, negativeButton, message);
  }

  private int getTopMargin(@FractionRes int fraction, DisplayMetrics displayMetrics) {
    return (int)
        requireContext()
            .getResources()
            .getFraction(fraction, displayMetrics.heightPixels, /* pbase= */ 1);
  }

  private void handleScrollPadding(
      ScrollView scrollView,
      ConstraintLayout constraintLayout,
      DisplayMetrics displayMetrics,
      int topMargin) {
    boolean canScroll =
        scrollView.getHeight()
            < constraintLayout.getHeight()
                + scrollView.getPaddingTop()
                + scrollView.getPaddingBottom();

    if (canScroll) {
      int scrollableBottomMargin =
          (int)
              requireContext()
                  .getResources()
                  .getFraction(
                      R.fraction.wear_alertdialog_scrollable_padding_bottom,
                      displayMetrics.heightPixels,
                      /* pbase= */ 1);

      constraintLayout.setPadding(
          constraintLayout.getPaddingLeft(),
          topMargin,
          constraintLayout.getPaddingRight(),
          scrollableBottomMargin);
    }
  }

  private void configureWearAlertDialogLayout(View view) {
    if (arguments != null) {
      int iconResId = arguments.getInt(ICON_KEY);
      if (iconResId != 0) {
        wearAlertDialogElementlist.add(new WearAlertDialogIconElement(iconResId));
      }

      CharSequence title = arguments.getCharSequence(TITLE_KEY);
      if (!TextUtils.isEmpty(title)) {
        wearAlertDialogElementlist.add(new WearAlertDialogTitleElement(title));
      }

      CharSequence message = arguments.getCharSequence(MESSAGE_KEY);
      if (!TextUtils.isEmpty(message)) {
        wearAlertDialogElementlist.add(new WearAlertDialogMessageTextElement(message));
      }

      initChipButtons();

      initSelectionControls();

      initPositiveNegativeChips();

      initPositiveNegativeButtons();

      wearRecyclerView = view.findViewById(R.id.wear_alertdialog);
      wearRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
      WearAlertDialogRecyclerAdapter wearAlertDialogRecyclerAdapter =
          new WearAlertDialogRecyclerAdapter(wearAlertDialogElementlist, wearAlertDialogListener);
      wearRecyclerView.setAdapter(wearAlertDialogRecyclerAdapter);
      wearRecyclerView.addItemDecoration(new ItemOffsetDecoration(requireContext()));

      ViewCompat.setAccessibilityDelegate(
          wearRecyclerView, new AlertDialogListAccessibilityDelegate(wearRecyclerView));
    }
  }

  private void initPositiveNegativeButtons() {
    int positiveButtonIconId = arguments.getInt(POSITIVE_ACTION_BUTTON_ICON);
    int negativeButtonIconId = arguments.getInt(NEGATIVE_ACTION_BUTTON_ICON);

    CharSequence positiveButtonContentDescription =
        arguments.getCharSequence(POSITIVE_ACTION_BUTTON_CONTENT_DESCRIPTION, "");
    CharSequence negativeButtonContentDescription =
        arguments.getCharSequence(NEGATIVE_ACTION_BUTTON_CONTENT_DESCRIPTION, "");

    wearAlertDialogElementlist.add(
        new WearAlertDialogActionButtonsElement(
            positiveButtonIconId,
            negativeButtonIconId,
            positiveButtonContentDescription,
            negativeButtonContentDescription));
  }

  // dereference of possibly-null reference layoutParams
  @SuppressWarnings("nullness:dereference.of.nullable")
  private void initPositiveNegativeButtonsSimpleLayout(
      WearCircularButton positiveButton,
      WearCircularButton negativeButton,
      @Nullable CharSequence message) {
    int positiveButtonIconResId = arguments.getInt(POSITIVE_ACTION_BUTTON_ICON);
    if (positiveButtonIconResId != 0) {
      positiveButton.setIcon(positiveButtonIconResId);
      positiveButton.setOnClickListener(v -> callOnActionClickListener(POSITIVE_ACTION_BUTTON_ID));
      if (!TextUtils.isEmpty(message)) {
        ConstraintLayout.LayoutParams layoutParams =
            (ConstraintLayout.LayoutParams) positiveButton.getLayoutParams();
        layoutParams.setMargins(
            (int) resources.getDimension(R.dimen.wear_alertdialog_button_margin), 0, 0, 0);
      }
      CharSequence positiveButtonContentDescription =
          arguments.getCharSequence(POSITIVE_ACTION_BUTTON_CONTENT_DESCRIPTION);
      if (!TextUtils.isEmpty(positiveButtonContentDescription)) {
        positiveButton.setContentDescription(positiveButtonContentDescription);
      }
    } else {
      positiveButton.setVisibility(View.GONE);
    }

    int negativeButtonIconResId = arguments.getInt(NEGATIVE_ACTION_BUTTON_ICON);
    if (negativeButtonIconResId != 0) {
      negativeButton.setIcon(negativeButtonIconResId);
      negativeButton.setOnClickListener(v -> callOnActionClickListener(NEGATIVE_ACTION_BUTTON_ID));
      if (!TextUtils.isEmpty(message)) {
        ConstraintLayout.LayoutParams layoutParams =
            (ConstraintLayout.LayoutParams) negativeButton.getLayoutParams();
        layoutParams.setMargins(
            0, 0, (int) resources.getDimension(R.dimen.wear_alertdialog_button_margin), 0);
      }
      CharSequence negativeButtonContentDescriptionResId =
          arguments.getCharSequence(NEGATIVE_ACTION_BUTTON_CONTENT_DESCRIPTION);
      if (!TextUtils.isEmpty(negativeButtonContentDescriptionResId)) {
        negativeButton.setContentDescription(negativeButtonContentDescriptionResId);
      }
    } else {
      negativeButton.setVisibility(View.GONE);
    }
  }

  private void initChipButtons() {
    ArrayList<Integer> chipButtonIdArrayList = arguments.getIntegerArrayList(CHIP_ARRAYLIST_ID_KEY);
    ArrayList<Integer> chipButtonIconIdArrayList =
        arguments.getIntegerArrayList(CHIP_ARRAYLIST_ICON_ID_KEY);
    ArrayList<Integer> chipButtonStringArrayList =
        arguments.getIntegerArrayList(CHIP_ARRAYLIST_STRING_KEY);
    ArrayList<Integer> chipButtonContentDescriptionArrayList =
        arguments.getIntegerArrayList(CHIP_ARRAYLIST_CONTENT_DESCRIPTION_KEY);
    if (chipButtonIdArrayList == null
        || chipButtonIconIdArrayList == null
        || chipButtonStringArrayList == null
        || chipButtonContentDescriptionArrayList == null) {
      return;
    }

    for (int index = 0; index < chipButtonIdArrayList.size(); index++) {
      Integer contentDescriptionResId = chipButtonContentDescriptionArrayList.get(index);
      String contentDescription = null;
      if (contentDescriptionResId != null && contentDescriptionResId > 0) {
        contentDescription = resources.getString(contentDescriptionResId);
      }
      WearAlertDialogChipButtonElement wearAlertDialogChipButtonElement =
          new WearAlertDialogChipButtonElement(
              chipButtonIconIdArrayList.get(index),
              resources.getString(chipButtonStringArrayList.get(index)),
              chipButtonIdArrayList.get(index),
              contentDescription);
      wearAlertDialogElementlist.add(wearAlertDialogChipButtonElement);
    }
  }

  @SuppressLint("NotifyDataSetChanged")
  private void initSelectionControls() {
    ArrayList<Integer> selectionControlIdArrayList =
        arguments.getIntegerArrayList(SELECTION_CONTROL_ARRAYLIST_ID_KEY);
    ArrayList<Integer> selectionControlIconIdArrayList =
        arguments.getIntegerArrayList(SELECTION_CONTROL_ARRAYLIST_ICON_ID_KEY);
    ArrayList<CharSequence> selectionControlStringArrayList =
        arguments.getCharSequenceArrayList(SELECTION_CONTROL_ARRAYLIST_STRING_KEY);
    ArrayList<Integer> selectionControlTypeArrayList =
        arguments.getIntegerArrayList(SELECTION_CONTROL_ARRAYLIST_TYPE_KEY);
    int selectionControlSelectedId =
        arguments.getInt(SELECTION_CONTROL_SELECTED_ID_KEY, SELECTION_CONTROL_MIN_ID - 1);

    if (selectionControlIdArrayList == null
        || selectionControlIconIdArrayList == null
        || selectionControlStringArrayList == null
        || selectionControlTypeArrayList == null
        || selectionControlIdArrayList.isEmpty()) {
      return;
    }

    WearButtonGroupController groupController = new WearButtonGroupController();

    for (int index = 0; index < selectionControlIdArrayList.size(); index++) {
      int selectionControlId = selectionControlIdArrayList.get(index);
      ControlType controlType = ControlType.values()[selectionControlTypeArrayList.get(index)];
      WearAlertDialogSelectionControlElement wearAlertDialogSelectionControlElement =
          new WearAlertDialogSelectionControlElement(
              selectionControlId,
              controlType,
              selectionControlIconIdArrayList.get(index),
              selectionControlStringArrayList.get(index));
      if (wearAlertDialogSelectionControlElement.getSelectionControlId()
          == selectionControlSelectedId) {
        wearAlertDialogSelectionControlElement.setChecked(true);
        groupController.check(selectionControlSelectedId);
      }
      wearAlertDialogElementlist.add(wearAlertDialogSelectionControlElement);
      if (controlType == ControlType.RADIO) {
        groupController.addButton(wearAlertDialogSelectionControlElement);
      }
    }
    groupController.setOnCheckedChangeListener(
        (button, isChecked) -> checkNotNull(wearRecyclerView.getAdapter()).notifyDataSetChanged());
  }

  private void initPositiveNegativeChips() {
    CharSequence positiveChipText = arguments.getCharSequence(POSITIVE_ACTION_CHIP_TEXT);
    CharSequence negativeChipText = arguments.getCharSequence(NEGATIVE_ACTION_CHIP_TEXT);

    if (!TextUtils.isEmpty(positiveChipText)) {
      wearAlertDialogElementlist.add(new WearAlertDialogPositiveChipElement(positiveChipText));
    }

    if (!TextUtils.isEmpty(negativeChipText)) {
      wearAlertDialogElementlist.add(new WearAlertDialogNegativeChipElement(negativeChipText));
    }
  }

  FadingWearableRecyclerView getRecyclerView() {
    return wearRecyclerView;
  }

  @Nullable OnAlertActionClickListener<T> getOnActionClickListener() {
    return onActionClickListener;
  }

  void callOnActionClickListener(int buttonId) {
    if (onActionClickListener != null) {
      onActionClickListener.onActionClicked(owner, buttonId);
    }
  }

  void callOnSelectionChangedListener(int buttonId, boolean isSelected) {
    if (onSelectionChangeListener != null) {
      onSelectionChangeListener.onSelectionChanged(owner, buttonId, isSelected);
    }
  }

  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  Bundle getArguments() {
    return arguments;
  }
}
