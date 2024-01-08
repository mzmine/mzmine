/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;

/**
 * Stores information on mobility type, axis labels and units.
 * <p>
 * For an overview of these ion mobility types see https://doi.org/10.1002/jssc.201700919
 *
 * @author https://github.com/SteffenHeu
 */
public enum MobilityType {

  /**
   * Undefined
   */
  NONE("none", "none", "none"), //
  /**
   * Mixed types in row bindings - aligned samples from different instruments
   */
  MIXED("none", "none", "mixed"), //
  OTHER("none", "none", "other IMS"), //
  TIMS("1/k0", "Vs/cm^2", "TIMS"), // trapped ion mobility spectrometry
  DRIFT_TUBE("Drift time", "ms", "DTIMS"), // drift tube
  TRAVELING_WAVE("Drift time", "ms", "TWIMS"), // traveling wave ion mobility spectrometry
  FAIMS("TODO", "TODO", "FAIMS"); // field asymmetric waveform ion mobility spectrometry

  private final String axisLabel;
  private final String unit;
  private final String name;

  MobilityType(String axisLabel, String unit, String name) {
    this.axisLabel = axisLabel;
    this.unit = unit;
    this.name = name;
  }

  public String getAxisLabel() {
    UnitFormat uf = MZmineCore.getConfiguration().getUnitFormat();
    final String sb = "Mobility (" + axisLabel + ")";
    return uf.format(sb, getUnit());
  }

  public String getUnit() {
    return unit;
  }


  @Override
  public String toString() {
    return name;
  }

  /**
   * Check if the mobility type of feature matches this type. Only if this feature is actually an
   * IMS feature so usually after IMS expanding
   *
   * @param f the tested feature
   * @return true if the types match and the feature is an IMS feature
   */
  public boolean isTypeOf(final Feature f) {
    return f.getMobilityUnit() == this;
  }

  /**
   * Check if the mobility type of raw data file matches this type
   *
   * @param raw the tested file
   * @return true if the types match
   */
  public boolean isTypeOf(final RawDataFile raw) {
    return raw instanceof IMSRawDataFile imsfile && imsfile.getMobilityType() == this;
  }

  /**
   * Check if the mobility type of feature or its raw data file matches this type
   *
   * @param f the tested feature and its raw data file
   * @return true if the types match in the feature or the underlying {@link RawDataFile}
   */
  public boolean isTypeOfBackingRawData(final Feature f) {
    return isTypeOf(f) || isTypeOf(f.getRawDataFile());
  }

  public String getCcsBaseEntryString() {
    return switch (this) {
      case NONE, FAIMS, OTHER, MIXED -> null;
      case TIMS -> "TIMS";
      case DRIFT_TUBE -> "DT";
      case TRAVELING_WAVE -> "TW";
    };
  }
}
