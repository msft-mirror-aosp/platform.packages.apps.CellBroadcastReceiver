package com.google.android.clockwork.common.wearable.wearmaterial.time;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.annotation.VisibleForTesting;

/**
 * Wrapper around {@link android.content.BroadcastReceiver} to receive time tick, time change
 * events. Different from {@link android.widget.TextClock} since this only needs to support
 * granularity down to minutes rather than seconds, and no support for RemoteViews and can use
 * standard broadcast receiver register apis.
 */
class TimeBroadcastReceiver {

  private final IntentFilter filter;
  private final Listener listener;
  private final BroadcastReceiver receiver;

  private boolean registered;

  @SuppressWarnings({"nullness:method.invocation", "nullness:methodref.receiver.bound"})
  TimeBroadcastReceiver(Listener listener) {
    this.listener = listener;
    filter = new IntentFilter();
    filter.addAction(Intent.ACTION_TIME_TICK);
    filter.addAction(Intent.ACTION_TIME_CHANGED);
    filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
    receiver =
        new BroadcastReceiver() {
          @Override
          public void onReceive(Context context, Intent intent) {
            TimeBroadcastReceiver.this.onReceive(intent);
          }
        };
  }

  @VisibleForTesting
  void onReceive(Intent intent) {
    if (Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())) {
      listener.onTimeZoneChange();
    } else {
      listener.onTimeChange();
    }
  }

  void register(Context context) {
    if (!registered) {
      context.registerReceiver(receiver, filter);
      registered = true;
    }
  }

  void unregister(Context context) {
    if (registered) {
      context.unregisterReceiver(receiver);
      registered = false;
    }
  }

  interface Listener {

    /** Called when time update broadcast received. */
    void onTimeChange();

    /** Called when time zone has changed. */
    void onTimeZoneChange();
  }
}
