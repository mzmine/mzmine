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

package io.github.mzmine.modules.visualization.twod;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.util.ArrayList;
import java.util.Arrays;
import javafx.application.Platform;
import org.jfree.data.xy.AbstractXYDataset;

class TwoDDataSet extends AbstractXYDataset implements Task {

  private static final long serialVersionUID = 1L;

  private RawDataFile rawDataFile;

  private float retentionTimes[];
  private double basePeaks[], mzValues[][], intensityValues[][];

  private final Range<Double> totalMZRange;
  private final Range<Float> totalRTRange;
  private int totalScans, processedScans;
  private final Scan scans[];

  private TaskStatus status = TaskStatus.WAITING;

  public double curMaxIntensity;
  private ArrayList<Float> rtValuesInUserRange;

  TwoDDataSet(RawDataFile rawDataFile, Scan scans[], Range<Float> rtRange, Range<Double> mzRange,
      TwoDVisualizerTab visualizer) {

    this.rawDataFile = rawDataFile;

    totalRTRange = rtRange;
    totalMZRange = mzRange;

    this.scans = scans;

    totalScans = scans.length;

    mzValues = new double[totalScans][];
    intensityValues = new double[totalScans][];
    retentionTimes = new float[totalScans];
    basePeaks = new double[totalScans];

    MZmineCore.getTaskController().addTask(this, TaskPriority.HIGH);

  }

  @Override
  public void run() {

    status = TaskStatus.PROCESSING;

    for (int index = 0; index < totalScans; index++) {

      // Cancel?
      if (status == TaskStatus.CANCELED)
        return;

      Scan scan = scans[index];
      Double scanBasePeakInt = scan.getBasePeakIntensity();
      retentionTimes[index] = scan.getRetentionTime();
      basePeaks[index] = (scanBasePeakInt == null ? 0 : scanBasePeakInt);
      mzValues[index] = new double[scan.getNumberOfDataPoints()];
      scan.getMzValues(mzValues[index]);
      intensityValues[index] = new double[scan.getNumberOfDataPoints()];
      scan.getIntensityValues(intensityValues[index]);
      processedScans++;
    }

    Platform.runLater(() -> fireDatasetChanged());

    status = TaskStatus.FINISHED;

  }

  /**
   * @see org.jfree.data.general.AbstractSeriesDataset#getSeriesCount()
   */
  @Override
  public int getSeriesCount() {
    return 2;
  }

  /**
   * @see org.jfree.data.general.AbstractSeriesDataset#getSeriesKey(int)
   */
  @Override
  public Comparable<?> getSeriesKey(int series) {
    return rawDataFile.getName();
  }

  /**
   * @see org.jfree.data.xy.XYDataset#getItemCount(int)
   */
  @Override
  public int getItemCount(int series) {
    return 2;
  }

  /**
   * @see org.jfree.data.xy.XYDataset#getX(int, int)
   */
  @Override
  public Number getX(int series, int item) {
    if (series == 0)
      return totalRTRange.lowerEndpoint();
    else
      return totalRTRange.upperEndpoint();
  }

  /**
   * @see org.jfree.data.xy.XYDataset#getY(int, int)
   */
  @Override
  public Number getY(int series, int item) {
    if (item == 0)
      return totalMZRange.lowerEndpoint();
    else
      return totalMZRange.upperEndpoint();
  }

  double upperEndpointIntensity(Range<Float> rtRange, Range<Double> mzRange, PlotMode plotMode) {

    double maxIntensity = 0;

    float searchRetentionTimes[] = retentionTimes;
    if (processedScans < totalScans) {
      searchRetentionTimes = new float[processedScans];
      System.arraycopy(retentionTimes, 0, searchRetentionTimes, 0, searchRetentionTimes.length);
    }

    int startScanIndex = Arrays.binarySearch(searchRetentionTimes, rtRange.lowerEndpoint());

    if (startScanIndex < 0)
      startScanIndex = (startScanIndex * -1) - 1;

    if (startScanIndex >= searchRetentionTimes.length) {
      return 0;
    }

    if (searchRetentionTimes[startScanIndex] > rtRange.upperEndpoint()) {
      if (startScanIndex == 0)
        return 0;

      if (startScanIndex == searchRetentionTimes.length - 1)
        return upperEndpointIntensity(startScanIndex - 1, mzRange, plotMode);

      // find which scan point is closer
      double diffNext = searchRetentionTimes[startScanIndex] - rtRange.upperEndpoint();
      double diffPrev = rtRange.lowerEndpoint() - searchRetentionTimes[startScanIndex - 1];

      if (diffPrev < diffNext)
        return upperEndpointIntensity(startScanIndex - 1, mzRange, plotMode);
      else
        return upperEndpointIntensity(startScanIndex, mzRange, plotMode);
    }

    for (int scanIndex = startScanIndex; ((scanIndex < searchRetentionTimes.length)
        && (searchRetentionTimes[scanIndex] <= rtRange.upperEndpoint())); scanIndex++) {

      // ignore scans where all peaks are smaller than current max
      if (basePeaks[scanIndex] < maxIntensity)
        continue;

      double scanMax = upperEndpointIntensity(scanIndex, mzRange, plotMode);

      if (scanMax > maxIntensity)
        maxIntensity = scanMax;

    }

    return maxIntensity;

  }

  public ArrayList getrtValuesInUserRange() {
    return rtValuesInUserRange;
  }

  private double upperEndpointIntensity(int index, Range<Double> mzRange, PlotMode plotMode) {

    double maxIntensity = 0;

    int startMZIndex = Arrays.binarySearch(mzValues[index], mzRange.lowerEndpoint());
    if (startMZIndex < 0)
      startMZIndex = (startMZIndex * -1) - 1;

    if (startMZIndex >= mzValues[index].length)
      return 0;

    if (mzValues[index][startMZIndex] > mzRange.upperEndpoint()) {
      if (plotMode != PlotMode.CENTROID) {
        if (startMZIndex == 0)
          return 0;
        if (startMZIndex == mzValues[index].length - 1)
          return intensityValues[index][startMZIndex - 1];

        // find which data point is closer
        double diffNext = mzValues[index][startMZIndex] - mzRange.upperEndpoint();
        double diffPrev = mzRange.lowerEndpoint() - mzValues[index][startMZIndex - 1];

        if (diffPrev < diffNext)
          return intensityValues[index][startMZIndex - 1];
        else
          return intensityValues[index][startMZIndex];
      } else {
        return 0;
      }

    }

    for (int mzIndex = startMZIndex; ((mzIndex < mzValues[index].length)
        && (mzValues[index][mzIndex] <= mzRange.upperEndpoint())); mzIndex++) {
      if (intensityValues[index][mzIndex] > maxIntensity)
        maxIntensity = intensityValues[index][mzIndex];
    }

    return maxIntensity;

  }

  @Override
  public void cancel() {
    status = TaskStatus.CANCELED;
  }

  @Override
  public String getErrorMessage() {
    return null;
  }

  @Override
  public double getFinishedPercentage() {
    if (totalScans == 0)
      return 0;
    return (double) processedScans / totalScans;
  }

  @Override
  public TaskStatus getStatus() {
    return status;
  }

  @Override
  public String getTaskDescription() {
    return "Updating 2D visualizer of " + rawDataFile;
  }

  @Override
  public TaskPriority getTaskPriority() {
    return TaskPriority.NORMAL;
  }
}
