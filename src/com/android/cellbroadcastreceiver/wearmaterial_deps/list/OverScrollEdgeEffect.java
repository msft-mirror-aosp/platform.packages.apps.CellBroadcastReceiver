package com.google.android.clockwork.common.wearable.wearmaterial.list;

import static java.lang.Math.max;
import static java.lang.Math.min;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.EdgeEffect;
import androidx.annotation.VisibleForTesting;
import com.google.android.clockwork.common.wearable.wearmaterial.util.MathUtils;

/**
 * A simplified version of RoundScrollbarRenderer to add squeeze effect on scrollbar when scrolling.
 * See
 * https://source.corp.google.com/rvc-wear-imr2-dev/frameworks/base/core/java/android/view/RoundScrollbarRenderer.java
 * which is partially copied from EdgeEffect:
 * https://source.corp.google.com/rvc-wear-imr2-dev/frameworks/base/core/java/android/widget/EdgeEffect.java
 */
class OverScrollEdgeEffect extends EdgeEffect {

  interface OverScrollListener {
    void onSqueezeScaleYChanged(float squeezeScaleY);
  }

  // Glow calculations are necessary for parity with EdgeEffect.java
  private static final float MAX_ALPHA = 0.15f;

  private static final float GLOW_ALPHA_START = .09f;
  private static final float PULL_DISTANCE_ALPHA_GLOW_FACTOR = 0.8f;
  private static final int VELOCITY_GLOW_FACTOR = 6;

  private static final int STATE_IDLE = 0;
  private static final int STATE_PULL = 1;
  private static final int STATE_ABSORB = 2;
  private static final int STATE_RECEDE = 3;
  private static final int STATE_PULL_DECAY = 4;

  // Time it will take the squeeze effect to fully recede in ms
  private static final int RECEDE_TIME = 600;
  // Time it will take before a pulled squeeze begins receding in ms
  private static final int PULL_TIME = 167;
  // Time it will take in ms for a pulled squeeze to decay to partial strength before release
  private static final int PULL_DECAY_TIME = 2000;
  // Minimum velocity that will be absorbed for squeeze
  private static final int MIN_VELOCITY = 100;
  // Maximum velocity, clamps at this value
  private static final int MAX_VELOCITY = 10000;

  private static final float EPSILON = 0.001f;

  private final Interpolator interpolator = new DecelerateInterpolator();
  private float pullDistance;
  private final Rect bounds = new Rect();
  @VisibleForTesting float squeezeScaleY;
  private float squeezeScaleYStart;
  private float squeezeScaleYFinish;
  private float glowAlpha;
  private float glowAlphaStart;
  private float glowAlphaFinish;

  private int state = STATE_IDLE;

  private long startTime;
  private float duration;
  private final OverScrollListener listener;

  OverScrollEdgeEffect(Context context, OverScrollListener listener) {
    super(context);
    this.listener = listener;
  }

  @Override
  public void onPull(float deltaDistance) {
    super.onPull(deltaDistance);
    onPull(deltaDistance, 0.5f);
  }

  @Override
  public void onPull(float deltaDistance, float displacement) {
    super.onPull(deltaDistance, displacement);
    final long now = AnimationUtils.currentAnimationTimeMillis();
    if (state == STATE_PULL_DECAY && now - startTime < duration) {
      return;
    }
    if (state != STATE_PULL) {
      squeezeScaleY = max(0f, squeezeScaleY);
    }
    state = STATE_PULL;

    startTime = now;
    duration = PULL_TIME;
    pullDistance += deltaDistance;

    final float absDeltaDistance = Math.abs(deltaDistance);
    glowAlpha = min(MAX_ALPHA, glowAlpha + (absDeltaDistance * PULL_DISTANCE_ALPHA_GLOW_FACTOR));
    glowAlphaStart = glowAlpha;

    if (pullDistance == 0) {
      squeezeScaleY = 0;
      squeezeScaleYStart = 0;
    } else {
      float pullScale = Math.abs(pullDistance) * bounds.height() * glowAlpha;
      // Equation designed to match the one in EdgeEffect
      final float scale = (float) (max(0, 0.7d - 1 / Math.sqrt(pullScale)) / 0.7d);
      squeezeScaleY = scale;
      squeezeScaleYStart = squeezeScaleY;
    }
    listener.onSqueezeScaleYChanged(squeezeScaleY);

    glowAlphaFinish = glowAlpha;
    squeezeScaleYFinish = squeezeScaleY;
  }

  @Override
  public void onRelease() {
    super.onRelease();

    if (state != STATE_PULL && state != STATE_PULL_DECAY) {
      return;
    }
    pullDistance = 0;

    state = STATE_RECEDE;
    glowAlphaStart = glowAlpha;
    squeezeScaleYStart = squeezeScaleY;

    glowAlphaFinish = 0.f;
    squeezeScaleYFinish = 0.f;

    startTime = AnimationUtils.currentAnimationTimeMillis();
    duration = RECEDE_TIME;
  }

  @Override
  public void onAbsorb(int velocity) {
    super.onAbsorb(velocity);
    state = STATE_ABSORB;
    velocity = MathUtils.clamp(Math.abs(velocity), MIN_VELOCITY, MAX_VELOCITY);

    startTime = AnimationUtils.currentAnimationTimeMillis();
    duration = 0.15f + (velocity * 0.02f);

    // The glow depends more on the velocity, and therefore starts out
    // nearly invisible.
    glowAlphaStart = GLOW_ALPHA_START;
    squeezeScaleYStart = max(squeezeScaleY, 0.f);

    // Growth for the size of the squeeze should be quadratic to properly
    // respond to a user's scrolling speed. The faster the scrolling speed, the more
    // intense the effect should be for both the size and the saturation.
    squeezeScaleYFinish = min(0.025f + (velocity * (velocity / 100f) * 0.00015f) / 2, 1.f);
    // Alpha should change for the glow as well as size.
    glowAlphaFinish =
        max(glowAlphaStart, min(velocity * VELOCITY_GLOW_FACTOR * .00001f, MAX_ALPHA));
  }

  @Override
  public boolean isFinished() {
    return state == STATE_IDLE;
  }

  void update() {
    final long time = AnimationUtils.currentAnimationTimeMillis();
    final float progress = min((time - startTime) / duration, 1.f);
    final float interp = interpolator.getInterpolation(progress);

    glowAlpha = glowAlphaStart + (glowAlphaFinish - glowAlphaStart) * interp;
    squeezeScaleY = squeezeScaleYStart + (squeezeScaleYFinish - squeezeScaleYStart) * interp;
    listener.onSqueezeScaleYChanged(squeezeScaleY);

    if ((time - startTime) >= duration * (1.f - EPSILON)) {
      switch (state) {
        case STATE_ABSORB:
          state = STATE_RECEDE;
          startTime = time;
          duration = RECEDE_TIME;

          glowAlphaStart = glowAlpha;
          squeezeScaleYStart = squeezeScaleY;

          // After absorb, the squeeze should fade to nothing.
          glowAlphaFinish = 0.f;
          squeezeScaleYFinish = 0.f;
          break;
        case STATE_PULL:
          state = STATE_PULL_DECAY;
          startTime = time;
          duration = PULL_DECAY_TIME;

          glowAlphaStart = glowAlpha;
          squeezeScaleYStart = squeezeScaleY;

          // After pull, the squeeze should fade to nothing.
          glowAlphaFinish = 0.f;
          squeezeScaleYFinish = 0.f;
          break;
        case STATE_PULL_DECAY:
          state = STATE_RECEDE;
          break;
        case STATE_RECEDE:
          state = STATE_IDLE;
          break;
        default: // fall out
      }
    }
  }

  @Override
  public boolean draw(Canvas canvas) {
    update();
    return super.draw(canvas);
  }

  @Override
  public void setSize(int width, int height) {
    super.setSize(width, height);
    bounds.set(bounds.left, bounds.top, width, height);
  }
}
