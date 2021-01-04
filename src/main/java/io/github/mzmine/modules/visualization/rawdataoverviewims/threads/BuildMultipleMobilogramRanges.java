/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.modules.visualization.rawdataoverviewims.threads;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.impl.SimpleMobilogram;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.FastColoredXYDataset;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_mobilogramsmoothing.PreviewMobilogram;
import io.github.mzmine.modules.visualization.rawdataoverviewims.IMSRawDataOverviewPane;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MobilogramUtils;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javafx.application.Platform;
import javax.annotation.Nonnull;

public class BuildMultipleMobilogramRanges extends AbstractTask {

  private final List<Range<Double>> mzRanges;
  private final Set<Frame> frames;
  private final IMSRawDataOverviewPane pane;
  private final IMSRawDataFile file;
  private double finishedPercentage;

  public BuildMultipleMobilogramRanges(@Nonnull List<Range<Double>> mzRanges,
      @Nonnull Set<Frame> frames, @Nonnull IMSRawDataFile file,
      @Nonnull IMSRawDataOverviewPane pane) {
    finishedPercentage = 0d;
    this.mzRanges = mzRanges;
    this.frames = frames;
    this.pane = pane;
    this.file = file;
  }

  @Override
  public void run() {
    Frame frame = frames.stream().findAny().orElse(null);
    if (frame == null) {
      setStatus(TaskStatus.FINISHED);
      return;
    }
    List<FastColoredXYDataset> mobilogramDataSets = new ArrayList<>();
    SimpleColorPalette colors = MZmineCore.getConfiguration().getDefaultColorPalette().clone();
    colors.remove(file.getColor());
    NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
    for (Range<Double> mzRange : mzRanges) {
      SimpleMobilogram mobilogram = MobilogramUtils.buildMobilogramForMzRange(frames, mzRange);
      final String seriesKey =
          "m/z " + mzFormat.format(mzRange.lowerEndpoint()) + " - " + mzFormat
              .format(mzRange.upperEndpoint());
      if (mobilogram != null) {
        PreviewMobilogram prev = new PreviewMobilogram(mobilogram, seriesKey, true);
        FastColoredXYDataset dataset = new FastColoredXYDataset(prev);
        dataset.setColor(colors.getAWT(mzRanges.indexOf(mzRange)));
        mobilogramDataSets.add(dataset);
      }
      finishedPercentage = mzRanges.indexOf(mzRange) / (double) mzRanges.size();
    }
    setStatus(TaskStatus.FINISHED);
    Platform
        .runLater(() -> pane.addMobilogramRangesToChart(mobilogramDataSets));
  }

  @Override
  public String getTaskDescription() {
    return "Building mobilograms and tic data sets for Ims raw data overview.";
  }

  @Override
  public double getFinishedPercentage() {
    return finishedPercentage;
  }
}
