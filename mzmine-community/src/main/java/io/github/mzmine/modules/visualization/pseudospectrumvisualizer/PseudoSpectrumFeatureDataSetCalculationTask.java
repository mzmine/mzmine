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

package io.github.mzmine.modules.visualization.pseudospectrumvisualizer;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.BuildingIonSeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.IonTimeSeriesToXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredAreaShapeRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYLineRenderer;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataprocessing.featdet_extract_mz_ranges.ExtractMzRangesIonSeriesFunction;
import io.github.mzmine.modules.dataprocessing.filter_diams2.IsolationWindow;
import io.github.mzmine.modules.visualization.chromatogram.TICPlot;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.RangeUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.Nullable;

class PseudoSpectrumFeatureDataSetCalculationTask extends AbstractTask {

  private final RawDataFile dataFile;
  private final TICPlot chromPlot;
  private final Scan pseudoScan;
  private final ModularFeature feature;
  private final MZTolerance mzTolerance;
  @Nullable
  private final Color featureColor;
  private ExtractMzRangesIonSeriesFunction extractFunction;


  PseudoSpectrumFeatureDataSetCalculationTask(RawDataFile dataFile, TICPlot chromPlot,
      Scan pseudoScan, ModularFeature feature, MZTolerance mzTolerance,
      @Nullable Color featureColor) {
    super(null, Instant.now());
    this.dataFile = dataFile;
    this.chromPlot = chromPlot;
    this.pseudoScan = pseudoScan;
    this.feature = feature;
    this.mzTolerance = mzTolerance;
    this.featureColor = featureColor;
  }

  @Override
  public String getTaskDescription() {
    return "Calculate feature datasets";
  }

  @Override
  public double getFinishedPercentage() {
    return extractFunction == null ? 0 : extractFunction.getFinishedPercentage();
  }


  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    if (getStatus() == TaskStatus.CANCELED) {
      return;
    }

    Range<Float> featureRtRange = feature.getRawDataPointsRTRange();
    final ScanSelection selection = new ScanSelection(pseudoScan.getMSLevel(), featureRtRange);

    // use scans from feature list
    List<Scan> scans = selection.streamMatchingScans(dataFile).<Scan>mapMulti((scan, c) -> {
      // MS1 like GC-EI-MS
      if (scan.getMSLevel() == 1) {
        c.accept(scan);
        return;
      }

      // SWATH / DIA PASEF etc
      switch (scan) {
        case Frame frame -> {
          frame.getImsMsMsInfos().stream().map(IsolationWindow::new)
              .filter(w -> w.contains(feature)).findAny().ifPresent(_ -> c.accept(frame));
        }
        default -> {
          final MsMsInfo msMsInfo = scan.getMsMsInfo();
          if (msMsInfo != null && msMsInfo.getIsolationWindow() != null
              && msMsInfo.getIsolationWindow().contains(feature.getMZ())) {
            c.accept(scan);
          }
        }
      }
    }).toList();

    if (scans.isEmpty()) {
      setErrorMessage(
          "scans were empty in the given RT range of feature " + FeatureUtils.featureToString(
              feature));
      setStatus(TaskStatus.ERROR);
      return;
    }

    // get all mz ranges
    List<Range<Double>> mzRangesSorted = Arrays.stream(pseudoScan.getMzValues(new double[0]))
        .sorted().mapToObj(mzTolerance::getToleranceRange).toList();

    // extract all IonSeries at once
    extractFunction = new ExtractMzRangesIonSeriesFunction(dataFile, scans, mzRangesSorted,
        ScanDataType.MASS_LIST, this);

    BuildingIonSeries[] ionSeries = extractFunction.get();

    if (isCanceled()) {
      return;
    }

    var format = ConfigService.getConfiguration().getGuiFormats();

    List<ColoredXYDataset> datasets = new ArrayList<>();

    var nextColor = featureColor != null ? featureColor
        : (feature.getRawDataFile() != null && feature.getRawDataFile().getColor() != null)
            ? feature.getRawDataFile().getColor()
            : ConfigService.getConfiguration().getDefaultColorPalette().getNextColor();

    for (int i = 0; i < ionSeries.length; i++) {
      var builder = ionSeries[i];
      double mz = RangeUtils.rangeCenter(mzRangesSorted.get(i));

      IonTimeSeries<? extends Scan> series = builder.toFullIonTimeSeries(null, scans);
      datasets.add(
          new ColoredXYDataset(new IonTimeSeriesToXYProvider(series, format.mz(mz), nextColor),
              RunOption.THIS_THREAD));
    }

    FxThread.runLater(() -> {
      if (getStatus() == TaskStatus.CANCELED) {
        return;
      }
      chromPlot.applyWithNotifyChanges(false, () -> {

        chromPlot.removeAllFeatureDataSets(false);

        //Build feature dataset
        IonTimeSeries<? extends Scan> featureData = feature.getFeatureData();
        ColoredXYDataset featureDataSet = new ColoredXYDataset(
            new IonTimeSeriesToXYProvider(featureData, format.mz(feature.getMZ()), nextColor),
            RunOption.THIS_THREAD);
        chromPlot.addDataSetAndRenderer(featureDataSet, new ColoredAreaShapeRenderer());

        // add other EICs
        for (var dataset : datasets) {
          chromPlot.addDataSetAndRenderer(dataset, new ColoredXYLineRenderer());
        }

      });
    });
    setStatus(TaskStatus.FINISHED);
  }
}
