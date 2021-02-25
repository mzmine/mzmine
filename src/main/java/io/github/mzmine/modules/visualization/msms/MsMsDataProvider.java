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

  MsMsDataProvider(ParameterSet parameters) {

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
    Scan lastMS1Scan = null;

    scansLoop:
    for (Scan scan : allScans) {

      // Check the scan for the mass list of
      if (scan.getMassList() == null) {
        Platform.runLater(() -> {
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
      if (scan.getBasePeakMz() == null || !mzRange.contains(scan.getPrecursorMZ())
          || !rtRange.contains(scan.getRetentionTime())
          || scan.getMassList().getNumberOfDataPoints() < 1) {
        processedScans++;
        continue;
      }

      MassList massList = scan.getMassList();

      // Filter scans according to the input parameters
      List<Integer> filteredScanIndices = new ArrayList<>();
      if (intensityFiltering) {

        // Intensity threshold
        if (intensityFilterType == IntensityFilteringType.BASE_PEAK_PERCENT
            || intensityFilterType == IntensityFilteringType.INTENSITY_THRESHOLD) {

          // Test base peak percent condition
          double intensityThreshold = intensityFilterValue;
          if (intensityFilterType == IntensityFilteringType.BASE_PEAK_PERCENT) {
            intensityThreshold = scan.getBasePeakIntensity() * (intensityFilterValue / 100);
          }

          // Filter scans
          for (int scanIndex = 0; scanIndex < massList.getNumberOfDataPoints(); scanIndex++) {
            if (massList.getIntensityValue(scanIndex) >= intensityThreshold) {
              filteredScanIndices.add(scanIndex);
            }
          }

          // Number of most intense fragments
        } else if (intensityFilterType == IntensityFilteringType.NUM_OF_BEST_FRAGMENTS) {
          filteredScanIndices = IntStream.range(0, massList.getNumberOfDataPoints() - 1)
              .boxed().sorted((i, j)
                  -> Doubles.compare(massList.getIntensityValue(j), massList.getIntensityValue(i)))
              .limit((int) intensityFilterValue).mapToInt(i -> i).boxed()
              .collect(Collectors.toList());
        }
      } else {
        filteredScanIndices = IntStream.rangeClosed(0, massList.getNumberOfDataPoints() - 1).boxed()
            .collect(Collectors.toList());
      }

      // Precursor intensity
      double precursorIntensity = 0;
      if (lastMS1Scan != null) {

        // Sum intensities of all ions from MS1 scan with similar m/z values
        MassList lastMS1ScanMassList = lastMS1Scan.getMassList();
        Range<Double> toleranceRange = mzTolerance.getToleranceRange(scan.getPrecursorMZ());
        for (int i = 0; i < lastMS1ScanMassList.getNumberOfDataPoints(); i++) {
          if (toleranceRange.contains(lastMS1ScanMassList.getMzValue(i))) {
            precursorIntensity += lastMS1ScanMassList.getIntensityValue(i);
          }
        }
      }

      // Scale precursor intensity
      precursorIntensity = scaleIntensity(precursorIntensity);

      // Find max precursor intensity for further normalization
      if (precursorIntensity > maxPrecursorIntensity) {
        maxPrecursorIntensity = precursorIntensity;
      }

      // Find max rt for further normalization
      if (scan.getRetentionTime() > maxRt) {
        maxRt = scan.getRetentionTime();
      }

      for (int scanIndex : filteredScanIndices) {

        if (status.getValue() == TaskStatus.CANCELED) {
          return;
        }

        double mzValue = massList.getMzValue(scanIndex);
        double intensityValue = massList.getIntensityValue(scanIndex);

        // Diagnostic fragmentation filtering (m/z)
        if (!(dffListMz == null || dffListMz.isEmpty())) {
          Range<Double> toleranceRange = mzTolerance.getToleranceRange(mzValue);
          for (double targetMz : dffListMz) {
            if (!toleranceRange.contains(targetMz)) {
              processedScans++;
              continue scansLoop;
            }
          }
        }

        // Diagnostic fragmentation filtering (neutral loss)
        if (!(dffListNl == null || dffListNl.isEmpty())) {
          double neutralLoss = scan.getPrecursorMZ() - mzValue;
          Range<Double> toleranceRange = mzTolerance.getToleranceRange(neutralLoss);
          for (double targetNeutralLoss : dffListNl) {
            if (!toleranceRange.contains(targetNeutralLoss)) {
              processedScans++;
              continue scansLoop;
            }
          }
        }

        // Product intensity(scaled)
        double productIntensity = scaleIntensity(intensityValue);
        if (productIntensity > maxProductIntensity) {
          maxProductIntensity = productIntensity;
        }

        // Create new data point
        MsMsDataPoint newPoint = new MsMsDataPoint(scan.getScanNumber(), mzValue, scan.getPrecursorMZ(),
            scan.getPrecursorCharge(), scan.getRetentionTime(), productIntensity, precursorIntensity);

        dataPoints.add(newPoint);

      }

      processedScans++;
    }

    System.out.println("[DEBUG] Total number of data points: " + dataPoints.size());

    // Show message, if there is nothing to plot
    if (dataPoints.isEmpty()) {
      Platform.runLater(() -> {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle("Suspicious module parameters");
        alert.setHeaderText("There are no data points in " + dataFile.getName()
            + " data file, satisfying module parameters");
        alert.showAndWait();
      });
    }
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
    return switch (axisType) {
      case PRODUCT_MZ -> dataPoints.get(index).getProductMZ();
      case PRECURSOR_MZ -> dataPoints.get(index).getPrecursorMz();
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
    return processedScans / (double) allScans.size();
  }

  @Nullable
  @Override
  public PaintScale getPaintScale () {
    return paintScale;
  }

  @Override
  public double getZValue (int index) {
    double zValue = switch (zAxisType) {
      case PRECURSOR_INTENSITY -> dataPoints.get(index).getPrecursorIntensity() / maxPrecursorIntensity;
      case PRODUCT_INTENSITY -> dataPoints.get(index).getProductIntensity() / maxProductIntensity;
      case RETENTION_TIME -> dataPoints.get(index).getRetentionTime() / maxRt;
    };

    if (dataPoints.get(index).isHighlighted()) {
      zValue += 1;
    }

    return zValue;
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

  public void setXAxisType(MsMsXYAxisType xAxisType) {
    this.xAxisType = xAxisType;
  }

  public void setYAxisType(MsMsXYAxisType yAxisType) {
    this.yAxisType = yAxisType;
  }

  public void setZAxisType(MsMsZAxisType zAxisType) {
    this.zAxisType = zAxisType;
  }

  public void highlightPrecursorMz(Range<Double> mzRange) {
    for (MsMsDataPoint dataPoint : dataPoints) {
      dataPoint.setHighlighted(mzRange.contains(dataPoint.getPrecursorMz()));
    }
  }

  private double scaleIntensity(double intensity) {
    return Math.pow(intensity, 0.2);
  }

  public MsMsDataPoint getDataPoint(int index) {
    return dataPoints.get(index);
  }

  public void setDataFile(RawDataFile dataFile) {
    dataPoints.clear();
    allScans = dataFile.getScans();
  }

}
