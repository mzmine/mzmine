/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_spectral_library_match;

import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilter;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilter.Options;

/**
 * Previously used to define merging and scan selection. Now this is replaced by other parameters.
 * Still here for import of old batches
 */
@Deprecated
public enum LegacyScanMatchingSelection {
  MS1, MERGED_MS2, MERGED_MSN, ALL_MS2, ALL_MSN;

  @Override
  public String toString() {
    return switch (this) {
      case MS1 -> "MS1";
      case MERGED_MS2 -> "MS2 (merged)";
      case MERGED_MSN -> "MS level ≥ 2 (merged)";
      case ALL_MS2 -> "MS2 (all scans)";
      case ALL_MSN -> "MS level ≥ 2 (all scans)";
    };
  }

  public MsLevelFilter getMsLevelFilter() {
    return switch (this) {
      case MS1 -> new MsLevelFilter(Options.MS1);
      case MERGED_MS2, ALL_MS2 -> new MsLevelFilter(Options.MS2);
      case MERGED_MSN, ALL_MSN -> new MsLevelFilter(Options.MSn);
    };
  }

  public boolean isAll() {
    return this == ALL_MS2 || this == ALL_MSN;
  }
}
