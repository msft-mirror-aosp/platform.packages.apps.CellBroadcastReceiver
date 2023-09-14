package com.google.android.clockwork.common.wearable.wearmaterial.alertdialog;

import static java.util.concurrent.TimeUnit.SECONDS;

import android.content.Context;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.Animatable2.AnimationCallback;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.VisibleForTesting;
import com.google.android.clockwork.common.wearable.wearmaterial.alertdialog.WearAlertDialogConfirmation.HapticTypeDef;
import com.google.android.clockwork.common.wearable.wearmaterial.alertdialog.WearAlertDialogConfirmation.OnDismissListener;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Controller class for WearAlertDialogConfirmation. */
final class WearAlertDialogConfirmationViewController {

  private static final long DEFAULT_DISMISS_TIMEOUT_MILLIS = SECONDS.toMillis(2);
  private static final String TITLE_KEY = "title";
  private static final String ICON_KEY = "icon";
  private static final String DISMISS_TIMEOUT_KEY = "dismiss_time";
  private static final String HAPTIC_TYPE_KEY = "haptic_type";

  @VisibleForTesting
  @SuppressWarnings("nullness:method.invocation")
  final AnimationCallback animationCallback =
      new AnimationCallback() {

        @Override
        public void onAnimationEnd(Drawable drawable) {
          onDismiss();
        }
      };

  @SuppressWarnings("nullness:methodref.receiver.bound")
  private final Runnable dismissRunnable = this::dismiss;

  @Nullable OnDismissListener dismissListener;
  private long dismissTimeoutMillis = DEFAULT_DISMISS_TIMEOUT_MILLIS;
  @HapticTypeDef private int hapticType = HapticTypeDef.NONE;
  private @Nullable Context context;
  private boolean isDismissible;
  private ImageView icon;
  private View view;

  WearAlertDialogConfirmationViewController(OnDismissListener dismissListener) {
    this.dismissListener = dismissListener;
  }

  void setContext(@Nullable Context context) {
    this.context = context;
  }

  void dismiss() {
    onDismiss();
  }

  int getContentLayoutId() {
    return R.layout.wear_alertdialog_confirmation;
  }

  void configureView(Bundle arguments, View view) {
    this.view = view;
    TextView titleView = view.findViewById(R.id.wear_alertdialog_confirmation_text);
    icon = view.findViewById(R.id.wear_alertdialog_confirmation_icon);

    if (arguments != null) {
      CharSequence title = arguments.getCharSequence(TITLE_KEY, "");
      titleView.setText(title);

      int iconResId = arguments.getInt(ICON_KEY);
      Context context = this.context;
      if (iconResId != 0 && context != null) {
        icon.setVisibility(View.VISIBLE);
        icon.setImageDrawable(context.getDrawable(iconResId));
      } else {
        icon.setVisibility(View.GONE);
      }

      hapticType = arguments.getInt(HAPTIC_TYPE_KEY, hapticType);
      int dismissTimeout = arguments.getInt(DISMISS_TIMEOUT_KEY);
      if (dismissTimeout != 0) {
        dismissTimeoutMillis = dismissTimeout;
      }
    }
  }

  ImageView getIcon() {
    return icon;
  }

  void onStart() {
    if (isDismissible) {
      return;
    }

    isDismissible = true;

    playHaptic();

    if (icon.getDrawable() instanceof Animatable) {
      ((Animatable) icon.getDrawable()).start();
    }
    // If the drawable is an instance of Animatable2 and a timeout was not specified dismiss after
    // the onAnimationEnd callback
    if (icon.getDrawable() instanceof Animatable2
        && dismissTimeoutMillis == DEFAULT_DISMISS_TIMEOUT_MILLIS) {
      ((Animatable2) icon.getDrawable()).registerAnimationCallback(animationCallback);
    } else {
      if (view != null) {
        view.postDelayed(dismissRunnable, dismissTimeoutMillis);
      }
    }
  }

  private void playHaptic() {
    if (view != null) {
      switch (hapticType) {
        case HapticTypeDef.SUCCESS:
          view.performHapticFeedback(
              Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                  ? HapticFeedbackConstants.CONFIRM
                  : HapticFeedbackConstants.VIRTUAL_KEY);
          break;
        case HapticTypeDef.ERROR:
          view.performHapticFeedback(
              Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                  ? HapticFeedbackConstants.REJECT
                  : HapticFeedbackConstants.VIRTUAL_KEY);
          break;
        case HapticTypeDef.NONE:
          // fall-through!
        default:
          break;
      }
    }
  }

  void onDismiss() {
    if (!isDismissible) {
      return;
    }

    isDismissible = false;
    if (dismissListener != null) {
      dismissListener.onDismissed();
    }
  }

  void onDestroy() {
    dismissListener = null;
  }

  static Bundle createArguments(
      CharSequence title, int iconResId, int dismissTimeoutMs, int hapticType) {
    Bundle args = new Bundle();
    args.putCharSequence(TITLE_KEY, title);
    args.putInt(ICON_KEY, iconResId);
    args.putInt(DISMISS_TIMEOUT_KEY, dismissTimeoutMs);
    args.putInt(HAPTIC_TYPE_KEY, hapticType);
    return args;
  }
}
