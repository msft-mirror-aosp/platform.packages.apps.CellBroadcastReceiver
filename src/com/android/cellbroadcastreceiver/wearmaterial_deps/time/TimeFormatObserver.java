package com.google.android.clockwork.common.wearable.wearmaterial.time;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.provider.Settings.System;

/**
 * {@link ContentResolver} to observe preference on whether time should be shown in 12 hour or 24
 * hour format.
 */
class TimeFormatObserver {

  private final ContentObserver observer;

  private boolean registered;

  TimeFormatObserver(Handler handler, Listener listener) {
    this.observer =
        new ContentObserver(handler) {
          @Override
          public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            listener.onTimeFormatChange();
          }
        };
  }

  void register(Context context) {
    if (!registered) {
      Uri uri = Settings.System.getUriFor(System.TIME_12_24);
      context.getContentResolver().registerContentObserver(uri, true, observer);
      registered = true;
    }
  }

  void unregister(Context context) {
    if (registered) {
      context.getContentResolver().unregisterContentObserver(observer);
      registered = false;
    }
  }

  interface Listener {

    /** Callback to refresh time format. */
    void onTimeFormatChange();
  }
}
