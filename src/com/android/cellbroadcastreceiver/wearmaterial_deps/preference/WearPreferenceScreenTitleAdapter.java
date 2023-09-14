package com.google.android.clockwork.common.wearable.wearmaterial.preference;

import android.content.res.Resources;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.res.ResourcesCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import com.google.android.clockwork.common.wearable.wearmaterial.preference.WearPreferenceScreenTitleAdapter.TitleViewHolder;

/**
 * An adapter than renders only a title. We need this as
 *
 * <ul>
 *   <li/>{@link PreferenceScreen} is final and provides no way to customize {@link
 *       PreferenceGroup#isOnSameScreenAsChildren()}. This even if you set the title and preference
 *       screen style, it does not render anything.
 *   <li/>There's no reliable way to modify the creation of the preference screen such that there is
 *       a "fake" preference in the hierarchy, as it's impossible to make any assumptions about the
 *       user-provided values for ordering if {@code android:orderingFromXml} is false. You also
 *       can't just set the preference order to {@link Integer#MIN_VALUE}, since the current
 *       implementation of preference comparison overflows (b/187779801).
 * </ul>
 */
final class WearPreferenceScreenTitleAdapter extends RecyclerView.Adapter<TitleViewHolder> {

  private final CharSequence title;
  private boolean showTitle;

  public WearPreferenceScreenTitleAdapter(CharSequence title, boolean showTitle) {
    this.title = title;
    this.showTitle = showTitle;
    setHasStableIds(true);
  }

  @Override
  public TitleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    View view = inflater.inflate(R.layout.wear_title_preference, parent, false);
    Resources resources = view.getResources();
    int displayWidth = resources.getDisplayMetrics().widthPixels;
    int horizontalPadding =
        Math.round(
            ResourcesCompat.getFloat(
                    resources, R.dimen.wear_preference_title_horizontal_padding_percent)
                * displayWidth);
    view.setPadding(
        horizontalPadding, view.getPaddingTop(), horizontalPadding, view.getPaddingBottom());
    return new TitleViewHolder(view);
  }

  @Override
  public void onBindViewHolder(TitleViewHolder holder, int position) {
    holder.titleView.setText(title);
  }

  @Override
  public int getItemCount() {
    return showTitle ? 1 : 0;
  }

  public void setShowTitle(boolean showTitle) {
    if (this.showTitle == showTitle) {
      return;
    }
    this.showTitle = showTitle;
    if (showTitle) {
      notifyItemInserted(0);
    } else {
      notifyItemRemoved(0);
    }
  }

  @VisibleForTesting
  CharSequence getTitle() {
    return title;
  }

  /** Basic view holder wrapping the title view. */
  public static class TitleViewHolder extends ViewHolder {

    public final TextView titleView;

    public TitleViewHolder(View itemView) {
      super(itemView);
      titleView = (TextView) itemView;
    }
  }
}
