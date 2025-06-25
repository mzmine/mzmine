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

package io.github.mzmine.modules.io.import_rawdata_bruker_uv;

import org.jetbrains.annotations.NotNull;

/**
 * package private conversion utils for bruker chromatography units to string.
 */
class BrukerUtils {

  static @NotNull String unitToString(Integer unit) {
    if (unit == null) {
      return "Unknown";
    }
    return switch (unit) {
      case 7 -> "Unknown";
      case 8 -> "AU";
      case 9 -> "mAU";
      case 10 -> "Counts";
      case 11 -> "A";
      case 12 -> "mA";
      case 13 -> "µA";
      case 43 -> "J";
      case 44 -> "mJ";
      case 45 -> "µJ";
      case 14 -> "mL/min";
      case 2 -> "µL/min";
      case 15 -> "nL/min";
      case 6 -> "Unknown";
      case 16 -> "cm";
      case 17 -> "mm";
      case 18 -> "µm";
      case 1 -> "nm";
      case 46 -> "Å";
      case 20 -> "Unknown";
      case 21 -> "mM";
      case 4 -> "%";
      case 22 -> "W";
      case 23 -> "mW";
      case 3 -> "bar";
      case 24 -> "mbar";
      case 25 -> "kPa";
      case 26 -> "MPa";
      case 27 -> "psi";
      case 28 -> "Unknown";
      case 5 -> "°C";
      case 30 -> "°F";
      case 31 -> "h";
      case 32 -> "min";
      case 33 -> "s";
      case 34 -> "ms";
      case 35 -> "µs";
      case 36 -> "cP";
      case 37 -> "kV";
      case 38 -> "V";
      case 39 -> "mV";
      case 40 -> "L";
      case 41 -> "mL";
      case 42 -> "µL";
      default -> "Unknown";
    };
  }

  static @NotNull String unitToLabel(Integer unit) {
    if (unit == null) {
      return "Unknown";
    }
    return switch (unit) {
      case 7 -> "Unknown";
      case 8 -> "Absorbance";
      case 9 -> "Absorbance";
      case 10 -> "Counts";
      case 11 -> "Current";
      case 12 -> "Current";
      case 13 -> "Current";
      case 43 -> "Energy";
      case 44 -> "Energy";
      case 45 -> "Energy";
      case 14 -> "Flow";
      case 2 -> "Flow";
      case 15 -> "Flow";
      case 6 -> "Intensity";
      case 16 -> "Length";
      case 17 -> "Length";
      case 18 -> "Length";
      case 1 -> "Length";
      case 46 -> "Length";
      case 20 -> "Luminescence";
      case 21 -> "Molarity";
      case 4 -> "Percent";
      case 22 -> "Power";
      case 23 -> "Power";
      case 3 -> "Pressure";
      case 24 -> "Pressure";
      case 25 -> "Pressure";
      case 26 -> "Pressure";
      case 27 -> "Pressure";
      case 28 -> "Refractive index";
      case 5 -> "Temperature";
      case 30 -> "Temperature";
      case 31 -> "Time";
      case 32 -> "Time";
      case 33 -> "Time";
      case 34 -> "Time";
      case 35 -> "Time";
      case 36 -> "Viscosity";
      case 37 -> "Voltage";
      case 38 -> "Voltage";
      case 39 -> "Voltage";
      case 40 -> "Volume";
      case 41 -> "Volume";
      case 42 -> "Volume";
      default -> "Unknown";
    };
  }
}
