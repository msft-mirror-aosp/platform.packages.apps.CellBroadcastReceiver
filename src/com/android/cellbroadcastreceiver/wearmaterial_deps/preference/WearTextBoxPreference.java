package com.google.android.clockwork.common.wearable.wearmaterial.preference;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.widget.TextView;
import androidx.core.content.res.ResourcesCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Wear implementation of sub-text blocks, used in descriptions or footers. The text is taken from
 * the title field of the text.
 */
public class WearTextBoxPreference extends Preference {

  private final int horizontalPadding;

  public WearTextBoxPreference(Context context) {
    this(context, null);
  }

  // AttributeSet can actually be null, setLayoutResource can be called in constructor
  @SuppressWarnings({"nullness:argument", "method.invocation"})
  public WearTextBoxPreference(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    setLayoutResource(R.layout.wear_text_block_preference);
    setSelectable(false);
    Resources resources = context.getResources();
    int displayWidth = resources.getDisplayMetrics().widthPixels;
    horizontalPadding =
        Math.round(
            ResourcesCompat.getFloat(
                    resources, R.dimen.wear_preference_text_box_horizontal_padding_percent)
                * displayWidth);
  }

  @Override
  public void onBindViewHolder(PreferenceViewHolder holder) {
    super.onBindViewHolder(holder);

    TextView textView = (TextView) holder.itemView;
    textView.setText(getTitle());
    // Keep the original padding intentionally, though the firstBaselineToTopHeight and the
    // lastBaselineToBottomHeight attributes that are set on the TextView make this redundant.
    textView.setPadding(
        horizontalPadding,
        textView.getPaddingTop(),
        horizontalPadding,
        textView.getPaddingBottom());
  }
}
