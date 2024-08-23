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

package io.github.mzmine.modules.dataprocessing.otherdata.filt_trimtraces;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeries;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeriesData;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.other_detectors.OtherTraceSelection;
import io.github.mzmine.taskcontrol.AbstractSimpleTask;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RangeUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TrimTracesTask extends AbstractSimpleTask {


  private final RawDataFile[] files;
  private final OtherTraceSelection selection;
  private final Range<Double> rtRange;

  protected TrimTracesTask(@Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      @NotNull ParameterSet parameters, @NotNull Class<? extends MZmineModule> moduleClass) {
    super(storage, moduleCallDate, parameters, moduleClass);

    files = parameters.getValue(TrimTracesParameters.files).getMatchingRawDataFiles();
    selection = parameters.getValue(TrimTracesParameters.traces);
    rtRange = parameters.getValue(TrimTracesParameters.rtRange);
  }

  @Override
  protected void process() {

    final List<OtherTimeSeriesData> timeSeriesDataList = selection.getMatchingTimeSeriesData(
        List.of(files));

    // Note: the current implementation will only trim features at are connected to the given range.
    // other features will not be altered
    for (OtherTimeSeriesData timeSeriesData : timeSeriesDataList) {
      final List<OtherFeature> matchingTraces = selection.getMatchingTraces(timeSeriesData);
      List<OtherFeature> trimmedFeatures = new ArrayList<>();
      for (OtherFeature trace : matchingTraces) {
        // skip traces that are not connected
        if (trace.getRtRange() == null || !rtRange.isConnected(
            RangeUtils.toDoubleRange(trace.getRtRange()))) {
          // not connected -> do not trim and just keep
          trimmedFeatures.add(trace);
          continue;
        }
        final OtherTimeSeries series = trace.getFeatureData();
        final OtherTimeSeries trimmed = series.subSeries(getMemoryMapStorage(),
            rtRange.lowerEndpoint().floatValue(), rtRange.upperEndpoint().floatValue());
        final OtherFeature trimmedFeature = trace.createSubFeature(trimmed);
        trimmedFeatures.add(trimmedFeature);
      }

      timeSeriesData.setPreprocessedTraces(trimmedFeatures);
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
    final NumberFormats formats = ConfigService.getGuiFormats();
    return "Trimming raw traces to " + formats.rt(rtRange);
  }
}
