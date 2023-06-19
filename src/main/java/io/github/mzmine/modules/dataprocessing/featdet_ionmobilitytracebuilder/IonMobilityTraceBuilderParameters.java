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

package io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ParameterSetParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.ToleranceType;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public class IonMobilityTraceBuilderParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter rawDataFiles = new RawDataFilesParameter();

  public static final ScanSelectionParameter scanSelection = new ScanSelectionParameter(
      ScanSelectionParameter.DEFAULT_NAME,
      "Filter scans based on their properties. Different noise levels ( -> mass "
          + "lists) are recommended for MS1 and MS/MS scans", new ScanSelection());

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter(
      ToleranceType.SCAN_TO_SCAN,
      "m/z tolerance between mobility scans to be assigned to the same mobilogram", 0.005, 10,
      false);

  public static final IntegerParameter minDataPointsRt = new IntegerParameter(
      "Minimum consecutive scans (RT)",
      "Minimum number of consecutive time resolved data points in an ion mobility trace."
          + " In other words, chromatographic peak width in number of data points", 7);

  public static final IntegerParameter minTotalSignals = new IntegerParameter(
      "Minimum total Signals", "Minimum number of signals (data points) in an ion mobility trace",
      200);

  public static final StringParameter suffix = new StringParameter("Suffix",
      "This string is added to filename as suffix", "ionmobilitytrace");

  public static final ParameterSetParameter advancedParameters = new ParameterSetParameter(
      "Advanced parameters", "Allows adjustment of internal binning parameters for mobilograms",
      new AdvancedImsTraceBuilderParameters());

  public IonMobilityTraceBuilderParameters() {
    super(
        new Parameter[]{rawDataFiles, scanSelection, mzTolerance, minDataPointsRt, minTotalSignals,
            suffix, advancedParameters},
        "https://mzmine.github.io/mzmine_documentation/module_docs/lc-ims-ms_featdet/featdet_ion_mobility_trace_builder/ion-mobility-trace-builder.html");
  }

  @Override
  public Map<String, Parameter<?>> getNameParameterMap() {
    // parameters were renamed but stayed the same type
    var nameParameterMap = super.getNameParameterMap();
    // we use the same parameters here so no need to increment the version. Loading will work fine
    nameParameterMap.put("Scan selection", scanSelection);
    nameParameterMap.put("m/z tolerance", mzTolerance);
    nameParameterMap.put("Minimum consecutive retention time data points", minDataPointsRt);
    return nameParameterMap;
  }

  @NotNull
  @Override
  public IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.ONLY;
  }
}
