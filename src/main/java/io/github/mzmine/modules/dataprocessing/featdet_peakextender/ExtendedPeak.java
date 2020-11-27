package io.github.mzmine.modules.dataprocessing.featdet_peakextender;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.impl.SimpleFeatureInformation;
import java.text.Format;
import java.util.Arrays;
import java.util.Hashtable;
import javax.annotation.Nonnull;
import com.google.common.collect.Range;
import com.google.common.primitives.Ints;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.scans.ScanUtils;

public class ExtendedPeak {
  private SimpleFeatureInformation peakInfo;

  // Data file of this chromatogram
  private RawDataFile dataFile;

  // Data points of the extened peak (map of scan number -> m/z peak)
  private Hashtable<Integer, DataPoint> dataPointsMap;

  // Chromatogram m/z, RT, height, area
  private double mz, rt, height, area;
  private Double fwhm = null, tf = null, af = null;

  // Top intensity scan, fragment scan
  private int representativeScan = -1, fragmentScan = -1;

  // All MS2 fragment scans
  private int[] allMS2FragmentScanNumbers;

  // Ranges of raw features points
  private Range<Double> rawDataPointsMZRange;
  private Range<Float> rawDataPointsIntensityRange, rawDataPointsRTRange;

  // Keep track of last added features point
  private DataPoint lastMzPeak;

  // Isotope pattern. Null by default but can be set later by deisotoping
  // method.
  private IsotopePattern isotopePattern;
  private int charge = 0;

  // Array of scan numbers
  private int[] scanNumbers;

  /**
   * Initializes this ExtendedPeak
   */
  public ExtendedPeak(RawDataFile dataFile) {
    this.dataFile = dataFile;

    rawDataPointsRTRange = dataFile.getDataRTRange(1);

    dataPointsMap = new Hashtable<Integer, DataPoint>();
  }

  /**
   * This method adds a MzPeak to this ExtendedPeak.
   *
   * @param mzValue
   */
  public void addMzPeak(int scanNumber, DataPoint mzValue) {
    dataPointsMap.put(scanNumber, mzValue);
  }

  public DataPoint getDataPoint(int scanNumber) {
    return dataPointsMap.get(scanNumber);
  }

  /**
   * Returns m/z value of last added features point
   */
  public DataPoint getLastMzPeak() {
    return lastMzPeak;
  }

  /**
   * This method returns m/z value of the extended peak
   */
  public double getMZ() {
    return mz;
  }

  /**
   * This method returns a string with the basic information that defines this peak
   *
   * @return String information
   */
  public String getName() {
    return "Extended peak " + MZmineCore.getConfiguration().getMZFormat().format(mz) + " m/z";
  }

  public double getArea() {
    return area;
  }

  public double getHeight() {
    return height;
  }

  public int getMostIntenseFragmentScanNumber() {
    return fragmentScan;
  }

  /**
   * Overwrite the scan number of fragment scan
   *
   * @param scanNumber
   */
  public void setMostIntenseFragmentScanNumber(int scanNumber) {
    this.fragmentScan = scanNumber;
  }

  public int[] getAllMS2FragmentScanNumbers() {
    return allMS2FragmentScanNumbers;
  }

  public @Nonnull FeatureStatus getFeatureStatus() {
    return FeatureStatus.DETECTED;
  }

  public double getRT() {
    return rt;
  }

  public @Nonnull Range<Float> getRawDataPointsIntensityRange() {
    return rawDataPointsIntensityRange;
  }

  public @Nonnull Range<Double> getRawDataPointsMZRange() {
    return rawDataPointsMZRange;
  }

  public @Nonnull Range<Float> getRawDataPointsRTRange() {
    return rawDataPointsRTRange;
  }

  public int getRepresentativeScanNumber() {
    return representativeScan;
  }

  public @Nonnull int[] getScanNumbers() {
    return scanNumbers;
  }

  public @Nonnull RawDataFile getRawDataFile() {
    return dataFile;
  }

  public IsotopePattern getIsotopePattern() {
    return isotopePattern;
  }

  public void setIsotopePattern(@Nonnull IsotopePattern isotopePattern) {
    this.isotopePattern = isotopePattern;
  }

  public void finishExtendedPeak() {

    int allScanNumbers[] = Ints.toArray(dataPointsMap.keySet());
    Arrays.sort(allScanNumbers);

    scanNumbers = allScanNumbers;

    // Calculate median m/z
    double allMzValues[] = new double[allScanNumbers.length];
    for (int i = 0; i < allScanNumbers.length; i++) {
      allMzValues[i] = dataPointsMap.get(allScanNumbers[i]).getMZ();
    }
    mz = MathUtils.calcQuantile(allMzValues, 0.5f);

    // Update raw features point ranges, height, rt and representative scan
    height = Double.MIN_VALUE;
    for (int i = 0; i < allScanNumbers.length; i++) {

      DataPoint mzPeak = dataPointsMap.get(allScanNumbers[i]);
      Scan aScan = dataFile.getScan(allScanNumbers[i]);

      // Replace the MzPeak instance with an instance of SimpleDataPoint,
      // to reduce the memory usage. After we finish this extended peak,
      // we don't need the additional features provided by the MzPeak
      SimpleDataPoint newDataPoint = new SimpleDataPoint(mzPeak);
      dataPointsMap.put(allScanNumbers[i], newDataPoint);

      if (i == 0) {
        rawDataPointsIntensityRange = Range.singleton((float) mzPeak.getIntensity());
        rawDataPointsMZRange = Range.singleton(mzPeak.getMZ());
        rawDataPointsRTRange = Range.singleton(aScan.getRetentionTime());
      } else {
        rawDataPointsIntensityRange =
            rawDataPointsIntensityRange.span(Range.singleton((float) mzPeak.getIntensity()));
        rawDataPointsMZRange = rawDataPointsMZRange.span(Range.singleton(mzPeak.getMZ()));
        rawDataPointsRTRange = rawDataPointsRTRange.span(Range.singleton(aScan.getRetentionTime()));
      }

      if (height < mzPeak.getIntensity()) {
        height = mzPeak.getIntensity();
        rt = aScan.getRetentionTime();
        representativeScan = allScanNumbers[i];
      }
    }

    // Update area
    area = 0;

    for (int i = 1; i < allScanNumbers.length; i++) {
      // For area calculation, we use retention time in seconds
      double previousRT = dataFile.getScan(allScanNumbers[i - 1]).getRetentionTime() * 60d;
      double currentRT = dataFile.getScan(allScanNumbers[i]).getRetentionTime() * 60d;

      double previousHeight = dataPointsMap.get(allScanNumbers[i - 1]).getIntensity();
      double currentHeight = dataPointsMap.get(allScanNumbers[i]).getIntensity();
      area += (currentRT - previousRT) * (currentHeight + previousHeight) / 2;
    }

    // Update fragment scan
    fragmentScan =
        ScanUtils.findBestFragmentScan(dataFile, dataFile.getDataRTRange(1), rawDataPointsMZRange);

    allMS2FragmentScanNumbers = ScanUtils.findAllMS2FragmentScans(dataFile,
        dataFile.getDataRTRange(1), rawDataPointsMZRange);

    if (fragmentScan > 0) {
      Scan fragmentScanObject = dataFile.getScan(fragmentScan);
      int precursorCharge = fragmentScanObject.getPrecursorCharge();
      if ((precursorCharge > 0) && (this.charge == 0))
        this.charge = precursorCharge;
    }

  }

  public int getCharge() {
    return charge;
  }

  public void setCharge(int charge) {
    this.charge = charge;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer();
    Format mzFormat = MZmineCore.getConfiguration().getMZFormat();
    Format timeFormat = MZmineCore.getConfiguration().getRTFormat();
    buf.append("m/z ");
    buf.append(mzFormat.format(getMZ()));
    buf.append(" (");
    buf.append(timeFormat.format(getRT()));
    buf.append(" min) [" + getRawDataFile().getName() + "]");
    return buf.toString();
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

  public void setTailingFactor(Double tf) {
    this.tf = tf;
  }

  public Double getAsymmetryFactor() {
    return af;
  }

  public void setAsymmetryFactor(Double af) {
    this.af = af;
  }

  // dulab Edit
  public void outputChromToFile() {
    int nothing = -1;
  }

  public void setPeakInformation(SimpleFeatureInformation peakInfoIn) {
    this.peakInfo = peakInfoIn;
  }

  public SimpleFeatureInformation getPeakInformation() {
    return peakInfo;
  }

  public void setFragmentScanNumber(int fragmentScanNumber) {
    this.fragmentScan = fragmentScanNumber;
  }

  public void setAllMS2FragmentScanNumbers(int[] allMS2FragmentScanNumbers) {
    this.allMS2FragmentScanNumbers = allMS2FragmentScanNumbers;
    // also set best scan by TIC
    int best = -1;
    double tic = 0;
    if (allMS2FragmentScanNumbers != null) {
      for (int i : allMS2FragmentScanNumbers) {
        if (tic < dataFile.getScan(i).getTIC())
          best = i;
      }
    }
    setFragmentScanNumber(best);
  }
  // End dulab Edit

  private FeatureList peakList;

  public FeatureList getPeakList() {
    return peakList;
  }

  public void setPeakList(FeatureList peakList) {
    this.peakList = peakList;
  }

}
