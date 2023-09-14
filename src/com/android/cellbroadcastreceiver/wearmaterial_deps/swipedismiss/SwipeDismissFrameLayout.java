package com.google.android.clockwork.common.wearable.wearmaterial.swipedismiss;

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @deprecated Use the AndroidX SwipeDismissFrameLayout:
 *     https://source.corp.google.com/aosp-androidx/wear/wear/src/main/java/androidx/wear/widget/SwipeDismissFrameLayout.java
 *     <p>Deprecated fork of SwipeDismissFrameLayout. The SwipeDismissFrameLayout in AndroidX is
 *     updated to match the platform specs of animation.
 *     <p>A layout enabling left-to-right swipe-to-dismiss, intended for use within an activity.
 *     <p>At least one listener must be {@link #addCallback(Callback) added} to act on a dismissal
 *     action. A listener will typically remove a containing view or fragment from the current
 *     activity.
 *     <p>To suppress a swipe-dismiss gesture, at least one contained view must be scrollable,
 *     indicating that it would like to consume any horizontal touch gestures in that direction. In
 *     this case this view will only allow swipe-to-dismiss on the very edge of the left-hand-side
 *     of the screen. If you wish to entirely disable the swipe-to-dismiss gesture, {@link
 *     #setSwipeable(boolean)} can be used for more direct control over the feature.
 */
@Deprecated
@UiThread
public class SwipeDismissFrameLayout extends androidx.wear.widget.SwipeDismissFrameLayout {

  /** Implement this callback to act on particular stages of the dismissal. */
  @UiThread
  public abstract static class Callback {

    /**
     * Notifies listeners that the view is now being dragged as part of a dismiss gesture.
     *
     * @param layout The layout associated with this callback.
     */
    public void onSwipeStarted(SwipeDismissFrameLayout layout) {}

    /**
     * Notifies listeners that the swipe gesture has ended without a dismissal.
     *
     * @param layout The layout associated with this callback.
     */
    public void onSwipeCanceled(SwipeDismissFrameLayout layout) {}

    /**
     * Notifies listeners that the dismissal is complete and the view is now off screen.
     *
     * @param layout The layout associated with this callback.
     */
    public void onDismissed(SwipeDismissFrameLayout layout) {}
  }

  // Map to track Callback mapping to AndroidX SwipeToDismiss Callback
  @VisibleForTesting
  final Map<Callback, androidx.wear.widget.SwipeDismissFrameLayout.Callback> callbacksMap =
      new HashMap<>();

  /**
   * Simple constructor to use when creating a view from code.
   *
   * @param context The {@link Context} the view is running in, through which it can access the
   *     current theme, resources, etc.
   */
  public SwipeDismissFrameLayout(Context context) {
    this(context, null, 0);
  }

  /**
   * Constructor that is called when inflating a view from XML. This is called when a view is being
   * constructed from an XML file, supplying attributes that were specified in the XML file. This
   * version uses a default style of 0, so the only attribute values applied are those in the
   * Context's Theme and the given AttributeSet.
   *
   * <p>
   *
   * <p>The method onFinishInflate() will be called after all children have been added.
   *
   * @param context The {@link Context} the view is running in, through which it can access the
   *     current theme, resources, etc.
   * @param attrs The attributes of the XML tag that is inflating the view.
   */
  public SwipeDismissFrameLayout(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  /**
   * Perform inflation from XML and apply a class-specific base style from a theme attribute. This
   * constructor allows subclasses to use their own base style when they are inflating.
   *
   * @param context The {@link Context} the view is running in, through which it can access the
   *     current theme, resources, etc.
   * @param attrs The attributes of the XML tag that is inflating the view.
   * @param defStyle An attribute in the current theme that contains a reference to a style resource
   *     that supplies default values for the view. Can be 0 to not look for defaults.
   */
  public SwipeDismissFrameLayout(Context context, @Nullable AttributeSet attrs, int defStyle) {
    this(context, attrs, defStyle, 0);
  }

  /**
   * Perform inflation from XML and apply a class-specific base style from a theme attribute. This
   * constructor allows subclasses to use their own base style when they are inflating.
   *
   * @param context The {@link Context} the view is running in, through which it can access the
   *     current theme, resources, etc.
   * @param attrs The attributes of the XML tag that is inflating the view.
   * @param defStyle An attribute in the current theme that contains a reference to a style resource
   *     that supplies default values for the view. Can be 0 to not look for defaults.
   * @param defStyleRes It allows a style resource to be specified when creating the view.
   */
  // Suppressing warnings as super class is not annotated with Nullable.
  @SuppressWarnings({
    "nullness:argument.type.incompatible",
    "nullness:type.argument.type.incompatible"
  })
  public SwipeDismissFrameLayout(
      Context context, @Nullable AttributeSet attrs, int defStyle, int defStyleRes) {
    super(context, attrs, defStyle, defStyleRes);
  }

  /** Adds a callback for dismissal. */
  public void addCallback(Callback callback) {

    androidx.wear.widget.SwipeDismissFrameLayout.Callback androidxCallback =
        convertToAndroidxCallback(callback);
    // Adding mapping between the two callbacks
    if (callback != null) {
      callbacksMap.put(callback, androidxCallback);
    }
    super.addCallback(androidxCallback);
  }

  /** Removes a callback that was added with {@link #addCallback(Callback)}. */
  // Suppressing warnings as super class is not annotated with Nullable.
  @SuppressWarnings({
    "nullness:argument.type.incompatible",
    "nullness:type.argument.type.incompatible"
  })
  public void removeCallback(Callback callback) {
    if (callback == null) {
      throw new NullPointerException("removeCallback called with null callback");
    }

    androidx.wear.widget.SwipeDismissFrameLayout.Callback androidxCallback =
        callbacksMap.get(callback);
    super.removeCallback(androidxCallback);
    // Removing the callback from the map.
    if (callback != null) {
      callbacksMap.remove(callback);
    }
  }

  // Transforms the internal callback to androidx callback object
  private androidx.wear.widget.SwipeDismissFrameLayout.Callback convertToAndroidxCallback(
      Callback callback) {
    return new androidx.wear.widget.SwipeDismissFrameLayout.Callback() {

      @Override
      public void onSwipeStarted(androidx.wear.widget.SwipeDismissFrameLayout layout) {
        callback.onSwipeStarted((SwipeDismissFrameLayout) layout);
      }

      @Override
      public void onSwipeCanceled(androidx.wear.widget.SwipeDismissFrameLayout layout) {
        callback.onSwipeCanceled((SwipeDismissFrameLayout) layout);
      }

      @Override
      public void onDismissed(androidx.wear.widget.SwipeDismissFrameLayout layout) {
        callback.onDismissed((SwipeDismissFrameLayout) layout);
      }
    };
  }
}
