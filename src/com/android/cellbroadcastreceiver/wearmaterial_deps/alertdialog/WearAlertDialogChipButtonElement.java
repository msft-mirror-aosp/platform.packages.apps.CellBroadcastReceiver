package com.google.android.clockwork.common.wearable.wearmaterial.alertdialog;

import org.checkerframework.checker.nullness.qual.Nullable;

/** A Pojo for holding data for WearAlertDialogChipButtonElement. */
class WearAlertDialogChipButtonElement implements WearAlertDialogElement {

  private final int chipButtonId;
  private final int chipButtonIconId;
  private final String chipButtonString;
  private final @Nullable String chipButtonContentDescription;

  public WearAlertDialogChipButtonElement(
      int chipButtonIconId, String chipButtonString, int chipButtonId) {
    this(chipButtonIconId, chipButtonString, chipButtonId, null);
  }

  public WearAlertDialogChipButtonElement(
      int chipButtonIconId,
      String chipButtonString,
      int chipButtonId,
      @Nullable String chipButtonContentDescription) {
    this.chipButtonId = chipButtonId;
    this.chipButtonIconId = chipButtonIconId;
    this.chipButtonString = chipButtonString;
    this.chipButtonContentDescription = chipButtonContentDescription;
  }

  public int getChipButtonId() {
    return chipButtonId;
  }

  public int getChipButtonIconId() {
    return chipButtonIconId;
  }

  public String getChipButtonString() {
    return chipButtonString;
  }

  public @Nullable String getChipButtonContentDescription() {
    return chipButtonContentDescription;
  }

  @Override
  public final int getViewType() {
    return WearAlertDialogViewType.CHIP_BUTTONS;
  }
}
