/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.chromatogramandspectra;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.BuildingIonSeries;
import io.github.mzmine.datamodel.featuredata.impl.BuildingIonSeries.IntensityMode;
import io.github.mzmine.datamodel.featuredata.impl.BuildingIonSeries.MzMode;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.modules.dataprocessing.featdet_extract_mz_ranges.ExtractMzRangesIonSeriesFunction;
import io.github.mzmine.modules.visualization.chromatogram.MzRangeEicDataSet;
import io.github.mzmine.modules.visualization.chromatogram.TICPlot;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Calculates The feature data sets in a new thread to safe perfomance and not make the gui freeze.
 * Nested class because it uses the {@link ChromatogramAndSpectraVisualizer#chromPlot} member.
 */
public class FeatureDataSetCalc extends AbstractTask {

  public static final Logger logger = Logger.getLogger(FeatureDataSetCalc.class.getName());

  private final Collection<RawDataFile> rawDataFiles;
  private final List<Range<Double>> mzRangesSorted;
  private final AtomicInteger doneFiles = new AtomicInteger(0);
  private final TICPlot chromPlot;
  private final ScanSelection scanSelection;
  private final MzMode mzMode;
  private final IntensityMode intensityMode;
  private final ScanDataType scanDataType;


  public FeatureDataSetCalc(final Collection<RawDataFile> rawDataFiles, final Range<Double> mzRange,
      ScanSelection scanSelection, TICPlot chromPlot, ScanDataType scanDataType) {
    this(rawDataFiles, mzRange, scanSelection, chromPlot, scanDataType, MzMode.DEFAULT,
        IntensityMode.DEFAULT);
  }

  public FeatureDataSetCalc(final Collection<RawDataFile> rawDataFiles, final Range<Double> mzRange,
      ScanSelection scanSelection, TICPlot chromPlot, ScanDataType scanDataType, MzMode mzMode,
      IntensityMode intensityMode) {
    super(null, Instant.now()); // no new data stored -> null, date irrelevant (not used in batch)
    this.rawDataFiles = rawDataFiles;
    this.mzRangesSorted = List.of(mzRange);
    this.chromPlot = chromPlot;
    this.scanSelection = scanSelection;
    this.mzMode = mzMode;
    this.intensityMode = intensityMode;
    this.scanDataType = scanDataType;
  }

  @Override
  public String getTaskDescription() {
    return "Calculating " + mzRangesSorted.size() + " base peak chromatogram(s) in "
        + rawDataFiles.size() + " files.";
  }

  @Override
  public double getFinishedPercentage() {
    // + 1 because we count the generation of the data sets, too.
    return ((double) doneFiles.get() / (rawDataFiles.size() + 1));
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    logger.info(getTaskDescription());
    // extract all IonSeries at once
    final List<ColoredXYDataset> datasets = rawDataFiles.stream().parallel()
        .map(this::extractDataset).flatMap(Collection::stream).toList();

    if (datasets.isEmpty()) {
      setStatus(TaskStatus.FINISHED);
      return;
    }

    // set datasets to plot
    FxThread.runLater(() -> {
      if (isCanceled()) {
        return;
      }
      logger.info("Adding EIC datasets to plot");
      chromPlot.applyWithNotifyChanges(false, true, () -> {
        chromPlot.removeAllDataSetsOf(MzRangeEicDataSet.class, false);
        chromPlot.addDataSets(datasets);
      });
    });

    setStatus(TaskStatus.FINISHED);
  }

  /**
   * @return list of datasets for each mzRange or empty if interrupted or empty
   */
  @NotNull
  public List<ColoredXYDataset> extractDataset(final RawDataFile dataFile) {
    if (isCanceled()) {
      doneFiles.incrementAndGet();
      return List.of();
    }

    List<Scan> scans = scanSelection.getMatchingScans(dataFile.getScans());

    var extractFunction = new ExtractMzRangesIonSeriesFunction(dataFile, scans, mzRangesSorted,
        scanDataType, this);
    extractFunction.setMzMode(mzMode);
    extractFunction.setIntensityMode(intensityMode);

    BuildingIonSeries[] ionSeries = extractFunction.get();

    List<ColoredXYDataset> datasets = new ArrayList<>();

    for (int i = 0; i < ionSeries.length; i++) {
      var builder = ionSeries[i];
      Range<Double> mzRange = mzRangesSorted.get(i);
      IonTimeSeries<? extends Scan> series = builder.toIonTimeSeriesWithLeadingAndTrailingZero(null,
          scans);
      datasets.add(
          new MzRangeEicDataSet(series, mzRange, dataFile.getColor()));
    }
    doneFiles.incrementAndGet();
    return datasets;
  }
}
