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

package io.github.mzmine.modules.visualization.combinedmodule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.XYDataset;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import javafx.application.Platform;
import javafx.collections.ObservableList;

public class CombinedModuleDataset extends AbstractXYDataset implements Task, XYToolTipGenerator {

  private RawDataFile rawDataFile;
  private Range<Double> totalMZRange;
  private Range<Float> totalRTRange;
  private CombinedModuleVisualizerTabController visualizer;
  private TaskStatus status = TaskStatus.WAITING;
  private int processedScans;
  private ObservableList<Scan> scanNumbers;
  private HashMap<Integer, Vector<CombinedModuleDataPoint>> dataSeries;
  int totalScans;
  private AxisType xAxisType, yAxisType;
  private String massListName;
  private Double noiseLevel;
  private ColorScale colorScale;
  private static int RAW_LEVEL = 0;
  private static int PRECURSOR_LEVEL = 1;
  private static int NEUTRALLOSS_LEVEL = 2;

  public CombinedModuleDataset(RawDataFile dataFile, Range<Float> rtRange, Range<Double> mzRange,
      CombinedModuleVisualizerTabController visualizer, AxisType xAxisType, AxisType yAxisType,
      Double noiseLevel, ColorScale colorScale, String massList) {
    this.rawDataFile = dataFile;
    this.totalMZRange = mzRange;
    this.totalRTRange = rtRange;
    this.visualizer = visualizer;
    this.xAxisType = xAxisType;
    this.yAxisType = yAxisType;
    this.noiseLevel = noiseLevel;
    this.colorScale = colorScale;
    this.massListName = massList;

    scanNumbers = rawDataFile.getScans();
    totalScans = scanNumbers.size();
    dataSeries = new HashMap<Integer, Vector<CombinedModuleDataPoint>>();
    dataSeries.put(RAW_LEVEL, new Vector<CombinedModuleDataPoint>(totalScans));
    dataSeries.put(PRECURSOR_LEVEL, new Vector<CombinedModuleDataPoint>(totalScans));
    dataSeries.put(NEUTRALLOSS_LEVEL, new Vector<CombinedModuleDataPoint>(totalScans));

    MZmineCore.getTaskController().addTask(this, TaskPriority.HIGH);
  }

  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);
    processedScans = 0;

    ArrayList<Float> retentionList = new ArrayList<Float>();
    ArrayList<Double> precursorList = new ArrayList<Double>();
    for (Scan scan : scanNumbers) {
      if (status == TaskStatus.CANCELED) {
        return;
      }

      // ignore scans of MS level 1
      if (scan.getMSLevel() == 1) {
        processedScans++;
        continue;
      }

      if (!totalRTRange.contains(scan.getRetentionTime())) {
        continue;
      }

      // skip empty scans
      if (scan.getBasePeakMz() == null) {
        processedScans++;
        continue;
      }

      retentionList.add(scan.getRetentionTime());
      precursorList.add(scan.getPrecursorMZ());

      MassList massList = scan.getMassList(massListName);
      if (massList == null) {
        setStatus(TaskStatus.ERROR);
        return;
      }
      DataPoint[] scanDataPoints = massList.getDataPoints();

      // top Features will contain indexes to mzValues in scan above a threshold
      List<Integer> topFeaturesIndexesList = new ArrayList<Integer>();

      for (int i = 0; i < scanDataPoints.length; i++) {
        // Cancel?
        if (status == TaskStatus.CANCELED) {
          return;
        }
        if (!totalMZRange.contains(scanDataPoints[i].getMZ())) {
          continue;
        }
        if (scanDataPoints[i].getIntensity() > noiseLevel) {
          // add the peaks
          topFeaturesIndexesList.add(i);
        }
      }
      for (int featureIndex : topFeaturesIndexesList) {

        // if we have a very few peaks, the array may not be full
        if (featureIndex < 0) {
          break;
        }

        CombinedModuleDataPoint newPoint =
            new CombinedModuleDataPoint(scanDataPoints[featureIndex].getMZ(), scan.getScanNumber(),
                scan.getPrecursorMZ(), scan.getPrecursorCharge(), scan.getRetentionTime());

        dataSeries.get(0).add(newPoint);
      }
      processedScans++;
    }

    refresh();
    setStatus(TaskStatus.FINISHED);
  }

  private void refresh() {
    Platform.runLater(this::fireDatasetChanged);
  }

  @Override
  public String getTaskDescription() {
    return "Updating MS/MS visualizer of " + rawDataFile;
  }

  @Override
  public double getFinishedPercentage() {
    if (totalScans == 0) {
      return 0;
    } else {
      return ((double) processedScans / totalScans);
    }
  }

  @Override
  public TaskStatus getStatus() {
    return status;
  }

  @Override
  public String getErrorMessage() {
    return null;
  }

  @Override
  public TaskPriority getTaskPriority() {
    return TaskPriority.NORMAL;
  }

  @Override
  public void cancel() {
    status = TaskStatus.CANCELED;
  }

  public void setStatus(TaskStatus newStatus) {
    this.status = newStatus;
  }


  @Override
  public int getSeriesCount() {
    return dataSeries.size();
  }

  @Override
  public Comparable<Integer> getSeriesKey(int series) {
    return series;
  }

  @Override
  public int getItemCount(int series) {
    return dataSeries.get(series).size();
  }

  @Override
  public Number getX(int series, int item) {
    double mz;
    CombinedModuleDataPoint point = dataSeries.get(series).get(item);
    if (xAxisType.equals(AxisType.PRECURSORIONMZ)) {
      mz = point.getPrecursorMZ();
    } else if (xAxisType.equals(AxisType.NEUTRALLOSS)) {
      mz = point.getNeutralLoss();
    } else if (xAxisType.equals(AxisType.PRODUCTIONMZ)) {
      mz = point.getProductIonMZ();
    } else {
      mz = point.getRetentionTime();
    }
    return mz;
  }

  @Override
  public Number getY(int series, int item) {
    CombinedModuleDataPoint point = dataSeries.get(series).get(item);
    double mz;
    if (yAxisType.equals(AxisType.PRECURSORIONMZ)) {
      mz = point.getPrecursorMZ();
    } else if (yAxisType.equals(AxisType.NEUTRALLOSS)) {
      mz = point.getNeutralLoss();
    } else if (yAxisType.equals(AxisType.PRODUCTIONMZ)) {
      mz = point.getProductIonMZ();
    } else {
      mz = point.getRetentionTime();
    }
    return mz;

  }

  @Override
  public String generateToolTip(XYDataset dataset, int series, int item) {
    return dataSeries.get(series).get(item).getName();
  }

  public void updateOnRangeDataPoints(String rangeType) {
    CombinedModulePlot plot = visualizer.getPlot();
    Range<Double> prRange = plot.getHighlightedPrecursorRange();
    Range<Double> nlRange = plot.getHighlightedNeutralLossRange();

    // Set type of search
    int level = NEUTRALLOSS_LEVEL;
    if (rangeType.equals("HIGHLIGHT_PRECURSOR")) {
      level = PRECURSOR_LEVEL;
    }

    // Clean previous selection
    dataSeries.get(level).clear();

    CombinedModuleDataPoint point;
    boolean b = false;
    for (int i = 0; i < dataSeries.get(RAW_LEVEL).size(); i++) {
      point = dataSeries.get(RAW_LEVEL).get(i);
      // Verify if the point is on range
      if (level == PRECURSOR_LEVEL) {
        b = prRange.contains(point.getPrecursorMass());
      } else {
        b = nlRange.contains(point.getNeutralLoss());
      }
      if (b) {
        dataSeries.get(level).add(point);
      }
    }

    refresh();
  }
}
