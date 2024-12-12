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

package io.github.mzmine.util.scans.merging;

import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MergedMassSpectrum.MergingType;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilter;
import io.github.mzmine.util.scans.ScanUtils;
import java.util.EnumSet;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Retain spectra where matches(spectrum) returns true
 */
public interface ScanSelectionFilter {

  boolean matches(MassSpectrum spectrum);

  default boolean matchesNot(MassSpectrum spectrum) {
    return !matches(spectrum);
  }

  /**
   * Combine multiple filters
   *
   * @return true if all filters match
   */
  static ScanSelectionFilter matchesAllOf(final @NotNull ScanSelectionFilter... filters) {
    return spectrum -> {
      for (ScanSelectionFilter filter : filters) {
        if (filter.matchesNot(spectrum)) {
          return false;
        }
      }
      return true;
    };
  }

  static ScanSelectionFilter all() {
    return new ScanSelectionFilter() {
      @Override
      public boolean matches(final MassSpectrum __) {
        return true;
      }

      @Override
      public boolean isFilter() {
        return false;
      }
    };
  }

  static ScanSelectionFilter none() {
    return _ -> true;
  }

  static ScanSelectionFilter includeOnly(final @NotNull EnumSet<MergingType> set) {
    return spectrum -> set.contains(ScanUtils.getMergingType(spectrum));
  }

  static ScanSelectionFilter includeOnly(final @NotNull List<MergingType> types) {
    var set = EnumSet.copyOf(types);
    return includeOnly(set);
  }

  static ScanSelectionFilter exclude(final @NotNull List<MergingType> types) {
    var set = EnumSet.copyOf(types);
    return exclude(set);
  }

  static ScanSelectionFilter exclude(final @NotNull EnumSet<MergingType> excluded) {
    return spectrum -> !excluded.contains(ScanUtils.getMergingType(spectrum));
  }

  static ScanSelectionFilter filterMsLevel(final @NotNull MsLevelFilter msLevelFilter) {
    return msLevelFilter::accept;
  }

  default boolean isFilter() {
    return true;
  }
}
