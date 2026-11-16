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

package io.github.mzmine.modules.dataprocessing.featdet_massdetection.local_max;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import org.jetbrains.annotations.NotNull;

/**
 * Source of the detector saturation threshold in the {@link AdaptiveLocalMaxMassDetector}.
 */
public enum SaturationThresholdOption implements UniqueIdSupplier {
  /**
   * No saturation handling
   */
  OFF,
  /**
   * User defined threshold in raw intensity of a data point, the same for all spectra
   */
  MANUAL,
  /**
   * Fraction of the mobility scan abundance limit in the raw file metadata for mobility scans,
   * {@link #AUTO} if the file has no limit and for frames and other scans
   */
  RAW_FILE_METADATA,
  /**
   * Derived from the data: fraction of the highest base peak of the mobility scans or other scans
   * of the ms level if it occurs repeatedly (saturation plateau). Frames: fraction of the highest
   * frame base peak if the mobility scans of the ms level have a saturation plateau.
   */
  AUTO;

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case OFF -> "off";
      case MANUAL -> "manual";
      case RAW_FILE_METADATA -> "raw_file_metadata";
      case AUTO -> "auto";
    };
  }

  @Override
  public String toString() {
    return switch (this) {
      case OFF -> "Off";
      case MANUAL -> "Manual";
      case RAW_FILE_METADATA -> "Raw file metadata (else auto)";
      case AUTO -> "Auto (saturation plateau)";
    };
  }
}
