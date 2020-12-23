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

package io.github.mzmine.datamodel.features.types.graphicalnodes;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYZScatterPlot;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYZDataProvider;
import io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder.RetentionTimeMobilityDataPoint;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;
import javafx.scene.layout.StackPane;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jfree.chart.renderer.PaintScale;

public class FeatureShapeIonMobilityRetentionTimeHeatMapChart extends StackPane {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private Float dataPointWidth;
  private Double dataPointHeight;

  public FeatureShapeIonMobilityRetentionTimeHeatMapChart(@Nonnull ModularFeatureListRow row,
      AtomicDouble progress) {
    /*try {
      Float[] xValues = null;
      Double[] yValues = null;
      Double[] zValues = null;

      int size = row.getFilesFeatures().size();
      int fi = 0;
      double minMobility = Double.MAX_VALUE, maxMobility = 0;
      double minRt = Double.MAX_VALUE, maxRt = 0;
      for (Feature f : row.getFeatures()) {
        List<? extends DataPoint> dps = f.getDataPoints();
        List<RetentionTimeMobilityDataPoint> dataPoints = new ArrayList<>();
        dataPoints.addAll((Collection<? extends RetentionTimeMobilityDataPoint>) dps);
        calculateDataPointSizeForPlots(dataPoints);
        // add data points retention time -> intensity
        List<Float> xValuesSet = new ArrayList<>();
        List<Double> yValuesSet = new ArrayList<>();
        List<Double> zValuesSet = new ArrayList<>();
        for (RetentionTimeMobilityDataPoint dp : dataPoints) {
          xValuesSet.add(dp.getRetentionTime());
          yValuesSet.add(dp.getMobility());
          zValuesSet.add(dp.getIntensity());
          if (dp.getMobility() > maxMobility) {
            maxMobility = dp.getMobility();
          }
          if (dp.getMobility() < minMobility) {
            minMobility = dp.getMobility();
          }
          if (dp.getRetentionTime() > maxRt) {
            maxRt = dp.getRetentionTime();
          }
          if (dp.getRetentionTime() < minRt) {
            minRt = dp.getRetentionTime();
          }
          if (progress != null)
            progress.addAndGet(1.0 / size / dataPoints.size());
        }
        xValues = new Float[xValuesSet.size()];
        xValues = xValuesSet.toArray(xValues);
        yValues = new Double[yValuesSet.size()];
        yValues = yValuesSet.toArray(yValues);
        zValues = new Double[zValuesSet.size()];
        zValues = zValuesSet.toArray(zValues);

        if (progress != null)
          progress.set((double) fi / size);
      }
      XYZDataset dataset = new RetentionTimeMobilityXYZDataset(xValues, yValues, zValues, "Test");
      RetentionTimeMobilityHeatMapPlot retentionTimeMobilityHeatMapPlot =
          new RetentionTimeMobilityHeatMapPlot(dataset, "Rainbow", dataPointWidth, dataPointHeight);
      this.getChildren().add(retentionTimeMobilityHeatMapPlot);
    } catch (Exception ex) {
      logger.log(Level.WARNING, "error in DP", ex);
    }*/

    SimpleXYZScatterPlot<DatasetCalc> retentionTimeMobilityHeatMapPlot = new SimpleXYZScatterPlot<>(
        "");
    DatasetCalc calc = new DatasetCalc(row, progress);
    ColoredXYZDataset dataset = new ColoredXYZDataset(calc);
    retentionTimeMobilityHeatMapPlot.setDataset(dataset);
    this.getChildren().add(retentionTimeMobilityHeatMapPlot);
  }

  private void calculateDataPointSizeForPlots(List<RetentionTimeMobilityDataPoint> dataPoints) {
    SortedSet<RetentionTimeMobilityDataPoint> dataPointsSortedByRt =
        new TreeSet<>(new Comparator<RetentionTimeMobilityDataPoint>() {
          @Override
          public int compare(RetentionTimeMobilityDataPoint o1, RetentionTimeMobilityDataPoint o2) {
            if (o1.getRetentionTime() > o2.getRetentionTime()) {
              return 1;
            } else {
              return -1;
            }
          }
        });
    dataPointsSortedByRt.addAll(dataPoints);
    List<Float> rtDeltas = new ArrayList<>();
    RetentionTimeMobilityDataPoint dpA = null;
    for (RetentionTimeMobilityDataPoint dp : dataPointsSortedByRt) {
      if (dpA == null) {
        dpA = dp;
        dataPointHeight = dp.getMobilityWidth();
      } else if (!(dpA.getRetentionTime().equals(dp.getRetentionTime()))) {
        rtDeltas.add(dpA.getRetentionTime() - dp.getRetentionTime());
        dpA = dp;
      }
    }
    Collections.sort(rtDeltas);
    Float medianRt = 0.f;
    if (rtDeltas.size() >= 2) {
      if (rtDeltas.size() % 2 == 0) {
        int indexA = rtDeltas.size() / 2;
        int indexB = rtDeltas.size() / 2 - 1;
        medianRt = (rtDeltas.get(indexA) + rtDeltas.get(indexB)) / 2;
      } else {
        int index = rtDeltas.size() / 2;
        medianRt = rtDeltas.get(index);
      }
    }
    dataPointWidth = medianRt;
  }

  private static class DatasetCalc implements PlotXYZDataProvider {

    private final ModularFeatureListRow row;
    private final List<Double> xValuesSet;
    private final List<Double> yValuesSet;
    private final List<Double> zValuesSet;
    private AtomicDouble progress;

    private double dataPointWidth;
    private double dataPointHeight;

    private DatasetCalc(ModularFeatureListRow row, AtomicDouble progress) {
      this.row = row;
      this.progress = progress;
      xValuesSet = new ArrayList<>();
      yValuesSet = new ArrayList<>();
      zValuesSet = new ArrayList<>();
    }

    @Override
    public Color getAWTColor() {
      return Color.black;
    }

    @Override
    public javafx.scene.paint.Color getFXColor() {
      return javafx.scene.paint.Color.BLACK;
    }

    @Override
    public String getLabel(int index) {
      return "";
    }

    @Nullable
    @Override
    public PaintScale getPaintScale() {
      return null;
    }

    /**
     * @return The series key to label the dataset in the chart's legend.
     */
    @Override
    public Comparable<?> getSeriesKey() {
      return "null";
    }

    @Override
    public String getToolTipText(int itemIndex) {
      return "null";
    }

    @Override
    public void computeValues() {

      try {
        Float[] xValues = null;
        Double[] yValues = null;
        Double[] zValues = null;

        int size = row.getFilesFeatures().size();
        int fi = 0;
        double minMobility = Double.MAX_VALUE, maxMobility = 0;
        double minRt = Double.MAX_VALUE, maxRt = 0;
        for (Feature f : row.getFeatures()) {
          List<? extends DataPoint> dps = f.getDataPoints();
          List<RetentionTimeMobilityDataPoint> dataPoints = new ArrayList<>();
          dataPoints.addAll((Collection<? extends RetentionTimeMobilityDataPoint>) dps);
          calculateDataPointSizeForPlots(dataPoints);
          // add data points retention time -> intensity

          for (RetentionTimeMobilityDataPoint dp : dataPoints) {
            xValuesSet.add(dp.getRetentionTime().doubleValue());
            yValuesSet.add(dp.getMobility());
            zValuesSet.add(dp.getIntensity());
            if (dp.getMobility() > maxMobility) {
              maxMobility = dp.getMobility();
            }
            if (dp.getMobility() < minMobility) {
              minMobility = dp.getMobility();
            }
            if (dp.getRetentionTime() > maxRt) {
              maxRt = dp.getRetentionTime();
            }
            if (dp.getRetentionTime() < minRt) {
              minRt = dp.getRetentionTime();
            }
            if (progress != null) {
              progress.addAndGet(1.0 / size / dataPoints.size());
            }
          }
//          xValues = new Float[xValuesSet.size()];
//          xValues = xValuesSet.toArray(xValues);
//          yValues = new Double[yValuesSet.size()];
//          yValues = yValuesSet.toArray(yValues);
//          zValues = new Double[zValuesSet.size()];
//          zValues = zValuesSet.toArray(zValues);

          if (progress != null) {
            progress.set((double) fi / size);
          }
        }
      } catch (Exception ex) {
//        logger.log(Level.WARNING, "error in DP", ex);
        System.out.println("error in DP");
      }
    }

    @Override
    public List<Double> getDomainValues() {
      return xValuesSet;
    }

    @Override
    public List<Double> getRangeValues() {
      return yValuesSet;
    }

    @Override
    public double getComputationFinishedPercentage() {
      return progress.doubleValue();
    }

    @Override
    public List<Double> getZValues() {
      return zValuesSet;
    }

    @Override
    public double getBoxHeight() {
      return dataPointHeight;
    }

    @Override
    public double getBoxWidth() {
      return dataPointWidth;
    }

    private void calculateDataPointSizeForPlots(List<RetentionTimeMobilityDataPoint> dataPoints) {
      SortedSet<RetentionTimeMobilityDataPoint> dataPointsSortedByRt =
          new TreeSet<>(new Comparator<RetentionTimeMobilityDataPoint>() {
            @Override
            public int compare(RetentionTimeMobilityDataPoint o1,
                RetentionTimeMobilityDataPoint o2) {
              if (o1.getRetentionTime() > o2.getRetentionTime()) {
                return 1;
              } else {
                return -1;
              }
            }
          });
      dataPointsSortedByRt.addAll(dataPoints);
      List<Float> rtDeltas = new ArrayList<>();
      RetentionTimeMobilityDataPoint dpA = null;
      for (RetentionTimeMobilityDataPoint dp : dataPointsSortedByRt) {
        if (dpA == null) {
          dpA = dp;
          dataPointHeight = Math.abs(dp.getMobilityWidth());
        } else if (!(dpA.getRetentionTime().equals(dp.getRetentionTime()))) {
          rtDeltas.add(dpA.getRetentionTime() - dp.getRetentionTime());
          dpA = dp;
        }
      }
      Collections.sort(rtDeltas);
      Float medianRt = 0.f;
      if (rtDeltas.size() >= 2) {
        if (rtDeltas.size() % 2 == 0) {
          int indexA = rtDeltas.size() / 2;
          int indexB = rtDeltas.size() / 2 - 1;
          medianRt = (rtDeltas.get(indexA) + rtDeltas.get(indexB)) / 2;
        } else {
          int index = rtDeltas.size() / 2;
          medianRt = rtDeltas.get(index);
        }
      }
      dataPointWidth = Math.abs(medianRt);
    }
  }

}

