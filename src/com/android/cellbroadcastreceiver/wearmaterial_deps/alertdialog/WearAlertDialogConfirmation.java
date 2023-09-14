package com.google.android.clockwork.common.wearable.wearmaterial.alertdialog;

import static com.google.android.clockwork.common.wearable.wearmaterial.alertdialog.WearAlertDialogConfirmationViewController.createArguments;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Animatable2;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A confirmation overlay which is a special use case of an alert/dialog where user input is not
 * required
 */
public final class WearAlertDialogConfirmation extends AlertDialogFragment {

  /** The type of haptic to play when displaying a dialog. */
  @Retention(RetentionPolicy.SOURCE)
  @IntDef({HapticTypeDef.NONE, HapticTypeDef.ERROR, HapticTypeDef.SUCCESS})
  public @interface HapticTypeDef {
    /** Do not play a haptic. */
    int NONE = 0;

    /** Haptic representing an error or rejection. */
    int ERROR = 1;

    /** Haptic representing success or confirmation. */
    int SUCCESS = 2;
  }

  private @Nullable OnDismissListener dismissListener;

  @SuppressWarnings({"nullness:initialization.fields.uninitialized", "nullness:method.invocation"})
  private final WearAlertDialogConfirmationViewController controller =
      new WearAlertDialogConfirmationViewController(
          () -> {
            if (dismissListener != null) {
              dismissListener.onDismissed();
            }
            dismissAllowingStateLoss();
          });

  @Override
  protected int getContentLayoutId() {
    return controller.getContentLayoutId();
  }

  @Override
  public final void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    controller.configureView(requireArguments(), view);
  }

  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  ImageView getIcon() {
    return controller.getIcon();
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    controller.setContext(context);
  }

  @Override
  public void onDetach() {
    controller.setContext(null);
    super.onDetach();
  }

  @Override
  public void onStart() {
    super.onStart();
    controller.onStart();
  }

  @Override
  public void onDismiss(DialogInterface dialogInterface) {
    super.onDismiss(dialogInterface);
    controller.onDismiss();
  }

  @Override
  public void onDestroyView() {
    controller.onDestroy();
    super.onDestroyView();
  }

  /** Interface for a callback to be invoked when the dialog is dismissed. */
  public interface OnDismissListener {
    /** Called when {@link WearAlertDialogConfirmation} is dismissed. */
    void onDismissed();
  }

  /** Builder to be used for the creation of {@link WearAlertDialogConfirmation} */
  public static final class Builder extends BuilderBase<Builder> {

    private final Context context;
    private @Nullable OnDismissListener listener;
    private CharSequence title = "";
    private int iconResId = 0;
    private int dismissTimeout = 0;
    @HapticTypeDef private int hapticType = HapticTypeDef.NONE;

    /**
     * Creates a builder for an confirmation dialog
     *
     * @param context the parent context
     */
    public Builder(Context context) {
      this.context = context;
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

    /**
     * Sets the drawable to be displayed
     *
     * @param iconResId if this is an instance of {@link Animatable2} then there is no need to set
     *     {@link #setDismissTimeout(int)} as the timeout can be tied to the end of the animation.
     *     For other icon types the default timeout will be used unless another is specified
     * @throws android.content.res.Resources.NotFoundException if the given ID does not exist.
     */
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

    /**
     * Sets a timeout for the dismissal of this confirmation
     *
     * <p>if the icon set in {@link #setIcon(int)} is an instance of {@link Animatable2} then this
     * can be ignored and the confirmation will dismiss when onAnimationEnd is called
     *
     * @param dismissTimeout The timeout in milliseconds
     */
    @CanIgnoreReturnValue
    public Builder setDismissTimeout(int dismissTimeout) {
      this.dismissTimeout = dismissTimeout;
      return this;
    }

    /** Sets the type of haptic to be played. Defaults to no haptic. */
    @CanIgnoreReturnValue
    public Builder setHaptic(@HapticTypeDef int hapticType) {
      this.hapticType = hapticType;
      return this;
    }

    public WearAlertDialogConfirmation create() {
      WearAlertDialogConfirmation confirmation = new WearAlertDialogConfirmation();
      Bundle args = createArguments(title, iconResId, dismissTimeout, hapticType);
      args.putAll(createThemeArguments());
      confirmation.setArguments(args);
      confirmation.dismissListener = listener;
      return confirmation;
    }

    @VisibleForTesting
    View createView(
        @Nullable ViewGroup parent, WearAlertDialogConfirmationViewController controller) {
      Bundle args = createArguments(title, iconResId, dismissTimeout, hapticType);
      controller.dismissListener = listener;
      Context viewContext =
          contextTheme == 0 ? context : new ContextThemeWrapper(context, contextTheme);
      LayoutInflater layoutInflater = LayoutInflater.from(viewContext);
      ViewGroup wearLayout =
          (ViewGroup) layoutInflater.inflate(R.layout.wear_alert_dialog_content, parent, false);
      View view = layoutInflater.inflate(controller.getContentLayoutId(), wearLayout, true);
      controller.setContext(viewContext);
      controller.configureView(args, view);
      view.addOnAttachStateChangeListener(
          new OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
              controller.onStart();
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
              controller.onDestroy();
            }
          });
      if (Build.VERSION.SDK_INT >= VERSION_CODES.P) {
        view.setAccessibilityPaneTitle(title);
      }
      return view;
    }

    @VisibleForTesting
    public View createView(@Nullable ViewGroup parent) {
      return createView(
          parent,
          new WearAlertDialogConfirmationViewController(listener == null ? () -> {} : listener));
    }
  }
}
