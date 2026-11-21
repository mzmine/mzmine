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

import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Taken from DeviceType from the MHDAC sdk.
 */
enum AgilentAnalogDevice {
  TCC("ThermostattedColumnCompartment"), //
  DAD("DiodeArrayDetector"),  //
  FID("FlameIonizationDetector"), //
  TCD("ThermalConductivityDetector"), //
  RID("RefractiveIndexDetector"), //
  MWD("MultiWavelengthDetector"), //
  VWD("VariableWavelengthDetector"), //
  ECD("ElectronCaptureDetector"), // some kind of current, but which order of magnitude?
  FD("FluorescenceDetector"), // No unit for emission yet?
  ELSD("EvaporativeLightScatteringDetector"), //
  IP("IsocraticPump"), //
  BP("BinaryPump"), //
  QP("QuarternaryPump"), //
  CP("CapillaryPump"), //
  NP("NanoPump"), //
  LFP("LowFlowPump"), //
  // "Universal interface box" (analog in, e.g. charged aerosol detector). The SDK reports UIB2,
  // the device is named UIB in the acquisition's Devices.xml
  UIB2("UIB2", "UIB"), //
  ;

  /**
   * enum name in agilent software
   */
  final String title;
  /**
   * other names of the device type, e.g. the device name in the raw file
   */
  private final List<String> aliases;

  AgilentAnalogDevice(String title, String... aliases) {
    this.title = title;
    this.aliases = List.of(aliases);
  }

  @Nullable
  public static AgilentAnalogDevice fromTitle(@Nullable String title) {
    if (title == null) {
      return null;
    }
    for (var device : values()) {
      if (device.title.equalsIgnoreCase(title) || device.aliases.stream()
          .anyMatch(title::equalsIgnoreCase)) {
        return device;
      }
    }
    return null;
  }
}
