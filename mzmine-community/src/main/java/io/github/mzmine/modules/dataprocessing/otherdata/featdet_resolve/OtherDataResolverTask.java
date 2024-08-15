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

package io.github.mzmine.modules.dataprocessing.otherdata.featdet_resolve;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.otherdectectors.OtherFeatureDataType;
import io.github.mzmine.datamodel.features.types.otherdectectors.RawTraceType;
import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeries;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeriesData;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolver;
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

public class OtherDataResolverTask extends AbstractSimpleTask {

  private final OtherTraceSelection traceSelection;

  protected OtherDataResolverTask(@Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, @NotNull ParameterSet parameters,
      @NotNull Class<? extends MZmineModule> moduleClass, MZmineProject project) {
    super(storage, moduleCallDate, parameters, moduleClass);

    traceSelection = parameters.getValue(OtherDataResolverParameters.otherTraces);
  }

  public static @NotNull List<OtherFeature> resolveFeatures(List<OtherFeature> inputData,
      MinimumSearchFeatureResolver resolver, MemoryMapStorage storage) {
    final List<OtherFeature> outputData = new ArrayList<>();

    for (OtherFeature feature : inputData) {
      final OtherTimeSeries timeSeries = feature.getFeatureData();

      double[] xBuffer = new double[timeSeries.getNumberOfValues()];
      double[] yBuffer = new double[timeSeries.getNumberOfValues()];

      for (int i = 0; i < timeSeries.getNumberOfValues(); i++) {
        xBuffer[i] = timeSeries.getRetentionTime(i);
      }
      yBuffer = timeSeries.getIntensityValues(yBuffer);

      final List<Range<Double>> ranges = resolver.resolve(xBuffer, yBuffer);
      final List<OtherTimeSeries> resolved = ranges.stream().map(
          range -> timeSeries.subSeries(storage, range.lowerEndpoint().floatValue(),
              range.upperEndpoint().floatValue())).toList();

      for (OtherTimeSeries series : resolved) {
        final OtherFeature resolvedFeature = feature.createSubFeature(series);
        resolvedFeature.set(OtherFeatureDataType.class, series);
        outputData.add(resolvedFeature);
      }
    }
    return outputData;
  }

  @Override
  protected void process() {

    if (!(parameters instanceof OtherDataResolverParameters otherParam)) {
      error("Resolving started with wrong parameter set instance.");
      return;
    }
    final ParameterSet resolverParam = otherParam.toResolverParameters();
    // we need a feature list and a raw file. it's more geared towards ims after the resolving,
    // but something needs to be set. What is set does not matter for the resolving of uv/other data.
    RawDataFileImpl dummyFile = new RawDataFileImpl("dummy", null, null);
    ModularFeatureList flist = new ModularFeatureList("dummy flist", null, dummyFile);

    final MinimumSearchFeatureResolver resolver = new MinimumSearchFeatureResolver(resolverParam,
        flist);
    final List<OtherTimeSeriesData> matchingData = traceSelection.getMatchingTimeSeriesData(
        ProjectService.getProject().getCurrentRawDataFiles());

    for (OtherTimeSeriesData data : matchingData) {
      final List<OtherFeature> inputData = traceSelection.getMatchingTraces(data);
      final List<OtherFeature> outputData = resolveFeatures(inputData, resolver,
          getMemoryMapStorage());

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
