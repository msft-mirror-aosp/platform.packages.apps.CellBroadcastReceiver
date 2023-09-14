package com.google.android.clockwork.common.wearable.wearmaterial.list;

import android.animation.ValueAnimator;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import com.google.android.clockwork.common.wearable.wearmaterial.animations.Durations;

/**
 * Default {@link RecyclerView.ItemAnimator} for {@link FadingWearableRecyclerView} which updates
 * fading and scaling based on the position of the child views. we should be able to remove this
 * without changing anything on ItemAnimator once we fix the vinson test. See b/203563439 and
 * b/205005285
 */
public class FadingWearableItemAnimator extends WearableBaseItemAnimator {

  private static final Interpolator INTERPOLATOR = new PathInterpolator(0.4f, 0f, 0.2f, 1f);
  private static final long DURATION = Durations.SLOW;
  private static final float MIN_SCALE = 0.1f;

  /**
   * Will assign this update listener to the ViewPropertyAnimator {@link View#animate()} when
   * animating that ViewHolder. See {@link
   * FadingWearableItemAnimator#updateAnimatorListener(ViewHolder)}
   */
  private final ValueAnimator.AnimatorUpdateListener listener;

  private final ViewGroupFader fader;

  public static FadingWearableItemAnimator create(ViewGroupFader fader) {
    FadingWearableItemAnimator itemAnimator = new FadingWearableItemAnimator(fader);
    itemAnimator.setAddDuration(DURATION);
    itemAnimator.setRemoveDuration(DURATION);
    itemAnimator.setMoveDuration(DURATION);
    itemAnimator.setChangeDuration(DURATION);
    return itemAnimator;
  }

  public FadingWearableItemAnimator(ViewGroupFader fader) {
    this.fader = fader;
    this.listener = v -> fader.updateFade();
  }

  /** Add Animation */
  @Override
  protected void prepAnimateAdd(RecyclerView.ViewHolder holder) {
    View view = holder.itemView;
    updateAnimatingItemTag(view, true);
    view.setScaleX(MIN_SCALE);
    view.setScaleY(MIN_SCALE);
    view.setAlpha(0f);
    view.setPivotY(view.getHeight() / 2f);
  }

  @Override
  protected ViewPropertyAnimator createAddAnimator(RecyclerView.ViewHolder holder) {
    View view = holder.itemView;
    fader.fadeViewProperties(view);
    ViewPropertyAnimator viewAnimator = view.animate().setUpdateListener(listener);
    viewAnimator.setDuration(getAddDuration()).setInterpolator(INTERPOLATOR);
    return viewAnimator;
  }

  @Override
  protected void resetAnimateAdd(RecyclerView.ViewHolder holder) {
    fader.fadeElement(holder.itemView);
    updateAnimatingItemTag(holder.itemView, false);
  }

  @Override
  protected void prepAnimateChangeEnter(RecyclerView.ViewHolder holder) {
    holder.itemView.setScaleX(MIN_SCALE);
    holder.itemView.setScaleY(MIN_SCALE);
  }

  @Override
  protected ViewPropertyAnimator createChangeEnterAnimator(RecyclerView.ViewHolder holder) {
    View view = holder.itemView;
    view.animate().scaleX(1f).scaleY(1f);
    return view.animate();
  }

  @Override
  protected void resetAnimateChangeEnter(RecyclerView.ViewHolder holder) {
    holder.itemView.setScaleY(1f);
    holder.itemView.setScaleX(1f);
  }

  @Override
  protected void prepAnimateChangeExit(RecyclerView.ViewHolder holder) {
    holder.itemView.setScaleY(1f);
    holder.itemView.setScaleX(1f);
  }

  @Override
  protected ViewPropertyAnimator createChangeExitAnimator(RecyclerView.ViewHolder holder) {
    View view = holder.itemView;
    ViewPropertyAnimator viewAnimator = view.animate();
    viewAnimator.scaleX(MIN_SCALE).scaleY(MIN_SCALE).setUpdateListener(listener);
    return viewAnimator;
  }

  @Override
  protected void resetAnimateChangeExit(RecyclerView.ViewHolder holder) {
    holder.itemView.setScaleY(1f);
    holder.itemView.setScaleX(1f);
  }

  @Override
  protected boolean prepAnimateMove(
      final RecyclerView.ViewHolder holder, int fromX, int fromY, int toX, int toY) {
    View view = holder.itemView;
    int deltaX = toX - fromX;
    int deltaY = toY - fromY;
    if (deltaX == 0 && deltaY == 0) {
      return false;
    }
    if (deltaX != 0) {
      view.setTranslationX(-deltaX);
    }
    if (deltaY != 0) {
      view.setTranslationY(-deltaY);
    }
    return true;
  }

  @Override
  protected ViewPropertyAnimator createMoveAnimator(
      final RecyclerView.ViewHolder holder, int fromX, int fromY, int toX, int toY) {
    ViewPropertyAnimator viewAnimator = holder.itemView.animate();
    int deltaX = toX - fromX;
    int deltaY = toY - fromY;
    if (deltaX != 0) {
      viewAnimator.translationX(0);
    }
    if (deltaY != 0) {
      viewAnimator.translationY(0);
    }
    viewAnimator.setUpdateListener(listener);
    return viewAnimator;
  }

  @Override
  protected void resetAnimateMove(RecyclerView.ViewHolder holder) {
    holder.itemView.setTranslationX(0f);
    holder.itemView.setTranslationY(0f);
  }

  @Override
  protected void prepAnimateRemove(RecyclerView.ViewHolder holder) {
    View view = holder.itemView;
    fader.fadeElement(view);
    view.setPivotY(view.getHeight() / 2f);
    view.setPivotX(view.getWidth() / 2f);
    updateAnimatingItemTag(view, true);
  }

  @Override
  protected ViewPropertyAnimator createRemoveAnimator(RecyclerView.ViewHolder holder) {
    View view = holder.itemView;
    ViewPropertyAnimator viewAnimator = view.animate();
    viewAnimator
        .scaleX(MIN_SCALE)
        .scaleY(MIN_SCALE)
        .alpha(0f)
        .setDuration(getRemoveDuration())
        .setInterpolator(INTERPOLATOR)
        .setUpdateListener(listener);
    return viewAnimator;
  }

  @Override
  protected void resetAnimateRemove(RecyclerView.ViewHolder holder) {
    updateAnimatingItemTag(holder.itemView, false);
    fader.fadeElement(holder.itemView);
  }

  private static void updateAnimatingItemTag(View view, boolean isAnimating) {
    view.setTag(R.id.animating_item, isAnimating);
  }

  @Override
  public boolean animateRemove(ViewHolder holder) {
    updateAnimatorListener(holder);
    return super.animateRemove(holder);
  }

  @Override
  public boolean animateAdd(ViewHolder holder) {
    updateAnimatorListener(holder);
    return super.animateAdd(holder);
  }

  @Override
  public boolean animateMove(ViewHolder holder, int fromX, int fromY, int toX, int toY) {
    updateAnimatorListener(holder);
    return super.animateMove(holder, fromX, fromY, toX, toY);
  }

  @Override
  public boolean animateChange(
      ViewHolder oldHolder,
      ViewHolder newHolder,
      int fromLeft,
      int fromTop,
      int toLeft,
      int toTop) {
    updateAnimatorListener(newHolder);
    return super.animateChange(oldHolder, newHolder, fromLeft, fromTop, toLeft, toTop);
  }

  private void updateAnimatorListener(ViewHolder holder) {
    holder.itemView.animate().setUpdateListener(listener);
  }
}
