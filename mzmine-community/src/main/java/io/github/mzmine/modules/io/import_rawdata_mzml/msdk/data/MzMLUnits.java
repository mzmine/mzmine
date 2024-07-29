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
import org.jetbrains.annotations.Nullable;

public enum MzMLUnits {
  ABSORBANCE(MzMLCV.cvUnitsAbsorbance, "", "Absorbance"), AREA(MzMLCV.cvUnitsAreaUnit, "",
      "Area"), COUNT(MzMLCV.cvUnitsCount, "count", "Signals"), DALTON(MzMLCV.cvUnitsDalton, "Da",
      "Mass"), DEGREE(MzMLCV.cvUnitsDegree, "°", "Angle"), DEGREE_CELSIUS(
      MzMLCV.cvUnitsDegreeCelsius, "°C", "Temperature"), DEGREE_KELVIN(MzMLCV.cvUnitsDegreeKelvin,
      "°K", "Temperature"), DIMENSIONLESS(MzMLCV.cvUnitsDimensionlessUnit, "",
      "Dimension less"), ELECTRON_VOLT(MzMLCV.cvUnitsElectronVolt, "eV", "Energy"), GRAM(
      MzMLCV.cvUnitsGram, "g", "Mass"), GRAM_PER_LITER(MzMLCV.cvUnitsGramsPerLiter, "(g/L)",
      "Concentraion"), HERTZ(MzMLCV.cvUnitsHertz, "Hz", "Frequency"), COUNTS(
      MzMLCV.cvUnitsIntensity1, "counts", "Intensity"), COUNTS_PER_SECOND(MzMLCV.cvUnitsIntensity2,
      "cps", "Intensity"), JOULE(MzMLCV.cvUnitsJoule, "J", "Energy"), KILO_DALTON(
      MzMLCV.cvUnitsKiloDalton, "kDa", "Mass"), METER(MzMLCV.cvUnitsMeter, "m",
      "Length"), MICRO_LITER_PER_MINUTE(MzMLCV.cvUnitsMicroLiterPerMinute, "(µL/min)",
      "Flow rate"), MICROMETER(MzMLCV.cvUnitsMicrometer, "µm", "Length"), MILLI_LITER(
      MzMLCV.cvUnitsMilliliter, "mL", "Volume"), MINUTES(MzMLCV.cvUnitsMin2, "min",
      "Time"), NANOMETER(MzMLCV.cvUnitsNanometer, "nm", "Length"), NANOSECOND(
      MzMLCV.cvUnitsNanosecond, "ns", "Time"), PARTS_PER_NOTATION_UNIT(
      MzMLCV.cvUnitsPartsPerNotationUnit, "", "Unknown"), PASCAL(MzMLCV.cvUnitsPascal, "Pa",
      "Pressure"), PERCENT(MzMLCV.cvUnitsPercent, "%", "Fraction"), PPM(MzMLCV.cvUnitsPPM, "ppm",
      "Concentration"), SECONDS(MzMLCV.cvUnitsSec, "s", "Time"), SQUARE_ANGSTROM(
      MzMLCV.cvUnitsSquareAngstrom, "\u212B\u00B2", "Surface area"), TESLA(MzMLCV.cvUnitsTesla, "T",
      "Magnetic flux density"), VOLT(MzMLCV.cvUnitsVolt, "V", "Voltage"), VOLT_PER_METER(
      MzMLCV.cvUnitsVoltPerMeter, "(V/m)", "Electromagnetic field strength"), UNKNOWN("unknown", "",
      "Unknown");

  private static final Map<String, MzMLUnits> map = Arrays.stream(values())
      .collect(Collectors.toMap(MzMLUnits::getAccession, cv -> cv));
  private final String accession;
  private final String sign;
  private final String label;

  MzMLUnits(@NotNull String accession, @NotNull String sign, String label) {

    this.accession = accession;
    this.sign = sign;
    this.label = label;
  }

  public static @NotNull MzMLUnits ofAccession(@Nullable String accession) {
    if (accession == null) {
      return UNKNOWN;
    }
    return map.getOrDefault(accession, UNKNOWN);
  }

  public String getAccession() {
    return accession;
  }

  public String getSign() {
    return sign;
  }

  public String getLabel() {
    return label;
  }
}
