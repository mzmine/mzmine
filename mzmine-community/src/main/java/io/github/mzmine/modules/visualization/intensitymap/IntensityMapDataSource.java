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

package io.github.mzmine.modules.visualization.intensitymap;

import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Data points the visualizer reads from each scan: the raw data or the mass list.
 */
public enum IntensityMapDataSource implements UniqueIdSupplier {
  AUTO, RAW, MASS_LIST;

  @Override
  public @NotNull String getUniqueID() {
    // stable ids, labels may change
    return switch (this) {
      case AUTO -> "AUTO";
      case RAW -> "RAW";
      case MASS_LIST -> "MASS_LIST";
    };
  }

  @Override
  public @NotNull String toString() {
    return switch (this) {
      case AUTO -> "Auto (mass list if available)";
      case RAW -> "Raw data";
      case MASS_LIST -> "Mass list";
    };
  }

  /**
   * @return the spectrum to read, null if a mass list is required and the scan has none
   */
  public @Nullable MassSpectrum spectrum(@NotNull final Scan scan) {
    return switch (this) {
      case RAW -> scan;
      case MASS_LIST -> scan.getMassList();
      // decision: per scan, so partly processed files still show all scans
      case AUTO -> {
        final MassList masses = scan.getMassList();
        yield masses != null ? masses : scan;
      }
    };
  }
}
