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

package io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ChromatogramBuilderAlgorithms;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.LegacyAdapChromatogramBuilderParameters;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.combowithinput.AutoOrCustomOption;
import io.github.mzmine.parameters.parametertypes.combowithinput.MZToleranceOrAuto;
import io.github.mzmine.parameters.parametertypes.combowithinput.MZToleranceOrAutoParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.ToleranceType;
import org.jetbrains.annotations.NotNull;

/**
 * Parameters of the {@link ChromatogramBuilderAlgorithms#FAST} algorithm.
 * <p>
 * decision: the names equal the {@link LegacyAdapChromatogramBuilderParameters}, the values
 * describe the same filters.
 */
public class FastChromatogramBuilderParameters extends SimpleParameterSet {

  public static final IntegerParameter minimumConsecutiveScans = new IntegerParameter(
      "Minimum consecutive scans", """
      This number of scans needs to be above the specified 'Minimum intensity for consecutive scans' to detect EICs.
      The optimal value depends on the chromatography system setup. The best way to set this parameter
      is by studying the raw data and determining what is the typical time span (number of data points) of chromatographic features.""",
      5, true, 1, null);

  public static final DoubleParameter minGroupIntensity = new DoubleParameter(
      "Minimum intensity for consecutive scans", """
      This threshold is only used to find consecutive scans (data points) above a certain intensity.
      All data points, even below this level can be added to a chromatogram but at least N consecutive scans need to be above.
      """, MZmineCore.getConfiguration().getIntensityFormat(), 0d, 0d, Double.MAX_VALUE);

  public static final DoubleParameter minHighestPoint = new DoubleParameter(
      "Minimum absolute height", """
      The consecutive scans need to reach this height. Signals below this intensity will not start a new chromatogram
      but can be added to an existing one.""", MZmineCore.getConfiguration().getIntensityFormat(),
      null, 0d, Double.MAX_VALUE);

  // decision: auto by default, the presets per instrument type are only rough guesses
  public static final MZToleranceOrAutoParameter mzTolerance = new MZToleranceOrAutoParameter(
      new MZToleranceParameter(ToleranceType.SCAN_TO_SCAN, """
          Auto estimates the tolerance from a few data files: from the m/z scatter of the same \
          signal in consecutive scans and from the m/z spread of the data points in a test build \
          of the chromatograms. The estimate is logged and stored with the feature lists.
          Custom uses the defined tolerance.""", 0.002, 10), AutoOrCustomOption.AUTO);

  public FastChromatogramBuilderParameters() {
    super(minimumConsecutiveScans, minGroupIntensity, minHighestPoint, mzTolerance);
  }

  @NotNull
  public static FastChromatogramBuilderParameters create(int minConsecutiveScans,
      double minGroupInt, double minHeight, @NotNull MZToleranceOrAuto mzTolScans) {
    final var param = new FastChromatogramBuilderParameters().cloneParameterSet();
    param.setParameter(minimumConsecutiveScans, minConsecutiveScans);
    param.setParameter(minGroupIntensity, minGroupInt);
    param.setParameter(minHighestPoint, minHeight);
    param.setParameter(mzTolerance, mzTolScans);
    return (FastChromatogramBuilderParameters) param;
  }
}
