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

package io.github.mzmine.modules.io.import_rawdata_agilent_d;

import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.ChromatogramType;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLUnits;
import java.util.function.DoubleUnaryOperator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

enum AgilentAnalogUnit {
  UNKNOWN("unknown", MzMLUnits.UNKNOWN, ChromatogramType.UNKNOWN), //
  PRESSURE("pressure", MzMLUnits.BAR, ChromatogramType.PRESSURE), //
  ABSORBANCE("absorbance", MzMLUnits.ABSORBANCE, ChromatogramType.ABSORPTION), // to be changed
  FLOW("flow", MzMLUnits.MICRO_LITER_PER_MINUTE, ChromatogramType.FLOW_RATE), //
  PICO_AMPERE("pico ampere", MzMLUnits.PICO_AMPERE, ChromatogramType.UNKNOWN), // to be changed
  TEMPERATURE("temperature", MzMLUnits.DEGREE_CELSIUS, ChromatogramType.UNKNOWN), EMISSION(
      "emission", MzMLUnits.DIMENSIONLESS,
      ChromatogramType.EMISSION), // emission unknown - to be changed
  SOLVENT_RATIO("solvent ratio", MzMLUnits.UNKNOWN, ChromatogramType.UNKNOWN),
  ;

  final String description;
  final MzMLUnits unit;
  final ChromatogramType type;


  AgilentAnalogUnit(String description, MzMLUnits unit, ChromatogramType type) {
    this.description = description;
    this.unit = unit;
    this.type = type;
  }

  @Nullable
  public DoubleUnaryOperator getMapper() {
    return switch (this) {
      case FLOW -> value -> value * 1000; // agilent flow is mL/min, Unit ontology is uL/min
      default -> null;
    };
  }

  @NotNull
  static AgilentAnalogUnit fromDescription(@Nullable String description) {
    if (description == null) {
      return UNKNOWN;
    }

    for (var unit : values()) {
      if (unit.description.equalsIgnoreCase(description) || description.toLowerCase()
          .contains(unit.description)) {
        return unit;
      }
    }
    return UNKNOWN;
  }

  @NotNull
  static AgilentAnalogUnit fromDevice(AgilentAnalogDevice device) {
    return switch (device) {
      case TCC, ECD, NP, BP, QP, CP, LFP, IP, ELSD, FD, UIB2 -> UNKNOWN;
      case DAD, VWD, MWD -> ABSORBANCE;
      case FID -> PICO_AMPERE;
      case TCD -> TEMPERATURE;
      case RID -> SOLVENT_RATIO;
    };
  }
}
