package com.google.android.clockwork.common.wearable.wearmaterial.animations;

/**
 * Pre-defined duration constants for animations in Wear.
 *
 * <p>See <a href="https://carbon.googleplex.com/wear-os-3/pages/speed">Wear OS Speed</a>
 */
public final class Durations {

  private Durations() {}

  /** Duration in milliseconds for a very fast transition. */
  public static final long FLASH = 75;

  /** Duration in milliseconds for a fast transition. */
  public static final long RAPID = 150;

  /** Duration in millisecond for a quick transition. */
  public static final long QUICK = 225;

  /** Duration in milliseconds for most standard transitions. */
  public static final long STANDARD = 300;

  /** Duration in milliseconds for a slower transition. */
  public static final long CASUAL = 375;

  /** Duration in milliseconds for a slow transition. */
  public static final long SLOW = 450;

  /** Duration in milliseconds for a slower transition. */
  public static final long EXTRA_1 = 750;

  /** Duration in milliseconds for an extra slow transition. */
  public static final long EXTRA_2 = 1000;

  /** Duration in milliseconds for the slowest transition. */
  public static final long EXTRA_3 = 1500;
}
