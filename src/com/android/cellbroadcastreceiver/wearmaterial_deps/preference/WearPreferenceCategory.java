package com.google.android.clockwork.common.wearable.wearmaterial.preference;

import android.content.Context;
import android.content.res.Resources;
import androidx.recyclerview.widget.RecyclerView.LayoutParams;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import androidx.core.content.res.ResourcesCompat;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceViewHolder;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Wear-specific implementation of {@link PreferenceCategory}. */
public class WearPreferenceCategory extends PreferenceCategory {

  private final int horizontalPadding;

  public WearPreferenceCategory(Context context) {
    this(context, null);
  }

  // AttributeSet can actually be null
  @SuppressWarnings("nullness:argument")
  public WearPreferenceCategory(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    Resources resources = context.getResources();
    int displayWidth = resources.getDisplayMetrics().widthPixels;
    horizontalPadding =
        Math.round(
            ResourcesCompat.getFloat(
                    resources, R.dimen.wear_preference_category_horizontal_padding_percent)
                * displayWidth);
  }

  @Override
  public void onBindViewHolder(PreferenceViewHolder holder) {
    super.onBindViewHolder(holder);

    CharSequence title = getTitle();
    TextView textView = (TextView) holder.itemView;

    if (TextUtils.isEmpty(title)) {
      // Hide Category label TextView if the title is empty
      // Setting View.GONE is insufficient for some reason, so need to set dimensions to 0 as well
      textView.setVisibility(View.GONE);
      textView.setLayoutParams(new LayoutParams(0, 0));
    } else {
      textView.setText(title);
      textView.setMaxLines(isSingleLineTitle() ? 1 : Integer.MAX_VALUE);
      // Keep the original padding intentionally, though the firstBaselineToTopHeight and the
      // lastBaselineToBottomHeight attributes that are set on the TextView make this redundant.
      textView.setPadding(
          horizontalPadding,
          textView.getPaddingTop(),
          horizontalPadding,
          textView.getPaddingBottom());
    }
  }
}
