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

package io.github.mzmine.modules.io.export_features_csv_legacy;

public enum LegacyExportRowDataFileElement {

  FEATURE_STATUS("Feature status"),//
  FEATURE_NAME("Feature name"), //
  FEATURE_MZ("Feature m/z"), //
  FEATURE_RT("Feature RT"), //
  FEATURE_RT_START("Feature RT start"), //
  FEATURE_RT_END("Feature RT end"), //
  FEATURE_DURATION("Feature duration time"), //
  // needs to be named Peak for GNPS (for now)
  FEATURE_HEIGHT("Peak height"), //
  FEATURE_AREA("Peak area"), //
  FEATURE_ION_MOBILITY("Ion mobility"), //
  FEATURE_ION_MOBILITY_UNIT("Ion mobility unit"), //
  FEATURE_CCS("CCS"), //
  FEATURE_CHARGE("Feature charge"), //
  FEATURE_DATAPOINTS("Feature # data points"), //
  FEATURE_FWHM("Feature FWHM"), //
  FEATURE_TAILINGFACTOR("Feature tailing factor"), //
  FEATURE_ASYMMETRYFACTOR("Feature asymmetry factor"), //
  FEATURE_MZMIN("Feature m/z min"), //
  FEATURE_MZMAX("Feature m/z max");

  private final String name;

  LegacyExportRowDataFileElement(String name) {
    this.name = name;
  }

  public String toString() {
    return this.name;
  }

}
