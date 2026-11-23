/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.datamodel.features.rawfiletypes;

import java.util.Locale;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Highest possible abundance of a mobility scan data point (detector saturation level) per MS
 * level, as reported by the raw file. NaN if unknown. Not defined for frames (summed spectra).
 *
 * @param ms1 limit of MS1 mobility scans
 * @param ms2 limit of MS2 (MSn) mobility scans
 */
public record MobilityScanAbundanceLimits(double ms1, double ms2) {

  public static final MobilityScanAbundanceLimits UNKNOWN = new MobilityScanAbundanceLimits(
      Double.NaN, Double.NaN);
  private static final String SEPARATOR = ";";

  /**
   * @return the limit for this ms level, NaN if unknown
   */
  public double forMsLevel(final int msLevel) {
    return msLevel > 1 ? ms2 : ms1;
  }

  /**
   * @return a copy with the limit of this ms level replaced
   */
  public @NotNull MobilityScanAbundanceLimits withMsLevel(final int msLevel, final double limit) {
    return msLevel > 1 ? new MobilityScanAbundanceLimits(ms1, limit)
        : new MobilityScanAbundanceLimits(limit, ms2);
  }

  public boolean isUnknown() {
    return Double.isNaN(ms1) && Double.isNaN(ms2);
  }

  /**
   * @return machine-readable form used for saving, see {@link #parse(String)}
   */
  public @NotNull String toSaveString() {
    return ms1 + SEPARATOR + ms2;
  }

  /**
   * @param text form of {@link #toSaveString()}
   * @return the limits or null if the text cannot be parsed
   */
  public static @Nullable MobilityScanAbundanceLimits parse(@Nullable final String text) {
    if (text == null || text.isBlank()) {
      return null;
    }
    final String[] split = text.strip().split(SEPARATOR);
    if (split.length != 2) {
      return null;
    }
    try {
      return new MobilityScanAbundanceLimits(Double.parseDouble(split[0]),
          Double.parseDouble(split[1]));
    } catch (NumberFormatException e) {
      return null;
    }
  }

  @Override
  public @NotNull String toString() {
    return "MS1: %s, MS2: %s".formatted(format(ms1), format(ms2));
  }

  private static @NotNull String format(final double value) {
    return Double.isNaN(value) ? "-" : String.format(Locale.ROOT, "%.0f", value);
  }
}
