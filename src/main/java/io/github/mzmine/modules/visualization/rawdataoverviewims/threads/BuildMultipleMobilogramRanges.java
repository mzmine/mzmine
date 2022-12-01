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
