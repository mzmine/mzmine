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


package io.github.mzmine.modules.dataprocessing.norm_rtcalibration2;

import static io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.ScanRtCorrectionTask.createMoreThanOneFileMessage;
import static io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.ScanRtCorrectionTask.createUnsatisfiedSampleFilterMessage;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.methods.RtCorrectionFunctions;
import io.github.mzmine.modules.visualization.projectmetadata.SampleType;
import io.github.mzmine.modules.visualization.projectmetadata.SampleTypeFilter;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterDialogWithPreviewPanes;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.CheckComboParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.StringUtils;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import javafx.application.Platform;

public class RTCorrectionParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter(1);

  public static final MZToleranceParameter MZTolerance = new MZToleranceParameter(0.01, 15);

  public static final RTToleranceParameter RTTolerance = new RTToleranceParameter(
      "Retention time tolerance", "Maximum allowed difference between two retention time values",
      new RTTolerance(0.03f, Unit.MINUTES));

  public static final DoubleParameter minHeight = new DoubleParameter("Minimum standard intensity",
      "Minimum height of a feature to be selected as standard for RT correction",
      MZmineCore.getConfiguration().getIntensityFormat());

  public static final CheckComboParameter<SampleType> sampleTypes = new CheckComboParameter<>(
      "Reference samples", """
      Select all sample types that shall be used to calculate the recalibration from.
      The recalibration of all other samples will be based on the acquisition order, which is
      determined by the acquisition type column in the metadata (CTRL/CMD + M).
      """, SampleType.values(), List.of(SampleType.values()));

  public static final ComboParameter<RTMeasure> rtMeasure = new ComboParameter<>(
      "RT standard calculation",
      "Specify how the standard RT shall be calculated, either by %s or %s.".formatted(
          StringUtils.inQuotes(RTMeasure.MEDIAN.toString()),
          StringUtils.inQuotes(RTMeasure.AVERAGE.toString())), RTMeasure.values(),
      RTMeasure.MEDIAN);

  public static final ModuleOptionsEnumComboParameter<RtCorrectionFunctions> calibrationFunctionModule = new ModuleOptionsEnumComboParameter<>(
      "Correction method", "Choose the RT correction method you want to apply",
      RtCorrectionFunctions.values(), RtCorrectionFunctions.MultiLinearCorrection);

  public RTCorrectionParameters() {
    super(new Parameter[]{featureLists, sampleTypes, MZTolerance, RTTolerance, minHeight, rtMeasure,
            calibrationFunctionModule},
        "https://mzmine.github.io/mzmine_documentation/module_docs/norm_rt_calibration/norm_rt_calibration.html");
  }

  @Override
  public boolean checkParameterValues(Collection<String> errorMessages,
      boolean skipRawDataAndFeatureListParameters) {

    if (!skipRawDataAndFeatureListParameters) {
      var flists = Arrays.stream(
              getValue(RTCorrectionParameters.featureLists).getMatchingFeatureLists())
          .sorted(Comparator.comparing(FeatureList::getNumberOfRows)).map(FeatureList.class::cast)
          .toList();

      final List<FeatureList> flistsWithMoreThanOneFile = flists.stream()
          .filter(fl -> fl.getNumberOfRawDataFiles() > 1).toList();
      if (!flistsWithMoreThanOneFile.isEmpty()) {
        errorMessages.add(createMoreThanOneFileMessage(flistsWithMoreThanOneFile));
      }

      var sampleTypeFilter = new SampleTypeFilter(getValue(RTCorrectionParameters.sampleTypes));
      final List<FeatureList> referenceFlists = flists.stream()
          .filter(flist -> flist.getRawDataFiles().stream().allMatch(sampleTypeFilter::matches))
          .sorted(Comparator.comparingInt(FeatureList::getNumberOfRows)).toList();
      if (referenceFlists.isEmpty()) {
        errorMessages.add(createUnsatisfiedSampleFilterMessage(sampleTypeFilter, flists));
      }
    }

    return errorMessages.isEmpty();
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    assert Platform.isFxApplicationThread();

    if ((parameters == null) || (parameters.length == 0)) {
      return ExitCode.OK;
    }
    final ParameterSetupDialog dialog = new ParameterDialogWithPreviewPanes(valueCheckRequired,
        this, null, ScanRtCorrectionPreviewPane::new, false);
    dialog.showAndWait();
    return dialog.getExitCode();
  }
}
