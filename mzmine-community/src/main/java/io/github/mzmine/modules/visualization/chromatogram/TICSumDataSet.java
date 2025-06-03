/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.AbstractTaskXYZDataset;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.scans.ScanUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;

/**
 * TIC visualizer data set. Sum of all TIC
 */
public class TICSumDataSet extends AbstractTaskXYZDataset {

  private static final long serialVersionUID = 1L;

  private static final Logger logger = Logger.getLogger(TICSumDataSet.class.getName());

  // rt bin for summing different files
  private final double RT_BIN = 0.001;

  // Refresh interval (in milliseconds).
  private static final long REDRAW_INTERVAL = 100L;

  // Last time the data set was redrawn.
  private static long lastRedrawTime = System.currentTimeMillis();

  private final RawDataFile[] dataFiles;

  private final @NotNull List<SummedTICDataPoint> data;

  private final Range<Double> mzRange;
  private final Range<Float> rangeRT;
  private double intensityMin;
  private double intensityMax;

  // Plot type
  private final TICPlotType plotType;

  private int totalScans;
  private int processedScans;

  /**
   * Create the data set.
   *
   * @param files   data file to plot.
   * @param rangeMZ range of m/z to plot.
   * @param window  visualizer window.
   */
  public TICSumDataSet(final RawDataFile[] files, final Range<Float> rangeRT,
      final Range<Double> rangeMZ, final TICVisualizerTab window) {
    this(files, rangeRT, rangeMZ, window,
        ((window != null) ? window.getPlotType() : TICPlotType.BASEPEAK));
  }

  /**
   * Create the data set + possibility to specify a plot type, even outside a "TICVisualizerWindow"
   * context.
   *
   * @param files    data file to plot.
   * @param rangeMZ  range of m/z to plot.
   * @param window   visualizer window.
   * @param plotType plot type.
   */
  public TICSumDataSet(final RawDataFile[] files, final Range<Float> rangeRT,
      final Range<Double> rangeMZ, final TICVisualizerTab window, TICPlotType plotType) {
    data = new ArrayList<>();
    mzRange = rangeMZ;
    dataFiles = files;
    this.rangeRT = rangeRT;
    intensityMin = 0.0;
    intensityMax = 0.0;

    status = TaskStatus.WAITING;
    errorMessage = null;

    this.plotType = plotType;

    calcTotalScans();

    // Start-up the refresh task.
    MZmineCore.getTaskController().addTask(this, TaskPriority.HIGH);
  }

  private void calcTotalScans() {
    totalScans = 0;
    for (RawDataFile raw : dataFiles) {
      Scan[] scans = raw.getScanNumbers(1, rangeRT);
      totalScans += scans.length;
    }
  }

  @Override
  public double getFinishedPercentage() {
    return totalScans == 0 ? 0.0 : (double) processedScans / totalScans;
  }

  @Override
  public String getTaskDescription() {
    return "Updating TIC visualizer of " + rawFileString();
  }

  private String rawFileString() {
    return Arrays.stream(dataFiles).map(RawDataFile::getName).collect(Collectors.joining(", "));
  }

  @Override
  public void run() {

    try {
      status = TaskStatus.PROCESSING;

      calculateValues();

      if (status != TaskStatus.CANCELED) {

        // Always redraw when we add last value.
        refresh();

        logger.info("TIC sum data calculated for " + rawFileString());
        status = TaskStatus.FINISHED;
      }
    } catch (Throwable t) {

      logger.log(Level.SEVERE, "Problem calculating data set values for " + rawFileString(), t);
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
    return rawFileString();
  }

  @Override
  public Number getZ(final int series, final int item) {
    return item < data.size() ? data.get(item).getMzBasePeak() : 0d;
  }

  @Override
  public int getItemCount(final int series) {
    return data.size();
  }

  @Override
  public Number getX(final int series, final int item) {
    return item < data.size() ? data.get(item).getRetentionTime() : 0d;
  }

  @Override
  public Number getY(final int series, final int item) {
    return item < data.size() ? data.get(item).getIntensity() : 0d;
  }

  public RawDataFile[] getDataFile() {
    return dataFiles;
  }

  public double getMinIntensity() {
    return intensityMin;
  }

  public TICPlotType getPlotType() {
    return this.plotType;
  }

  private void calculateValues() {
    intensityMin = Double.MAX_VALUE;
    intensityMax = Double.NEGATIVE_INFINITY;
    // Determine plot type (now done from constructor).
    final TICPlotType plotType = this.plotType;

    // all raw data files
    for (int r = 0; r < dataFiles.length; r++) {
      RawDataFile raw = dataFiles[r];
      Scan[] scans = raw.getScanNumbers(1, rangeRT);
      // Process each scan.
      for (int index = 0; status != TaskStatus.CANCELED && index < scans.length; index++) {
        // Current scan.
        final Scan scan = scans[index];
        float rt = scan.getRetentionTime();
        double mzBasePeak = 0;
        double intensityBasePeak = 0;
        double intensity = 0.0;

        // Determine base peak value.
        final DataPoint basePeak = ScanUtils.findBasePeak(scan, mzRange);
        if (basePeak != null) {
          mzBasePeak = basePeak.getMZ();
          intensityBasePeak = basePeak.getIntensity();
        }

        // Determine peak intensity.
        if (plotType == TICPlotType.TIC) {

          // Total ion count.
          intensity = mzRange.encloses(scan.getDataPointMZRange()) ? scan.getTIC()
              : ScanUtils.calculateTIC(scan, mzRange);

        } else if (plotType == TICPlotType.BASEPEAK && basePeak != null) {

          intensity = basePeak.getIntensity();
        }

        // search for fitting value to calc sum
        if (!data.isEmpty() && rt < data.get(data.size() - 1).getRetentionTime() + RT_BIN) {
          for (int i = 0; i < data.size(); i++) {
            SummedTICDataPoint dp = data.get(i);
            // matches rt
            if (matchesRT(rt, dp.getRetentionTime())) {
              // sum values
              if (intensityBasePeak > dp.getIntensityBasePeak()) {
                dp.setMzBasePeak(mzBasePeak);
              }
              intensity += dp.getIntensity();
              rt = (rt + dp.getRetentionTime()) / 2.0f;
              dp.setIntensity(intensity);
              dp.setRetentionTime(rt);
            } else if (dp.getRetentionTime() > rt) {
              // insert
              data.add(i, new SummedTICDataPoint(rt, intensity, mzBasePeak, intensityBasePeak));
              break;
            }
          }
        } else {
          // add new data point
          data.add(new SummedTICDataPoint(rt, intensity, mzBasePeak, intensityBasePeak));
        }

        // Update min and max.
        intensityMin = Math.min(intensity, intensityMin);
        intensityMax = Math.max(intensity, intensityMax);

        // Refresh every REDRAW_INTERVAL ms.
        synchronized (TICSumDataSet.class) {
          if (System.currentTimeMillis() - lastRedrawTime > REDRAW_INTERVAL) {
            refresh();
            lastRedrawTime = System.currentTimeMillis();
          }
        }

        processedScans++;
      }
    }
  }

  /**
   * difference smaller than RT_BIN?
   *
   * @param a
   * @param b
   * @return
   */
  private boolean matchesRT(double a, double b) {
    return Math.abs(a - b) <= RT_BIN;
  }

  /**
   * Notify data set listener (on the EDT).
   */
  private void refresh() {
    Platform.runLater(() -> fireDatasetChanged());
  }

}
