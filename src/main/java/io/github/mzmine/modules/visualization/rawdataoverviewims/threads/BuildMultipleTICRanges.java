/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
