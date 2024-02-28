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

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleFeatureInformation;
import io.github.mzmine.datamodel.msms.DDAMsMsInfo;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.javafx.FxColorUtil;
import io.github.mzmine.util.maths.CenterFunction;
import io.github.mzmine.util.scans.ScanUtils;
import java.awt.Color;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.beans.property.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * ResolvedPeak
 */
@Deprecated
public class ResolvedPeak implements PlotXYDataProvider {

  private SimpleFeatureInformation peakInfo;

  // Data file of this chromatogram
  private final RawDataFile dataFile;

  // Chromatogram m/z, RT, height, area
  private final double mz;
  private final Double tf = null;
  private final Double af = null;
  // Scan numbers
  private final Scan[] scanNumbers;
  // We store the values of data points as double[] arrays in order to save
  // memory, which would be wasted by keeping a lot of instances of
  // SimpleDataPoint (each instance takes 16 or 32 bytes of extra memory)
  private final double[] dataPointMZValues;
  private final double[] dataPointIntensityValues;
  // All MS2 fragment scan numbers
  private final List<Scan> allMS2FragmentScanNumbers;
  private final javafx.scene.paint.Color color;
  private double rt;
  private double height;

  // Top intensity scan, fragment scan
  private Scan representativeScan;
  private double area;

  // Ranges of raw data points
  private Range<Double> rawDataPointsMZRange;
  private Range<Float> rawDataPointsIntensityRange, rawDataPointsRTRange;

  // Isotope pattern. Null by default but can be set later by deisotoping
  // method.
  private IsotopePattern isotopePattern = null;
  private int charge = 0;

  // PeakListRow.ID of the chromatogram where this feature is detected. Null
  // by default but can be
  // set by
  // chromatogram deconvolution method.
  private Integer parentChromatogramRowID = null;
  private FeatureList peakList;
  private Double fwhm = null;

  /**
   * Initializes this peak using data points from a given chromatogram - regionStart marks the index
   * of the first data point (inclusive), regionEnd marks the index of the last data point
   * (inclusive). The selected region MUST NOT contain any zero-intensity data points, otherwise
   * exception is thrown.
   */
  public ResolvedPeak(Feature chromatogram, int regionStart, int regionEnd,
      CenterFunction mzCenterFunction, double msmsRange, float RTRangeMSMS) {

    assert regionEnd >= regionStart;

    this.peakList = chromatogram.getFeatureList();
    this.dataFile = chromatogram.getRawDataFile();

    color = MZmineCore.getConfiguration().getDefaultColorPalette().getNextColor();

    // Make an array of scan numbers of this peak
    scanNumbers = new Scan[regionEnd - regionStart + 1];

    Scan[] chromatogramScanNumbers = chromatogram.getScanNumbers().stream().toArray(Scan[]::new);

    System.arraycopy(chromatogramScanNumbers, regionStart, scanNumbers, 0,
        regionEnd - regionStart + 1);

    dataPointMZValues = new double[regionEnd - regionStart + 1];
    dataPointIntensityValues = new double[regionEnd - regionStart + 1];

    // Set raw data point ranges, height, rt and representative scan
    height = Double.MIN_VALUE;

    double mzValue = chromatogram.getMZ();
    for (int i = 0; i < scanNumbers.length; i++) {

      dataPointMZValues[i] = mzValue;

      if (chromatogram instanceof ModularFeature) {
        final IonTimeSeries<? extends Scan> data = chromatogram.getFeatureData();
        dataPointMZValues[i] = data.getMzForSpectrum(scanNumbers[i]);
        dataPointIntensityValues[i] = data.getIntensityForSpectrum(scanNumbers[i]);
      } else {
        DataPoint dp = chromatogram.getDataPoint(scanNumbers[i]);

        if (dp == null) {
          continue;
          /*
           * String error =
           * "Cannot create a resolved peak in a region with missing data points: chromatogram " +
           * chromatogram + " scans " + chromatogramScanNumbers[regionStart] + "-" +
           * chromatogramScanNumbers[regionEnd] + ", missing data point in scan " + scanNumbers[i];
           *
           * throw new IllegalArgumentException(error);
           */
        }

        dataPointMZValues[i] = dp.getMZ();
        dataPointIntensityValues[i] = dp.getIntensity();
      }

      if (rawDataPointsIntensityRange == null) {
        rawDataPointsIntensityRange = Range.singleton((float) dataPointMZValues[i]);
        rawDataPointsRTRange = Range.singleton(scanNumbers[i].getRetentionTime());
        rawDataPointsMZRange = Range.singleton(dataPointMZValues[i]);
      } else {
        rawDataPointsRTRange = rawDataPointsRTRange.span(
            Range.singleton(scanNumbers[i].getRetentionTime()));
        rawDataPointsIntensityRange = rawDataPointsIntensityRange.span(
            Range.singleton((float) dataPointIntensityValues[i]));
        rawDataPointsMZRange = rawDataPointsMZRange.span(Range.singleton(dataPointMZValues[i]));
      }

      if (height < dataPointMZValues[i]) {
        height = dataPointMZValues[i];
        rt = scanNumbers[i].getRetentionTime();
        representativeScan = scanNumbers[i];
      }
    }

    // Calculate m/z as median, average or weighted-average
    mz = mzCenterFunction.calcCenter(dataPointMZValues, dataPointIntensityValues);

    // Update area
    area = 0;
    for (int i = 1; i < scanNumbers.length; i++) {

      // For area calculation, we use retention time in seconds
      double previousRT = scanNumbers[i - 1].getRetentionTime() * 60d;
      double currentRT = scanNumbers[i].getRetentionTime() * 60d;

      double previousHeight = dataPointIntensityValues[i - 1];
      double currentHeight = dataPointIntensityValues[i];
      area += (currentRT - previousRT) * (currentHeight + previousHeight) / 2;
    }

    // Update fragment scan
    double lowerBound = rawDataPointsMZRange.lowerEndpoint();
    double upperBound = rawDataPointsMZRange.upperEndpoint();
    double mid = (upperBound + lowerBound) / 2;
    lowerBound = mid - msmsRange / 2;
    upperBound = mid + msmsRange / 2;
    if (lowerBound < 0) {
      lowerBound = 0;
    }
    Range<Double> searchingRange = Range.closed(lowerBound, upperBound);
    float lowerBoundRT = rawDataPointsRTRange.lowerEndpoint();
    float upperBoundRT = rawDataPointsRTRange.upperEndpoint();
    float midRT = (upperBoundRT + lowerBoundRT) / 2;
    lowerBoundRT = midRT - RTRangeMSMS / 2;
    upperBoundRT = midRT + RTRangeMSMS / 2;
    if (lowerBound < 0) {
      lowerBound = 0;
    }
    Range<Float> searchingRangeRT = Range.closed(lowerBoundRT, upperBoundRT);

    if (msmsRange == 0) {
      searchingRange = rawDataPointsMZRange;
    }
    if (RTRangeMSMS == 0) {
      searchingRangeRT = rawDataPointsRTRange;
    }

    allMS2FragmentScanNumbers = ScanUtils.streamAllMS2FragmentScans(dataFile, searchingRangeRT,
        searchingRange).toList();

    if (!allMS2FragmentScanNumbers.isEmpty()) {
      Scan fragmentScan = allMS2FragmentScanNumbers.get(0);
      int precursorCharge = fragmentScan.getMsMsInfo() != null
          && fragmentScan.getMsMsInfo() instanceof DDAMsMsInfo dda
          && dda.getPrecursorCharge() != null ? dda.getPrecursorCharge() : 0;
      if (precursorCharge > 0) {
        this.charge = precursorCharge;
      }
    }

  }

  /**
   * This method returns a representative datapoint of this peak in a given scan
   */
  public DataPoint getDataPoint(int scanNumber) {
    int index = Arrays.binarySearch(scanNumbers, scanNumber);
    if (index < 0) {
      return null;
    }
    return new SimpleDataPoint(dataPointMZValues[index], dataPointIntensityValues[index]);
  }

  /**
   * This method returns m/z value of the chromatogram
   */
  public double getMZ() {
    return mz;
  }

  /**
   * This method returns a string with the basic information that defines this peak
   *
   * @return String information
   */
  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    Format mzFormat = MZmineCore.getConfiguration().getMZFormat();
    Format timeFormat = MZmineCore.getConfiguration().getRTFormat();
    buf.append("m/z ");
    buf.append(mzFormat.format(getMZ()));
    buf.append(" (");
    buf.append(timeFormat.format(getRT()));
    buf.append(" min) [").append(getRawDataFile().getName()).append("]");
    return buf.toString();
  }

  public double getArea() {
    return area;
  }

  public double getHeight() {
    return height;
  }

  public List<Scan> getAllMS2FragmentScanNumbers() {
    return allMS2FragmentScanNumbers;
  }

  public @NotNull FeatureStatus getFeatureStatus() {
    return FeatureStatus.DETECTED;
  }

  public double getRT() {
    return rt;
  }

  public @NotNull Range<Float> getRawDataPointsIntensityRange() {
    return rawDataPointsIntensityRange;
  }

  public @NotNull Range<Double> getRawDataPointsMZRange() {
    return rawDataPointsMZRange;
  }

  public @NotNull Range<Float> getRawDataPointsRTRange() {
    return rawDataPointsRTRange;
  }

  public Scan getRepresentativeScanNumber() {
    return representativeScan;
  }

  public @NotNull Scan[] getScanNumbers() {
    return scanNumbers;
  }

  public @NotNull RawDataFile getRawDataFile() {
    return dataFile;
  }

  public IsotopePattern getIsotopePattern() {
    return isotopePattern;
  }

  public void setIsotopePattern(@NotNull IsotopePattern isotopePattern) {
    this.isotopePattern = isotopePattern;
  }

  public int getCharge() {
    return charge;
  }

  public void setCharge(int charge) {
    this.charge = charge;
  }

  public Double getFWHM() {
    return fwhm;
  }

  public void setFWHM(Double fwhm) {
    this.fwhm = fwhm;
  }

  public Double getTailingFactor() {
    return tf;
  }

  public Double getAsymmetryFactor() {
    return af;
  }

  // End dulab Edit

  public SimpleFeatureInformation getPeakInformation() {
    return peakInfo;
  }

  public void setPeakInformation(SimpleFeatureInformation peakInfoIn) {
    this.peakInfo = peakInfoIn;
  }

  @Nullable
  public Integer getParentChromatogramRowID() {
    return this.parentChromatogramRowID;
  }

  public void setParentChromatogramRowID(@Nullable Integer id) {
    this.parentChromatogramRowID = id;
  }

  public FeatureList getPeakList() {
    return peakList;
  }

  public void setPeakList(FeatureList peakList) {
    this.peakList = peakList;
  }


  public List<DataPoint> getDataPoints() {
    List<DataPoint> dp = new ArrayList<>(dataPointMZValues.length);
    for (int i = 0; i < dataPointMZValues.length; i++) {
      dp.add(new SimpleDataPoint(dataPointMZValues[i], dataPointIntensityValues[i]));
    }
    return dp;
  }

  @NotNull
  @Override
  public Color getAWTColor() {
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
    return String.format("%f - %f min", getRawDataPointsIntensityRange().lowerEndpoint(),
        getRawDataPointsRTRange().upperEndpoint());
  }

  @Nullable
  @Override
  public String getToolTipText(int itemIndex) {
    return null;
  }

  @Override
  public void computeValues(Property<TaskStatus> status) {
    // nothing to compute
  }

  @Override
  public double getDomainValue(int index) {
    return getScanNumbers()[index].getRetentionTime();
  }

  @Override
  public double getRangeValue(int index) {
    return getDataPoints().get(index).getIntensity();
  }

  @Override
  public int getValueCount() {
    return getDataPoints().size();
  }

  @Override
  public double getComputationFinishedPercentage() {
    return 1d;
  }
}
