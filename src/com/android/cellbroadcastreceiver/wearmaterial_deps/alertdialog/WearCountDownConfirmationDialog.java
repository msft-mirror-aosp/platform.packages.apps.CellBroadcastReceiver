package com.google.android.clockwork.common.wearable.wearmaterial.alertdialog;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import com.google.android.clockwork.common.wearable.wearmaterial.progressindicator.AnimatorUtils;
import com.google.android.clockwork.common.wearable.wearmaterial.progressindicator.ProgressSpinnerDrawable;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A confirmation overlay which is a special use case of an alert/dialog where user input is not
 * required
 */
public final class WearCountDownConfirmationDialog extends AlertDialogFragment {

  /** Interface for a callback to be invoked when the dialog is dismissed. */
  public interface OnDismissListener {
    /** Called when {@link WearCountDownConfirmationDialog} is dismissed. */
    void onDismissed();
  }

  private static final long COUNT_DOWN_DURATION_MILLIS = 2450;
  private static final long DARK_TO_LIGHT_COLOR_DELAY_MILLIS = 2350;
  private static final long EXIT_DELAY_MILLIS = 1000;
  private static final String TITLE_KEY = "title";
  private static final String ICON_KEY = "icon";

  private @Nullable OnDismissListener dismissListener;
  private boolean isDismissible;
  private ProgressSpinnerDrawable progressIndicator;
  private @Nullable Animator animations;
  private ImageView icon;

  @Override
  protected int getContentLayoutId() {
    return R.layout.wear_alertdialog_countdown_confirmation;
  }

  @Override
  public final void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    TextView titleView = view.findViewById(R.id.wear_alertdialog_countdown_confirmation_text);
    icon = view.findViewById(R.id.wear_alertdialog_countdown_openonphone_icon);
    progressIndicator = (ProgressSpinnerDrawable) icon.getForeground();

    Bundle arguments = getArguments();
    if (arguments != null) {
      CharSequence title = arguments.getCharSequence(TITLE_KEY);
      titleView.setText(title);
      int iconResId = arguments.getInt(ICON_KEY);
      if (iconResId != 0 && view.getContext() != null) {
        icon.setImageDrawable(view.getContext().getDrawable(iconResId));
      }
    }

    icon.setOnClickListener(imageView -> dismissAllowingStateLoss());

    prepareAnimations(view);
  }

  private void prepareAnimations(View view) {
    view.getViewTreeObserver()
        .addOnPreDrawListener(
            new OnPreDrawListener() {
              @Override
              public boolean onPreDraw() {
                view.getViewTreeObserver().removeOnPreDrawListener(this);
                startAnimations(view);
                return false;
              }
            });
  }

  @Override
  public void onStart() {
    super.onStart();
    isDismissible = true;
  }

  @Override
  public void onDismiss(DialogInterface dialogInterface) {
    super.onDismiss(dialogInterface);

    if (!isDismissible) {
      return;
    }

    OnDismissListener dismissListener = this.dismissListener;
    if (dismissListener != null) {
      dismissListener.onDismissed();
    }
  }

  @Override
  public void onDestroyView() {
    stopAnimations();
    dismissListener = null;
    super.onDestroyView();
  }

  private void startAnimations(View view) {
    stopAnimations();
    animations = getCombinedAnimations(view);
    animations.start();
  }

  private void stopAnimations() {
    if (animations != null) {
      animations.cancel();
      animations = null;
    }
  }

  @VisibleForTesting
  AnimatorSet getCombinedAnimations(View view) {
    AnimatorSet combinedAnimator = new AnimatorSet();
    combinedAnimator.addListener(
        new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animation) {
            dismissAllowingStateLoss();
          }
        });

    Animator countDownAnimator = getCountDownAnimator();
    Animator exitDelayAnimator = ValueAnimator.ofInt(0, 1);
    exitDelayAnimator.setDuration(EXIT_DELAY_MILLIS);
    combinedAnimator.playSequentially(countDownAnimator, exitDelayAnimator);
    return combinedAnimator;
  }

  @VisibleForTesting
  Animator getCountDownAnimator() {
    Animator progressIndicatorAnimator =
        AnimatorUtils.countDown(progressIndicator, COUNT_DOWN_DURATION_MILLIS, false, () -> {});

    Animator delayedSelectionAnimator = ValueAnimator.ofInt(0, 1);
    delayedSelectionAnimator.setDuration(DARK_TO_LIGHT_COLOR_DELAY_MILLIS);
    delayedSelectionAnimator.addListener(
        new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animation) {
            icon.setSelected(true);
          }
        });

    AnimatorSet animatorSet = new AnimatorSet();
    animatorSet.playTogether(progressIndicatorAnimator, delayedSelectionAnimator);
    return animatorSet;
  }

  /** Builder to be used for the creation of {@link WearAlertDialogConfirmation} */
  public static final class Builder extends BuilderBase<Builder> {

    private final Bundle args;
    private final Context context;

    private @Nullable OnDismissListener listener;
    private int iconResId = 0;
    private CharSequence title;

    /**
     * Creates a builder for an confirmation dialog
     *
     * @param context the parent context
     */
    public Builder(Context context) {
      this.context = context;
      args = new Bundle();
    }

    @CanIgnoreReturnValue
    public Builder setTitle(@StringRes int titleResId) {
      this.title = context.getResources().getString(titleResId);
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setTitle(CharSequence title) {
      this.title = title;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setIcon(@DrawableRes int iconResId) {
      this.iconResId = iconResId;
      return this;
    }

    /** Sets a listener to be called with this confirmation is dismissed */
    @CanIgnoreReturnValue
    public Builder setOnDismissListener(OnDismissListener onDismissListener) {
      this.listener = onDismissListener;
      return this;
    }

    public WearCountDownConfirmationDialog create() {
      if (title == null) {
        throw new IllegalArgumentException("Title should not be null");
      }
      WearCountDownConfirmationDialog confirmation = new WearCountDownConfirmationDialog();
      args.putCharSequence(TITLE_KEY, title);
      args.putInt(ICON_KEY, iconResId);
      args.putAll(createThemeArguments());
      confirmation.setArguments(args);
      confirmation.dismissListener = listener;
      return confirmation;
    }
  }
}
