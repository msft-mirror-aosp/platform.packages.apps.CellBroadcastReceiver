package com.google.android.clockwork.common.wearable.wearmaterial.picker;

import static android.view.animation.AnimationUtils.loadInterpolator;
import static androidx.core.math.MathUtils.clamp;
import static com.google.android.clockwork.common.wearable.wearmaterial.util.MathUtils.lerp;
import static java.lang.Math.abs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.view.View;
import android.view.animation.Interpolator;
import com.google.android.clockwork.common.wearable.wearmaterial.picker.CenteredLinearLayoutManager.OnMovedItemViewListener;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Implementation of a {@link OnMovedItemViewListener} that skews list-items in a vertical {@code
 * RecyclerView}.
 *
 * <p>It does so by scaling the list-item views depending on their distance from the center of the
 * {@code RecyclerView}, giving the it a 'skewed' appearance.
 */
public final class SkewMovedItemViewListener implements OnMovedItemViewListener {

  private static final float MAX_SCALE = 1;
  private static final float MIN_SCALE = 0.45f;

  private @Nullable Interpolator scalingInterpolator;

  @SuppressLint("ObsoleteSdkInt")
  @Override
  public void onMoved(LinearLayoutManager layoutManager, View itemView) {
    if (layoutManager.getOrientation() == LinearLayoutManager.HORIZONTAL) {
      return;
    }

    float height = layoutManager.getHeight();
    int itemHeight = itemView.getHeight();
    int itemWidth = itemView.getWidth();

    float centerOfLayout = height / 2f;
    float centerOfChild =
        (itemView.getTop() + itemView.getBottom()) / 2f + itemView.getTranslationY();
    float distanceFromCenter = centerOfLayout - centerOfChild;

    float maxDistance = centerOfLayout + (itemHeight / 2f);
    float distanceRatio = clamp(abs(distanceFromCenter) / maxDistance, 0, 1);
    float scale = scale(itemView.getContext(), distanceRatio);

    if (scale == 1f) {
      if (VERSION.SDK_INT >= VERSION_CODES.P) {
        itemView.resetPivot();
      } else {
        itemView.setPivotY(itemHeight / 2f);
        itemView.setPivotX(itemWidth / 2f);
      }
    } else {
      // The vertical pivot-point (PivotY) of the 'itemView' must be on the same height as the
      // vertical center-line of the RecyclerView. However, it should never be above the 'itemView's
      // top or below the 'itemView's bottom.
      float defaultPivot = itemHeight / 2f;
      float pivot = clamp(defaultPivot + distanceFromCenter, 0, itemHeight);
      itemView.setPivotY(pivot);
      itemView.setPivotX(itemWidth / 2f);
    }

    itemView.setScaleX(scale);
    itemView.setScaleY(scale);
  }

  private float scale(Context context, float input) {
    return lerp(MIN_SCALE, MAX_SCALE, getScalingInterpolator(context).getInterpolation(1 - input));
  }

  private Interpolator getScalingInterpolator(Context context) {
    if (scalingInterpolator == null) {
      scalingInterpolator = loadInterpolator(context, R.anim.wear_picker_skew_interpolator);
    }
    return scalingInterpolator;
  }
}
