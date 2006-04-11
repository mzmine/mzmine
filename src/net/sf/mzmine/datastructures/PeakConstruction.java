/*
    Copyright 2005 VTT Biotechnology

    This file is part of MZmine.

    MZmine is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    MZmine is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MZmine; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/

package net.sf.mzmine.datastructures;
import net.sf.mzmine.util.MyMath;


/**
 * This class is used to construct peaks during peak detection
 */
public class PeakConstruction {

	int[] scanNums;
	int[] centroidMZIndexes;
	double[] centroidMZs;
	double[] centroidIntensities;

	double centroidMZMedian;		// This is precalculated after adding each m/z peak

	int usedSize;
	int reservedSize;
	int reservedSizeIncrement;

	RawDataAtNode rawData;

	boolean peakGrowing;

	/**
	 * Constructor: initializes a new, empty peak object
	 */
    public PeakConstruction() {
		usedSize = 0;
		reservedSize = 0;
		reservedSizeIncrement = 1;

		peakGrowing = false;

		scanNums = new int[reservedSize];
		centroidMZIndexes = new int[reservedSize];
		centroidMZs = new double[reservedSize];
		centroidIntensities = new double[reservedSize];
	}

	/**
	 * Constructor: initializes a new peak and reserves memory for given size of mz,rt datapoints
	 * @param	initialReservedSize		Amount of memory (length of peak in scans) reserved immediately
	 * @param	initialReservedSizeIncrement	Increment (num of new scans) used when more memory is needed
	 */
    public PeakConstruction(RawDataAtNode _rawData, int initialReservedSize, int initialReservedSizeIncrement) {
		rawData = _rawData;

		reservedSize = initialReservedSize;
		reservedSizeIncrement = initialReservedSizeIncrement;
		if (reservedSizeIncrement<1) { reservedSizeIncrement = 1; }

		scanNums = new int[reservedSize];
		centroidMZIndexes = new int[reservedSize];
		centroidMZs = new double[reservedSize];
		centroidIntensities = new double[reservedSize];

	}


	/**
	 * Adds new M/Z peak to this Peak
	 * @param	_scanNum	Scan number of the new measurement
	 * @param	_centroidMZIndex	Location of this M/Z peak in scan's measurement vector
	 * @param	_centroidMZ		Centroid M/Z value of this M/Z peak
	 * @param	_centroidIntensity		Centroid M/Z value of this M/Z peak
	 */
	public void addScan(int _scanNum, int _centroidMZIndex, double _centroidMZ, double _centroidIntensity) {

		// Resize arrays if needed
		if (usedSize>=reservedSize) {

			int[] tmpIntArray =  new int[reservedSize+reservedSizeIncrement];
			System.arraycopy(scanNums,0,tmpIntArray,0,reservedSize);
			scanNums = tmpIntArray;

			tmpIntArray =  new int[reservedSize+reservedSizeIncrement];
			System.arraycopy(centroidMZIndexes,0,tmpIntArray,0,reservedSize);
			centroidMZIndexes = tmpIntArray;

			double[] tmpDoubleArray = new double[reservedSize+reservedSizeIncrement];
			System.arraycopy(centroidMZs,0,tmpDoubleArray,0,reservedSize);
			centroidMZs = tmpDoubleArray;

			tmpDoubleArray = new double[reservedSize+reservedSizeIncrement];
			System.arraycopy(centroidIntensities,0,tmpDoubleArray,0,reservedSize);
			centroidIntensities = tmpDoubleArray;

			reservedSize += reservedSizeIncrement;

		}

		// Add new values to the end of arrays
		usedSize++;
		scanNums[usedSize-1] = _scanNum;
		centroidMZIndexes[usedSize-1] = _centroidMZIndex;
		centroidMZs[usedSize-1] = _centroidMZ;
		centroidIntensities[usedSize-1] = _centroidIntensity;

		// Precalculate centroid MZ median
		centroidMZMedian = calcCentroidMZMedian();

		// Keep track of ups and downs in intensity along the peak trace
		/*
		if (constructionStillRising) {
			if (constructionLowestIntensityFirstHalf>=_centroidIntensity) {	constructionLowestIntensityFirstHalf = _centroidIntensity; }
			if (constructionHighestIntensity<=_centroidIntensity) {	constructionHighestIntensity = _centroidIntensity; }
		} else {
			if (constructionLowestIntensitySecondHalf>=_centroidIntensity) { constructionLowestIntensitySecondHalf = _centroidIntensity; }
		}

		constructionLastIntensity = _centroidIntensity;
		*/

	}


	public void setGrowingStatus(boolean b) {
		peakGrowing = b;
	}

	public boolean isGrowing() {
		return peakGrowing;
	}



	/**
	 * This method returns the median M/Z value of all existing M/Z peaks
	 */
	public double getCentroidMZMedian() {
		return centroidMZMedian;
	}

	/**
	 * This method tests if any of existing M/Z peaks has M/Z value equal to given M/Z value with given tolerance
	 * @param	centroidMZ	M/Z value for testing
	 * @param	tolerance	Tolerance in testing.
	 * @return	True if any of M/Z peak's in this peak has M/Z value within centroidMZ+-tolerance
	 */
	public boolean centroidMZEquals(double centroidMZ, double tolerance) {
		for (int i=0; i<usedSize; i++) {
			if ( (centroidMZs[i]>=(centroidMZ-tolerance)) && (centroidMZs[i]<=(centroidMZ+tolerance)) ) {
				return true;
			}
		}
		return false;
	}



	/**
	 * This method is called when nothing is to be added to this peak.
	 * Method freezes data structures to their final length.
	 */
	public void finalizePeak() {

			int[] tmpIntArray =  new int[usedSize];
			System.arraycopy(scanNums,0,tmpIntArray,0,usedSize);
			scanNums = tmpIntArray;

			tmpIntArray =  new int[usedSize];
			System.arraycopy(centroidMZIndexes,0,tmpIntArray,0,usedSize);
			centroidMZIndexes = tmpIntArray;

			double[] tmpDoubleArray = new double[usedSize];
			System.arraycopy(centroidMZs,0,tmpDoubleArray,0,usedSize);
			centroidMZs = tmpDoubleArray;

			tmpDoubleArray = new double[usedSize];
			System.arraycopy(centroidIntensities,0,tmpDoubleArray,0,usedSize);
			centroidIntensities = tmpDoubleArray;

			reservedSize = usedSize;
	}

	/**
	 * This method is called to crop current data structures to given interval
	 */
	public void finalizePeak(int startInd, int stopInd) {

			int[] tmpIntArray =  new int[usedSize];
			System.arraycopy(scanNums,startInd,tmpIntArray,0,stopInd-startInd+1);
			scanNums = tmpIntArray;

			tmpIntArray =  new int[usedSize];
			System.arraycopy(centroidMZIndexes,startInd,tmpIntArray,0,stopInd-startInd+1);
			centroidMZIndexes = tmpIntArray;

			double[] tmpDoubleArray = new double[usedSize];
			System.arraycopy(centroidMZs,startInd,tmpDoubleArray,0,stopInd-startInd+1);
			centroidMZs = tmpDoubleArray;

			tmpDoubleArray = new double[usedSize];
			System.arraycopy(centroidIntensities,startInd,tmpDoubleArray,0,stopInd-startInd+1);
			centroidIntensities = tmpDoubleArray;

			reservedSize = usedSize;
	}


	/**
	 * Returns the number of m/z peaks really added to this peak
	 */
	public int getUsedSize() {
		return usedSize;
	}

	/**
	 * Returns numbers of scans that this peak goes through
	 */
	public int[] getScanNums() {
		return scanNums;
	}

	/**
	 * Returns centroid M/Z values of all M/Z peaks in this peak
	 */
	public double[] getCentroidMZs() {
		return centroidMZs;
	}

	/**
	 * Returns centroid intensity values of all M/Z peaks in this peak
	 */
	public double[] getCentroidIntensities() {
		return centroidIntensities;
	}

	/**
	 * Returns index (to vector of scan's datapoints) of one M/Z peak
	 */
	 /*
	 NOT USED
	public int getCentroidMZIndex(int _scanNum) {
		int ind = -1;
		int i = 0;
		while ( (ind == -1) && (i<usedSize) ) {
			if (scanNums[i]==_scanNum) { ind = i; }
			i++;
		}
		if (ind==-1) { return -1;}

		return centroidMZIndexes[ind];
	}
	*/

	/**
	 * Returns M/Z value of M/Z peak at one scan
	 * @param	_scanNum	Scan number of M/Z peak
	 */
	public double getCentroidMZ(int _scanNum) {
		int ind = -1;
		int i = 0;
		while ( (ind == -1) && (i<usedSize) ) {
			if (scanNums[i]==_scanNum) { ind = i; }
			i++;
		}
		if (ind==-1) { return -1;}
		return centroidMZs[ind];

	}

	/**
	 * Returns intensity of M/Z peak at one scan
	 * @param	_scanNum	Scan number of M/Z peak
	 */
	public double getCentroidIntensity(int _scanNum) {
		int ind = -1;
		int i = 0;
		while ( (ind == -1) && (i<usedSize) ) {
			if (scanNums[i]==_scanNum) { ind = i; }
			i++;
		}
		if (ind==-1) { return -1;}
		return centroidIntensities[ind];
	}


	/**
	 * Returns average M/Z value of all M/Z peaks included in this peak
	 */
	public double getCentroidMZAverage() {
		double MZSum = 0;
		for (int i=0; i<usedSize; i++) {
			MZSum += centroidMZs[i];
		}
		return (MZSum / (double)(usedSize));
	}

	/**
	 * Returns average intensity of all M/Z peaks included in this peak
	 */
	public double getCentroidIntensityAverage() {
		double IntSum = 0;
		for (int i=0; i<usedSize; i++) {
			IntSum += centroidIntensities[i];
		}
		return (IntSum / (double)(usedSize));
	}



	/**
	 * Returns median M/Z value of all M/Z peaks included in this peak
	 */
	private double calcCentroidMZMedian() {

		double[] tmpMZVals = new double[usedSize];
		for (int i=0; i<usedSize; i++) {
			tmpMZVals[i] = centroidMZs[i];
		}

		return MyMath.calcQuantile(tmpMZVals, (double)0.5);
	}


	/**
	 * Returns standard deviation of all M/Z values
	 */
	public double getCentroidMZStdev() {
		// Calc standard deviation of MZs
		double MZSum = 0;
		for (int i=0; i<usedSize; i++) {
			MZSum += centroidMZs[i];
		}
		double averageMZ = MZSum / ((double)usedSize);

		double diffSum = 0;
		for (int i=0; i<usedSize; i++) {
			diffSum += (averageMZ-centroidMZs[i])*(averageMZ-centroidMZs[i]);
		}
		double stdMZ = (double)java.lang.Math.sqrt((double)(diffSum / ((double)usedSize)));

		return stdMZ;
	}


	/**
	 * Returns scan number of M/Z peak with strongest intensity
	 */
	public int getMaxIntensityScanNum() {
		double maxIntensity = centroidIntensities[0];
		int maxIntensityScanInd = scanNums[0];
		for (int i=0; i<usedSize; i++) {
			if (centroidIntensities[i]>maxIntensity) {
				maxIntensity = centroidIntensities[i];
				maxIntensityScanInd = scanNums[i];
			}
		}
		return maxIntensityScanInd;
	}

	/**
	 * Returns RT in seconds of M/Z peak with strongest intensity
	 */
	public double getMaxIntensityTime() {
		return rawData.getScanTime(getMaxIntensityScanNum());
	}


	/**
	 * Returns intensity of strongest M/Z peak
	 */
	public double getMaxIntensity() {
		double maxIntensity = centroidIntensities[0];
		double maxIntensityScanInd = scanNums[0];
		for (int i=0; i<usedSize; i++) {
			if (centroidIntensities[i]>maxIntensity) {
				maxIntensity = centroidIntensities[i];
				maxIntensityScanInd = scanNums[i];
			}
		}
		return maxIntensity;
	}

	/**
	 * Returns the median of all M/Z peak intensities
	 */
	public double getMedianIntensity() {
		return MyMath.calcQuantile(centroidIntensities, (double)0.5);
	}

	/**
	 * Returns the sum of all M/Z peak intensities
	 */
	public double getSumOfIntensities() {
		double area = 0;
		for (int i=0; i<usedSize; i++) {
			area += centroidIntensities[i];
		}
		return area;
	}


	/**
	 * Returns scan number of the first M/Z peak
	 */
	public int getStartScan() {
		if (usedSize>0) {
			return scanNums[0];
		} else {
			return -1;
		}
	}

	/**
	 * Returns scan number of the last M/Z peak
	 */
	public int getEndScan() {
		if (usedSize>0) {
			return scanNums[usedSize-1];
		} else {
			return -1;
		}
	}

	/**
	 * Returns number of consecutive M/Z peaks
	 */
	public int getLengthInScans() {
		return usedSize;
	}

	/**
	 * Returns difference in RT (in secs) between first and last M/Z peak
	 */
	public double getLengthInSecs() {
		if (usedSize<=0) { return 0; }
		return ( rawData.getScanTime(scanNums[usedSize-1]) - rawData.getScanTime(scanNums[0]) );
	}

	/**
	 * Returns the Run object where this peak belongs to
	 */
	public RawDataAtNode getRawData() {
		return rawData;
	}



 } // end Peak



