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

package io.github.mzmine.modules.dataprocessing.otherdata.featdet_baseline;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.otherdectectors.RawTraceType;
import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeries;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeriesData;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.BaselineCorrector;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.BaselineCorrectors;
import io.github.mzmine.modules.dataprocessing.otherdata.featdet_resolve.OtherDataResolverParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.other_detectors.OtherTraceSelection;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.AbstractSimpleTask;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OtherDataBaselineCorrectionTask extends AbstractSimpleTask {

  private final OtherTraceSelection traceSelection;
  private final BaselineCorrector corrector;

  protected OtherDataBaselineCorrectionTask(@Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, @NotNull ParameterSet parameters,
      @NotNull Class<? extends MZmineModule> moduleClass, MZmineProject project) {
    super(storage, moduleCallDate, parameters, moduleClass);

    // we need a feature list and a raw file. it's more geared towards ims after the resolving,
    // but something needs to be set. What is set does not matter for the resolving of uv/other data.
    RawDataFileImpl dummyFile = new RawDataFileImpl("dummy", null, null);
    ModularFeatureList flist = new ModularFeatureList("dummy flist", null, dummyFile);

    traceSelection = parameters.getValue(OtherDataResolverParameters.otherTraces);
    final BaselineCorrectors value = parameters.getParameter(
        OtherDataBaselineCorrectionParameters.correctionAlgorithm).getValue();
    corrector = value.getModuleInstance().newInstance(
        parameters.getParameter(OtherDataBaselineCorrectionParameters.correctionAlgorithm)
            .getEmbeddedParameters(), null, flist);
  }

  public static @NotNull List<OtherFeature> correctBaseline(List<OtherFeature> inputData,
      BaselineCorrector corrector) {
    final List<OtherFeature> outputData = new ArrayList<>();

    for (OtherFeature feature : inputData) {
      final OtherTimeSeries timeSeries = feature.getFeatureData();

      final OtherTimeSeries corrected = corrector.correctBaseline(timeSeries);
      final OtherFeature correctedFeature = feature.createSubFeature(corrected);

      outputData.add(correctedFeature);
    }
    return outputData;
  }

  @Override
  protected void process() {
    final List<OtherTimeSeriesData> matchingData = traceSelection.getMatchingTimeSeriesData(
        ProjectService.getProject().getCurrentRawDataFiles());

    for (OtherTimeSeriesData data : matchingData) {
      final List<OtherFeature> inputData = traceSelection.getMatchingTraces(data);
      final List<OtherFeature> outputData = correctBaseline(inputData, corrector);

      // technically this is not needed, because all traces will be processed from a OtherTimeSeriesData,
      // but we might be changing to specific traces afterward.
      final Map<OtherFeature, List<OtherFeature>> groupedByRawTrace = outputData.stream()
          .collect(Collectors.groupingBy(f -> f.get(RawTraceType.class)));
      for (Entry<OtherFeature, List<OtherFeature>> grouped : groupedByRawTrace.entrySet()) {
        data.replaceProcessedFeaturesForTrace(grouped.getKey(), grouped.getValue());
      }
    }
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of();
  }

  @Override
  protected @NotNull List<RawDataFile> getProcessedDataFiles() {
    return List.of();
  }

  @Override
  public String getTaskDescription() {
    return "Resolving chromatograms for " + traceSelection.toString();
  }
}
