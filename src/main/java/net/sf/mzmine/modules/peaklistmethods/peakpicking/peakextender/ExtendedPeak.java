package net.sf.mzmine.modules.peaklistmethods.peakpicking.peakextender;

import java.util.Arrays;
import java.util.Hashtable;

import javax.annotation.Nonnull;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.PeakStatus;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.MathUtils;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.ScanUtils;

public class ExtendedPeak implements ChromatographicPeak {

    // Data file of this chromatogram
    private RawDataFile dataFile;

    // Data points of the extened peak (map of scan number -> m/z peak)
    private Hashtable<Integer, DataPoint> dataPointsMap;

    // Chromatogram m/z, RT, height, area
    private double mz, rt, height, area;

    // Top intensity scan, fragment scan
    private int representativeScan = -1, fragmentScan = -1;

    // Ranges of raw data points
    private Range rawDataPointsIntensityRange, rawDataPointsMZRange,
	    rawDataPointsRTRange;

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

	// Create a copy, not a reference
	rawDataPointsRTRange = new Range(dataFile.getDataRTRange(1));

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
     * Returns m/z value of last added data point
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
     * This method returns a string with the basic information that defines this
     * peak
     * 
     * @return String information
     */
    public String getName() {
	return "Extended peak "
		+ MZmineCore.getConfiguration().getMZFormat().format(mz)
		+ " m/z";
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

    public @Nonnull PeakStatus getPeakStatus() {
	return PeakStatus.DETECTED;
    }

    public double getRT() {
	return rt;
    }

    public @Nonnull Range getRawDataPointsIntensityRange() {
	return rawDataPointsIntensityRange;
    }

    public @Nonnull Range getRawDataPointsMZRange() {
	return rawDataPointsMZRange;
    }

    public @Nonnull Range getRawDataPointsRTRange() {
	return rawDataPointsRTRange;
    }

    public int getRepresentativeScanNumber() {
	return representativeScan;
    }

    public @Nonnull int[] getScanNumbers() {
	return scanNumbers;
    }

    public @Nonnull RawDataFile getDataFile() {
	return dataFile;
    }

    public IsotopePattern getIsotopePattern() {
	return isotopePattern;
    }

    public void setIsotopePattern(@Nonnull IsotopePattern isotopePattern) {
	this.isotopePattern = isotopePattern;
    }

    public void finishExtendedPeak() {

	int allScanNumbers[] = CollectionUtils.toIntArray(dataPointsMap
		.keySet());
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

	    // Replace the MzPeak instance with an instance of SimpleDataPoint,
	    // to reduce the memory usage. After we finish this extended peak,
	    // we
	    // don't need the additional data provided by the MzPeak
	    SimpleDataPoint newDataPoint = new SimpleDataPoint(mzPeak);
	    dataPointsMap.put(allScanNumbers[i], newDataPoint);

	    if (i == 0) {
		rawDataPointsIntensityRange = new Range(mzPeak.getIntensity());
		rawDataPointsMZRange = new Range(mzPeak.getMZ());
	    } else {
		rawDataPointsIntensityRange.extendRange(mzPeak.getIntensity());
		rawDataPointsMZRange.extendRange(mzPeak.getMZ());
	    }

	    if (height < mzPeak.getIntensity()) {
		height = mzPeak.getIntensity();
		rt = dataFile.getScan(allScanNumbers[i]).getRetentionTime();
		representativeScan = allScanNumbers[i];
	    }
	}

	// Update area
	area = 0;

	for (int i = 1; i < allScanNumbers.length; i++) {
	    // For area calculation, we use retention time in seconds
	    double previousRT = dataFile.getScan(allScanNumbers[i - 1])
		    .getRetentionTime() * 60d;
	    double currentRT = dataFile.getScan(allScanNumbers[i])
		    .getRetentionTime() * 60d;

	    double previousHeight = dataPointsMap.get(allScanNumbers[i - 1])
		    .getIntensity();
	    double currentHeight = dataPointsMap.get(allScanNumbers[i])
		    .getIntensity();
	    area += (currentRT - previousRT) * (currentHeight + previousHeight)
		    / 2;
	}

	// Update fragment scan
	fragmentScan = ScanUtils.findBestFragmentScan(dataFile,
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

}
