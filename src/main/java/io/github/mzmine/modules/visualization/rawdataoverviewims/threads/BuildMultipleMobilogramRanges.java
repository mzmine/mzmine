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
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.impl.SummedIntensityMobilitySeries;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.SummedMobilogramXYProvider;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.IonMobilityUtils;
import io.github.mzmine.util.IonMobilityUtils.MobilogramType;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.NotNull;

public class BuildMultipleMobilogramRanges extends AbstractTask {

  private final List<Range<Double>> mzRanges;
  private final Set<Frame> frames;
  private final IMSRawDataFile file;
  private final Consumer<List<ColoredXYDataset>> onProcessingFinished;
  private final BinningMobilogramDataAccess binning;
  private double finishedPercentage;

  public BuildMultipleMobilogramRanges(@NotNull List<Range<Double>> mzRanges,
      @NotNull Set<Frame> frames, @NotNull IMSRawDataFile file,
      @NotNull Consumer<List<ColoredXYDataset>> onProcessingFinished,
      @NotNull BinningMobilogramDataAccess binning, @NotNull Date moduleCallDate) {
    super(null,
        Instant.now()); // no new data stored -> null, date is irrelevant (not used in batch mode)
    this.onProcessingFinished = onProcessingFinished;
    this.binning = binning;
    finishedPercentage = 0d;
    this.mzRanges = mzRanges;
    this.frames = frames;
    this.file = file;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    if (frames.stream().findAny().orElse(null) == null) {
      setStatus(TaskStatus.FINISHED);
      return;
    }

    List<ColoredXYDataset> mobilogramDataSets = new ArrayList<>();
    SimpleColorPalette colors = MZmineCore.getConfiguration().getDefaultColorPalette().clone();
    colors.remove(file.getColor());
    NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
    for (Range<Double> mzRange : mzRanges) {
      List<IonMobilitySeries> mobilograms = new ArrayList<>();
      for (Frame frame : frames) {
        final IonMobilitySeries mobilogram = IonMobilityUtils.buildMobilogramForMzRange(frame,
            mzRange, MobilogramType.TIC, null);
        mobilograms.add(mobilogram);
      }

      final String seriesKey =
          "m/z " + mzFormat.format(mzRange.lowerEndpoint()) + " - " + mzFormat.format(
              mzRange.upperEndpoint());
      if (!mobilograms.isEmpty()) {
        binning.setMobilogram(mobilograms);
        final SummedIntensityMobilitySeries summed = binning.toSummedMobilogram(null);
        if (summed.getNumberOfDataPoints() > 0) {
          SummedMobilogramXYProvider provider = new SummedMobilogramXYProvider(summed,
              new SimpleObjectProperty<>(colors.get(mzRanges.indexOf(mzRange))), seriesKey, true);
          ColoredXYDataset dataset = new ColoredXYDataset(provider, RunOption.THIS_THREAD);
          mobilogramDataSets.add(dataset);
        }
      }
      finishedPercentage = mzRanges.indexOf(mzRange) / (double) mzRanges.size();

      if (isCanceled()) {
        return;
      }
    }

    setStatus(TaskStatus.FINISHED);
    onProcessingFinished.accept(mobilogramDataSets);
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
