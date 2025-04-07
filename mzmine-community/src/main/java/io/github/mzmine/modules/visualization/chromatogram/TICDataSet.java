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

package io.github.mzmine.modules.visualization.chromatogram;

import com.google.common.collect.Range;
import com.google.common.primitives.Ints;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.AbstractTaskXYZDataset;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.scans.ScanUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jfree.chart.axis.NumberAxis;

/**
 * TIC visualizer data set. One data set is created per file shown in this visualizer. We need to
 * create separate data set for each file because the user may add/remove files later.
 * <p>
 * Added the possibility to switch to TIC plot type from a "non-TICVisualizerWindow" context.
 */
public class TICDataSet extends AbstractTaskXYZDataset {

  private static final long serialVersionUID = 1L;
  // For comparing small differences.
  private static final double EPSILON = 0.0000001;
  // Logger.
  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private final RawDataFile dataFile;

  private final List<Scan> scans;
  private final int totalScans;
  private final double[] basePeakMZValues;
  private final double[] intensityValues;
  private final double[] rtValues;
  private final Range<Double> mzRange;
  // Plot type
  private final TICPlotType plotType;
  private int processedScans;
  private double intensityMin;
  private double intensityMax;
  private TICVisualizerTab window;
  private String customSeriesKey = null;

  /**
   * Create the data set.
   *
   * @param file    data file to plot.
   * @param scans   scans to plot.
   * @param rangeMZ range of m/z to plot.
   * @param window  visualizer window.
   */
  public TICDataSet(final RawDataFile file, final ObservableList<Scan> scans,
      final Range<Double> rangeMZ, final TICVisualizerTab window) {
    this(file, scans, rangeMZ, window,
        ((window != null) ? window.getPlotType() : TICPlotType.BASEPEAK));
  }

  /**
   * Create the data set + possibility to specify a plot type, even outside a "TICVisualizerWindow"
   * context.
   *
   * @param file     data file to plot.
   * @param scans    scans to plot.
   * @param rangeMZ  range of m/z to plot.
   * @param window   visualizer window.
   * @param plotType plot type.
   */
  public TICDataSet(final RawDataFile file, final List<Scan> scans, final Range<Double> rangeMZ,
      final TICVisualizerTab window, TICPlotType plotType) {

    mzRange = rangeMZ;
    dataFile = file;
    this.scans = scans;
    totalScans = scans.size();
    basePeakMZValues = new double[totalScans];
    intensityValues = new double[totalScans];
    rtValues = new double[totalScans];
    processedScans = 0;
    intensityMin = 0.0;
    intensityMax = 0.0;
    this.window = window;

    status = TaskStatus.WAITING;
    errorMessage = null;

    this.plotType = plotType;

    // this will call to many update events if many datasets are added
    // Start-up the refresh task.
//    MZmineCore.getTaskController().addTask(this, TaskPriority.HIGH);

    // directly calculate data
    calculateValues();
  }

  /**
   * Creates a TICDataSet from a {@link ModularFeature}. Effectively a XIC.
   *
   * @param feature The feature.
   */
  public TICDataSet(Feature feature) {

    dataFile = feature.getRawDataFile();
    mzRange = feature.getRawDataPointsMZRange();
    Range<Float> rtRange = feature.getRawDataPointsRTRange();
    scans = feature.getScanNumbers();

    totalScans = scans.size();
    basePeakMZValues = new double[totalScans];
    intensityValues = new double[totalScans];
    rtValues = new double[totalScans];
    processedScans = 0;
    intensityMin = 0.0;
    intensityMax = 0.0;

    status = TaskStatus.WAITING;
    errorMessage = null;

    this.plotType = TICPlotType.TIC;

    // this will call to many update events if many datasets are added
    // Start-up the refresh task.
//    MZmineCore.getTaskController().addTask(this, TaskPriority.HIGH);

    // directly calculate data
    calculateValues();
  }

  public TICDataSet(RawDataFile newFile, Scan[] scans, Range<Double> mzRange,
      TICVisualizerTab window) {
    this(newFile, FXCollections.observableArrayList(scans), mzRange, window);
  }

  @Override
  public double getFinishedPercentage() {
    return totalScans == 0 ? 0.0 : (double) processedScans / (double) totalScans;
  }

  @Override
  public String getTaskDescription() {
    return "Updating TIC visualizer of " + dataFile;
  }

  @Override
  public void run() {

    try {
      status = TaskStatus.PROCESSING;

      calculateValues();

      if (status != TaskStatus.CANCELED) {

        // Always redraw when we add last value.
        refresh();

        logger.info("TIC data calculated for " + dataFile);
        status = TaskStatus.FINISHED;
      }
    } catch (Throwable t) {
      logger.log(Level.SEVERE, "Problem calculating data set values for " + dataFile, t);
      status = TaskStatus.ERROR;
      errorMessage = t.getMessage();
    }
  }

  @Override
  public int getSeriesCount() {

    return 1;
  }

  @Override
  public Comparable<String> getSeriesKey(final int series) {
    return (customSeriesKey == null) ? dataFile.getName() : customSeriesKey;
  }

  @Override
  public Number getZ(final int series, final int item) {

    return basePeakMZValues[item];
  }

  @Override
  public int getItemCount(final int series) {

    return processedScans;
  }

  @Override
  public Number getX(final int series, final int item) {

    return rtValues[item];
  }

  @Override
  public Number getY(final int series, final int item) {

    return intensityValues[item];
  }

  /**
   * Returns index of data point which exactly matches given X and Y values
   *
   * @param retentionTime retention time.
   * @param intensity     intensity.
   * @return the nearest data point index.
   */
  public int getIndex(final double retentionTime, final double intensity) {

    int index = -1;
    for (int i = 0; index < 0 && i < processedScans; i++) {

      if (Math.abs(retentionTime - rtValues[i]) < EPSILON
          && Math.abs(intensity - intensityValues[i]) < EPSILON) {

        index = i;
      }
    }

    return index;
  }

  public Scan getScan(final int item) {
    return scans.get(item);
  }

  public RawDataFile getDataFile() {
    return dataFile;
  }

  /**
   * Checks if given data point is local maximum.
   *
   * @param item the index of the item to check.
   * @return true/false if the item is a local maximum.
   */
  public boolean isLocalMaximum(final int item) {

    final boolean isLocalMaximum;
    if (item <= 0 || item >= processedScans - 1) {

      isLocalMaximum = false;

    } else {

      final double intensity = intensityValues[item];
      isLocalMaximum =
          intensityValues[item - 1] <= intensity && intensity >= intensityValues[item + 1];
    }

    return isLocalMaximum;
  }

  /**
   * Gets indexes of local maxima within given range.
   *
   * @param xMin minimum of range on x-axis.
   * @param xMax maximum of range on x-axis.
   * @param yMin minimum of range on y-axis.
   * @param yMax maximum of range on y-axis.
   * @return the local maxima in the given range.
   */
  public int[] findLocalMaxima(final double xMin, final double xMax, final double yMin,
      final double yMax) {

    // Save data set size.
    final int currentSize = processedScans;
    final double[] rtCopy;

    // If the RT values array is not filled yet, create a smaller copy.
    if (currentSize < rtValues.length) {

      rtCopy = new double[currentSize];
      System.arraycopy(rtValues, 0, rtCopy, 0, currentSize);

    } else {

      rtCopy = rtValues;
    }

    int startIndex = Arrays.binarySearch(rtCopy, xMin);
    if (startIndex < 0) {

      startIndex = -startIndex - 1;
    }

    final int length = rtCopy.length;
    final Collection<Integer> indices = new ArrayList<Integer>(length);
    for (int index = startIndex; index < length && rtCopy[index] <= xMax; index++) {

      // Check Y range..
      final double intensity = intensityValues[index];
      if (yMin <= intensity && intensity <= yMax && isLocalMaximum(index)) {

        indices.add(index);
      }
    }

    return Ints.toArray(indices);
  }

  public double getMinIntensity() {

    return intensityMin;
  }

  public TICPlotType getPlotType() {
    return this.plotType;
  }

  private void calculateValues() {

    // Determine plot type (now done from constructor).
    final TICPlotType plotType = this.plotType;

    // should be changed to something like this in the future, rather redo whole plot
//    ExtractMzRangesIonSeriesFunction extractor = new ExtractMzRangesIonSeriesFunction(dataFile, scans,
//        List.of(mzRange), ScanDataType.RAW, this);
//    extractor.setIntensityMode(IntensityMode.from(plotType));
//    var series = Arrays.stream(extractor.get()).findFirst();

    if (scans.isEmpty()) {
      return;
    }
    // fix for imZML files without a retention time in their scans -> crashes TIC Plot
    boolean useScanNumberAsRt = Double.compare(scans.get(0).getRetentionTime(),
        scans.get(scans.size() - 1).getRetentionTime()) == 0;
    if (useScanNumberAsRt && window != null) {
      final NumberAxis axis = (NumberAxis) window.getTICPlot().getXYPlot().getDomainAxis();
      FxThread.runLater(() -> axis.setLabel("Scan number"));
    }

    // Process each scan.
    for (int index = 0; status != TaskStatus.CANCELED && index < totalScans; index++) {

      // Current scan.
      final Scan scan = scans.get(index);

      // Determine base peak value.
      DataPoint basePeak = ScanUtils.findBasePeak(scan, mzRange);
      Double basePeakIntensity = null;

      if (basePeak != null) {
        basePeakMZValues[index] = basePeak.getMZ();
        basePeakIntensity = basePeak.getIntensity();
      }

      // Determine peak intensity.
      double intensity = 0.0;
      if (plotType == TICPlotType.TIC) {

        // Total ion count.
        if (scan.getDataPointMZRange() != null) {
          intensity = mzRange.encloses(scan.getDataPointMZRange()) ? scan.getTIC() : ScanUtils.calculateTIC(scan, mzRange);
        }

      } else if (plotType == TICPlotType.BASEPEAK && basePeakIntensity != null) {

        intensity = basePeakIntensity;
      }

      intensityValues[index] = intensity;
      rtValues[index] = useScanNumberAsRt ? scan.getScanNumber() : scan.getRetentionTime();

      // Update min and max.
      if (index == 0) {

        intensityMin = intensity;
        intensityMax = intensity;

      } else {

        intensityMin = Math.min(intensity, intensityMin);
        intensityMax = Math.max(intensity, intensityMax);
      }

      processedScans++;
    }
  }

  /**
   * Notify data set listener (on the EDT).
   */
  private void refresh() {
    Platform.runLater(() -> fireDatasetChanged());
  }

  public void setCustomSeriesKey(String customSeriesKey) {
    this.customSeriesKey = customSeriesKey;
  }

}
