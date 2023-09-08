package com.google.android.clockwork.common.wearable.wearmaterial.picker;

import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;

import android.annotation.SuppressLint;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Consumer;
import com.google.android.clockwork.common.wearable.wearmaterial.picker.CenteredRecyclerView.OnItemA11ySelected;
import com.google.android.clockwork.common.wearable.wearmaterial.picker.RowAdapter.PickerColumnViewHolder;
import com.google.android.clockwork.common.wearable.wearmaterial.picker.WearPicker.OnPickerColumnItemChanged;
import com.google.android.clockwork.common.wearable.wearmaterial.picker.WearPickerColumn.ColumnAppearance;
import com.google.android.clockwork.common.wearable.wearmaterial.util.TextUtils;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The {@code Adapter} for the {@code PickerRow.pickerColumns} that represents the row of {@link
 * WearPickerColumn}s.
 *
 * @see WearPicker#setColumnAdapters(List)
 */
class RowAdapter extends CenteredRecyclerViewAdapter<PickerColumnViewHolder> {

  /** Used to allow {@link WearPickerColumn}s of this adapter to <B>not</B> be recycled. */
  static final int PICKER_COLUMN_VIEW_TYPE = 0;

  static final int A11Y_PICKER_COLUMN_VIEW_TYPE = 1;

  /** Represents the row of {@link WearPickerColumn}s. */
  private final List<? extends WearPickerColumnAdapter<?>> pickerColumnAdapters;

  /** Listens to changes in the highlighted items of all its {@link WearPickerColumn}s. */
  private final OnPickerColumnItemChanged onPickerColumnItemChangedListener;

  /** Listens to double-taps in Talkback-mode on items of all its {@link WearPickerColumn}s. */
  private final OnItemA11ySelected onItemA11ySelectedListener;

  /** Listens to clicks on all of its {@link WearPickerColumn}s. */
  private final Consumer<Integer> onPickerColumnClickedListener;

  /** If true, the active {@code WearPickerColumn}'s label must be shown when it collapses. */
  private boolean showLabelOnCollapsed = true;

  /** If true, haptics for the {@code WearPickerColumn}s are enabled. */
  private boolean isHapticsEnabled = true;

  /**
   * If true, the watch's Accessibility is enabled and the columns should be as wide as the
   * recycler-view itself.
   */
  private boolean isA11yEnabled;

  @SuppressWarnings({"nullness:argument", "nullness:method.invocation"})
  RowAdapter(
      List<? extends WearPickerColumnAdapter<?>> pickerColumnAdapters,
      OnPickerColumnItemChanged onPickerColumnItemChangedListener,
      OnItemA11ySelected onItemA11ySelectedListener,
      Consumer<Integer> onPickerColumnClickedListener) {
    this.pickerColumnAdapters = pickerColumnAdapters;
    this.onPickerColumnItemChangedListener = onPickerColumnItemChangedListener;
    this.onItemA11ySelectedListener = onItemA11ySelectedListener;
    this.onPickerColumnClickedListener = onPickerColumnClickedListener;

    setHasStableIds(true);
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public PickerColumnViewHolder onCreateViewHolder(ViewGroup viewGroup, int itemType) {
    LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
    int layoutId =
        isA11yEnabled ? R.layout.wear_picker_row_item_a11y : R.layout.wear_picker_row_item;
    View itemView = inflater.inflate(layoutId, viewGroup, false);
    return new PickerColumnViewHolder(
        itemView,
        isHapticsEnabled,
        onPickerColumnItemChangedListener,
        onItemA11ySelectedListener,
        onPickerColumnClickedListener);
  }

  @Override
  public void onBindViewHolder(PickerColumnViewHolder pickerColumnViewHolder, int position) {
    pickerColumnViewHolder.bind(pickerColumnAdapters.get(position));
    pickerColumnViewHolder.setAppearance(position, getHighlightedItemIndex(), showLabelOnCollapsed);
  }

  @Override
  public int getItemCount() {
    return pickerColumnAdapters.size();
  }

  @Override
  public int getItemViewType(int position) {
    return isA11yEnabled ? A11Y_PICKER_COLUMN_VIEW_TYPE : PICKER_COLUMN_VIEW_TYPE;
  }

  @Nullable WearPickerColumnAdapter<?> getColumnAdapter(int position) {
    boolean isInRange = position >= 0 && position < pickerColumnAdapters.size();
    return isInRange ? pickerColumnAdapters.get(position) : null;
  }

  List<? extends WearPickerColumnAdapter<?>> getClearedPickerColumnAdapters() {
    for (WearPickerColumnAdapter<?> columnAdapter : pickerColumnAdapters) {
      columnAdapter.clear();
    }
    return pickerColumnAdapters;
  }

  void setShowLabelOnCollapsed(boolean showLabelOnCollapsed) {
    this.showLabelOnCollapsed = showLabelOnCollapsed;
  }

  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  boolean isShowLabelOnCollapsed() {
    return showLabelOnCollapsed;
  }

  void setHapticsEnabled(boolean hapticsEnabled) {
    isHapticsEnabled = hapticsEnabled;
  }

  @SuppressLint("NotifyDataSetChanged")
  void setA11yEnabled(boolean a11yEnabled) {
    isA11yEnabled = a11yEnabled;
    notifyDataSetChanged();
  }

  /** The {@code ViewHolder} of each {@link WearPickerColumn} in the {@link RowAdapter}. */
  static final class PickerColumnViewHolder extends ViewHolder {

    /** The {@link WearPickerColumn} in this {@code ViewHolder}. */
    private final WearPickerColumn<? super CenteredRecyclerViewAdapter<?>> pickerColumn;

    /** Listens to changes in the highlighted item of its {@link #pickerColumn}s. */
    private final OnPickerColumnItemChanged onPickerColumnItemChangedListener;

    /** Listens to clicks on this {@link #pickerColumn}. */
    private final Consumer<Integer> onPickerColumnClickedListener;

    @SuppressWarnings("nullness:methodref.receiver.bound")
    PickerColumnViewHolder(
        View itemView,
        boolean isHapticsEnabled,
        OnPickerColumnItemChanged onPickerColumnItemChangedListener,
        OnItemA11ySelected onItemA11ySelectedListener,
        Consumer<Integer> onPickerColumnClickedListener) {
      super(itemView);

      pickerColumn = itemView.findViewById(R.id.wear_picker_row_column);
      pickerColumn.addOnHighlightedItemIndexChangedListener(this::onHighlightedItemIndexChanged);
      pickerColumn.addOnItemA11ySelectedListener(onItemA11ySelectedListener);
      pickerColumn.setOnClickListener(this::onPickerColumnClicked);
      pickerColumn.setHapticsEnabled(isHapticsEnabled);
      pickerColumn.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

      this.onPickerColumnItemChangedListener = onPickerColumnItemChangedListener;
      this.onPickerColumnClickedListener = onPickerColumnClickedListener;
    }

    private void onHighlightedItemIndexChanged(int index) {
      onPickerColumnItemChangedListener.onPickerColumnItemChanged(
          getAbsoluteAdapterPosition(), index);
    }

    private void onPickerColumnClicked(View view) {
      onPickerColumnClickedListener.accept(getAbsoluteAdapterPosition());
    }

    /**
     * Binds the {@code columnAdapter} to the {@link #pickerColumn}.
     *
     * <p>It makes sure that the adapter, the label, the content description and the currently
     * highlighted item are up-to-date. Column Description is nullable, thus it is set only if it's
     * not null and differs from the old content description
     */
    void bind(WearPickerColumnAdapter<?> columnAdapter) {
      pickerColumn.setAdapter(columnAdapter);

      pickerColumn.setHighlightedIndex(columnAdapter.getHighlightedItemIndex());

      CharSequence oldText = pickerColumn.getLabel();
      CharSequence newText = columnAdapter.getLabel();
      CharSequence newContentDescription = columnAdapter.getColumnDescription();
      CharSequence oldContentDescription = pickerColumn.getColumnDescription();
      if (TextUtils.contentsMayDiffer(oldText, newText)) {
        pickerColumn.setLabel(newText);
      }
      /* If content description is not given, set label as the content description
         Else set the new content description only if it differs from the old one
      */
      if (newContentDescription == null) {
        pickerColumn.setColumnDescription(newText);
      } else if (TextUtils.contentsMayDiffer(oldContentDescription, newContentDescription)) {
        pickerColumn.setColumnDescription(newContentDescription);
      }
      columnAdapter.clearPostedPickerColumnItemIndex();
    }

    /**
     * Determines the appearance of the {@link #itemView} based on the position of this item view
     * and the position of the currently highlighted item.
     *
     * @param itemViewPosition The position of this {@link #itemView} in the adapter
     * @param highlightedPosition The position of the currently highlighted item in the adapter
     * @param showLabelOnCollapsed If true, hides label when the {@code WearPickerColumn} collapses.
     *     If false, never shows the label.
     */
    void setAppearance(
        int itemViewPosition, int highlightedPosition, boolean showLabelOnCollapsed) {
      if (highlightedPosition != NO_POSITION) {
        boolean isActivated = itemViewPosition == highlightedPosition;
        itemView.setActivated(isActivated);
        pickerColumn.setColumnAppearance(
            isActivated ? ColumnAppearance.EXPANDED : ColumnAppearance.COLLAPSED);

        if (isActivated) {
          pickerColumn.requestFocus();
        }
      }

      pickerColumn.setIsLabelShownOnCollapsedList(showLabelOnCollapsed);
    }
  }
}
