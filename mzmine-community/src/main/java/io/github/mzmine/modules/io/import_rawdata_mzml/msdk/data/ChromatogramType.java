/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public enum ChromatogramType {
  TIC(MzMLCV.cvChromatogramTIC, "Total ion chromatogram"), //
  MRM_SRM(MzMLCV.cvChromatogramMRM_SRM, "Mixed reaction monitoring"),//
  SIM(MzMLCV.cvChromatogramSIM, "Selected ion monitoring"),//
  SIC(MzMLCV.cvChromatogramSIC, "Selected ion chromatogram"), //
  BPC(MzMLCV.cvChromatogramBPC, "Base peak chromatogram"), //
  ELECTROMAGNETIC_RADIATION(MzMLCV.cvChromatogramElectromagneticRadiation,
      "Radiation chromatogram"),//
  ABSORPTION(MzMLCV.cvChromatogramAbsorption, "Absorption chromatogram"), //
  EMISSION(MzMLCV.cvChromatogramEmission, "Emission chromatogram"), //
  ION_CURRENT(MzMLCV.cvChromatogramIonCurrent, "Ion current chromatogram"),//
  PRESSURE(MzMLCV.cvChromatogramPressure, "Pressure chromatogram"), //
  FLOW_RATE(MzMLCV.cvChromatogramFlowRate, "Flow rate chromatogram"), //
  UNKNOWN("Unknown", "Unknown chromatogram");

  private static final Map<String, ChromatogramType> map = Arrays.stream(values())
      .collect(Collectors.toMap(ct -> ct.getAccession(), ct -> ct));
  private final String description;
  String accession;

  ChromatogramType(String accession, String description) {
    this.accession = accession;
    this.description = description;
  }

  @NotNull
  public static ChromatogramType ofAccession(String accession) {
    return map.getOrDefault(accession, UNKNOWN);
  }

  public boolean isMsType() {
    return switch (this) {
      case TIC, BPC, SIC, SIM, MRM_SRM -> true;
      case ELECTROMAGNETIC_RADIATION, FLOW_RATE, PRESSURE, ION_CURRENT, EMISSION, ABSORPTION,
           UNKNOWN -> false;
    };
  }

  public boolean isOtherType() {
    return !isMsType();
  }

  @NotNull
  public String getAccession() {
    return accession;
  }

  public String getDescription() {
    return description;
  }
}
