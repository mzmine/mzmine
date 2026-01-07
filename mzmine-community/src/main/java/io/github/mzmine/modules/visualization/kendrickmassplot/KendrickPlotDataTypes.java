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

package io.github.mzmine.modules.visualization.kendrickmassplot;

public enum KendrickPlotDataTypes {

  MZ("m/z", "m/z"),//
  KENDRICK_MASS("Kendrick Mass", "KM"),//
  KENDRICK_MASS_DEFECT("Kendrick Mass Defect", "KMD"),//
  REMAINDER_OF_KENDRICK_MASS("Remainder of Kendrick Mass", "RKM"),//
  RETENTION_TIME("Retention Time", "rt"),//
  MOBILITY("Mobility", "mobility"),//
  INTENSITY("Intensity", "Int."),//
  AREA("Area", "Area"),//
  TAILING_FACTOR("Tailing Factor", "Tailing Factor"),//
  ASYMMETRY_FACTOR("Asymmetry Factor", "Asymmetry Factor"),//
  FWHM("FWHM", "FWHM");//

  private final String name;

  private final String abbr;

  KendrickPlotDataTypes(String name, String abbr) {
    this.name = name;
    this.abbr = abbr;
  }

  public String getName() {
    return name;
  }

  public String getAbbr() {
    return abbr;
  }

  @Override
  public String toString() {
    return this.getName();
  }

  public boolean isKendrickType() {
    return switch (this) {
      case KENDRICK_MASS, KENDRICK_MASS_DEFECT, REMAINDER_OF_KENDRICK_MASS -> true;
      default -> false;
    };
  }
}
