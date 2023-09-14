package com.google.android.clockwork.common.wearable.wearmaterial.picker;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controller to implement the {@link WearPicker} into a Wear Time Picker. It controls the process
 * of configuring the component and setting the click listeners afterwards.
 */
public final class WearTimePickerController {

  private static final int TIME_PICKER_HOUR_COLUMN_INDEX = 0;
  private static final int TIME_PICKER_MINUTE_COLUMN_INDEX = 1;
  private static final int TIME_PICKER_SECOND_COLUMN_INDEX = 2;
  private static final int HOURS_12 = 12;
  private static final int COLUMN_24_HOURS_MIN_VALUE = 0;
  private static final int COLUMN_24_HOURS_MAX_VALUE = 23;
  private static final int COLUMN_12_HOURS_MIN_VALUE = 0;
  private static final int COLUMN_12_HOURS_MAX_VALUE = 11;
  private static final int COLUMN_MINUTES_MIN_VALUE = 0;
  private static final int COLUMN_MINUTES_MAX_VALUE = 59;
  private static final int COLUMN_SECONDS_MIN_VALUE = 0;
  private static final int COLUMN_SECONDS_MAX_VALUE = 59;
  private static final String DOUBLE_DECIMAL_FORMAT_SPECIFIER = "%02d";

  /** Listener interface to be implemented by the view that is hosting the Picker Row. */
  public interface OnTimeSelectedListener {

    /**
     * Called when the PickerRow Action Button is clicked. It returns the time that is displayed in
     * the Picker Row by column
     *
     * @param hour The hour value returned by the Picker Row. Using 24hrs format (0-23)
     * @param minute The minute value returned by the Picker Row. Using standard format (0-59)
     * @param seconds The second value returned by the Picker Row. If the second's column is enabled
     *     in the picker row the return is between 0-59, else it returns 0.
     */
    void onTimeSelected(int hour, int minute, int seconds);
  }

  /**
   * Create Method that initialize the WearTimePickerController and populates its corresponding
   * PickerRow object.
   *
   * @deprecated Use the {@link Builder} class instead to create a {@link WearTimePickerController}.
   * @param pickerRow Initialized PickerRow object from parent activity.
   * @param is24Hours Boolean describing the state of the Hour setting in device.
   */
  
  @Deprecated
  public static WearTimePickerController create(
      WearPicker pickerRow, boolean is24Hours, boolean includeSeconds) {
    return new Builder(pickerRow)
        .setIs24Hours(is24Hours)
        .setIncludeSeconds(includeSeconds)
        .create();
  }

  /** Builder that can be use to construct a {@link WearTimePickerController}. */
  public static class Builder {
    private final WearPicker wearPicker;

    private boolean is24Hours;
    private int amStringId = 0;
    private int pmStringId = 0;

    private boolean includeSeconds = false;

    @StringRes private int hoursStringId = R.string.wear_picker_hour_column_header_text;
    @StringRes private int minutesStringId = R.string.wear_picker_minute_column_header_text;
    @StringRes private int secondsStringId = R.string.wear_picker_second_column_header_text;
    @StringRes private int hoursDescStringId = R.string.wear_picker_hour_column_desc_text;
    @StringRes private int minutesDescStringId = R.string.wear_picker_minute_column_desc_text;
    @StringRes private int secondsDescStringId = R.string.wear_picker_second_column_desc_text;
    @StringRes private int separatorStringId = R.string.wear_picker_separator_string;

    @StringRes
    private final int itemViewDescriptionStringId =
        R.string.wear_picker_picker_column_item_description;

    /**
     * Creates a {@link Builder} to create a {@link WearTimePickerController} for the given {@code
     * wearPicker}.
     */
    public Builder(WearPicker wearPicker) {
      this.wearPicker = wearPicker;
      this.is24Hours = DateFormat.is24HourFormat(wearPicker.getContext());
    }

    /**
     * Sets whether the picker needs to use 24 hours (0..23) format or 12 (1..12) hours format. The
     * default value for this property is determined by the date/time settings of the device.
     */
    @CanIgnoreReturnValue
    public Builder setIs24Hours(boolean is24Hours) {
      this.is24Hours = is24Hours;
      return this;
    }

    /**
     * Provides the string-resource-ids for the AM and PM strings (for 12 hours format only). The
     * default values for the AM and PM strings are determined by the locale of the device.
     */
    @CanIgnoreReturnValue
    public Builder setAmPmStringIds(@StringRes int amStringId, @StringRes int pmStringId) {
      this.amStringId = amStringId;
      this.pmStringId = pmStringId;
      return this;
    }

    /**
     * Set whether the picker needs to include a seconds-column or not. The default value of this
     * property is {@code false}.
     */
    @CanIgnoreReturnValue
    public Builder setIncludeSeconds(boolean includeSeconds) {
      this.includeSeconds = includeSeconds;
      return this;
    }

    /**
     * Provides the string-resource-id for the "Hours" column header. The default value of this
     * property is "Hours".
     */
    @CanIgnoreReturnValue
    public Builder setHoursStringId(@StringRes int hoursStringId) {
      this.hoursStringId = hoursStringId;
      return this;
    }

    /**
     * Provides the string-resource-id for the "Minutes" column header. The default value of this
     * property is "Minutes".
     */
    @CanIgnoreReturnValue
    public Builder setMinutesStringId(@StringRes int minutesStringId) {
      this.minutesStringId = minutesStringId;
      return this;
    }

    /**
     * Provides the string-resource-id for the "Seconds" column header, if seconds are included. The
     * default value of this property is "Seconds".
     */
    @CanIgnoreReturnValue
    public Builder setSecondsStringId(@StringRes int secondsStringId) {
      this.secondsStringId = secondsStringId;
      return this;
    }

    /**
     * Provides the string-resource-id for the "Hours" column content description. The default value
     * of this property is "Hour".
     */
    @CanIgnoreReturnValue
    public Builder setHoursDescStringId(@StringRes int hoursDescStringId) {
      this.hoursDescStringId = hoursDescStringId;
      return this;
    }

    /**
     * Provides the string-resource-id for the "Minutes" column content description. The default
     * value of this property is "Minute".
     */
    @CanIgnoreReturnValue
    public Builder setMinutesDescStringId(@StringRes int minutesDescStringId) {
      this.minutesDescStringId = minutesDescStringId;
      return this;
    }

    /**
     * Provides the string-resource-id for the "Seconds" column content description, if seconds are
     * included. The default value of this property is "Second".
     */
    @CanIgnoreReturnValue
    public Builder setSecondsContentDescriptionStringId(@StringRes int secondsContentDescStringId) {
      this.secondsDescStringId = secondsContentDescStringId;
      return this;
    }

    /**
     * Provides the string-resource-id for the separator string of the columns. The default value of
     * this property is ":".
     */
    @CanIgnoreReturnValue
    public Builder setSeparatorStringId(@StringRes int separatorStringId) {
      this.separatorStringId = separatorStringId;
      return this;
    }

    /** Configures the {@code wearPicker} and returns the {@link WearTimePickerController}. */
    public WearTimePickerController create() {
      WearTimePickerController controller = new WearTimePickerController(wearPicker);
      controller.initialize(this);
      return controller;
    }
  }

  private OnTimeSelectedListener listener;
  private final WearPicker timePicker;
  private final Context context;
  private final Locale locale;

  private boolean is24Hours;
  private boolean includeSeconds;

  private WearTimePickerController(WearPicker pickerRow) {
    this.timePicker = pickerRow;
    this.context = timePicker.getContext();
    this.locale = this.context.getResources().getConfiguration().locale;
  }

  private void initialize(Builder builder) {
    is24Hours = builder.is24Hours;
    includeSeconds = builder.includeSeconds;

    timePicker.setColumnAdapters(getTimeAdapters(builder));
    if (!builder.is24Hours) {
      timePicker.setToggleTexts(getAmPmToggleTexts(builder));
    }

    timePicker.setSeparatorString(context.getString(builder.separatorStringId));

    timePicker.setOnActionButtonClickListener(
        view -> {
          if (listener != null) {
            int hours = timePicker.getPickerColumnItem(TIME_PICKER_HOUR_COLUMN_INDEX);
            int minutes = timePicker.getPickerColumnItem(TIME_PICKER_MINUTE_COLUMN_INDEX);
            int seconds = 0;

            if (builder.includeSeconds) {
              seconds = timePicker.getPickerColumnItem(TIME_PICKER_SECOND_COLUMN_INDEX);
            }

            if (!builder.is24Hours && timePicker.getToggleTextIndex() == 1) {
              hours += HOURS_12;
            }
            listener.onTimeSelected(hours, minutes, seconds);
          }
        });
  }

  /** Sets a Listener that returns the time values of the WearTime Picker to the Parent Activity */
  public void setOnTimeSelectedListener(OnTimeSelectedListener listener) {
    this.listener = listener;
  }

  /**
   * Sets the initial time to be shown in the {@link WearPicker}'s Columns to the desired values.
   *
   * @param hours Hour value to be set on the hourColumn in the wearPicker. The expected format is
   *     always 24 hours (0-23).
   * @param minutes Minute value to be set on the minuteColumn in the wearPicker. The expected
   *     format is standard (0-59).
   * @param seconds Second value to be set on the secondColumn in the wearPicker if enabled by the
   *     parent activity. The expected value is standard (0-59).
   */
  public void setPickerTime(int hours, int minutes, int seconds) {
    final int indicatorAM = 0;
    final int indicatorPM = 1;

    if (!is24Hours) {
      timePicker.setPickerColumnItem(TIME_PICKER_HOUR_COLUMN_INDEX, hours % HOURS_12);
      int currentAMPM = hours < HOURS_12 ? indicatorAM : indicatorPM;
      timePicker.setToggleTextIndex(currentAMPM);
    } else {
      timePicker.setPickerColumnItem(TIME_PICKER_HOUR_COLUMN_INDEX, hours);
    }
    timePicker.setPickerColumnItem(TIME_PICKER_MINUTE_COLUMN_INDEX, minutes);
    if (includeSeconds) {
      timePicker.setPickerColumnItem(TIME_PICKER_SECOND_COLUMN_INDEX, seconds);
    }
  }

  private List<CharSequence> getAmPmToggleTexts(Builder builder) {
    String[] amPmStrings = DateFormatSymbols.getInstance().getAmPmStrings();
    String amString =
        builder.amStringId == 0 ? amPmStrings[0] : context.getString(builder.amStringId);
    String pmString =
        builder.pmStringId == 0 ? amPmStrings[1] : context.getString(builder.pmStringId);

    List<CharSequence> toggleTexts = new ArrayList<>();
    toggleTexts.add(amString);
    toggleTexts.add(pmString);
    return toggleTexts;
  }

  private List<TimePickerAdapter> getTimeAdapters(Builder builder) {
    List<TimePickerAdapter> timeValues = new ArrayList<>();
    if (builder.is24Hours) {
      timeValues.add(
          new TimePickerAdapter(
              locale,
              context.getString(builder.hoursStringId),
              COLUMN_24_HOURS_MIN_VALUE,
              COLUMN_24_HOURS_MAX_VALUE,
              DOUBLE_DECIMAL_FORMAT_SPECIFIER,
              false,
              context.getString(builder.hoursDescStringId),
              context.getString(builder.itemViewDescriptionStringId)));
    } else {
      timeValues.add(
          new TimePickerAdapter(
              locale,
              context.getString(builder.hoursStringId),
              COLUMN_12_HOURS_MIN_VALUE,
              COLUMN_12_HOURS_MAX_VALUE,
              DOUBLE_DECIMAL_FORMAT_SPECIFIER,
              true,
              context.getString(builder.hoursDescStringId),
              context.getString(builder.itemViewDescriptionStringId)));
    }
    timeValues.add(
        new TimePickerAdapter(
            locale,
            context.getString(builder.minutesStringId),
            COLUMN_MINUTES_MIN_VALUE,
            COLUMN_MINUTES_MAX_VALUE,
            DOUBLE_DECIMAL_FORMAT_SPECIFIER,
            false,
            context.getString(builder.minutesDescStringId),
            context.getString(builder.itemViewDescriptionStringId)));
    if (builder.includeSeconds) {
      timeValues.add(
          new TimePickerAdapter(
              locale,
              context.getString(builder.secondsStringId),
              COLUMN_SECONDS_MIN_VALUE,
              COLUMN_SECONDS_MAX_VALUE,
              DOUBLE_DECIMAL_FORMAT_SPECIFIER,
              false,
              context.getString(builder.secondsDescStringId),
              context.getString(builder.itemViewDescriptionStringId)));
    }
    return timeValues;
  }

  static final class TimePickerAdapter extends WearPickerColumnAdapter<ColumnViewHolder> {
    private static final String VALUE_FOR_WIDEST_ITEM = "00";

    private final int min;
    private final int max;
    private final String format;
    private final boolean showZeroAsTwelve;
    private final Locale locale;
    private final String itemViewDescriptionFormatString;

    public TimePickerAdapter(
        Locale locale,
        String label,
        int min,
        int max,
        String format,
        boolean showZeroAsTwelve,
        CharSequence contentDescription,
        String itemViewDescriptionFormatString) {
      super(label, contentDescription);
      this.locale = locale;
      this.showZeroAsTwelve = showZeroAsTwelve;
      this.min = min;
      this.max = max;
      this.format = format;
      this.itemViewDescriptionFormatString = itemViewDescriptionFormatString;
    }

    @Override
    public ColumnViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
      LayoutInflater inflater = TextViewFixUpLayoutInflater.wrap(viewGroup.getContext());
      return new ColumnViewHolder(
          locale,
          inflater.inflate(R.layout.wear_picker_column_time_item, viewGroup, false),
          format,
          showZeroAsTwelve);
    }

    @Override
    public void onBindViewHolder(ColumnViewHolder horizontalViewHolder, int position) {
      horizontalViewHolder.showPosition(position + min);
      horizontalViewHolder.setItemViewContentDescription(getItemViewDescription(position + min));
    }

    @Override
    public int getItemCount() {
      return (max - min) + 1;
    }

    @Override
    protected ColumnViewHolder onCreateWidestViewHolder(ViewGroup parent) {
      ColumnViewHolder viewHolder = onCreateViewHolder(parent, 0);
      // The value "00" would need the widest TextView to show it.
      ((TextView) viewHolder.itemView).setText(VALUE_FOR_WIDEST_ITEM);
      return viewHolder;
    }

    /**
     * Returns the content description for the given text view This is constructed by appending the
     * position(ex. 2), a space and column description of the picker column (Hours, Minutes etc.) If
     * the column description is not present, label text is used instead
     *
     * @param position position of the text view
     * @return The content description to be set for the text view at the given position
     */
    @VisibleForTesting
    String getItemViewDescription(int position) {
      CharSequence pickerColumnDescription =
          this.getColumnDescription() == null ? this.getLabel() : this.getColumnDescription();
      return String.format(
          locale,
          itemViewDescriptionFormatString,
          getConvertedPosition(position),
          pickerColumnDescription);
    }

    /**
     * Returns the modified position for a given absolute position. If zeros are to be shown as 12,
     * they are converted and the modified value of position is returned Example if showZeroAsTwelve
     * is set: position: 11, return value: 11 position: 0, return value: 12 position: 12, return
     * value: 12
     *
     * <p>if showZeroAsTwelve is not set: position: 11, return value: 11 position: 0, return value:
     * 0 position: 12, return value: 12
     *
     * @param position Absolute position of the item view in the column
     * @return The modified position of based on value of showZeroAsTwelve
     */
    private int getConvertedPosition(int position) {
      if (showZeroAsTwelve && position == 0) {
        position = 12;
      }
      return position;
    }
  }

  private static final class ColumnViewHolder extends ViewHolder {

    private final Locale locale;
    private final String format;
    private final boolean showZeroAsTwelve;

    ColumnViewHolder(Locale locale, View itemView, String format, boolean showZeroAsTwelve) {
      super(itemView);
      this.locale = locale;
      this.format = format;
      this.showZeroAsTwelve = showZeroAsTwelve;
    }

    @SuppressLint("SetTextI18n")
    void showPosition(int position) {
      if (showZeroAsTwelve && position == 0) {
        position = 12;
      }
      String positionText = String.format(locale, format, position);
      ((TextView) itemView).setText(positionText);
    }

    void setItemViewContentDescription(String contentText) {
      itemView.setContentDescription(contentText);
    }
  }
}
