package com.google.android.clockwork.common.wearable.wearmaterial.list;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import android.view.View;
import android.view.ViewPropertyAnimator;
import androidx.core.view.ViewCompat;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A Fork of DefaultItemAnimator to allow for easy animation extendability in all animation cases.
 * See
 * https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-recyclerview-release/recyclerview/recyclerview/src/main/java/androidx/recyclerview/widget/DefaultItemAnimator.java
 * <br>
 * Every animation type (Add, ChangeEnter, ChangeExit, Move and Remove) has three methods which
 * require implementation.
 *
 * <ul>
 *   <li>prepAnimateX - Where views prepared for animation before it starts.
 *   <li>createXAnimator - Where the actual animator which does the work.
 *   <li>resetAnimateX - Where views are reset to be recycled after the animation finished or was
 *       canceled
 * </ul>
 */
public abstract class WearableBaseItemAnimator extends SimpleItemAnimator {
  private static final boolean DEBUG = false;
  protected TimeInterpolator defaultInterpolator = new ValueAnimator().getInterpolator();
  private final ArrayList<RecyclerView.ViewHolder> pendingRemovals = new ArrayList<>();

  private final ArrayList<RecyclerView.ViewHolder> pendingAdditions = new ArrayList<>();
  private final ArrayList<MoveInfo> pendingMoves = new ArrayList<>();
  private final ArrayList<ChangeInfo> pendingChanges = new ArrayList<>();
  ArrayList<ArrayList<RecyclerView.ViewHolder>> additionsList = new ArrayList<>();
  ArrayList<ArrayList<MoveInfo>> movesList = new ArrayList<>();
  ArrayList<ArrayList<ChangeInfo>> changesList = new ArrayList<>();
  ArrayList<RecyclerView.ViewHolder> addAnimations = new ArrayList<>();
  ArrayList<RecyclerView.ViewHolder> moveAnimations = new ArrayList<>();
  ArrayList<RecyclerView.ViewHolder> removeAnimations = new ArrayList<>();
  ArrayList<RecyclerView.ViewHolder> changeAnimations = new ArrayList<>();
  /**
   * Prepare View for Add animation
   *
   * <p>Example, if you going to animate a view to 1 alpha, you would set the view's alpha to 0
   * here.
   */
  protected void prepAnimateAdd(RecyclerView.ViewHolder holder) {}

  /**
   * Returns a {@link ViewPropertyAnimator} and {@link Animator.AnimatorListener}.<br>
   * <br>
   * <strong>Note</strong> {@link ViewPropertyAnimator#setListener} will be overrode. Please use
   * {@link #prepAnimateAdd} and {@link #resetAnimateAdd} instead.
   */
  protected abstract ViewPropertyAnimator createAddAnimator(RecyclerView.ViewHolder holder);

  /** This could be called before an animation, at animation end or before recycling. */
  protected void resetAnimateAdd(RecyclerView.ViewHolder holder) {}

  /**
   * Returns a {@link ViewPropertyAnimator} and {@link Animator.AnimatorListener}. <br>
   * <br>
   * <strong>Note</strong> {@link ViewPropertyAnimator#setListener} will be overrode. Please use
   * {@link #prepAnimateChangeEnter} and {@link #resetAnimateChangeEnter} instead.
   */
  protected abstract ViewPropertyAnimator createChangeEnterAnimator(RecyclerView.ViewHolder holder);

  /**
   * Prep Views for animation, translationX and translationY are handled automatically.
   *
   * <p>Example, if you going to animate a view to 1 alpha, you would set the view's alpha to 0
   * here.
   */
  protected void prepAnimateChangeEnter(RecyclerView.ViewHolder holder) {}

  /** This could be called before an animation, at animation end or before recycling. */
  protected void resetAnimateChangeEnter(RecyclerView.ViewHolder holder) {}

  /**
   * Returns a {@link ViewPropertyAnimator} and {@link Animator.AnimatorListener}. <br>
   * <br>
   * <strong>Note</strong> {@link ViewPropertyAnimator#setListener} will be overrode. Please use
   * {@link #prepAnimateChangeExit} and {@link #resetAnimateChangeExit} instead.<br>
   * {@link WearableBaseItemAnimator} will override translate X and Y of this animator.
   */
  protected abstract ViewPropertyAnimator createChangeExitAnimator(RecyclerView.ViewHolder holder);

  /**
   * Prep Views for animation, translationX and translationY are handled automatically.
   *
   * <p>Example, if you going to animate a view to 1 alpha, you would set the view's alpha to 0
   * here.
   */
  protected void prepAnimateChangeExit(RecyclerView.ViewHolder holder) {}

  /** This could be called before an animation, at animation end or before recycling. */
  protected void resetAnimateChangeExit(RecyclerView.ViewHolder holder) {}

  /**
   * Prep Views for animation and returns true if an animation is required.
   *
   * <p>Example, if you going to animate a view to 1 alpha, you would set the view's alpha to 0
   * here.
   */
  protected abstract boolean prepAnimateMove(
      RecyclerView.ViewHolder holder, int fromX, int fromY, int toX, int toY);

  /**
   * Returns a {@link ViewPropertyAnimator} and {@link Animator.AnimatorListener}.
   *
   * <p><strong>Note</strong> {@link ViewPropertyAnimator#setListener} will be overrode. Please use
   * {@link #prepAnimateMove} and {@link #resetAnimateMove} instead.
   */
  protected abstract ViewPropertyAnimator createMoveAnimator(
      RecyclerView.ViewHolder holder, int fromX, int fromY, int toX, int toY);

  /** This could be called before an animation, at animation end or before recycling. */
  protected abstract void resetAnimateMove(RecyclerView.ViewHolder holder);

  /**
   * Prep Views for animation
   *
   * <p>Example, if you going to animate a view to 1 alpha, you would set the view's alpha to 0
   * here.
   */
  protected abstract void prepAnimateRemove(RecyclerView.ViewHolder holder);

  /**
   * Returns a {@link ViewPropertyAnimator} and {@link Animator.AnimatorListener}.
   *
   * <p>{@link ViewPropertyAnimator#setListener} will be overrode and the returned {@link
   * Animator.AnimatorListener} will be used instead.
   */
  protected abstract ViewPropertyAnimator createRemoveAnimator(RecyclerView.ViewHolder holder);

  /** This could be called before an animation, at animation end or before recycling. */
  protected abstract void resetAnimateRemove(RecyclerView.ViewHolder holder);

  private static class MoveInfo {
    public RecyclerView.ViewHolder holder;
    public int fromX;
    public int fromY;
    public int toX;
    public int toY;

    MoveInfo(RecyclerView.ViewHolder holder, int fromX, int fromY, int toX, int toY) {
      this.holder = holder;
      this.fromX = fromX;
      this.fromY = fromY;
      this.toX = toX;
      this.toY = toY;
    }
  }

  private static class ChangeInfo {
    public RecyclerView.@Nullable ViewHolder oldHolder;
    public RecyclerView.@Nullable ViewHolder newHolder;
    public float fromX;
    public float fromY;
    public float toX;
    public float toY;

    private ChangeInfo(RecyclerView.ViewHolder oldHolder, RecyclerView.ViewHolder newHolder) {
      this.oldHolder = oldHolder;
      this.newHolder = newHolder;
    }

    ChangeInfo(
        RecyclerView.ViewHolder oldHolder,
        RecyclerView.ViewHolder newHolder,
        float fromX,
        float fromY,
        float toX,
        float toY) {
      this(oldHolder, newHolder);
      this.fromX = fromX;
      this.fromY = fromY;
      this.toX = toX;
      this.toY = toY;
    }

    @Override
    public String toString() {
      return "ChangeInfo{"
          + "oldHolder="
          + oldHolder
          + ", newHolder="
          + newHolder
          + ", fromX="
          + fromX
          + ", fromY="
          + fromY
          + ", toX="
          + toX
          + ", toY="
          + toY
          + '}';
    }
  }

  @Override
  public void runPendingAnimations() {
    boolean removalsPending = !pendingRemovals.isEmpty();
    boolean movesPending = !pendingMoves.isEmpty();
    boolean changesPending = !pendingChanges.isEmpty();
    boolean additionsPending = !pendingAdditions.isEmpty();
    if (!removalsPending && !movesPending && !additionsPending && !changesPending) {
      // nothing to animate
      return;
    }
    // First, remove stuff
    for (RecyclerView.ViewHolder holder : pendingRemovals) {
      animateRemoveImpl(holder);
    }
    pendingRemovals.clear();
    // Next, move stuff
    if (movesPending) {
      final ArrayList<MoveInfo> moves = new ArrayList<>();
      moves.addAll(pendingMoves);
      movesList.add(moves);
      pendingMoves.clear();
      Runnable mover =
          new Runnable() {
            @Override
            public void run() {
              for (MoveInfo moveInfo : moves) {
                animateMoveImpl(
                    moveInfo.holder, moveInfo.fromX, moveInfo.fromY, moveInfo.toX, moveInfo.toY);
              }
              moves.clear();
              movesList.remove(moves);
            }
          };
      if (removalsPending) {
        View view = moves.get(0).holder.itemView;
        ViewCompat.postOnAnimationDelayed(view, mover, getRemoveDuration());
      } else {
        mover.run();
      }
    }
    // Next, change stuff, to run in parallel with move animations
    if (changesPending) {
      final ArrayList<ChangeInfo> changes = new ArrayList<>();
      changes.addAll(pendingChanges);
      changesList.add(changes);
      pendingChanges.clear();
      Runnable changer =
          new Runnable() {
            @Override
            public void run() {
              for (ChangeInfo change : changes) {
                animateChangeImpl(change);
              }
              changes.clear();
              changesList.remove(changes);
            }
          };
      if (removalsPending) {
        RecyclerView.ViewHolder holder = changes.get(0).oldHolder;
        if (holder != null) {
          ViewCompat.postOnAnimationDelayed(holder.itemView, changer, getRemoveDuration());
        }
      } else {
        changer.run();
      }
    }
    // Next, add stuff
    if (additionsPending) {
      final ArrayList<RecyclerView.ViewHolder> additions = new ArrayList<>();
      additions.addAll(pendingAdditions);
      additionsList.add(additions);
      pendingAdditions.clear();
      Runnable adder =
          new Runnable() {
            @Override
            public void run() {
              for (RecyclerView.ViewHolder holder : additions) {
                animateAddImpl(holder);
              }
              additions.clear();
              additionsList.remove(additions);
            }
          };
      if (removalsPending || movesPending || changesPending) {
        long removeDuration = removalsPending ? getRemoveDuration() : 0;
        long moveDuration = movesPending ? getMoveDuration() : 0;
        long changeDuration = changesPending ? getChangeDuration() : 0;
        long totalDelay = removeDuration + Math.max(moveDuration, changeDuration);
        View view = additions.get(0).itemView;
        ViewCompat.postOnAnimationDelayed(view, adder, totalDelay);
      } else {
        adder.run();
      }
    }
  }

  @Override
  public boolean animateRemove(final RecyclerView.ViewHolder holder) {
    resetAnimation(holder);
    prepAnimateRemove(holder);
    pendingRemovals.add(holder);
    return true;
  }

  private void animateRemoveImpl(final RecyclerView.ViewHolder holder) {
    final ViewPropertyAnimator viewAnimator = createRemoveAnimator(holder);
    removeAnimations.add(holder);
    viewAnimator
        .setDuration(getRemoveDuration())
        .setListener(
            new AnimatorListenerAdapter() {
              @Override
              public void onAnimationStart(Animator animator) {
                dispatchRemoveStarting(holder);
              }

              @Override
              public void onAnimationEnd(Animator animator) {
                viewAnimator.setListener(null);
                resetAnimateRemove(holder);
                dispatchRemoveFinished(holder);
                removeAnimations.remove(holder);
                dispatchFinishedWhenDone();
              }
            })
        .start();
  }

  @Override
  public boolean animateAdd(final RecyclerView.ViewHolder holder) {
    resetAnimation(holder);
    prepAnimateAdd(holder);
    pendingAdditions.add(holder);
    return true;
  }

  void animateAddImpl(final RecyclerView.ViewHolder holder) {
    final ViewPropertyAnimator viewAnimator = createAddAnimator(holder);
    addAnimations.add(holder);
    viewAnimator
        .setDuration(getAddDuration())
        .setListener(
            new AnimatorListenerAdapter() {
              @Override
              public void onAnimationStart(Animator animator) {
                dispatchAddStarting(holder);
              }

              @Override
              public void onAnimationCancel(Animator animation) {
                resetAnimateAdd(holder);
              }

              @Override
              public void onAnimationEnd(Animator animator) {
                viewAnimator.setListener(null);
                resetAnimateAdd(holder);
                dispatchAddFinished(holder);
                addAnimations.remove(holder);
                dispatchFinishedWhenDone();
              }
            })
        .start();
  }

  @Override
  public boolean animateMove(
      final RecyclerView.ViewHolder holder, int fromX, int fromY, int toX, int toY) {
    fromX += (int) holder.itemView.getTranslationX();
    fromY += (int) holder.itemView.getTranslationY();
    resetAnimation(holder);
    if (!prepAnimateMove(holder, fromX, fromY, toX, toY)) {
      dispatchMoveFinished(holder);
      return false;
    }
    pendingMoves.add(new MoveInfo(holder, fromX, fromY, toX, toY));
    return true;
  }

  void animateMoveImpl(
      final RecyclerView.ViewHolder holder, int fromX, int fromY, int toX, int toY) {
    final ViewPropertyAnimator animation = createMoveAnimator(holder, fromX, fromY, toX, toY);
    moveAnimations.add(holder);
    animation
        .setDuration(getMoveDuration())
        .setListener(
            new AnimatorListenerAdapter() {
              @Override
              public void onAnimationStart(Animator animator) {
                dispatchMoveStarting(holder);
              }

              @Override
              public void onAnimationCancel(Animator animator) {
                resetAnimateMove(holder);
              }

              @Override
              public void onAnimationEnd(Animator animator) {
                animation.setListener(null);
                resetAnimateMove(holder);
                dispatchMoveFinished(holder);
                moveAnimations.remove(holder);
                dispatchFinishedWhenDone();
              }
            })
        .start();
  }

  @Override
  public boolean animateChange(
      RecyclerView.ViewHolder oldHolder,
      RecyclerView.ViewHolder newHolder,
      int fromX,
      int fromY,
      int toX,
      int toY) {
    if (oldHolder == newHolder) {
      // Don't know how to run change animations when the same view holder is re-used.
      // run a move animation to handle position changes.
      return animateMove(oldHolder, fromX, fromY, toX, toY);
    }
    final float prevTranslationX = oldHolder.itemView.getTranslationX();
    final float prevTranslationY = oldHolder.itemView.getTranslationY();
    resetAnimation(oldHolder);
    float deltaX = toX - fromX - prevTranslationX;
    float deltaY = toY - fromY - prevTranslationY;
    // recover prev translation state after ending animation
    oldHolder.itemView.setTranslationX(prevTranslationX);
    oldHolder.itemView.setTranslationY(prevTranslationY);
    prepAnimateChangeExit(oldHolder);
    if (newHolder != null) {
      // carry over translation values
      resetAnimation(newHolder);
      newHolder.itemView.setTranslationX(-deltaX);
      newHolder.itemView.setTranslationY(-deltaY);
      prepAnimateChangeEnter(newHolder);
    }
    pendingChanges.add(new ChangeInfo(oldHolder, newHolder, fromX, fromY, toX, toY));
    return true;
  }

  void animateChangeImpl(final ChangeInfo changeInfo) {
    final RecyclerView.ViewHolder oldHolder = changeInfo.oldHolder;
    final View oldView = oldHolder == null ? null : oldHolder.itemView;
    final RecyclerView.ViewHolder newHolder = changeInfo.newHolder;
    final View newView = newHolder != null ? newHolder.itemView : null;
    // check oldHolder to suppress nullable warning
    if (oldView != null && oldHolder != null) {
      final ViewPropertyAnimator oldViewAnim = createChangeExitAnimator(oldHolder);
      oldViewAnim.setDuration(getChangeDuration());
      changeAnimations.add(oldHolder);
      oldViewAnim.translationX(changeInfo.toX - changeInfo.fromX);
      oldViewAnim.translationY(changeInfo.toY - changeInfo.fromY);
      oldViewAnim
          .setListener(
              new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animator) {
                  dispatchChangeStarting(oldHolder, true);
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                  oldViewAnim.setListener(null);
                  oldView.setTranslationX(0);
                  oldView.setTranslationY(0);
                  resetAnimateChangeExit(oldHolder);
                  dispatchChangeFinished(oldHolder, true);
                  changeAnimations.remove(oldHolder);
                  dispatchFinishedWhenDone();
                }
              })
          .start();
    }
    // check newHolder to suppress nullable warning
    if (newView != null && newHolder != null) {
      final ViewPropertyAnimator newViewAnimation = createChangeEnterAnimator(newHolder);
      changeAnimations.add(newHolder);
      newViewAnimation.translationX(0).translationY(0);
      newViewAnimation.setDuration(getChangeDuration());
      newViewAnimation
          .setListener(
              new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animator) {
                  dispatchChangeStarting(newHolder, false);
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                  newViewAnimation.setListener(null);
                  resetAnimateChangeEnter(newHolder);
                  dispatchChangeFinished(newHolder, false);
                  changeAnimations.remove(newHolder);
                  dispatchFinishedWhenDone();
                }
              })
          .start();
    }
  }

  private void endChangeAnimation(List<ChangeInfo> infoList, RecyclerView.ViewHolder item) {
    for (int i = infoList.size() - 1; i >= 0; i--) {
      ChangeInfo changeInfo = infoList.get(i);
      if (endChangeAnimationIfNecessary(changeInfo, item)) {
        if (changeInfo.oldHolder == null && changeInfo.newHolder == null) {
          infoList.remove(changeInfo);
        }
      }
    }
  }

  private void endChangeAnimationIfNecessary(ChangeInfo changeInfo) {
    if (changeInfo.oldHolder != null) {
      endChangeAnimationIfNecessary(changeInfo, changeInfo.oldHolder);
    }
    if (changeInfo.newHolder != null) {
      endChangeAnimationIfNecessary(changeInfo, changeInfo.newHolder);
    }
  }

  private boolean endChangeAnimationIfNecessary(
      ChangeInfo changeInfo, RecyclerView.ViewHolder item) {
    boolean oldItem = false;
    if (changeInfo.newHolder == item) {
      changeInfo.newHolder = null;
    } else if (changeInfo.oldHolder == item) {
      changeInfo.oldHolder = null;
      oldItem = true;
    } else {
      return false;
    }
    if (oldItem) {
      resetAnimateChangeExit(item);
    } else {
      resetAnimateChangeEnter(item);
    }
    item.itemView.setTranslationX(0);
    item.itemView.setTranslationY(0);
    dispatchChangeFinished(item, oldItem);
    return true;
  }

  @Override
  public void endAnimation(RecyclerView.ViewHolder item) {
    final View view = item.itemView;
    // this will trigger end callback which should set properties to their target values.
    view.animate().cancel();
    // if some other animations are chained to end, how do we cancel them as well?
    for (int i = pendingMoves.size() - 1; i >= 0; i--) {
      MoveInfo moveInfo = pendingMoves.get(i);
      if (moveInfo.holder == item) {
        resetAnimateMove(item);
        dispatchMoveFinished(item);
        pendingMoves.remove(i);
      }
    }
    endChangeAnimation(pendingChanges, item);
    if (pendingRemovals.remove(item)) {
      resetAnimateRemove(item);
      dispatchRemoveFinished(item);
    }
    if (pendingAdditions.remove(item)) {
      resetAnimateAdd(item);
      dispatchAddFinished(item);
    }
    for (int i = changesList.size() - 1; i >= 0; i--) {
      ArrayList<ChangeInfo> changes = changesList.get(i);
      endChangeAnimation(changes, item);
      if (changes.isEmpty()) {
        changesList.remove(i);
      }
    }
    for (int i = movesList.size() - 1; i >= 0; i--) {
      ArrayList<MoveInfo> moves = movesList.get(i);
      for (int j = moves.size() - 1; j >= 0; j--) {
        MoveInfo moveInfo = moves.get(j);
        if (moveInfo.holder == item) {
          resetAnimateMove(item);
          dispatchMoveFinished(item);
          moves.remove(j);
          if (moves.isEmpty()) {
            movesList.remove(i);
          }
          break;
        }
      }
    }
    for (int i = additionsList.size() - 1; i >= 0; i--) {
      ArrayList<RecyclerView.ViewHolder> additions = additionsList.get(i);
      if (additions.remove(item)) {
        resetAnimateAdd(item);
        dispatchAddFinished(item);
        if (additions.isEmpty()) {
          additionsList.remove(i);
        }
      }
    }
    // animations should be ended by the cancel above.
    //noinspection PointlessBooleanExpression,ConstantConditions
    if (removeAnimations.remove(item) && DEBUG) {
      throw new IllegalStateException(
          "after animation is cancelled, item should not be in " + "mRemoveAnimations list");
    }
    //noinspection PointlessBooleanExpression,ConstantConditions
    if (addAnimations.remove(item) && DEBUG) {
      throw new IllegalStateException(
          "after animation is cancelled, item should not be in " + "mAddAnimations list");
    }
    //noinspection PointlessBooleanExpression,ConstantConditions
    if (changeAnimations.remove(item) && DEBUG) {
      throw new IllegalStateException(
          "after animation is cancelled, item should not be in " + "mChangeAnimations list");
    }
    //noinspection PointlessBooleanExpression,ConstantConditions
    if (moveAnimations.remove(item) && DEBUG) {
      throw new IllegalStateException(
          "after animation is cancelled, item should not be in " + "mMoveAnimations list");
    }
    dispatchFinishedWhenDone();
  }

  private void resetAnimation(RecyclerView.ViewHolder holder) {
    holder.itemView.animate().setInterpolator(defaultInterpolator);
    endAnimation(holder);
  }

  @Override
  public boolean isRunning() {
    return (!pendingAdditions.isEmpty()
        || !pendingChanges.isEmpty()
        || !pendingMoves.isEmpty()
        || !pendingRemovals.isEmpty()
        || !moveAnimations.isEmpty()
        || !removeAnimations.isEmpty()
        || !addAnimations.isEmpty()
        || !changeAnimations.isEmpty()
        || !movesList.isEmpty()
        || !additionsList.isEmpty()
        || !changesList.isEmpty());
  }

  /**
   * Check the state of currently pending and running animations. If there are none pending/running,
   * call {@link #dispatchAnimationsFinished()} to notify any listeners.
   */
  void dispatchFinishedWhenDone() {
    if (!isRunning()) {
      dispatchAnimationsFinished();
    }
  }

  @Override
  public void endAnimations() {
    int count = pendingMoves.size();
    for (int i = count - 1; i >= 0; i--) {
      MoveInfo item = pendingMoves.get(i);
      resetAnimateMove(item.holder);
      dispatchMoveFinished(item.holder);
      pendingMoves.remove(i);
    }
    count = pendingRemovals.size();
    for (int i = count - 1; i >= 0; i--) {
      RecyclerView.ViewHolder item = pendingRemovals.get(i);
      resetAnimateRemove(item);
      dispatchRemoveFinished(item);
      pendingRemovals.remove(i);
    }
    count = pendingAdditions.size();
    for (int i = count - 1; i >= 0; i--) {
      RecyclerView.ViewHolder item = pendingAdditions.get(i);
      resetAnimateAdd(item);
      dispatchAddFinished(item);
      pendingAdditions.remove(i);
    }
    count = pendingChanges.size();
    for (int i = count - 1; i >= 0; i--) {
      endChangeAnimationIfNecessary(pendingChanges.get(i));
    }
    pendingChanges.clear();
    if (!isRunning()) {
      return;
    }
    int listCount = movesList.size();
    for (int i = listCount - 1; i >= 0; i--) {
      ArrayList<MoveInfo> moves = movesList.get(i);
      count = moves.size();
      for (int j = count - 1; j >= 0; j--) {
        MoveInfo moveInfo = moves.get(j);
        RecyclerView.ViewHolder item = moveInfo.holder;
        resetAnimateMove(item);
        dispatchMoveFinished(moveInfo.holder);
        moves.remove(j);
        if (moves.isEmpty()) {
          movesList.remove(moves);
        }
      }
    }
    listCount = additionsList.size();
    for (int i = listCount - 1; i >= 0; i--) {
      ArrayList<RecyclerView.ViewHolder> additions = additionsList.get(i);
      count = additions.size();
      for (int j = count - 1; j >= 0; j--) {
        RecyclerView.ViewHolder item = additions.get(j);
        resetAnimateAdd(item);
        dispatchAddFinished(item);
        additions.remove(j);
        if (additions.isEmpty()) {
          additionsList.remove(additions);
        }
      }
    }
    listCount = changesList.size();
    for (int i = listCount - 1; i >= 0; i--) {
      ArrayList<ChangeInfo> changes = changesList.get(i);
      count = changes.size();
      for (int j = count - 1; j >= 0; j--) {
        endChangeAnimationIfNecessary(changes.get(j));
        if (changes.isEmpty()) {
          changesList.remove(changes);
        }
      }
    }
    cancelAll(removeAnimations);
    cancelAll(moveAnimations);
    cancelAll(addAnimations);
    cancelAll(changeAnimations);
    dispatchAnimationsFinished();
  }

  void cancelAll(List<RecyclerView.ViewHolder> viewHolders) {
    for (int i = viewHolders.size() - 1; i >= 0; i--) {
      viewHolders.get(i).itemView.animate().cancel();
    }
  }
  /**
   * {@inheritDoc}
   *
   * <p>If the payload list is not empty, WearableBaseItemAnimator returns <code>true</code>. When
   * this is the case:
   *
   * <ul>
   *   <li>If you override {@link #animateChange(RecyclerView.ViewHolder, RecyclerView.ViewHolder,
   *       int, int, int, int)}, both ViewHolder arguments will be the same instance.
   *   <li>If you are not overriding {@link #animateChange(RecyclerView.ViewHolder,
   *       RecyclerView.ViewHolder, int, int, int, int)}, then WearableBaseItemAnimator will call
   *       {@link #animateMove(RecyclerView.ViewHolder, int, int, int, int)} and run a move
   *       animation instead.
   * </ul>
   */
  @Override
  public boolean canReuseUpdatedViewHolder(
      RecyclerView.ViewHolder viewHolder, List<Object> payloads) {
    return !payloads.isEmpty() || super.canReuseUpdatedViewHolder(viewHolder, payloads);
  }
}
