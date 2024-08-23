/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.otherdata.align_msother;

import io.github.mzmine.main.ConfigService;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.other_detectors.OtherRawOrProcessed;
import io.github.mzmine.parameters.parametertypes.other_detectors.OtherTraceSelection;
import io.github.mzmine.parameters.parametertypes.other_detectors.OtherTraceSelectionParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import java.util.List;

public class MsOtherCorrelationParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flists = new FeatureListsParameter();

  public static final DoubleParameter minPearson = new DoubleParameter("Minimum correlation score",
      "Minimum pearson score for a correlation to be valid.",
      ConfigService.getGuiFormats().scoreFormat(), 0.8, 0d, 1d);

  public static final RTToleranceParameter rtTolerance = new RTToleranceParameter("RT tolerance",
      "Maximum allowed tolerance between the apex of the MS feature\nand the trace "
          + "of the other detector.", new RTTolerance(4, Unit.SECONDS));

  public static final OtherTraceSelectionParameter traces = new OtherTraceSelectionParameter(
      "Trace selection", "Select the type of other traces you want to correlate to.",
      OtherTraceSelection.featureUv(), List.of(OtherRawOrProcessed.FEATURES));

  public MsOtherCorrelationParameters() {
    super(flists, traces, rtTolerance, minPearson);
  }
}
