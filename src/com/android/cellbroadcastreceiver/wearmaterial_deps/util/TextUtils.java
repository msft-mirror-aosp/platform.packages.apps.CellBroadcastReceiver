package com.google.android.clockwork.common.wearable.wearmaterial.util;

import android.text.Spanned;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Shared text-related utility functions. */
public final class TextUtils {

  /**
   * Determines whether there is a chance that the contents of {@code text1} and {@code text2} are
   * different.
   *
   * <p>If both arguments refer to the same instance, this method returns {@code false}.
   *
   * <p>If only one of the arguments is {@code null}, this method returns {@code true}.
   *
   * <p>If either argument is an instances of {@link Spanned} or their contents differ, this method
   * returns {@code true}.
   *
   * <p>See also http://go/bugpattern/UndefinedEquals#for-charsequence
   */
  public static boolean contentsMayDiffer(
      @Nullable CharSequence text1, @Nullable CharSequence text2) {
    if (text1 == text2) {
      return false;
    }

    if (text1 == null || text2 == null) {
      return true;
    }

    if (text1 instanceof Spanned || text2 instanceof Spanned) {
      // Expensive to compare contents of Spanned instance. Assume they may be different.
      return true;
    }

    return !text1.toString().contentEquals(text2);
  }

  private TextUtils() {}
}
