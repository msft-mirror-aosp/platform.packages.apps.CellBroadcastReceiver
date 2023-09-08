package com.google.android.clockwork.common.wearable.wearmaterial.progressindicator;

import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import androidx.annotation.StyleableRes;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This class helps dealing with obtaining attribute values from a {@link TypedArray} that first may
 * not have a theme associated with it, but may later get a theme applied to it.
 *
 * <p>This is mainly for helping out with {@code Drawable}s that get inflated but need themed
 * attribute values. They can implement {@code canApplyTheme()} and {@code applyTheme()} with the
 * help of this class.
 *
 * <p>The {@link #styleableAttrs} is the set of attributes that will be retrieved by the {@link
 * #inflate(Theme, AttributeSet, Resources)} method.
 */
final class TypedArrayHelper {
  private final int[] styleableAttrs;
  private final int[] themedAttributeIds;

  private boolean canApplyTheme = false;

  public TypedArrayHelper(@StyleableRes int[] styleableAttrs) {
    this.styleableAttrs = styleableAttrs;
    this.themedAttributeIds = new int[styleableAttrs.length];
  }

  /**
   * Returns the {@link TypedArray} for {@link #styleableAttrs} and marks which attributes are
   * referring to themed-attribute references.
   */
  public TypedArray inflate(@Nullable Theme theme, AttributeSet attrs, Resources r) {
    TypedArray a;
    if (theme != null) {
      a = theme.obtainStyledAttributes(attrs, styleableAttrs, 0, 0);
    } else {
      a = r.obtainAttributes(attrs, styleableAttrs);
    }

    TypedValue tv = new TypedValue();
    for (int i = 0; i < styleableAttrs.length; i++) {
      if (!a.hasValue(i)) {
        continue;
      }

      a.getValue(i, tv);
      if (tv.type == TypedValue.TYPE_ATTRIBUTE) {
        themedAttributeIds[i] = tv.data;
        canApplyTheme = true;
      }
    }
    return a;
  }

  /**
   * Returns true if some of the attributes in {@link #styleableAttrs} referred to themed-attribute
   * references, as determined during the {@link #inflate(Theme, AttributeSet, Resources)} step.
   */
  public boolean canApplyTheme() {
    return canApplyTheme;
  }

  /**
   * Returns a {@link TypedArray} for the themed-attributes that were marked as such during the
   * {@link #inflate(Theme, AttributeSet, Resources)} step.
   */
  public TypedArray applyTheme(Theme theme) {
    return theme.obtainStyledAttributes(themedAttributeIds);
  }

  /**
   * Returns true if the {@code TypedArray} {@code a} has the value of the given {@code attributeId}
   * and that value is not an attribute ({@link TypedValue#TYPE_ATTRIBUTE}).
   */
  public boolean hasResolvedValue(TypedArray a, @StyleableRes int attributeId) {
    return (a.hasValue(attributeId) && a.getType(attributeId) != TypedValue.TYPE_ATTRIBUTE);
  }
}
