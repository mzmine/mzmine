
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

package io.github.mzmine.modules.tools.timstofmaldiacq.imaging;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.tools.timstofmaldiacq.imaging.acquisitionwriters.MaldiMs2AcquisitionWriters;
import io.github.mzmine.modules.tools.timstofmaldiacq.imaging.acquisitionwriters.SingleSpotMs2Writer;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter;
import java.text.DecimalFormat;

public class AdvancedImageMsMsParameters extends SimpleParameterSet {

  public static final SingleSpotMs2Writer single = MZmineCore.getModuleInstance(
      SingleSpotMs2Writer.class);

  public static final OptionalParameter<ModuleOptionsEnumComboParameter<MaldiMs2AcquisitionWriters>> ms2ImagingMode = new OptionalParameter<>(
      new ModuleOptionsEnumComboParameter<>("MS2 acquisition mode", "",
          MaldiMs2AcquisitionWriters.SINGLE_SPOT));

  public static final double MIN_MOBILITY_WIDTH = 0.02;
  public static final OptionalParameter<DoubleParameter> minMobilityWidth = new OptionalParameter<>(
      new DoubleParameter("Minimum mobility window", """
          Minimum width of the mobility isolation window.
          The default value is 0.02.
          """, new DecimalFormat("0.000"), MIN_MOBILITY_WIDTH), false);
  public static final double MAX_MOBILITY_WIDTH = 0.04;
  public static final OptionalParameter<DoubleParameter> maxMobilityWidth = new OptionalParameter<>(
      new DoubleParameter("Maximum mobility window", """
          Maximum width of the mobility isolation window.
          The default value is 0.04.
          """, new DecimalFormat("0.000"), MAX_MOBILITY_WIDTH), false);
  public static final double QUAD_SWITCH_TIME = 1.65d;
  public static final OptionalParameter<DoubleParameter> quadSwitchTime = new OptionalParameter<>(
      new DoubleParameter("Quadrupole switch time (ms)", """
          Minimum jump time for the quad to jump between two precursors.
          The default (1.65 ms) is computed from the ramp time and acquisition mobility range and should be widely applicable.
          """, new DecimalFormat("0.00"), QUAD_SWITCH_TIME), false);
  public static final double MIN_ISOLATION_WIDTH = 1.7d;
  public static final OptionalParameter<DoubleParameter> isolationWidth = new OptionalParameter<>(
      new DoubleParameter("Isolation width", "The isolation width for precursors. (Default = 1.7)",
          new DecimalFormat("0.0"), MIN_ISOLATION_WIDTH));

  public AdvancedImageMsMsParameters() {
    super(quadSwitchTime, isolationWidth, minMobilityWidth, maxMobilityWidth, ms2ImagingMode);
  }
}
