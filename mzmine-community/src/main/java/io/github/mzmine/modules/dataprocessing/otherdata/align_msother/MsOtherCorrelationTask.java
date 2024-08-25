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

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.featuredata.IntensityTimeSeries;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.correlation.CorrelationData;
import io.github.mzmine.datamodel.features.types.numbers.RTRangeType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.features.types.otherdectectors.MsOtherCorrelationResultType;
import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeries;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.correlation.FeatureCorrelationUtil.DIA;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.other_detectors.OtherTraceSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractSimpleTask;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MsOtherCorrelationTask extends AbstractSimpleTask {

  private final FeatureList flist;
  private final Double minCorrelation;
  private final OtherTraceSelection traceSelection;
  private final RTTolerance rtTolerance;
  /**
   * Caches the traces of other time series for correlation since the shape may be needed for
   * multiple accesses.
   */
  private final Map<OtherTimeSeries, ExtractedShape> seriesShapeCache = new HashMap<>();

  /**
   * @param storage        The {@link MemoryMapStorage} used to store results of this task (e.g.
   *                       RawDataFiles, MassLists, FeatureLists). May be null if results shall be
   *                       stored in ram. For now, one storage should be created per module call in
   * @param moduleCallDate the call date of module to order execution order
   * @param parameters
   * @param moduleClass
   */
  protected MsOtherCorrelationTask(FeatureList flist, @Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, @NotNull ParameterSet parameters,
      @NotNull Class<? extends MZmineModule> moduleClass) {
    super(storage, moduleCallDate, parameters, moduleClass);
    this.flist = flist;
    minCorrelation = parameters.getValue(MsOtherCorrelationParameters.minPearson);
    traceSelection = parameters.getValue(MsOtherCorrelationParameters.traces);
    rtTolerance = parameters.getValue(MsOtherCorrelationParameters.rtTolerance);
    totalItems = flist.streamFeatures().count();
  }

  public static @NotNull ExtractedShape extractFeatureShape(IntensityTimeSeries series) {
    final int numValues = series.getNumberOfValues();
    final double[] intensities = new double[numValues];
    final double[] rts = new double[numValues];

    series.getIntensityValues(intensities);
    for (int i = 0; i < numValues; i++) {
      rts[i] = series.getRetentionTime(i);
    }
    return new ExtractedShape(numValues, rts, intensities);
  }

  public static @Nullable CorrelationData correlateMsFeatureToTrace(ExtractedShape msShape,
      ExtractedShape otherShape, double minCorrelation) {
    final CorrelationData correlationData = DIA.corrFeatureShape(msShape.rts(),
        msShape.intensities(), otherShape.rts(), otherShape.intensities(), 5, 2, 0);

    if (correlationData == null || correlationData.getPearsonR() < minCorrelation) {
      return null;
    }

    return correlationData;
  }

  @Override
  protected void process() {

    final ObservableList<RawDataFile> files = flist.getRawDataFiles();

    for (RawDataFile file : files) {
      // first check if we even have matching traces for this file
      final List<OtherFeature> otherFeatures = traceSelection.getMatchingTraces(List.of(file));
      if (otherFeatures.isEmpty()) {
        continue;
      }

      for (FeatureListRow row : flist.getRows()) {
        final ModularFeature feature = (ModularFeature) row.getFeature(file);
        if (feature == null || feature.getFeatureStatus() == FeatureStatus.UNKNOWN) {
          finishedItems.getAndIncrement();
          continue;
        }

        final Float rt = feature.getRT();
        final List<OtherFeature> matchingTraces = otherFeatures.stream().filter(
            t -> t.get(RTType.class) != null && rtTolerance.checkWithinTolerance(rt,
                t.get(RTType.class))).toList();

        final ExtractedShape msShape = extractFeatureShape(feature.getFeatureData());
        final var msRtRange = feature.getRawDataPointsRTRange();

        List<MsOtherCorrelationResult> correlationResults = new ArrayList<>();
        for (OtherFeature matchingTrace : matchingTraces) {
          if (matchingTrace.get(RTRangeType.class) == null || !msRtRange.isConnected(
              matchingTrace.get(RTRangeType.class))) {
            continue;
          }
          final MsOtherCorrelationResult correlationResult = correlateMsFeatureToTrace(
              matchingTrace, msShape);

          if (correlationResult == null) {
            continue;
          }

          correlationResults.add(correlationResult);
        }

        if (!correlationResults.isEmpty()) {
          feature.set(MsOtherCorrelationResultType.class, correlationResults);
        }
        finishedItems.getAndIncrement();
      }
    }
  }

  @Nullable
  private MsOtherCorrelationResult correlateMsFeatureToTrace(OtherFeature matchingTrace,
      ExtractedShape msShape) {
    final OtherTimeSeries otherFeatureData = matchingTrace.getFeatureData();
    final ExtractedShape otherShape = seriesShapeCache.computeIfAbsent(otherFeatureData,
        MsOtherCorrelationTask::extractFeatureShape);

    var correlation = correlateMsFeatureToTrace(msShape, otherShape, minCorrelation);
    if (correlation != null) {
      return new MsOtherCorrelationResult(matchingTrace, MsOtherCorrelationType.CALCULATED);
    }
    return null;
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(flist);
  }

  @Override
  protected @NotNull List<RawDataFile> getProcessedDataFiles() {
    // at the time of project loading, no feature list exists so the batch would fail. So dont add to files
    return List.of();
  }

  @Override
  public String getTaskDescription() {
    return "Checking feature list for correlated features from other detector traces.";
  }

  public record ExtractedShape(int numValues, double[] rts, double[] intensities) {

  }
}
