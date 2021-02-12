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

package io.github.mzmine.modules.visualization.msms_new;

import com.google.common.collect.Range;
import com.google.common.primitives.Doubles;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYZDataProvider;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.javafx.FxColorUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.paint.Color;
import javafx.beans.property.SimpleObjectProperty;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jfree.chart.renderer.PaintScale;

public class MsMsDataset implements PlotXYZDataProvider {

  // Parameters

  // Basic parameters
  private final RawDataFile dataFile;
  private final Range<Float> rtRange;
  private final Range<Double> mzRange;
  private MsMsAxisType xAxisType;
  private MsMsAxisType yAxisType;

  private final Color color = MZmineCore.getConfiguration().getDefaultColorPalette().getNextColor();

  // Most intense fragments filtering
  IntensityFilteringType intensityFilterType;
  double intensityFilterValue;

  // Diagnostic fragmentation filtering
  private MZTolerance mzDifference;
  private List<Double> dffListMz;
  private List<Double> dffListNl;

  private final List<Scan> scans;
  private final List<MsMsDataPoint> dataPoints = new ArrayList<>();

  private int processedScans = 0;
  private double maxIntensity = 0;

  MsMsDataset(ParameterSet parameters) {
    // Basic parameters
    dataFile = parameters.getParameter(MsMsParameters.dataFiles).getValue()
        .getMatchingRawDataFiles()[0];
    rtRange = RangeUtils.toFloatRange(
        parameters.getParameter(MsMsParameters.retentionTimeRange).getValue());
    mzRange = parameters.getParameter(MsMsParameters.mzRange).getValue();
    xAxisType = parameters.getParameter(MsMsParameters.xAxisType).getValue();
    yAxisType = parameters.getParameter(MsMsParameters.yAxisType).getValue();

    int msLevel = parameters.getParameter(MsMsParameters.msLevel).getValue();
    if (msLevel < 2) {
      Alert alert = new Alert(AlertType.ERROR);
      alert.setTitle("Invalid MS level");
      alert.setHeaderText("MS level must be greater then 1 for the MS/MS visualizer");
      alert.showAndWait();
    }
    scans = dataFile.getScanNumbers(msLevel);

    // Most intense fragments filtering
    intensityFilterType
        = parameters.getParameter(MsMsParameters.intensityFiltering).getValue().getValueType();
    try {
      String intensityFilter = parameters.getParameter(MsMsParameters.intensityFiltering)
          .getValue().getFieldText();
      if (!intensityFilter.equals("")) {
        if (intensityFilterType == IntensityFilteringType.NUM_OF_BEST_FRAGMENTS) {
          intensityFilterValue = Integer.parseInt(intensityFilter);
        } else if (intensityFilterType == IntensityFilteringType.BASE_PEAK_PERCENT
            || intensityFilterType == IntensityFilteringType.INTENSITY_THRESHOLD) {
          intensityFilterValue = Double.parseDouble(intensityFilter);
        }
      }
    } catch (NumberFormatException exception) {
      Alert alert = new Alert(AlertType.ERROR);
      alert.setTitle("Invalid intensity filtering value level");
      alert.setHeaderText("Intensity filtering value must be a double number for \""
          + IntensityFilteringType.BASE_PEAK_PERCENT.toString()
          + "\" option and an integer number for \""
          + IntensityFilteringType.NUM_OF_BEST_FRAGMENTS.toString() + "\" option");
      alert.showAndWait();
    }

    // Diagnostic fragmentation filtering
    OptionalModuleParameter dffParameter = parameters.getParameter(MsMsParameters.dffParameters);
    if (dffParameter.getValue()) {
      ParameterSet dffParameters = dffParameter.getEmbeddedParameters();
      this.mzDifference = dffParameters.getParameter(MsMsParameters.mzDifference).getValue();
      this.dffListMz = dffParameters.getParameter(MsMsParameters.targetedMZ_List).getValue();
      this.dffListNl = dffParameters.getParameter(MsMsParameters.targetedNF_List).getValue();
    }

  }

  @Nonnull
  @Override
  public java.awt.Color getAWTColor() {
    return FxColorUtil.fxColorToAWT(color);
  }

  @Nonnull
  @Override
  public javafx.scene.paint.Color getFXColor() {
    return color;
  }

  @Nullable
  @Override
  public String getLabel(int index) {
    return null;
  }

  @Nonnull
  @Override
  public Comparable<?> getSeriesKey() {
    return "MS/MS dataset";
  }

  @Nullable
  @Override
  public String getToolTipText(int itemIndex) {
    return dataPoints.get(itemIndex).toString();
  }

  @Override
  public void computeValues(SimpleObjectProperty<TaskStatus> status) {

    // Do not recompute values if they are already present
    if (!dataPoints.isEmpty()) {
      return;
    }

    processedScans = 0;

    scansLoop:
    for (Scan scan : scans) {

      // Skip empty scans and check parent m/z and rt bounds
      if (scan == null || scan.getBasePeakMz() == null || !mzRange.contains(scan.getPrecursorMZ())
          || !rtRange.contains(scan.getRetentionTime())) {
        processedScans++;
        continue;
      }

      // Filter features
      List<Integer> filteredScanIndices = new ArrayList<>();

      // Intensity threshold
      if (intensityFilterType == IntensityFilteringType.BASE_PEAK_PERCENT
          || intensityFilterType == IntensityFilteringType.INTENSITY_THRESHOLD) {

        // Base peak percent
        double intensityThreshold = intensityFilterValue;
        if (intensityFilterType == IntensityFilteringType.BASE_PEAK_PERCENT) {
          intensityThreshold = scan.getBasePeakIntensity() * (intensityFilterValue / 100);
        }

        for (int scanIndex = 0; scanIndex < scan.getNumberOfDataPoints(); scanIndex++) {
          if (scan.getIntensityValue(scanIndex) >= intensityThreshold) {
            filteredScanIndices.add(scanIndex);
          }
        }

      // Number of most intense fragments
      } else if (intensityFilterType == IntensityFilteringType.NUM_OF_BEST_FRAGMENTS) {
        filteredScanIndices = IntStream.range(0, scan.getNumberOfDataPoints())
            .boxed().sorted((i, j) -> Doubles.compare(scan.getIntensityValue(j), scan.getIntensityValue(i)))
            .limit((int) intensityFilterValue).mapToInt(i -> i).boxed().collect(Collectors.toList());
      }

      for (int scanIndex : filteredScanIndices) {

        if (status.getValue() == TaskStatus.CANCELED) {
          return;
        }

        // Diagnostic fragmentation filtering (m/z)
        if (!(dffListMz == null || dffListMz.isEmpty())) {
          for (double targetMz : dffListMz) {
            if (!mzDifference.getToleranceRange(targetMz)
                .contains(scan.getMzValue(scanIndex))) {
              processedScans++;
              continue scansLoop;
            }
          }
        }

        // Diagnostic fragmentation filtering (neutral loss)
        if (!(dffListNl == null || dffListNl.isEmpty())) {
          double neutralLoss = scan.getPrecursorMZ() - scan.getMzValue(scanIndex);
          for (double targetNeutralLoss : dffListNl) {
            if (!mzDifference.getToleranceRange(targetNeutralLoss)
                .contains(neutralLoss)) {
              processedScans++;
              continue scansLoop;
            }
          }
        }

        // Base peak intensity
        double intensity = scan.getBasePeakIntensity();
        if (intensity > maxIntensity) {
          maxIntensity = intensity;
        }

        MsMsDataPoint newPoint = new MsMsDataPoint(scan.getMzValue(scanIndex), scan.getPrecursorMZ(),
            scan.getPrecursorCharge(), scan.getRetentionTime(), intensity);

        dataPoints.add(newPoint);

      }

      processedScans++;
    }

    // Show message, if there are nothing to plot
    if (dataPoints.isEmpty()) {
      Platform.runLater(() -> {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle("Suspicious module parameters");
        alert.setHeaderText("There are no data points satisfying module parameters in "
            + dataFile.getName() + " data file");
        alert.showAndWait();
      });
    }
  }

  @Override
  public double getDomainValue(int index) {
    return getValue(xAxisType, index);
  }

  @Override
  public double getRangeValue(int index) {
    return getValue(yAxisType, index);
  }

  private double getValue(MsMsAxisType axisType, int index) {
    return switch (axisType) {
      case PRODUCT_MZ -> dataPoints.get(index).getProductMZ();
      case PRECURSOR_MZ -> dataPoints.get(index).getPrecursorMass();
      case RETENTION_TIME -> dataPoints.get(index).getRetentionTime();
      case NEUTRAL_LOSS -> dataPoints.get(index).getNeutralLoss();
    };
  }

  @Override
  public int getValueCount() {
    return dataPoints.size();
  }

  @Override
  public double getComputationFinishedPercentage() {
    return processedScans / (double) scans.size();
  }

  @Nullable
  @Override
  public PaintScale getPaintScale () {
    return null;
  }

  @Override
  public double getZValue (int index) {
    double zValue = dataPoints.get(index).getIntensity();
    if (dataPoints.get(index).isHighlighted()) {
      return zValue / maxIntensity + 1;
    }
    return  zValue / maxIntensity;
  }

  @Nullable
  @Override
  public Double getBoxHeight () {
    return null;
  }

  @Nullable
  @Override
  public Double getBoxWidth () {
    return null;
  }

  public void setXAxisType(MsMsAxisType xAxisType) {
    this.xAxisType = xAxisType;
  }

  public void setYAxisType(MsMsAxisType yAxisType) {
    this.yAxisType = yAxisType;
  }

  public void highlightPrecursorMz(Range<Double> mzRange) {
    for (MsMsDataPoint dataPoint : dataPoints) {
      dataPoint.setHighlighted(mzRange.contains(dataPoint.getPrecursorMZ()));
    }
  }

}
