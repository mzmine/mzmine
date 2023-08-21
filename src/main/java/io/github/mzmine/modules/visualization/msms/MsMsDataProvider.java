/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.msms;

import static java.util.Objects.requireNonNullElse;

import com.google.common.collect.Range;
import com.google.common.primitives.Doubles;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYZDataProvider;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboFieldParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.javafx.FxColorUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javafx.beans.property.Property;
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
  private final MZTolerance mzTolerance;
  private final Color color = MZmineCore.getConfiguration().getDefaultColorPalette().getNextColor();
  private final List<MsMsDataPoint> dataPoints = new ArrayList<>();
  MsLevelFilter msLevel;
  // Most intense fragments filtering
  boolean intensityFiltering;
  IntensityFilteringType intensityFilterType;
  double intensityFilterValue;
  private MsMsXYAxisType xAxisType;
  private MsMsXYAxisType yAxisType;
  private MsMsZAxisType zAxisType;
  // Diagnostic fragmentation filtering
  private List<Double> dffListMz;
  private List<Double> dffListNl;
  private List<Scan> allScans;
  private int processedScans = 0;
  private double maxProductIntensity = 0;
  private double maxPrecursorIntensity = 0;
  private float maxRt = 0;

  public MsMsDataProvider(ParameterSet parameters) {

    // Basic parameters
    dataFile = parameters.getParameter(MsMsParameters.dataFiles).getValue()
        .getMatchingRawDataFiles()[0];
    rtRange = RangeUtils.toFloatRange(
        parameters.getEmbeddedParameterValueIfSelectedOrElse(MsMsParameters.rtRange, Range.all()));
    mzRange = parameters.getEmbeddedParameterValueIfSelectedOrElse(MsMsParameters.mzRange,
        Range.all());
    xAxisType = parameters.getParameter(MsMsParameters.xAxisType).getValue();
    yAxisType = parameters.getParameter(MsMsParameters.yAxisType).getValue();
    zAxisType = parameters.getParameter(MsMsParameters.zAxisType).getValue();
    mzTolerance = parameters.getParameter(MsMsParameters.mzTolerance).getValue();

    msLevel = parameters.getParameter(MsMsParameters.msLevel).getValue();
    if (msLevel.isMs1Only()) {
      Alert alert = new Alert(AlertType.ERROR);
      alert.setTitle("Invalid MS level");
      alert.setHeaderText("MS level must be greater then 1 for the MS/MS visualizer");
      alert.showAndWait();
    }
    allScans = dataFile.getScans();

    // Most intense fragments filtering
    OptionalParameter<ComboFieldParameter<IntensityFilteringType>> intensityFilterParameter = parameters.getParameter(
        MsMsParameters.intensityFiltering);
    intensityFiltering = intensityFilterParameter.getValue();
    if (intensityFiltering) {
      try {
        ComboFieldParameter<IntensityFilteringType> intensityFilterTypeParameter = intensityFilterParameter.getEmbeddedParameter();

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
                            + IntensityFilteringType.BASE_PEAK_PERCENT
                            + "\" option and an integer number for \""
                            + IntensityFilteringType.NUM_OF_BEST_FRAGMENTS + "\" option");
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
  public void computeValues(Property<TaskStatus> status) {

    // Do not recompute values if they are already present
    if (!dataPoints.isEmpty()) {
      return;
    }

    processedScans = 0;
    Scan lastMS1Scan = null;

    scansLoop:
    for (Scan scan : allScans) {

      // Check the scan for the mass list or the scan is centroided
      if (scan.getMassList() == null && !scan.getSpectrumType().isCentroided()) {
        status.setValue(TaskStatus.CANCELED);
        MZmineCore.runLater(() -> {
          Alert alert = new Alert(AlertType.ERROR);
          alert.setTitle("Mass detection issue");
          alert.setHeaderText("Masses are not detected properly for the " + dataFile.getName()
                              + " data file. Scan #" + scan.getScanNumber()
                              + " has no mass list. Run"
                              + " \"Raw data methods\" -> \"Mass detection\", if you haven't done it yet. Scans are marked as profile mode.");
          alert.showAndWait();
        });
        return;
      }

      if (!msLevel.accept(scan)) {
        processedScans++;
        // Save current MS1 scan to store the intensity of precursor ion
        if (scan.getMSLevel() == 1) {
          lastMS1Scan = scan;
        }
        continue;
      }

      // Skip empty scans and check parent m/z and rt bounds
      int precursorCharge = requireNonNullElse(scan.getPrecursorCharge(), -1);

      // use mass list if not null or else use raw centroid scan (checked before)
      MassSpectrum masses = requireNonNullElse(scan.getMassList(), scan);

      if (scan.getBasePeakMz() == null || !mzRange.contains(scan.getPrecursorMz())
          || !rtRange.contains(scan.getRetentionTime()) || masses.getNumberOfDataPoints() < 1) {
        processedScans++;
        continue;
      }

      // Filter scans according to the input parameters
      List<Integer> filteredPointsIndices = new ArrayList<>();
      if (intensityFiltering) {

        // Intensity threshold
        if (intensityFilterType == IntensityFilteringType.BASE_PEAK_PERCENT
            || intensityFilterType == IntensityFilteringType.INTENSITY_THRESHOLD) {

          // Test base peak percent condition
          double intensityThreshold = intensityFilterValue;
          if (intensityFilterType == IntensityFilteringType.BASE_PEAK_PERCENT) {
            intensityThreshold = masses.getBasePeakIntensity() * (intensityFilterValue / 100);
          }

          // Filter scans
          for (int pointIndex = 0; pointIndex < masses.getNumberOfDataPoints(); pointIndex++) {
            if (masses.getIntensityValue(pointIndex) >= intensityThreshold) {
              filteredPointsIndices.add(pointIndex);
            }
          }

          // Number of most intense fragments
        } else if (intensityFilterType == IntensityFilteringType.NUM_OF_BEST_FRAGMENTS) {
          filteredPointsIndices = IntStream.range(0, masses.getNumberOfDataPoints() - 1).boxed()
              .sorted((i, j) -> Doubles.compare(masses.getIntensityValue(j),
                  masses.getIntensityValue(i))).limit((int) intensityFilterValue).mapToInt(i -> i)
              .boxed().collect(Collectors.toList());
        }
      } else {
        filteredPointsIndices = IntStream.rangeClosed(0, masses.getNumberOfDataPoints() - 1).boxed()
            .collect(Collectors.toList());
      }

      // Precursor intensity
      double precursorIntensity = 0;
      double precursorMz = requireNonNullElse(scan.getPrecursorMz(), -1d);
      if (lastMS1Scan != null) {

        // Sum intensities of all ions from MS1 scan with similar m/z values
        MassSpectrum lastMS1ScanMassList = requireNonNullElse(lastMS1Scan.getMassList(),
            lastMS1Scan);
        Range<Double> toleranceRange = mzTolerance.getToleranceRange(precursorMz);
        int ind = lastMS1ScanMassList.binarySearch(precursorMz, true);
        for (int i = ind; i < lastMS1ScanMassList.getNumberOfDataPoints(); i++) {
          if (toleranceRange.contains(lastMS1ScanMassList.getMzValue(i))) {
            precursorIntensity += lastMS1ScanMassList.getIntensityValue(i);
          } else {
            break;
          }
        }
        for (int i = ind - 1; i >= 0; i--) {
          if (toleranceRange.contains(lastMS1ScanMassList.getMzValue(i))) {
            precursorIntensity += lastMS1ScanMassList.getIntensityValue(i);
          } else {
            break;
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

        double productMz = masses.getMzValue(pointIndex);
        double productIntensity = masses.getIntensityValue(pointIndex);

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
      case PRECURSOR_INTENSITY ->
          scalePrecursorIntensity(dataPoint.getPrecursorIntensity()) / maxPrecursorIntensity;
      case PRODUCT_INTENSITY ->
          scaleProductIntensity(dataPoint.getProductIntensity()) / maxProductIntensity;
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

  public void setXAxisType(MsMsXYAxisType xAxisType) {
    this.xAxisType = xAxisType;
  }

  public MsMsXYAxisType getYAxisType() {
    return yAxisType;
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
