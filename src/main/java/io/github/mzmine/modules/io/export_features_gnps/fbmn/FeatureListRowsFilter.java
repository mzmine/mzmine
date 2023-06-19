/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.io.export_features_gnps.fbmn;

import io.github.mzmine.datamodel.features.FeatureListRow;

/**
 * Define which rows to export
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public enum FeatureListRowsFilter {
  ALL, ONLY_WITH_MS2, MS2_OR_ION_IDENTITY, MS2_AND_ION_IDENTITY;

  @Override
  public String toString() {
    return super.toString().replaceAll("_", " ");
  }

  /**
   * Filter a row
   *
   * @return true if row conforms to the filter
   */
  public boolean accept(FeatureListRow row) {
    return switch (this) {
      case ALL -> true;
      case ONLY_WITH_MS2 -> row.hasMs2Fragmentation();
      case MS2_OR_ION_IDENTITY -> row.hasMs2Fragmentation() || row.hasIonIdentity();
      case MS2_AND_ION_IDENTITY -> row.hasMs2Fragmentation() && row.hasIonIdentity();
    };
  }

  /**
   * @return all rows accepted by this filter require MS2
   */
  public boolean requiresMS2() {
    return switch (this) {
      case ALL, MS2_OR_ION_IDENTITY -> false;
      case ONLY_WITH_MS2, MS2_AND_ION_IDENTITY -> true;
    };
  }
}
