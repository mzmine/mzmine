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

package io.github.mzmine.datamodel;

import org.jetbrains.annotations.NotNull;

/**
 * Kinds of problems detected while a raw data file was imported. Critical warnings indicate that
 * data may be missing or wrong, non-critical ones hint at a possibly suboptimal conversion.
 */
public enum RawFileWarningType {

  DUPLICATE_MOBILITY_SCANS(true, """
      A frame contained multiple spectra at the same mobility. Only the first spectrum at that coordinate was kept."""),

  ZERO_INTENSITY_CENTROIDS(false, """
      Scans were detected as centroid but contain zero-intensity values. This might indicate incorrect conversion by msconvert.
      Make sure to run "peak picking" with vendor algorithm as the first step (even before title maker), otherwise msconvert uses
      a different algorithm that picks the highest data point of a profile spectral peak and adds zero intensities next to each signal.
      This leads to degraded mass accuracies."""),

  EMPTY_SCANS(false, """
      Some MS1 scans were recognized as empty (no detected signals).
      The possible reason might be high noise levels influencing mzml conversion.
      Files can be processed anyway, but consider re-converting if you encounter unexpected results.""");

  private final boolean critical;
  private final @NotNull String description;

  RawFileWarningType(boolean critical, @NotNull String description) {
    this.critical = critical;
    this.description = description;
  }

  /**
   * @return true if data may be missing or wrong
   */
  public boolean isCritical() {
    return critical;
  }

  /**
   * @return user-facing description of the problem
   */
  public @NotNull String getDescription() {
    return description;
  }


  @Override
  public String toString() {
    return switch (this) {
      case DUPLICATE_MOBILITY_SCANS -> "Repeated mobility scans";
      case EMPTY_SCANS -> "Empty scan warning";
      case ZERO_INTENSITY_CENTROIDS -> "Zero intensity centroids warning";
    };
  }
}
