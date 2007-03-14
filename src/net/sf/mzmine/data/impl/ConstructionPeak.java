package net.sf.mzmine.data.impl;

import java.util.ArrayList;
import java.util.Hashtable;

import net.sf.mzmine.data.Peak;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.MathUtils;

/**
 * This class is an implementation of the peak interface for peak picking methods.
 */
public class ConstructionPeak extends AbstractDataUnit implements Peak {

	private PeakStatus peakStatus;

	// This table maps a scanNumber to an array of m/z and intensity pairs
	private Hashtable<Integer, ArrayList<double[]>> datapointsMap; 

	// Raw M/Z, RT, Height and Area
	private double mz;
	private double rt;
	private double height;
	private double area;

	// Normalized versions of peak's basic properties
	private double normalizedMZ;
	private double normalizedRT;
	private double normalizedHeight;
	private double normalizedArea;
	
	// Boundaries of the peak 
	private double minRT;
	private double maxRT;
	private double minMZ;
	private double maxMZ;
	
	// These are used for constructing the peak
	private boolean precalcRequiredMZ;
	private boolean precalcRequiredRT;
	private boolean precalcRequiredMins;
	private boolean precalcRequiredArea;
	private ArrayList<Double> datapointsMZs;
	private ArrayList<Double> datapointsRTs;
	private ArrayList<Double> datapointsIntensities;
	
	private boolean growing;
	


	/**
	 * Initializes empty peak for adding data points to
	 */
	public ConstructionPeak() {
		intializeAddingDatapoints();
	}
	

	/**
	 * This method returns the status of the peak
	 */
	public PeakStatus getPeakStatus() {
		return peakStatus;
	}

	public void setPeakStatus(PeakStatus peakStatus) {
		this.peakStatus = peakStatus;
	}

	/* Get methods for basic properties of the peak as defined by the peak picking method */

	/**
	 * This method returns M/Z value of the peak
	 */
	public double getRawMZ() {
		if (precalcRequiredMZ) precalculateMZ();
		return mz;
	}

	/**
	 * This method returns retention time of the peak
	 */
	public double getRawRT() {
		if (precalcRequiredRT) precalculateRT();
		return rt;
	}

	/**
	 * This method returns the raw height of the peak
	 */
	public double getRawHeight() {
		if (precalcRequiredRT) precalculateRT();
		return height;
	}

	/**
	 * This method returns the raw area of the peak
	 */
	public double getRawArea() {
		if (precalcRequiredArea) precalculateArea();
		return area;
	}


	/**
	 * This method returns numbers of scans that contain this peak
	 */
	public int[] getScanNumbers() {	
		return CollectionUtils.toIntArray(datapointsMap.keySet());
	}

	/**
	 * This method returns an array of double[2] (mz and intensity) points for a given scan number
	 */
	public double[][] getRawDatapoints(int scanNumber) {

		ArrayList<double[]> datapoints = datapointsMap.get(scanNumber);
		
		if (datapoints == null) return new double[0][0];
		
		double[][] res = new double[datapoints.size()][];
		int ind=0;
		for (double[] datapoint : datapoints) {
			res[ind] = datapoint;
			ind++;
		}
		
		return res;
	}

	/**
	 * Returns the first scan number of all datapoints
	 */
	public double getMinRT() {
		if (precalcRequiredMins) precalculateMins();
		return minRT;
	}

	/**
	 * Returns the last scan number of all datapoints
	 */
	public double getMaxRT() {
		if (precalcRequiredMins) precalculateMins();
		return maxRT;
	}

	/**
	 * Returns minimum M/Z value of all datapoints
	 */
	public double getMinMZ() {
		if (precalcRequiredMins) precalculateMins();
		return minMZ;
	}

	/**
	 * Returns maximum M/Z value of all datapoints
	 */
	public double getMaxMZ() {
		if (precalcRequiredMins) precalculateMins();
		return maxMZ;
	}

	/**
	 * Returns the normalized M/Z of the peak
	 */
	public double getNormalizedMZ() {
		if (precalcRequiredMZ) precalculateMZ();
		return normalizedMZ;
	}

	/**
	 * Returns the normalized RT of the peak
	 */
	public double getNormalizedRT() {
		if (precalcRequiredRT) precalculateRT();
		return normalizedRT;
	}

	/**
	 * Returns the normalized height of the peak
	 */
	public double getNormalizedHeight() {
		if (precalcRequiredRT) precalculateRT();
		return normalizedHeight;
	}

	/**
	 * Returns the normalized area of the peak
	 */
	public double getNormalizedArea() {
		if (precalcRequiredArea) precalculateArea();
		return normalizedArea;
	}



	private void intializeAddingDatapoints() {

		datapointsMap = new Hashtable<Integer, ArrayList<double[]>>();
		
		precalcRequiredMZ = true;
		precalcRequiredRT = true;
		precalcRequiredMins = true;
		precalcRequiredArea = true;
		
		growing = false;
		
		datapointsMZs = new ArrayList<Double>();
		datapointsRTs = new ArrayList<Double>();
		datapointsIntensities = new ArrayList<Double>();

	}
	

	private void precalculateMZ() {
		// Calculate median MZ
		mz = MathUtils.calcQuantile(CollectionUtils.toDoubleArray(datapointsMZs), 0.5);
		normalizedMZ = mz;
		precalcRequiredMZ = false;
	}
	
	private void precalculateRT() {
		// Find maximum intensity datapoint and use its RT
		double maxIntensity = 0.0;
		for (int ind=0; ind<datapointsIntensities.size(); ind++) {
			if (maxIntensity<=datapointsIntensities.get(ind)) {
				maxIntensity = datapointsIntensities.get(ind);
				rt = datapointsRTs.get(ind);
				height = maxIntensity;
			}
		}
		normalizedRT = rt;
		normalizedHeight = height;
		precalcRequiredRT = false;
	}
	
	private void precalculateArea() {
		double sum = 0.0;
		for (Double intensity : datapointsIntensities) 
			sum += intensity;
		
		area = sum;
		normalizedArea = area;
		
		precalcRequiredArea = false;
	}
	
	private void precalculateMins() {
		minMZ = Double.MAX_VALUE;
		maxMZ = Double.MIN_VALUE;
		minRT = Double.MAX_VALUE;
		maxRT = Double.MIN_VALUE;
		
		for (int ind=0; ind<datapointsMZs.size(); ind++) {
			if (datapointsMZs.get(ind)<minMZ) minMZ = datapointsMZs.get(ind); 
			if (datapointsMZs.get(ind)>maxMZ) maxMZ = datapointsMZs.get(ind);
			if (datapointsRTs.get(ind)<minRT) minRT = datapointsRTs.get(ind); 
			if (datapointsRTs.get(ind)>maxRT) maxRT = datapointsRTs.get(ind);			
		}
		precalcRequiredMins = false;
	}

	public void addDatapoint(int scanNumber, double mz, double rt, double intensity) {

		growing = true;
		precalcRequiredMZ = true;
		precalcRequiredRT = true;
		precalcRequiredMins = true;
		precalcRequiredArea = true;
		
		// Add datapoint
		ArrayList<double[]> datapoints = datapointsMap.get(scanNumber);
		if (datapoints==null) { datapoints = new ArrayList<double[]>(); datapointsMap.put(scanNumber, datapoints); }
		
		double[] datapoint = new double[2];
		datapoint[0] = mz;
		datapoint[1] = intensity;
		
		datapoints.add(datapoint);

		// Update construction time variables
		datapointsMZs.add(mz);
		datapointsRTs.add(rt);
		datapointsIntensities.add(intensity);
	

	}
	
	public boolean isGrowing() {
		return growing;
	}
	
	public void resetGrowingState() {
		growing = false;
	}

	public void finalizedAddingDatapoints() {

		if (precalcRequiredMZ) precalculateMZ();
		if (precalcRequiredRT) precalculateRT();
		if (precalcRequiredMins) precalculateMins();
		if (precalcRequiredArea) precalculateArea();
		
		datapointsMZs = null;
		datapointsRTs = null;
		datapointsIntensities = null;
		
	}
	
	public ArrayList<Double> getConstructionIntensities() {
		return datapointsIntensities;
	}

	public void setNormalizedMZ(double normalizedMZ) {
		this.normalizedMZ = normalizedMZ;
	}
	
	public void setNormalizedRT(double normalizedRT) {
		this.normalizedRT = normalizedRT;
	}
	
	public void setNormalizedHeight(double normalizedHeight) {
		this.normalizedHeight = normalizedHeight;
	}
	
	public void setNormalizedArea(double normalizedArea) {
		this.normalizedArea = normalizedArea;
	}


}
