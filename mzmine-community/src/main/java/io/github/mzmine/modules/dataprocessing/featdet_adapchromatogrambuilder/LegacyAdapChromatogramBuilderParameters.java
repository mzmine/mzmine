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

package io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.ToleranceType;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Parameters of the {@link ChromatogramBuilderAlgorithms#LEGACY_ADAP} algorithm, the ADAP
 * chromatogram builder of mzmine before 4.11.
 * <p>
 * decision: the names equal the parameters of the module before the algorithm selection, old batch
 * steps load into this parameter set, see {@link ADAPChromatogramBuilderParameters}. When changing
 * any of the names, reflect the changes in the
 * {@link io.github.mzmine.modules.dataprocessing.featdet_imagebuilder.ImageBuilderParameters} to
 * keep the compatibility.
 */
public class LegacyAdapChromatogramBuilderParameters extends SimpleParameterSet {

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
      """, MZmineCore.getConfiguration().getIntensityFormat(), 0d);

  public static final DoubleParameter minHighestPoint = new DoubleParameter(
      "Minimum absolute height",
      "Points below this intensity will not be considered in starting a new chromatogram",
      MZmineCore.getConfiguration().getIntensityFormat());

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter(
      ToleranceType.SCAN_TO_SCAN, 0.002, 10);

  public LegacyAdapChromatogramBuilderParameters() {
    super(minimumConsecutiveScans, minGroupIntensity, minHighestPoint, mzTolerance);
  }

  @NotNull
  public static LegacyAdapChromatogramBuilderParameters create(int minConsecutiveScans,
      double minGroupInt, double minHeight, @NotNull MZTolerance mzTolScans) {
    final var param = new LegacyAdapChromatogramBuilderParameters().cloneParameterSet();
    param.setParameter(minimumConsecutiveScans, minConsecutiveScans);
    param.setParameter(minGroupIntensity, minGroupInt);
    param.setParameter(minHighestPoint, minHeight);
    param.setParameter(mzTolerance, mzTolScans);
    return (LegacyAdapChromatogramBuilderParameters) param;
  }

  @Override
  public Map<String, Parameter<?>> getNameParameterMap() {
    // parameters were renamed but stayed the same type
    final var nameParameterMap = super.getNameParameterMap();
    nameParameterMap.putAll(legacyNames(this));
    return nameParameterMap;
  }

  /**
   * Names of the parameters in old batch files.
   *
   * @param parameters the legacy parameters that receive the values
   */
  @NotNull
  static Map<String, Parameter<?>> legacyNames(@NotNull ParameterSet parameters) {
    return Map.of( //
        "Min group size in # of scans", parameters.getParameter(minimumConsecutiveScans), //
        "Group intensity threshold", parameters.getParameter(minGroupIntensity), //
        "Min highest intensity", parameters.getParameter(minHighestPoint), //
        "Scan to scan accuracy (m/z)", parameters.getParameter(mzTolerance));
  }
}
