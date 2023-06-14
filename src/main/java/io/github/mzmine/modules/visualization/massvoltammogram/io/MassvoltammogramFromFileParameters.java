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

package io.github.mzmine.modules.visualization.massvoltammogram.io;


import io.github.mzmine.modules.visualization.massvoltammogram.utils.ReactionMode;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import io.github.mzmine.parameters.parametertypes.ranges.MZRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import java.text.DecimalFormat;

public class MassvoltammogramFromFileParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter files = new RawDataFilesParameter(1, 1);

  public static final ScanSelectionParameter scanSelection = new ScanSelectionParameter(
      "Scan Selection", "Filter to choose the scans to be used.", new ScanSelection());

  public static final DoubleParameter delayTime = new DoubleParameter("Delay Time / s",
      "Delay time before analytes from the EC cell reach the mass spectrometer",
      new DecimalFormat("0.0"), 30d);

  public static final DoubleParameter potentialRampSpeed = new DoubleParameter(
      "Potential Ramp / mV/s", "Potential ramp speed in mV/s.", new DecimalFormat("0.0"), 10d);

  public static final DoubleParameter stepSize = new DoubleParameter("Potential Steps / mV",
      "Potential step between drawn Spectra.", new DecimalFormat("0.0"), 100d);

  public static final DoubleRangeParameter potentialRange = new DoubleRangeParameter(
      "Potential Range / mV", "Minimal and maximal potential of ramp.", new DecimalFormat("0.0"));

  public static final MZRangeParameter mzRange = new MZRangeParameter("m/z Range",
      "Minimal and maximal m/z.");

  public static final ComboParameter<ReactionMode> reactionMode = new ComboParameter<>(
      "Reaction Mode", "Reaction mode of the experiment.", ReactionMode.values(),
      ReactionMode.OXIDATIVE);

  public MassvoltammogramFromFileParameters() {
    super(new Parameter[]{files, scanSelection, reactionMode, delayTime, potentialRampSpeed,
        potentialRange, stepSize, mzRange});
  }
}

