package com.google.android.clockwork.common.wearable.wearmaterial.picker;

import static java.lang.Math.round;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnGenericMotionListener;
import android.view.ViewConfiguration;
import androidx.annotation.VisibleForTesting;
import androidx.arch.core.util.Function;
import androidx.core.util.Consumer;
import androidx.core.view.ViewConfigurationCompat;
import com.google.android.clockwork.common.wearable.wearmaterial.rotaryinput.RotaryInputHapticsHelper;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Listens to scroll events from the rotary/crown and scrolls the currently activated {@code
 * WearPickerColumn} accordingly.
 *
 * <p>Note that this class can only be a generic motion listener for a {@link RecyclerView}. If it
 * is set as a generic motion listener for another type of view, this class will crash.
 */
final class WearPickerRotaryListener implements OnGenericMotionListener {

  private static final long SCROLL_SNAP_DELAY_MS = 80;

  private static final Function<RecyclerView, RotaryInputHapticsHelper>
      DEFAULT_HAPTICS_HELPER_FACTORY =
          view -> {
            RotaryInputHapticsHelper helper = new RotaryInputHapticsHelper(view.getContext(), view);
            helper.setFixedIntervalHapticsEnabled(false);
            helper.setEndOfListHapticsEnabled(false);
            return helper;
          };

  private Function<RecyclerView, RotaryInputHapticsHelper> hapticsHelperFactory =
      DEFAULT_HAPTICS_HELPER_FACTORY;

  @SuppressWarnings({"nullness:method.invocation", "nullness:methodref.receiver.bound"})
  private final Runnable snapScroll = this::snapScroll;

  private final Consumer<RotaryInputHapticsHelper> hapticsConsumer;

  private @Nullable RotaryInputHapticsHelper hapticsHelper;

  private OnGenericMotionListener rotaryInputSubscriber;

  private RecyclerView recyclerView;

  /**
   * Constructs a new {@link WearPickerRotaryListener} with a {@code hapticsConsumer} that gets
   * called when the user starts and ends scrolling using the rotary.
   *
   * <p>When the user starts scrolling the {@code hapticsConsumer} gets called with a non-null
   * instance of a {@link RotaryInputHapticsHelper}. When the user ends scrolling, the {@code
   * hapticsConsumer} gets called with a {@code null} value.
   */
  WearPickerRotaryListener(Consumer<RotaryInputHapticsHelper> hapticsConsumer) {
    this.hapticsConsumer = hapticsConsumer;
  }

  /**
   * Examines the generic motion {@code event} and scrolls the currently activated {@code
   * WearPickerColumn} if this event originates from turning the watch's crown/rotary.
   *
   * <p>If the {@code event} is any other type of event, it is ignored.
   *
   * @return {@code true} if this event was from a crown/rotary scroll, {@code false} otherwise.
   */
  @SuppressWarnings("nullness:argument")
  @Override
  public boolean onGenericMotion(View view, MotionEvent event) {
    if (event.getAction() != MotionEvent.ACTION_SCROLL
        || !event.isFromSource(InputDevice.SOURCE_ROTARY_ENCODER)) {
      return false;
    }

    if (!view.isActivated()) {
      return false;
    }

    // Should crash if this is not a RecyclerView
    RecyclerView recyclerView = (RecyclerView) view;
    this.recyclerView = recyclerView;

    if (hapticsHelper == null) {
      hapticsHelper = hapticsHelperFactory.apply(recyclerView);
      hapticsConsumer.accept(hapticsHelper);
    }

    if (rotaryInputSubscriber != null) {
      rotaryInputSubscriber.onGenericMotion(view, event);
    }

    view.removeCallbacks(snapScroll);
    view.postDelayed(snapScroll, SCROLL_SNAP_DELAY_MS);

    int scrollDistance = getScrollDistance(view.getContext(), event);
    if (scrollDistance != 0) {
      view.scrollBy(0, scrollDistance);
    }

    return true;
  }

  @SuppressWarnings("InlinedApi") // AXIS_SCROLL is inlined and exists on API 23
  private int getScrollDistance(Context context, MotionEvent ev) {
    float axisValue = ev.getAxisValue(MotionEvent.AXIS_SCROLL);
    float scrollFactor =
        ViewConfigurationCompat.getScaledVerticalScrollFactor(
            ViewConfiguration.get(context), context);
    return round(-axisValue * scrollFactor);
  }

  /** Snaps closest list item view into place. */
  private void snapScroll() {
    recyclerView.smoothScrollBy(0, 1);
  }

  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  void setHapticsHelperFactory(@Nullable Function<RecyclerView, RotaryInputHapticsHelper> factory) {
    hapticsHelperFactory = factory == null ? DEFAULT_HAPTICS_HELPER_FACTORY : factory;
  }

  void setRotaryInputSubscriber(OnGenericMotionListener listener) {
    this.rotaryInputSubscriber = listener;
  }
}
