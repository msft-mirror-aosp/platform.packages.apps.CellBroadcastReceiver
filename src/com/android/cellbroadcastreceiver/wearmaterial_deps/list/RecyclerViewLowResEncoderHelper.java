package com.google.android.clockwork.common.wearable.wearmaterial.list;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.LayoutManager;
import androidx.recyclerview.widget.RecyclerView.OnScrollListener;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View.OnGenericMotionListener;
import android.view.ViewConfiguration;
import androidx.core.view.ViewConfigurationCompat;

/**
 * Helper to make a {@link RecyclerView} smoothly scroll when used with a low-resolution RSB.
 *
 * <p>This can be used in two modes. For many cases, you can simply call {@link
 * #injectSmoothScroller(RecyclerView)} to add this functionality into a standard RecyclerView. This
 * will add a {@link OnScrollListener}, and override the RecyclerView's {@link
 * OnGenericMotionListener} in order to properly respond to scroll events. Note though that this
 * will override any other generic motion listeners you add using {@link
 * RecyclerView#onGenericMotionEvent(MotionEvent)}. This can be used like in the following example:
 *
 * <pre>{@code
 * RecyclerView rv = findViewById(R.id.my_recycler);
 * RecyclerViewLowResEncoderHelper helper = new RecyclerViewLowResEncoderHelper(context);
 * helper.injectSmoothScroller(rv);
 * }</pre>
 *
 * <p>For more advanced use cases (e.g. if you clear scroll listeners using {@link
 * RecyclerView#clearOnScrollListeners()}, or you add your own generic motion listener), you can
 * forward events to this class instead. This would take on the following form:
 *
 * <pre>{@code
 * RecyclerView rv = findViewById(R.id.my_recycler);
 * RecyclerViewLowResEncoderHelper helper = new RecyclerViewLowResEncoderHelper(context);
 *
 * rv.setOnGenericMotionListener((view, event) -> {
 *   // Try dispatching via the helper first.
 *   if (helper.getGenericMotionListener().onGenericMotion(view, event)) {
 *     return true;
 *   }
 *
 *   // Handle accordingly.
 * });
 * rv.addOnScrollListener(helper.getScrollListener());
 *
 * }</pre>
 */
public class RecyclerViewLowResEncoderHelper {
  private static final String LOW_RES_ROTARY_ENCODER_FEATURE =
      "android.hardware.rotaryencoder.lowres";

  private final Context context;

  private final float scaledHorizontalScrollFactor;
  private final float scaledVerticalScrollFactor;

  private final boolean hasLowResRsb;

  // These vars are used to implement smooth scrolling. The generic motion listener below will call
  // smoothScrollBy() if a low-res RSB is in use, and we receive a scroll event. Problem is,
  // smoothScrollBy() scrolls from the current position, so we get some strange scroll behaviour if
  // the user manages to fire another scroll event before the previous smooth scroll has completed.
  // Instead, this allows for scroll accumulation; this is used to track the remaining scroll so
  // we can add that on for every scroll event. This also listens for the scroll state; if the state
  // is not SETTLING (i.e. it's IDLE at the end of a scroll, or DRAGGING because the user touched
  // the screen), then we cancel any remaining scroll.
  //
  // Ideally, these would be inlined into RecyclerView (where we can just access the scroll settling
  // parameters), but that will be difficult to do in the short term.
  private int scrollRemaining = 0;

  private final OnScrollListener scrollListener =
      new OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
          if (newState != RecyclerView.SCROLL_STATE_SETTLING) {
            scrollRemaining = 0;
          }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
          if (!hasLowResRsb) {
            // Don't need to do anything unless we're on a device with a low-res RSB.
            return;
          }

          if (scrollRemaining == 0) {
            return;
          }

          int oldScrollRemaining = scrollRemaining;

          LayoutManager layoutManager = recyclerView.getLayoutManager();
          if (layoutManager == null) {
            return;
          }

          if (layoutManager.canScrollVertically()) {
            scrollRemaining -= dy;
          } else {
            scrollRemaining -= dx;
          }

          if (oldScrollRemaining < 0 && scrollRemaining > 0) {
            scrollRemaining = 0;
          } else if (oldScrollRemaining > 0 && scrollRemaining < 0) {
            scrollRemaining = 0;
          }
        }
      };

  private final OnGenericMotionListener motionListener;

  public RecyclerViewLowResEncoderHelper(Context context) {
    this.context = context;
    this.hasLowResRsb =
        context.getPackageManager().hasSystemFeature(LOW_RES_ROTARY_ENCODER_FEATURE);

    this.scaledHorizontalScrollFactor =
        ViewConfigurationCompat.getScaledHorizontalScrollFactor(
            ViewConfiguration.get(context), context);
    this.scaledVerticalScrollFactor =
        ViewConfigurationCompat.getScaledVerticalScrollFactor(
            ViewConfiguration.get(context), context);

    this.motionListener =
        (v, event) -> {
          if (!hasLowResRsb) {
            // Don't intercept anything unless where's a low-res RSB on the device.
            return false;
          }

          RecyclerView rv = (RecyclerView) v;
          if (event.getAction() == MotionEvent.ACTION_SCROLL
              && event.getSource() == InputDevice.SOURCE_ROTARY_ENCODER) {
            float axisScroll = event.getAxisValue(MotionEvent.AXIS_SCROLL);

            LayoutManager layoutManager = rv.getLayoutManager();

            if (layoutManager == null) {
              return false;
            }

            if (layoutManager.canScrollVertically()) {
              scrollRemaining =
                  (int) ((-axisScroll * scaledVerticalScrollFactor) + scrollRemaining);
              rv.smoothScrollBy(0, scrollRemaining);
              return true;
            } else if (layoutManager.canScrollHorizontally()) {
              scrollRemaining =
                  (int) ((-axisScroll * scaledHorizontalScrollFactor) + scrollRemaining);
              rv.smoothScrollBy(scrollRemaining, 0);
              return true;
            }
          }

          return false;
        };
  }

  /**
   * Inject a {@link OnGenericMotionListener} and a {@link OnScrollListener} into the given {@link
   * RecyclerView} to cause it to do smooth scrolling in the presence of a low-res RSB.
   *
   * <p>Note that this is a no-op if the device does not have a low-resolution RSB, so it is safe to
   * call this method on all devices.
   */
  public void injectSmoothScroller(RecyclerView recyclerView) {
    if (!context.getPackageManager().hasSystemFeature(LOW_RES_ROTARY_ENCODER_FEATURE)) {
      return;
    }

    recyclerView.addOnScrollListener(scrollListener);
    recyclerView.setOnGenericMotionListener(motionListener);
  }

  /**
   * Gets a motion event listener, which will translate scroll events into calls to {@link
   * RecyclerView#smoothScrollBy(int, int)} if the device has a low-res RSB. Note that the returned
   * {@link OnGenericMotionListener} will ignore any input (and just return {@code false}) if the
   * device does not have a low-res RSB, so it is safe to pass events into the listener on all
   * devices.
   */
  public OnGenericMotionListener getGenericMotionListener() {
    return motionListener;
  }

  /**
   * Gets an {@link OnScrollListener} which should be injected into the RecyclerView when using
   * smooth scrolling. This is responsible for tracking the current accumulated scroll amount, which
   * ensures that the View scrolls properly in response to many rapid RSB events, and so must be
   * injected into the RecyclerView if you are passing events to the GenericMotionListener above.
   */
  public OnScrollListener getScrollListener() {
    return scrollListener;
  }
}
