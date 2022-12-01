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

package io.github.mzmine.modules.io.export_features_sql;

public enum SQLExportDataType {

  // Common row elements
  TITLE1("Common row elements", false, false, ""), //
  ID("      ID", false, true, "INT"), //
  MZ("      Average m/z", false, true, "DOUBLE"), //
  RT("      Average retention time", false, true, "DOUBLE"), //
  HEIGHT("      Average feature height", false, true, "DOUBLE"), //
  AREA("      Average feature area", false, true, "DOUBLE"), //
  COMMENT("      Comment", false, true, "STRING"), //

  // Identity elements
  TITLE2("Identity elements", false, false, ""), //
  IDENTITY("      Identity name", false, true, "STRING"), //
  ISOTOPEPATTERN("      Isotope pattern", false, true, "BLOB"), //
  MSMS("      MS/MS pattern", false, true, "BLOB"), //

  // Data file elements
  TITLE3("Data file elements", false, false, ""), //
  FEATURESTATUS("      Status", false, true, "STRING"), //
  FEATUREMZ("      m/z", false, true, "DOUBLE"), //
  FEATURERT("      RT", false, true, "DOUBLE"), //
  FEATURERT_START("      RT start", false, true, "DOUBLE"), //
  FEATURERT_END("      RT end", false, true, "DOUBLE"), //
  FEATUREDURATION("      Duration", false, true, "DOUBLE"), //
  FEATUREHEIGHT("      Height", false, true, "DOUBLE"), //
  FEATUREAREA("      Area", false, true, "DOUBLE"), //
  FEATURECHARGE("      Charge", false, true, "INT"), //
  DATAPOINTS("      # Data points", false, true, "INT"), //
  FWHM("      FWHM", false, true, "DOUBLE"), //
  TAILINGFACTOR("      Tailing factor", false, true, "DOUBLE"), //
  ASYMMETRYFACTOR("      Asymmetry factor", false, true, "DOUBLE"), //
  RAWFILE("      Raw data file name", false, true, "STRING"), //

  TITLE4("Other", false, false, ""), //
  CONSTANT("      Constant value", true, true, "");

  private final String name;
  private final boolean hasAdditionalValue;
  private final boolean isSelectableValue;
  private final String valueType;

  SQLExportDataType(String name, boolean hasAdditionalValue, boolean isSelectableValue,
      String valueType) {
    this.name = name;
    this.hasAdditionalValue = hasAdditionalValue;
    this.isSelectableValue = isSelectableValue;
    this.valueType = valueType;
  }

  public String toString() {
    return this.name;
  }

  public boolean hasAdditionalValue() {
    return hasAdditionalValue;
  }

  public boolean isSelectableValue() {
    return isSelectableValue;
  }

  public String valueType() {
    return this.valueType;
  }
}
