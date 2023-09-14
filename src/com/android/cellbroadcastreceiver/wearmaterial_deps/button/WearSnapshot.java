package com.google.android.clockwork.common.wearable.wearmaterial.button;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewOverlay;
import androidx.annotation.Keep;
import androidx.annotation.VisibleForTesting;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A snapshot of a {@link View}.
 *
 * <p>The {@code snapshot} is represented by a {@link BitmapDrawable} and the current position of
 * its {@code view}.
 *
 * <p>The class' scope must be public, so that its 'alpha' setter/getter can be accessed by {@code
 * PropertyValuesHolder} used in other packages.
 */
public final class WearSnapshot {

  static @Nullable WearSnapshot create(View view) {
    BitmapDrawable snapshot = createSnapshot(view);
    return snapshot == null ? null : new WearSnapshot(snapshot, view);
  }

  private final BitmapDrawable snapshot;
  private final View view;

  private WearSnapshot(BitmapDrawable snapshot, View view) {
    this.view = view;
    this.snapshot = snapshot;
  }

  @Keep
  public int getAlpha() {
    // The scope of getAlpha must be 'public' so that it can be accessed by the Animator
    // (PropertyValuesHolder).
    return snapshot.getAlpha();
  }

  /**
   * Used by {@link #setAlpha(int)} avoiding unnecessary object creations when that method is called
   * repeatedly and often by {@code Animator}s.
   */
  private static final Rect tmpRect = new Rect();

  @Keep
  public void setAlpha(int alpha) {
    snapshot.setAlpha(alpha);

    getUpdatedBounds(tmpRect);
    snapshot.setBounds(tmpRect);
  }

  void prepareToDraw() {
    snapshot.getBitmap().prepareToDraw();
  }

  void addToOverlay() {
    getOverlay().add(snapshot);
  }

  void removeFromOverlay() {
    getOverlay().remove(snapshot);
  }

  private ViewOverlay getOverlay() {
    // To avoid clipping to the boundaries of the 'view', the view's parent is used instead as the
    // overlay (the bounds for the 'snapshot' are also in the parent's coordinates).
    return ((View) view.getParent()).getOverlay();
  }

  /**
   * Updates the rectangle {@code rect}, whose start/top position will be equal to the ones of the
   * {@code view} and whose width will be the same as the width of {@link Drawable#getBounds()}
   * (prevents clipping).
   */
  private void getUpdatedBounds(Rect rect) {
    getViewBounds(view, rect);
    int top = rect.top;
    int left = rect.left;
    int right = rect.right;

    getSnapshotBounds(rect);
    if (view.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
      left = right - rect.width();
    }

    rect.offsetTo(left, top);
  }

  @VisibleForTesting
  void getSnapshotBounds(Rect rect) {
    snapshot.copyBounds(rect);
  }

  @VisibleForTesting
  static void getViewBounds(View view, Rect rect) {
    view.getHitRect(rect);
  }

  private static @Nullable BitmapDrawable createSnapshot(View view) {
    Rect bounds = new Rect();
    getViewBounds(view, bounds);
    if (bounds.isEmpty() || view.getVisibility() != View.VISIBLE) {
      return null;
    }

    Bitmap bitmap = Bitmap.createBitmap(bounds.width(), bounds.height(), Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    view.draw(canvas);

    BitmapDrawable drawable = new BitmapDrawable(view.getResources(), bitmap);
    drawable.setBounds(bounds);
    return drawable;
  }
}
