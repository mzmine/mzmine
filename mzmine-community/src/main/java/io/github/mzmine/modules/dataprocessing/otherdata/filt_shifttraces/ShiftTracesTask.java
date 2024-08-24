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

package io.github.mzmine.modules.dataprocessing.otherdata.filt_shifttraces;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.featuredata.OtherFeatureUtils;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeriesData;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.other_detectors.OtherTraceSelection;
import io.github.mzmine.taskcontrol.AbstractSimpleTask;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ShiftTracesTask extends AbstractSimpleTask {


  private final RawDataFile[] files;
  private final OtherTraceSelection selection;
  private final float rtShift;

  protected ShiftTracesTask(@Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      @NotNull ParameterSet parameters, @NotNull Class<? extends MZmineModule> moduleClass) {
    super(storage, moduleCallDate, parameters, moduleClass);

    files = parameters.getValue(ShiftTracesParameters.files).getMatchingRawDataFiles();
    selection = parameters.getValue(ShiftTracesParameters.traces);
    rtShift = parameters.getValue(ShiftTracesParameters.rtShift).floatValue();
  }

  @Override
  protected void process() {

    final List<OtherTimeSeriesData> timeSeriesDataList = selection.getMatchingTimeSeriesData(
        List.of(files));

    for (OtherTimeSeriesData timeSeriesData : timeSeriesDataList) {
      final List<OtherFeature> matchingTraces = selection.getMatchingTraces(timeSeriesData);
      List<OtherFeature> shiftedFeatures = new ArrayList<>();
      for (OtherFeature trace : matchingTraces) {
        final OtherFeature shifted = OtherFeatureUtils.shiftRtAxis(getMemoryMapStorage(), trace,
            rtShift);
        shiftedFeatures.add(shifted);
      }
      timeSeriesData.setPreprocessedTraces(shiftedFeatures);
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
    return "Shifting traces by " + formats.rt(rtShift) + " min";
  }
}
