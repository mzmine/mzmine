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

package io.github.mzmine.modules.visualization.rawdataoverviewims.threads;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.chromatogram.TICDataSet;
import io.github.mzmine.modules.visualization.rawdataoverviewims.IMSRawDataOverviewPane;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.awt.Color;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;

public class BuildMultipleTICRanges extends AbstractTask {

  private final List<Range<Double>> mzRanges;
  private final IMSRawDataOverviewPane pane;
  private final IMSRawDataFile file;
  private final ScanSelection scanSelection;
  private double finishedPercentage;

  public BuildMultipleTICRanges(@NotNull List<Range<Double>> mzRanges, @NotNull IMSRawDataFile file,
      @NotNull ScanSelection scanSelection,
      @NotNull IMSRawDataOverviewPane pane) {
    super(null, Instant.now()); // no new data stored -> null, date is irrelevant (not used in batch mode)
    finishedPercentage = 0d;
    this.mzRanges = mzRanges;
    this.pane = pane;
    this.file = file;
    this.scanSelection = scanSelection;
  }

  @Override
  public String getTaskDescription() {
    return "Setting up EIC dataset calculations.";
  }

  @Override
  public double getFinishedPercentage() {
    return finishedPercentage;
  }

  @Override
  public void run() {
    List<TICDataSet> ticDataSets = new ArrayList<>();
    List<Color> ticDataSeColors = new ArrayList<>();
    SimpleColorPalette colors = MZmineCore.getConfiguration().getDefaultColorPalette().clone();
    colors.remove(file.getColor());
    NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
    for (Range<Double> mzRange : mzRanges) {
      final String seriesKey =
          "m/z " + mzFormat.format(mzRange.lowerEndpoint()) + " - " + mzFormat
              .format(mzRange.upperEndpoint());
      TICDataSet ticDataSet = new TICDataSet(file, scanSelection.getMatchingScans(file),
          mzRange, null);
      ticDataSets.add(ticDataSet);
      ticDataSet.setCustomSeriesKey(seriesKey);
      ticDataSeColors.add(colors.getAWT(mzRanges.indexOf(mzRange)));
      finishedPercentage = mzRanges.indexOf(mzRange) / (double) mzRanges.size();
    }
    setStatus(TaskStatus.FINISHED);
    Platform
        .runLater(() -> pane.setTICRangesToChart(ticDataSets, ticDataSeColors));
  }
}
