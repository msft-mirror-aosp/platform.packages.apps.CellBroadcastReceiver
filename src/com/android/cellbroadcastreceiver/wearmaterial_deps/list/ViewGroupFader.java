package com.google.android.clockwork.common.wearable.wearmaterial.list;

import static java.lang.Math.max;
import static java.lang.Math.min;

import android.content.Context;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewParent;
import android.view.animation.BaseInterpolator;
import android.view.animation.PathInterpolator;
import androidx.annotation.VisibleForTesting;
import com.google.android.clockwork.common.wearable.wearmaterial.util.GLUtil;
import com.google.android.clockwork.common.wearable.wearmaterial.util.MathUtils;

/**
 * Fades of the children of a {@link ViewGroup} in and out, based on the position of the child.
 *
 * <p>Children are "faded" they lie entirely in a region on the top and bottom of a {@link
 * ViewGroup}. This region is sized as a percentage of the {@link ViewGroup}'s height, based on the
 * height of the child. When not in the top or bottom regions, children have their default alpha and
 * scale.
 */
public class ViewGroupFader {
  public static final float SCALE_DEFAULT = 1.f;
  public static final float ALPHA_DEFAULT = 1.f;
  public static final float SCALE_LOWER_BOUND = 0.7f;
  private float scaleLowerBound = SCALE_LOWER_BOUND;

  public static final float ALPHA_LOWER_BOUND = 0.5f;
  private float alphaLowerBound = ALPHA_LOWER_BOUND;

  private static final float CHAINED_BOUNDS_TOP_PERCENT = 0.6f;
  private static final float CHAINED_BOUNDS_BOTTOM_PERCENT = 0.2f;
  private static final float CHAINED_LOWER_REGION_PERCENT = 0.35f;
  private static final float CHAINED_UPPER_REGION_PERCENT = 0.55f;

  public float chainedBoundsTop = CHAINED_BOUNDS_TOP_PERCENT;
  public float chainedBoundsBottom = CHAINED_BOUNDS_BOTTOM_PERCENT;
  public float chainedLowerRegion = CHAINED_LOWER_REGION_PERCENT;
  public float chainedUpperRegion = CHAINED_UPPER_REGION_PERCENT;
  private static String REDUCE_MOTION = "reduce_motion";

  protected final ViewGroup parent;

  protected final Rect containerBounds = new Rect();
  private final Rect offsetViewBounds = new Rect();
  private final AnimationCallback callback;
  private final ChildViewBoundsProvider childViewBoundsProvider;

  private ContainerBoundsProvider containerBoundsProvider;
  private float topBoundPixels;
  private float bottomBoundPixels;

  /**
   * Disables fading/scaling effect based on global {@link WearSettings.Global.REDUCE_MOTION}
   * setting.
   *
   * <p>Ideally, we would have ViewGroupFader being an interface and have two different
   * implementations, one that implements fading/scaling effect and the other which is essentially a
   * noop. It is not easy to pull this off at this point, though, because calls to ViewGroupFader
   * constructor are all over the codebase used across multiple apps.
   */
  private boolean isReduceMotionEnabled;

  @VisibleForTesting final ContentObserver reduceMotionSettingObserver;
  @VisibleForTesting boolean isReduceMotionSettingObserverRegistered;
  private BaseInterpolator topInterpolator = new PathInterpolator(0.3f, 0f, 0.7f, 1f);
  private BaseInterpolator bottomInterpolator = new PathInterpolator(0.3f, 0f, 0.7f, 1f);

  /** This callback is used if {@link WearSettings.Global.REDUCE_MOTION} setting value is 1 */
  private static final AnimationCallback NOOP_ANIMATION_CALLBACK =
      new AnimationCallback() {
        @Override
        public boolean shouldFadeFromTop(View view) {
          return false;
        }

        @Override
        public boolean shouldFadeFromBottom(View view) {
          return false;
        }

        @Override
        public void viewHasBecomeFullSize(View view) {}
      };

  @VisibleForTesting
  final OnAttachStateChangeListener onAttachStateChangeListener =
      new OnAttachStateChangeListener() {
        @Override
        public void onViewAttachedToWindow(View view) {
          if (!isReduceMotionSettingObserverRegistered && reduceMotionSettingObserver != null) {
            view.getContext()
                .getContentResolver()
                .registerContentObserver(
                    Settings.Global.getUriFor(REDUCE_MOTION), true, reduceMotionSettingObserver);
            isReduceMotionSettingObserverRegistered = true;
            isReduceMotionEnabled = isReduceMotionEnabled(view.getContext());
          }
        }

        @Override
        public void onViewDetachedFromWindow(View view) {
          if (isReduceMotionSettingObserverRegistered && reduceMotionSettingObserver != null) {
            view.getContext()
                .getContentResolver()
                .unregisterContentObserver(reduceMotionSettingObserver);
            isReduceMotionSettingObserverRegistered = false;
          }
        }
      };

  /** Callback which is called when attempting to fade a view. */
  public interface AnimationCallback {
    boolean shouldFadeFromTop(View view);

    boolean shouldFadeFromBottom(View view);

    void viewHasBecomeFullSize(View view);
  }

  /**
   * Interface for providing the bounds of the child views. This is needed because for
   * RecyclerViews, we might need to use bounds that represents the post-layout position, instead of
   * the current position.
   */
  // TODO(b/182846214): Clean up the interface design to avoid exposing too much details to users.
  public interface ChildViewBoundsProvider {
    void provideBounds(ViewGroup parent, View child, Rect bounds);
  }

  /** Interface for providing the bounds of the container for use in calculating item fades. */
  public interface ContainerBoundsProvider {
    void provideBounds(ViewGroup parent, Rect bounds);
  }

  /**
   * Implementation of {@link ContainerBoundsProvider} that returns the screen bounds as the
   * container that is used for calculating the animation of the child elements in the ViewGroup.
   */
  public static final class ScreenContainerBoundsProvider implements ContainerBoundsProvider {
    @Override
    public void provideBounds(ViewGroup parent, Rect bounds) {
      bounds.set(
          0,
          0,
          parent.getResources().getDisplayMetrics().widthPixels,
          parent.getResources().getDisplayMetrics().heightPixels);
    }
  }

  /**
   * Implementation of {@link ContainerBoundsProvider} that returns the parent ViewGroup bounds as
   * the container that is used for calculating the animation of the child elements in the
   * ViewGroup.
   */
  public static final class ParentContainerBoundsProvider implements ContainerBoundsProvider {
    @Override
    public void provideBounds(ViewGroup parent, Rect bounds) {
      parent.getGlobalVisibleRect(bounds);
    }
  }

  /**
   * Default implementation of {@link ChildViewBoundsProvider} that returns the post-layout bounds
   * of the child view. This should be used when the {@link ViewGroupFader} is used together with a
   * RecyclerView.
   */
  public static final class DefaultViewBoundsProvider implements ChildViewBoundsProvider {
    @Override
    public void provideBounds(ViewGroup parent, View child, Rect bounds) {
      child.getDrawingRect(bounds);
      bounds.offset(0, (int) child.getTranslationY());
      parent.offsetDescendantRectToMyCoords(child, bounds);

      // Additionally offset the bounds based on parent container's absolute position.
      Rect parentGlobalVisibleBounds = new Rect();
      parent.getGlobalVisibleRect(parentGlobalVisibleBounds);
      bounds.offset(parentGlobalVisibleBounds.left, parentGlobalVisibleBounds.top);
    }
  }

  /**
   * Implementation of {@link ChildViewBoundsProvider} that returns the global visible bounds of the
   * child view. This should be used when the {@link ViewGroupFader} is not used together with a
   * RecyclerView.
   */
  public static final class GlobalVisibleViewBoundsProvider implements ChildViewBoundsProvider {
    @Override
    public void provideBounds(ViewGroup parent, View child, Rect bounds) {
      // Get the absolute position of the child. Normally we'd need to also reset the transformation
      // matrix before computing this, but the transformations we apply set a pivot that preserves
      // the coordinate of the top/bottom boundary used to compute the scaling factor in the first
      // place.
      child.getGlobalVisibleRect(bounds);
    }
  }

  public ViewGroupFader(
      ViewGroup parent,
      AnimationCallback callback,
      ChildViewBoundsProvider childViewBoundsProvider) {
    this.parent = parent;
    this.callback = callback;
    this.childViewBoundsProvider = childViewBoundsProvider;
    this.containerBoundsProvider = new ScreenContainerBoundsProvider();
    reduceMotionSettingObserver = new ReduceMotionSettingObserver(parent.getHandler());
    this.parent.addOnAttachStateChangeListener(onAttachStateChangeListener);
    isReduceMotionEnabled = isReduceMotionEnabled(parent.getContext());
  }

  private static boolean isReduceMotionEnabled(Context context) {
    return Settings.Global.getInt(context.getContentResolver(), REDUCE_MOTION, 0) != 0;
  }

  public AnimationCallback getAnimationCallback() {
    return isReduceMotionEnabled ? NOOP_ANIMATION_CALLBACK : callback;
  }

  public void setScaleLowerBound(float scale) {
    scaleLowerBound = scale;
  }

  public void setAlphaLowerBound(float alpha) {
    alphaLowerBound = alpha;
  }

  public void setTopInterpolator(BaseInterpolator interpolator) {
    this.topInterpolator = interpolator;
  }

  public void setBottomInterpolator(BaseInterpolator interpolator) {
    this.bottomInterpolator = interpolator;
  }

  public void setContainerBoundsProvider(ContainerBoundsProvider boundsProvider) {
    this.containerBoundsProvider = boundsProvider;
  }

  public void updateFade() {
    containerBoundsProvider.provideBounds(parent, containerBounds);
    topBoundPixels = containerBounds.height() * chainedBoundsTop;
    bottomBoundPixels = containerBounds.height() * chainedBoundsBottom;

    updateListElementFades(parent, true);
  }

  /**
   * For each list element and child views in {@link FadingWearableContainer}, calculate and adjust
   * the scale and alpha based on its position
   */
  private void updateListElementFades(ViewGroup parent, boolean shouldFade) {
    for (int i = 0; i < parent.getChildCount(); i++) {
      View child = parent.getChildAt(i);
      if (child.getVisibility() != View.VISIBLE) {
        continue;
      }

      if (child instanceof ViewGroup) {
        updateListElementFades((ViewGroup) child, child instanceof FadingWearableContainer);
      }

      if (shouldFade) {
        fadeElement(child);
      }
    }
  }

  /** Set the bounds and change the view's scale and alpha accordingly */
  void fadeElement(View child) {
    if (!isDescendant(child)) {
      return;
    }
    float offset = calculateOffset(child);
    scaleAndFadeByRelativeOffset(child, offset);
  }

  private float calculateOffset(View view) {
    childViewBoundsProvider.provideBounds(parent, view, offsetViewBounds);
    AnimationCallback animationCallback = getAnimationCallback();
    Rect bounds = offsetViewBounds;
    setLayerType(view);
    float fadeOutRegionPercent;
    if (view.getHeight() < topBoundPixels && view.getHeight() > bottomBoundPixels) {
      // Scale from LOWER_REGION_PERCENT to UPPER_REGION_PERCENT based on the ratio of view height
      // to chain region height
      fadeOutRegionPercent =
          MathUtils.lerp(
              chainedLowerRegion,
              chainedUpperRegion,
              (view.getHeight() - bottomBoundPixels) / (topBoundPixels - bottomBoundPixels));
    } else if (view.getHeight() < bottomBoundPixels) {
      fadeOutRegionPercent = chainedLowerRegion;
    } else {
      fadeOutRegionPercent = chainedUpperRegion;
    }
    int fadeOutRegionHeight = (int) (containerBounds.height() * fadeOutRegionPercent);
    if (!animationCallback.shouldFadeFromTop(view)
        || !animationCallback.shouldFadeFromBottom(view)) {
      fadeOutRegionHeight = min(fadeOutRegionHeight, view.getHeight());
    }
    int topFadeBoundary = fadeOutRegionHeight + containerBounds.top;
    int bottomFadeBoundary = containerBounds.bottom - fadeOutRegionHeight;
    boolean wasFullSize = (view.getScaleX() == 1);

    final MarginLayoutParams lp = (MarginLayoutParams) view.getLayoutParams();
    view.setPivotX(parent.getWidth() * 0.5f - view.getLeft());
    float offset = 1f;
    if (lp == null) {
      return offset;
    }
    if (bounds.top > bottomFadeBoundary) {
      view.setPivotY((float) -lp.topMargin);
      offset =
          bottomInterpolator.getInterpolation(
              (float) (containerBounds.bottom - bounds.top) / fadeOutRegionHeight);
    } else if (bounds.bottom < topFadeBoundary) {
      view.setPivotY(view.getMeasuredHeight() + (float) lp.bottomMargin);
      offset =
          topInterpolator.getInterpolation(
              (float) (bounds.bottom - containerBounds.top) / fadeOutRegionHeight);
    } else {
      if (!wasFullSize) {
        animationCallback.viewHasBecomeFullSize(view);
      }
    }
    return offset;
  }

  /** Change the scale and opacity of the view based on its offset to the determining bound */
  protected void scaleAndFadeByRelativeOffset(View view, float offset) {
    // Do not override the scale and alpha if the view is animating
    Object animatingTag = view.getTag(R.id.animating_item);
    if (animatingTag != null && (boolean) animatingTag) {
      return;
    }
    if (isReduceMotionEnabled) {
      // if reduce_motion setting is enabled, set default scaling and alpha.
      setDefaultViewProperties(view);
      return;
    }
    setViewPropertiesWithOffset(view, offset);
  }

  private void setDefaultViewProperties(View view) {
    view.setAlpha(ALPHA_DEFAULT);
    view.setScaleX(SCALE_DEFAULT);
    view.setScaleY(SCALE_DEFAULT);
  }

  void fadeViewProperties(View view) {
    if (!isDescendant(view)) {
      return;
    }
    if (isReduceMotionEnabled) {
      // if reduce_motion setting is enabled, set default scaling and alpha.
      setDefaultViewProperties(view);
      return;
    }
    float offset = calculateOffset(view);
    setViewPropertiesWithOffset(view, offset);
  }

  private void setViewPropertiesWithOffset(View view, float offset) {
    float alpha = getAlphaWithOffset(offset);
    float scale = getScaleWithOffset(offset);
    view.setAlpha(alpha);
    view.setScaleX(scale);
    view.setScaleY(scale);
  }

  private float getAlphaWithOffset(float offset) {
    return MathUtils.lerp(alphaLowerBound, 1, offset);
  }

  private float getScaleWithOffset(float offset) {
    return MathUtils.lerp(scaleLowerBound, 1, offset);
  }

  private boolean isDescendant(View view) {
    ViewParent viewParent = view.getParent();
    if (viewParent == parent) {
      return true;
    }
    if (viewParent instanceof View) {
      return isDescendant((View) viewParent);
    }
    return false;
  }

  /**
   * Enables hardware-layer acceleration rendering for {@code view} on Android-R or if the {@code
   * view} is not too large.
   */
  @VisibleForTesting
  static void setLayerType(View view) {
    long maxViewSize =
        VERSION.SDK_INT >= VERSION_CODES.R ? Integer.MAX_VALUE : getMaxViewSize(view.getContext());
    int layerType =
        (view.getWidth() <= maxViewSize && view.getHeight() <= maxViewSize)
            ? View.LAYER_TYPE_HARDWARE
            : View.LAYER_TYPE_NONE;
    if (view.getLayerType() != layerType) {
      view.setLayerType(layerType, null);
    }
  }

  @VisibleForTesting
  static int getMaxViewSize(Context context) {
    int glMaxTextureSize = GLUtil.getMaxTextureSize();
    DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
    int maxScreenSize = max(displayMetrics.widthPixels, displayMetrics.heightPixels);
    return glMaxTextureSize == 0 ? maxScreenSize : glMaxTextureSize;
  }

  final class ReduceMotionSettingObserver extends ContentObserver {
    /**
     * Creates a content observer to listen to global {@link WearSettings.Global.REDUCE_MOTION}
     * setting.
     *
     * @param handler The handler to run {@link #onChange} on, or null if none.
     */
    public ReduceMotionSettingObserver(Handler handler) {
      super(handler);
    }

    @Override
    public void onChange(boolean selfChange) {
      super.onChange(selfChange);
      isReduceMotionEnabled = isReduceMotionEnabled(parent.getContext());
    }
  }
}
