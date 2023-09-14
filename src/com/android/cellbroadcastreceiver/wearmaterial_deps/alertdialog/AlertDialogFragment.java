package com.google.android.clockwork.common.wearable.wearmaterial.alertdialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.StyleRes;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Adds a container for custom alertDialog definitions. */
public abstract class AlertDialogFragment extends DialogFragment {
  private static final String DIALOG_THEME_OVERLAY_KEY = "dialogTheme";
  private static final String CONTEXT_THEME_KEY = "contextTheme";

  @SuppressLint("InlinedApi")
  private static final int ID_NULL = Resources.ID_NULL;

  @StyleRes private int contextTheme = ID_NULL;

  /**
   * Returns the id of the layout-resource that will be inflated inside the {@code
   * R.layout.wear_alert_dialog_content}.
   */
  protected abstract int getContentLayoutId();

  @Override
  public void onCreate(@Nullable Bundle bundle) {
    super.onCreate(bundle);
    Bundle arguments = getArguments();

    if (arguments != null) {
      this.contextTheme = arguments.getInt(CONTEXT_THEME_KEY, ID_NULL);
      int dialogThemeOverlay = arguments.getInt(DIALOG_THEME_OVERLAY_KEY, ID_NULL);
      setStyle(DialogFragment.STYLE_NO_TITLE, dialogThemeOverlay);
    } else {
      setStyle(DialogFragment.STYLE_NO_TITLE, ID_NULL);
    }
  }

  @Override
  public final @Nullable View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    ViewGroup wearLayout =
        (ViewGroup) inflater.inflate(R.layout.wear_alert_dialog_content, container, false);

    return inflater.inflate(getContentLayoutId(), wearLayout, true);
  }

  @Override
  public final Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    Context themedContext;
    if (contextTheme != ID_NULL) {
      // Setting a theme on a ContextThemeWrapper which is ID_NULL will wrap the theme in the device
      // default theme, which we do not want, so only apply the wrapper when the value is non-null
      themedContext = new ContextThemeWrapper(requireContext(), contextTheme);
    } else {
      themedContext = requireContext();
    }
    Dialog dialog = new Dialog(themedContext, getTheme());

    // Turn off back key
    dialog.setCancelable(false);
    return dialog;
  }

  /** Abstract builder for {@link AlertDialogFragment}. */
  public abstract static class BuilderBase<T extends BuilderBase<T>> {
    protected int dialogThemeOverlay = ID_NULL;
    protected int contextTheme = ID_NULL;

    /**
     * Sets the theme of an Alert Dialog.
     *
     * <p>The theme should inherit from an overlay theme like {@code ThemeOverlay.Material.Dialog}
     * and not a full theme like {@code Theme.DeviceDefault} or {@code WearMaterialTheme}
     *
     * @deprecated It is generally not advisable to call this, as the majority of Wear apps should
     *     use the device default animations for dialogs.
     */
    @CanIgnoreReturnValue
    @Deprecated
    public T setTheme(@StyleRes int themeResId) {
      return setTheme(ID_NULL, themeResId);
    }

    /**
     * Sets the theme of an Alert Dialog.
     *
     * @param contextTheme this theme will be applied as a wrapper around the context's theme. This
     *     is typically not needed, unless using the dialog from a non-application context, for
     *     example if not using an activity context in unit tests, or creating a dialog from a
     *     service context. This should generally be set to {@link
     *     android.content.res.Resources#ID_NULL}
     * @param dialogThemeOverlay this theme should inherit from an overlay theme like {@code
     *     ThemeOverlay.Material.Dialog} and not a full theme like {@code Theme.DeviceDefault} or
     *     {@code WearMaterialTheme}. It will override the attribute {@code android:dialogTheme} in
     *     your applications theme. See also {@link android.app.Dialog#Dialog(Context, int)}
     */
    @SuppressWarnings("unchecked")
    public T setTheme(@StyleRes int contextTheme, @StyleRes int dialogThemeOverlay) {
      this.contextTheme = contextTheme;
      this.dialogThemeOverlay = dialogThemeOverlay;
      return (T) this;
    }

    Bundle createThemeArguments() {
      Bundle args = new Bundle();
      args.putInt(CONTEXT_THEME_KEY, contextTheme);
      args.putInt(DIALOG_THEME_OVERLAY_KEY, dialogThemeOverlay);

      return args;
    }
  }
}
