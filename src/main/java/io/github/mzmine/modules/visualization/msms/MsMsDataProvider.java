/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.visualization.msms;

import com.google.common.collect.Range;
import com.google.common.primitives.Doubles;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYZDataProvider;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboFieldParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.javafx.FxColorUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.util.SortOrder;

public class MsMsDataProvider implements PlotXYZDataProvider {

  private static final PaintScale paintScale = new MsMsPaintScale();

  // Parameters

  // Basic parameters
  private final RawDataFile dataFile;
  private final Range<Float> rtRange;
  private final Range<Double> mzRange;
  private MsMsXYAxisType xAxisType;
  private MsMsXYAxisType yAxisType;
  private MsMsZAxisType zAxisType;
  int msLevel;
  private final MZTolerance mzTolerance;

  private final Color color = MZmineCore.getConfiguration().getDefaultColorPalette().getNextColor();

  // Most intense fragments filtering
  boolean intensityFiltering;
  IntensityFilteringType intensityFilterType;
  double intensityFilterValue;

  // Diagnostic fragmentation filtering
  private List<Double> dffListMz;
  private List<Double> dffListNl;

  private List<Scan> allScans;
  private final List<MsMsDataPoint> dataPoints = new ArrayList<>();

  private int processedScans = 0;
  private double maxProductIntensity = 0;
  private double maxPrecursorIntensity = 0;
  private float maxRt = 0;

  public MsMsDataProvider(ParameterSet parameters) {

    // Basic parameters
    dataFile = parameters.getParameter(MsMsParameters.dataFiles).getValue()
        .getMatchingRawDataFiles()[0];
    rtRange = RangeUtils.toFloatRange(
        parameters.getParameter(MsMsParameters.rtRange).getValue());
    mzRange = parameters.getParameter(MsMsParameters.mzRange).getValue();
    xAxisType = parameters.getParameter(MsMsParameters.xAxisType).getValue();
    yAxisType = parameters.getParameter(MsMsParameters.yAxisType).getValue();
    zAxisType = parameters.getParameter(MsMsParameters.zAxisType).getValue();
    mzTolerance = parameters.getParameter(MsMsParameters.mzTolerance).getValue();

    msLevel = parameters.getParameter(MsMsParameters.msLevel).getValue();
    if (msLevel < 2) {
      Alert alert = new Alert(AlertType.ERROR);
      alert.setTitle("Invalid MS level");
      alert.setHeaderText("MS level must be greater then 1 for the MS/MS visualizer");
      alert.showAndWait();
    }
    allScans = dataFile.getScans();

    // Most intense fragments filtering
    OptionalParameter<ComboFieldParameter<IntensityFilteringType>> intensityFilterParameter
        = parameters.getParameter(MsMsParameters.intensityFiltering);
    intensityFiltering = intensityFilterParameter.getValue();
    if (intensityFiltering) {
      try {
        ComboFieldParameter<IntensityFilteringType> intensityFilterTypeParameter
            = intensityFilterParameter.getEmbeddedParameter();

        String intensityFilter = intensityFilterTypeParameter.getValue().getFieldText();
        intensityFilterType = intensityFilterTypeParameter.getValue().getValueType();

        // Parse the filed depending on the selected filtering option
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
    }

    // Diagnostic fragmentation filtering
    OptionalModuleParameter<?> dffParameter = parameters.getParameter(MsMsParameters.dffParameters);
    if (dffParameter.getValue()) {
      ParameterSet dffParameters = dffParameter.getEmbeddedParameters();
      dffListMz = dffParameters.getParameter(MsMsParameters.targetedMZ_List).getValue();
      dffListNl = dffParameters.getParameter(MsMsParameters.targetedNF_List).getValue();
    }

  }

  @NotNull
  @Override
  public java.awt.Color getAWTColor() {
    return FxColorUtil.fxColorToAWT(color);
  }

  @NotNull
  @Override
  public javafx.scene.paint.Color getFXColor() {
    return color;
  }

  @Nullable
  @Override
  public String getLabel(int index) {
    return null;
  }

  @NotNull
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
    Scan lastMS1Scan = null;

    scansLoop:
    for (Scan scan : allScans) {

      // Check the scan for the mass list of
      if (scan.getMassList() == null) {
        status.setValue(TaskStatus.CANCELED);
        MZmineCore.runLater(() -> {
          Alert alert = new Alert(AlertType.ERROR);
          alert.setTitle("Mass detection issue");
          alert.setHeaderText("Masses are not detected properly for the " + dataFile.getName()
              + " data file. Scan #" + scan.getScanNumber() + " has no mass list. Run"
              + " \"Raw data methods\" -> \"Mass detection\", if you haven't done it yet.");
          alert.showAndWait();
        });
        return;
      }

      if (scan.getMSLevel() != 1 && scan.getMSLevel() != msLevel) {
        processedScans++;
        continue;
      }

      // Save current MS1 scan to store the intensity of precursor ion
      if (scan.getMSLevel() == 1) {
        lastMS1Scan = scan;
        processedScans++;
        continue;
      }

      // Skip empty scans and check parent m/z and rt bounds
      int precursorCharge = Objects.requireNonNullElse(scan.getPrecursorCharge(), -1);

      if (scan.getBasePeakMz() == null || !mzRange.contains(scan.getPrecursorMz())
          || !rtRange.contains(scan.getRetentionTime())
          || scan.getMassList().getNumberOfDataPoints() < 1) {
        processedScans++;
        continue;
      }

      MassList massList = scan.getMassList();

      // Filter scans according to the input parameters
      List<Integer> filteredPointsIndices = new ArrayList<>();
      if (intensityFiltering) {

        // Intensity threshold
        if (intensityFilterType == IntensityFilteringType.BASE_PEAK_PERCENT
            || intensityFilterType == IntensityFilteringType.INTENSITY_THRESHOLD) {

          // Test base peak percent condition
          double intensityThreshold = intensityFilterValue;
          if (intensityFilterType == IntensityFilteringType.BASE_PEAK_PERCENT) {
            intensityThreshold = massList.getBasePeakIntensity() * (intensityFilterValue / 100);
          }

          // Filter scans
          for (int pointIndex = 0; pointIndex < massList.getNumberOfDataPoints(); pointIndex++) {
            if (massList.getIntensityValue(pointIndex) >= intensityThreshold) {
              filteredPointsIndices.add(pointIndex);
            }
          }

          // Number of most intense fragments
        } else if (intensityFilterType == IntensityFilteringType.NUM_OF_BEST_FRAGMENTS) {
          filteredPointsIndices = IntStream.range(0, massList.getNumberOfDataPoints() - 1)
              .boxed().sorted((i, j)
                  -> Doubles.compare(massList.getIntensityValue(j), massList.getIntensityValue(i)))
              .limit((int) intensityFilterValue).mapToInt(i -> i).boxed()
              .collect(Collectors.toList());
        }
      } else {
        filteredPointsIndices = IntStream.rangeClosed(0, massList.getNumberOfDataPoints() - 1).boxed()
            .collect(Collectors.toList());
      }

      // Precursor intensity
      double precursorIntensity = 0;
      double precursorMz = Objects.requireNonNullElse(scan.getPrecursorMz(), -1d);
      if (lastMS1Scan != null) {

        // Sum intensities of all ions from MS1 scan with similar m/z values
        MassList lastMS1ScanMassList = lastMS1Scan.getMassList();
        Range<Double> toleranceRange = mzTolerance.getToleranceRange(precursorMz);
        for (int i = 0; i < lastMS1ScanMassList.getNumberOfDataPoints(); i++) {
          if (toleranceRange.contains(lastMS1ScanMassList.getMzValue(i))) {
            precursorIntensity += lastMS1ScanMassList.getIntensityValue(i);
          }
        }
      }

      // Find max precursor intensity for further normalization
      if (precursorIntensity > maxPrecursorIntensity) {
        maxPrecursorIntensity = precursorIntensity;
      }

      // Find max rt for further normalization
      if (scan.getRetentionTime() > maxRt) {
        maxRt = scan.getRetentionTime();
      }

      for (int pointIndex : filteredPointsIndices) {

        if (status.getValue() == TaskStatus.CANCELED) {
          return;
        }

        double productMz = massList.getMzValue(pointIndex);
        double productIntensity = massList.getIntensityValue(pointIndex);

        // Diagnostic fragmentation filtering (m/z)
        if (!(dffListMz == null || dffListMz.isEmpty())) {
          Range<Double> toleranceRange = mzTolerance.getToleranceRange(productMz);
          for (double targetMz : dffListMz) {
            if (!toleranceRange.contains(targetMz)) {
              processedScans++;
              continue scansLoop;
            }
          }
        }

        // Diagnostic fragmentation filtering (neutral loss)
        if (!(dffListNl == null || dffListNl.isEmpty())) {
          double neutralLoss = precursorMz - productMz;
          Range<Double> toleranceRange = mzTolerance.getToleranceRange(neutralLoss);
          for (double targetNeutralLoss : dffListNl) {
            if (!toleranceRange.contains(targetNeutralLoss)) {
              processedScans++;
              continue scansLoop;
            }
          }
        }

        // Product intensity
        if (productIntensity > maxProductIntensity) {
          maxProductIntensity = productIntensity;
        }

        // Create new data point
        MsMsDataPoint newPoint = new MsMsDataPoint(scan.getScanNumber(), productMz, precursorMz,
            precursorCharge, scan.getRetentionTime(), productIntensity, precursorIntensity);

        dataPoints.add(newPoint);
      }

      processedScans++;
    }

    // Show message, if there is nothing to plot
    if (dataPoints.isEmpty()) {
      status.setValue(TaskStatus.CANCELED);
      MZmineCore.runLater(() -> {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle("Suspicious module parameters");
        alert.setHeaderText("There are no data points in " + dataFile.getName()
            + " data file, satisfying module parameters");
        alert.showAndWait();
      });
      return;
    }

    // Scale max intensities
    maxProductIntensity = scaleProductIntensity(maxProductIntensity);
    maxPrecursorIntensity = scalePrecursorIntensity(maxPrecursorIntensity);
  }

  @Override
  public double getDomainValue(int index) {
    return getXYValue(xAxisType, index);
  }

  @Override
  public double getRangeValue(int index) {
    return getXYValue(yAxisType, index);
  }

  private double getXYValue(MsMsXYAxisType axisType, int index) {
    return getXYValue(axisType, dataPoints.get(index));
  }

  private double getXYValue(MsMsXYAxisType axisType, MsMsDataPoint dataPoint) {
    return switch (axisType) {
      case PRODUCT_MZ -> dataPoint.getProductMZ();
      case PRECURSOR_MZ -> dataPoint.getPrecursorMz();
      case RETENTION_TIME -> dataPoint.getRetentionTime();
      case NEUTRAL_LOSS -> dataPoint.getNeutralLoss();
    };
  }

  @Override
  public int getValueCount() {
    return dataPoints.size();
  }

  @Override
  public double getComputationFinishedPercentage() {
    return processedScans / (double) allScans.size();
  }

  @Nullable
  @Override
  public PaintScale getPaintScale() {
    return paintScale;
  }

  @Override
  public double getZValue(int index) {
    // Z value given by data point + int increment corresponding to highlight color
    return getRawZValue(dataPoints.get(index)) + dataPoints.get(index).getHighlighted();
  }

  private double getRawZValue(MsMsDataPoint dataPoint) {
    return switch (zAxisType) {
      case PRECURSOR_INTENSITY
          -> scalePrecursorIntensity(dataPoint.getPrecursorIntensity()) / maxPrecursorIntensity;
      case PRODUCT_INTENSITY
          -> scaleProductIntensity(dataPoint.getProductIntensity()) / maxProductIntensity;
      case RETENTION_TIME -> dataPoint.getRetentionTime() / maxRt;
    };
  }

  private double scalePrecursorIntensity(double intensity) {
    return Math.pow(intensity, 0.2);
  }

  private double scaleProductIntensity(double intensity) {
    return Math.pow(intensity, 0.15);
  }

  @Nullable
  @Override
  public Double getBoxHeight() {
    return null;
  }

  @Nullable
  @Override
  public Double getBoxWidth() {
    return null;
  }

  public MsMsXYAxisType getXAxisType() {
    return xAxisType;
  }

  public MsMsXYAxisType getYAxisType() {
    return yAxisType;
  }

  public void setXAxisType(MsMsXYAxisType xAxisType) {
    this.xAxisType = xAxisType;
  }

  public void setYAxisType(MsMsXYAxisType yAxisType) {
    this.yAxisType = yAxisType;
  }

  public void setZAxisType(MsMsZAxisType zAxisType) {
    this.zAxisType = zAxisType;
  }

  public void highlightPoints(MsMsXYAxisType valuesType1, @Nullable Range<Double> range1,
      MsMsXYAxisType valuesType2, @Nullable Range<Double> range2) {

    // Loop over data points and set highlight type given by input ranges
    int highlightType;
    for (MsMsDataPoint dataPoint : dataPoints) {
      highlightType = 0;
      if (range1 != null && range1.contains(getXYValue(valuesType1, dataPoint))) {
        highlightType += 1;
      }

      if (range2 != null && range2.contains(getXYValue(valuesType2, dataPoint))) {
        highlightType += 2;
      }

      dataPoint.setHighlighted(highlightType);
    }
  }

  public MsMsDataPoint getDataPoint(int index) {
    return dataPoints.get(index);
  }

  public void setDataFile(RawDataFile dataFile) {
    dataPoints.clear();
    allScans = dataFile.getScans();
  }

  public void sortZValues(@Nullable SortOrder sortOrder) {
    if (sortOrder == SortOrder.ASCENDING) {
      dataPoints.sort((a, b) -> Doubles.compare(getRawZValue(a), getRawZValue(b)));
    } else if (sortOrder == SortOrder.DESCENDING) {
      dataPoints.sort((a, b) -> Doubles.compare(getRawZValue(b), getRawZValue(a)));
    }
  }

}
