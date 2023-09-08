package com.google.android.clockwork.common.wearable.wearmaterial.time;

import static com.google.android.clockwork.common.wearable.wearmaterial.util.MathUtils.clamp;
import static com.google.android.clockwork.common.wearable.wearmaterial.util.MathUtils.lerp;
import static java.lang.Math.min;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.annotation.VisibleForTesting;
import androidx.wear.widget.CurvedTextView;
import com.google.android.clockwork.common.wearable.wearmaterial.time.TimeBroadcastReceiver.Listener;
import com.google.android.clockwork.common.wearable.wearmaterial.util.TextViewWrapper;
import java.util.Calendar;
import java.util.Locale;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Layout to show title and current time, supported for both round and rectangular screens. */
public class WearTimeText extends FrameLayout {

  /** A Clock interface that retrieves the current time in milliseconds since 1970 midnight. */
  public interface Clock {

    /** Returns the number of milliseconds elapsed since 1970 midnight. */
    long getCurrentTimeMs();
  }

  private static final String DIVIDER = "·";
  private static final String SKELETON_12_HR = "hm";
  private static final String SKELETON_24_HR = "Hm";
  private static final float MIN_FADE_OUT = 1f;
  private static final float MAX_FADE_OUT = 0.5f;

  @VisibleForTesting TimeBroadcastReceiver timeBroadcastReceiver;
  @VisibleForTesting TimeBroadcastReceiver.Listener timeBroadcastListener;
  @VisibleForTesting @Nullable TimeFormatObserver timeFormatObserver;
  @VisibleForTesting TimeFormatObserver.Listener timeFormatListener;

  private final TextViewWrapper dividerText;
  private final TextViewWrapper timeText;
  private final TextViewWrapper titleText;
  private final float maxFadeOutScroll;

  private Clock clock;
  private Calendar time;
  private boolean use24HourFormat;

  public WearTimeText(Context context) {
    this(context, null);
  }

  public WearTimeText(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  // Suppress warnings for uninitialized warnings inside receiver listener callbacks.  Receiver
  // listeners are not triggered until later on when they are registered upon attachment to window.
  @SuppressWarnings({
    "nullness:argument",
    "nullness:method.invocation",
    "nullness:methodref.receiver.bound"
  })
  public WearTimeText(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    boolean isScreenRound = context.getResources().getConfiguration().isScreenRound();
    if (isScreenRound) {
      LayoutInflater.from(context).inflate(R.layout.curved_time_text, this, true);
      timeText = TextViewWrapper.wrap((CurvedTextView) findViewById(R.id.wear_time_text_clock));
      dividerText =
          TextViewWrapper.wrap((CurvedTextView) findViewById(R.id.wear_time_text_divider));
      titleText = TextViewWrapper.wrap((CurvedTextView) findViewById(R.id.wear_time_text_title));
    } else {
      LayoutInflater.from(context).inflate(R.layout.straight_time_text, this, true);
      timeText = TextViewWrapper.wrap((TextView) findViewById(R.id.wear_time_text_clock));
      dividerText = TextViewWrapper.wrap((TextView) findViewById(R.id.wear_time_text_divider));
      titleText = TextViewWrapper.wrap((TextView) findViewById(R.id.wear_time_text_title));
    }
    dividerText.setText(DIVIDER);
    maxFadeOutScroll = getResources().getDimensionPixelSize(R.dimen.wear_time_max_fade_out_scroll);
    applyAttributes(context, attrs, defStyleAttr, defStyleAttr);

    time = Calendar.getInstance();
    timeBroadcastListener =
        new Listener() {
          @Override
          public void onTimeChange() {
            updateTime();
          }

          @Override
          public void onTimeZoneChange() {
            updateTimeZone();
          }
        };
    timeBroadcastReceiver = new TimeBroadcastReceiver(timeBroadcastListener);
    timeFormatListener =
        () -> {
          updateFormat();
          updateTime();
        };
  }

  private void applyAttributes(
      final Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    TypedArray a =
        context.obtainStyledAttributes(attrs, R.styleable.WearTimeText, defStyleAttr, defStyleRes);
    setTitleTextColor(a.getColor(R.styleable.WearTimeText_android_titleTextColor, Color.WHITE));
    setTitle(a.getString(R.styleable.WearTimeText_titleText));
    a.recycle();
  }

  @Override
  public void onAttachedToWindow() {
    super.onAttachedToWindow();

    updateFormat();
    updateTime();
    timeBroadcastReceiver.register(getContext());
    // getHandler() needs to be called after it has been attached to the UI.
    timeFormatObserver = new TimeFormatObserver(getHandler(), timeFormatListener);
    timeFormatObserver.register(getContext());
  }

  @Override
  public void onDetachedFromWindow() {
    super.onDetachedFromWindow();

    timeBroadcastReceiver.unregister(getContext());
    if (timeFormatObserver != null) {
      timeFormatObserver.unregister(getContext());
      timeFormatObserver = null;
    }
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    // set scroll animation pivot to center bottom of text
    setPivotY(getPaddingTop() + getResources().getDimensionPixelSize(R.dimen.wear_time_text_size));
    setPivotX(getMeasuredWidth() / 2f);
  }

  public @Nullable CharSequence getTitle() {
    return titleText.getText();
  }

  public void setTitle(@Nullable CharSequence title) {
    boolean showTitle = TextUtils.isEmpty(title);
    titleText.setText(title);
    titleText.getView().setVisibility(showTitle ? GONE : VISIBLE);
    dividerText.getView().setVisibility(showTitle ? GONE : VISIBLE);
  }

  @ColorInt
  public int getTitleTextColor() {
    return titleText.getTextColor();
  }

  public void setTitleTextColor(@ColorInt int color) {
    titleText.setTextColor(color);
    dividerText.setTextColor(color);
  }

  public void setClock(Clock clock) {
    this.clock = clock;
    updateTime();
  }

  public void setClock(com.google.android.clockwork.common.time.Clock clock) {
    setClock(clock::getCurrentTimeMs);
  }

  /**
   * Apply scroll position from an external scroll view to fade out the time widget, as though it is
   * tracking the scroll view.
   *
   * @param y the vertical scroll position, where positive values mean the top of view has been
   *     scrolled past the top edge of screen.
   */
  public void applyScrollPositionFade(float y) {
    float fadePercent = clamp(y / maxFadeOutScroll, 0f, 1f);
    float fadeOut = lerp(MIN_FADE_OUT, MAX_FADE_OUT, fadePercent);
    setAlpha(fadeOut);
    setScaleX(fadeOut);
    setScaleY(fadeOut);
    setTranslationY(min(0f, -y));
  }

  private void updateFormat() {
    use24HourFormat = DateFormat.is24HourFormat(getContext());
  }

  private void updateTimeZone() {
    // refresh for new default time zone.
    time = Calendar.getInstance();
    updateTime();
  }

  private void updateTime() {
    if (clock == null) {
      return;
    }

    String pattern =
        DateFormat.getBestDateTimePattern(
            Locale.getDefault(), use24HourFormat ? SKELETON_24_HR : SKELETON_12_HR);

    // getBestDateTimePattern always creates pattern with am/pm regardless of whether skeleton
    // contains am/pm.  Stripping the 'a' (am/pm) seems to be a locale safe solution as it is done
    // in ComplicationTextUtils#shortTextTimeFormat and also done in TimeOnlyModeWatchFace.
    time.setTimeInMillis(clock.getCurrentTimeMs());
    String patternWithoutAmPm = pattern.replace("a", "").trim();
    String formattedTime = DateFormat.format(patternWithoutAmPm, time).toString();
    timeText.setText(formattedTime);
  }

  @VisibleForTesting
  boolean isUse24HourFormat() {
    return use24HourFormat;
  }
}
