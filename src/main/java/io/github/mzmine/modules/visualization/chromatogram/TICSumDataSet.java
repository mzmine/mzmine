/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.visualization.chromatogram;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.jfree.data.xy.AbstractXYZDataset;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.scans.ScanUtils;
import javafx.application.Platform;

/**
 * TIC visualizer data set. Sum of all TIC
 */
public class TICSumDataSet extends AbstractXYZDataset implements Task {

  private static final long serialVersionUID = 1L;

  // Logger.
  private final Logger logger = Logger.getLogger(this.getClass().getName());

  // rt bin for summing different files
  private final double RT_BIN = 0.001;

  // Refresh interval (in milliseconds).
  private static final long REDRAW_INTERVAL = 100L;

  // Last time the data set was redrawn.
  private static long lastRedrawTime = System.currentTimeMillis();

  private final RawDataFile[] dataFiles;

  private final @Nonnull List<SummedTICDataPoint> data;

  private final Range<Double> mzRange;
  private Range<Double> rangeRT;
  private double intensityMin;
  private double intensityMax;

  private TaskStatus status;
  private String errorMessage;

  // Plot type
  private TICPlotType plotType;

  private int totalScans;
  private int processedScans;

  /**
   * Create the data set.
   *
   * @param file data file to plot.
   * @param theScanNumbers scans to plot.
   * @param rangeMZ range of m/z to plot.
   * @param window visualizer window.
   */
  public TICSumDataSet(final RawDataFile[] files, final Range<Double> rangeRT,
      final Range<Double> rangeMZ, final TICVisualizerWindow window) {
    this(files, rangeRT, rangeMZ, window,
        ((window != null) ? window.getPlotType() : TICPlotType.BASEPEAK));
  }

  /**
   * Create the data set + possibility to specify a plot type, even outside a "TICVisualizerWindow"
   * context.
   *
   * @param file data file to plot.
   * @param theScanNumbers scans to plot.
   * @param rangeMZ range of m/z to plot.
   * @param window visualizer window.
   * @param plotType plot type.
   */
  public TICSumDataSet(final RawDataFile[] files, final Range<Double> rangeRT,
      final Range<Double> rangeMZ, final TICVisualizerWindow window, TICPlotType plotType) {
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
      int[] scans = raw.getScanNumbers(1, rangeRT);
      totalScans += scans.length;
    }
  }

  @Override
  public String getErrorMessage() {
    return errorMessage;
  }

  @Override
  public double getFinishedPercentage() {
    return totalScans == 0 ? 0.0 : (double) processedScans / totalScans;
  }

  @Override
  public TaskStatus getStatus() {
    return status;
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
      int[] scans = raw.getScanNumbers(1, rangeRT);
      // Process each scan.
      for (int index = 0; status != TaskStatus.CANCELED && index < scans.length; index++) {
        // Current scan.
        final Scan scan = raw.getScan(scans[index]);
        double rt = scan.getRetentionTime();
        double mzBasePeak = 0;
        double intensityBasePeak = 0;
        double intensity = 0.0;

        // Determine base peak value.
        final DataPoint basePeak =
            mzRange.encloses(scan.getDataPointMZRange()) ? scan.getHighestDataPoint()
                : ScanUtils.findBasePeak(scan, mzRange);
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
              if (intensityBasePeak > dp.getIntensityBasePeak())
                dp.setMzBasePeak(mzBasePeak);
              intensity += dp.getIntensity();
              rt = (rt + dp.getRetentionTime()) / 2.0;
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

  @Override
  public void cancel() {
    status = TaskStatus.CANCELED;
  }

  @Override
  public TaskPriority getTaskPriority() {
    return TaskPriority.NORMAL;
  }
}
