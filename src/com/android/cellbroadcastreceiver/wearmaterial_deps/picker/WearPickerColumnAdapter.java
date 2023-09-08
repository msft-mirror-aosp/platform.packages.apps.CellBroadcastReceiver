package com.google.android.clockwork.common.wearable.wearmaterial.picker;

import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;

import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import android.view.ViewGroup;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This is an {@code Adapter} that represents a {@link WearPickerColumn}. Each {@code PickerColumn}
 * in a {@code PickerRow} must be represented by one instance of this class.
 *
 * @see WearPicker#setColumnAdapters(java.util.List)
 * @param <V> The type of this adapter's {@code ViewHolder}s
 */
public abstract class WearPickerColumnAdapter<V extends ViewHolder>
    extends CenteredRecyclerViewAdapter<V> {

  /** The text of the label of the {@code PickerColumn}. */
  private final CharSequence label;

  /** The content description of the {@code PickerColumn}. */
  private final @Nullable CharSequence contentDescription;

  /**
   * If set to a non-negative value, the {@link ViewHolder}s must set the {@code PickerColumn}'s
   * highlighted item to this value.
   */
  private int postedPickerColumnItemIndex = NO_POSITION;

  @SuppressWarnings({"nullness:argument", "nullness:method.invocation"})
  protected WearPickerColumnAdapter(CharSequence label, @Nullable CharSequence contentDescription) {
    this.label = label;
    this.contentDescription = contentDescription;
    setHasStableIds(true);
  }

  @Override
  public final long getItemId(int position) {
    return position;
  }

  /**
   * Returns either the posted index of the highlighted item of this adapter's {@code PickerColumn}
   * or, if if that index was not posted, the actual highlighted item index of the {@code
   * PickerColumn}.
   */
  @Override
  protected int getHighlightedItemIndex() {
    return postedPickerColumnItemIndex != NO_POSITION
        ? postedPickerColumnItemIndex
        : super.getHighlightedItemIndex();
  }

  /** Returns the text of the {@code PickerColumn}'s label. */
  public final CharSequence getLabel() {
    return label;
  }

  /** Returns the content description of the {@code PickerColumn}. */
  public final @Nullable CharSequence getColumnDescription() {
    return contentDescription;
  }

  /**
   * Returns a {@link ViewHolder} that will be measured to determine the width of the widest
   * possible item view. If it returns {@code null}, no such measuring will take place.
   *
   * <p>For example, if this adapter creates View Holders with TextViews that show two-digit
   * numbers, then this method should return a ViewHolder with a TextView whose text is "00" or
   * "88", because "00" or "88" will need the widest possible TextView.
   *
   * <p>If this method does not return null, the item views being inflated by this adapter should
   * specify {@link android.view.ViewGroup.LayoutParams#MATCH_PARENT} as their layout-widths. This
   * will allow for a more efficient measurement pass for the recycler view.
   *
   * <p>If the item views inflated by this adapter already have a fixed width, this method should
   * return {@code null}, because then there is no item view with a widest width; they all have the
   * same width.
   *
   * <p>This method will be called right before this adapter is assigned to a {@code
   * WearPickerColumn}.
   *
   * <p>By default, this method returns {@code null}.
   */
  protected @Nullable V onCreateWidestViewHolder(ViewGroup parent) {
    return null;
  }

  /**
   * Posts a new {@code index} for the {@code PickerColumn}'s highlighted item so that it can be
   * properly set as the actual highlighted item after this adapter notifies the view that its data
   * has changed.
   *
   * @see #clearPostedPickerColumnItemIndex()
   */
  void postPickerColumnItemIndex(int index) {
    postedPickerColumnItemIndex = index;
  }

  /**
   * Clears the posted index of the highlighted item.
   *
   * @see #postPickerColumnItemIndex(int)
   */
  void clearPostedPickerColumnItemIndex() {
    postedPickerColumnItemIndex = NO_POSITION;
  }
}
