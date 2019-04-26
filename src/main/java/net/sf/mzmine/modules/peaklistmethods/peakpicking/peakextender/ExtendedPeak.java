package net.sf.mzmine.modules.peaklistmethods.peakpicking.peakextender;

import java.util.Arrays;
import java.util.Hashtable;
import javax.annotation.Nonnull;
import com.google.common.collect.Range;
import com.google.common.primitives.Ints;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimplePeakInformation;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.MathUtils;
import net.sf.mzmine.util.PeakUtils;
import net.sf.mzmine.util.ScanUtils;

public class ExtendedPeak implements Feature {
  private SimplePeakInformation peakInfo;

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

  // Ranges of raw data points
  private Range<Double> rawDataPointsIntensityRange, rawDataPointsMZRange, rawDataPointsRTRange;

  // Keep track of last added data point
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

  @Override
  public DataPoint getDataPoint(int scanNumber) {
    return dataPointsMap.get(scanNumber);
  }

  /**
   * Returns m/z value of last added data point
   */
  public DataPoint getLastMzPeak() {
    return lastMzPeak;
  }

  /**
   * This method returns m/z value of the extended peak
   */
  @Override
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

  @Override
  public double getArea() {
    return area;
  }

  @Override
  public double getHeight() {
    return height;
  }

  @Override
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

  @Override
  public int[] getAllMS2FragmentScanNumbers() {
    return allMS2FragmentScanNumbers;
  }

  @Override
  public @Nonnull FeatureStatus getFeatureStatus() {
    return FeatureStatus.DETECTED;
  }

  @Override
  public double getRT() {
    return rt;
  }

  @Override
  public @Nonnull Range<Double> getRawDataPointsIntensityRange() {
    return rawDataPointsIntensityRange;
  }

  @Override
  public @Nonnull Range<Double> getRawDataPointsMZRange() {
    return rawDataPointsMZRange;
  }

  @Override
  public @Nonnull Range<Double> getRawDataPointsRTRange() {
    return rawDataPointsRTRange;
  }

  @Override
  public int getRepresentativeScanNumber() {
    return representativeScan;
  }

  @Override
  public @Nonnull int[] getScanNumbers() {
    return scanNumbers;
  }

  @Override
  public @Nonnull RawDataFile getDataFile() {
    return dataFile;
  }

  @Override
  public IsotopePattern getIsotopePattern() {
    return isotopePattern;
  }

  @Override
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

    // Update raw data point ranges, height, rt and representative scan
    height = Double.MIN_VALUE;
    for (int i = 0; i < allScanNumbers.length; i++) {

      DataPoint mzPeak = dataPointsMap.get(allScanNumbers[i]);
      Scan aScan = dataFile.getScan(allScanNumbers[i]);

      // Replace the MzPeak instance with an instance of SimpleDataPoint,
      // to reduce the memory usage. After we finish this extended peak,
      // we don't need the additional data provided by the MzPeak
      SimpleDataPoint newDataPoint = new SimpleDataPoint(mzPeak);
      dataPointsMap.put(allScanNumbers[i], newDataPoint);

      if (i == 0) {
        rawDataPointsIntensityRange = Range.singleton(mzPeak.getIntensity());
        rawDataPointsMZRange = Range.singleton(mzPeak.getMZ());
        rawDataPointsRTRange = Range.singleton(aScan.getRetentionTime());
      } else {
        rawDataPointsIntensityRange =
            rawDataPointsIntensityRange.span(Range.singleton(mzPeak.getIntensity()));
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

  @Override
  public int getCharge() {
    return charge;
  }

  @Override
  public void setCharge(int charge) {
    this.charge = charge;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return PeakUtils.peakToString(this);
  }

  @Override
  public Double getFWHM() {
    return fwhm;
  }

  @Override
  public void setFWHM(Double fwhm) {
    this.fwhm = fwhm;
  }

  @Override
  public Double getTailingFactor() {
    return tf;
  }

  @Override
  public void setTailingFactor(Double tf) {
    this.tf = tf;
  }

  @Override
  public Double getAsymmetryFactor() {
    return af;
  }

  @Override
  public void setAsymmetryFactor(Double af) {
    this.af = af;
  }

  // dulab Edit
  @Override
  public void outputChromToFile() {
    int nothing = -1;
  }

  @Override
  public void setPeakInformation(SimplePeakInformation peakInfoIn) {
    this.peakInfo = peakInfoIn;
  }

  @Override
  public SimplePeakInformation getPeakInformation() {
    return peakInfo;
  }

  @Override
  public void setFragmentScanNumber(int fragmentScanNumber) {
    this.fragmentScan = fragmentScanNumber;
  }

  @Override
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


}
