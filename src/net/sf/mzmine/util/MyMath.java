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
package net.sf.mzmine.util;
import java.util.Arrays;
import java.util.ArrayList;

public class MyMath {

	/**
	 * Calculates q-quantile value of values. Modifies the order of elements in values array.
	 * q=0.5 => median
	 *
	 */
	public static double calcQuantile(double[] values, double q) {

		if (values.length==0) { return 0; }
		if (values.length==1) { return values[0]; }

		if (q>1) { q = 1; }
		if (q<0) { q = 0; }
		double[] vals = (double[])values.clone();

		Arrays.sort(vals);

		int ind1 = (int)java.lang.Math.floor((vals.length-1)*q);
		int ind2 = (int)java.lang.Math.ceil((vals.length-1)*q);

		return ( vals[ind1] + vals[ind2] ) / (double)2;

	}

	public static double[] calcQuantile(double[] values, double[] qs) {


		double[] retVals = new double[qs.length];

		if (values.length==0) { for (int qInd=0; qInd<qs.length; qInd++) { retVals[qInd] = 0; } return retVals; }
		if (values.length==1) { for (int qInd=0; qInd<qs.length; qInd++) { retVals[qInd] = values[0]; } return retVals; }

		double[] vals = (double[])values.clone();
		Arrays.sort(vals);

		double q;
		int ind1, ind2;
		for (int qInd=0; qInd<qs.length; qInd++) {
			q = qs[qInd];

			if (q>1) { q = 1; }
			if (q<0) { q = 0; }

			ind1 = (int)java.lang.Math.floor((vals.length-1)*q);
			ind2 = (int)java.lang.Math.ceil((vals.length-1)*q);

			retVals[qInd] = ( vals[ind1] + vals[ind2] ) / (double)2;
		}

		return retVals;
	}


	public static double calcStd(double[] values) {
		double avg, stdev;
		double sum=0;
		for (double d : values) { sum+=d; }
		avg = sum / values.length;

		sum = 0;
		for (double d: values) { sum+=(d-avg)*(d-avg); }

		stdev = (double)java.lang.Math.sqrt((double)sum/(double)(values.length-1));
		return stdev;
	}

	public static double calcCV(double[] values) {
		double avg, stdev;
		double sum=0;
		for (double d : values) { sum+=d; }
		avg = sum / values.length;

		sum = 0;
		for (double d: values) { sum+=(d-avg)*(d-avg); }

		stdev = (double)java.lang.Math.sqrt((double)sum/(double)(values.length-1));

		return stdev/avg;
	}


	/**
	 * This method bins values on x-axis.
	 * Each bin is assigned biggest y-value of all values in the same bin.
	 *
	 * @param	x				X-coordinates of the data
	 * @param	y				Y-coordinates of the data
	 * @param	firstBinStart	Value at the "left"-edge of the first bin
	 * @param	lastBinStop		Value at the "right"-edge of the last bin
	 * @param	numberOfBins	Number of bins
	 * @param	interpolate		If true, then empty bins will be filled with interpolation using other bins
	 * @param	binningType		Type of binning (sum of all 'y' within a bin, max of 'y', min of 'y')
	 * @return	Values for each bin
	 */
    public static enum BinningType {
        SUM, MAX, MIN
    };
	public static double[] binValues(double[] x, double[] y, double firstBinStart, double lastBinStop, int numberOfBins, boolean interpolate, BinningType binningType) {

		Double[] binValues = new Double[numberOfBins];
		double binWidth = (lastBinStop-firstBinStart)/numberOfBins;

		double beforeX = Double.MIN_VALUE;
		double beforeY = 0.0;
		double afterX = Double.MAX_VALUE;
		double afterY = 0.0;

		// Binnings
		for (int valueIndex=0; valueIndex<x.length; valueIndex++) {

			// Before first bin?
			if ((x[valueIndex]-firstBinStart)<0) {
				if (x[valueIndex]>beforeX) {
					beforeX = x[valueIndex];
					beforeY = y[valueIndex];
				}
				continue;
			}

			// After last bin?
			if ((lastBinStop-x[valueIndex])<0) {
				if (x[valueIndex]<afterX) {
					afterX = x[valueIndex];
					afterY = y[valueIndex];
				}
				continue;
			}

			int binIndex = (int)((x[valueIndex]-firstBinStart)/binWidth);

			switch(binningType) {
				case SUM:
					if (binValues[binIndex]==null) { binValues[binIndex] = y[valueIndex]; } else { binValues[binIndex] += y[valueIndex]; }
					break;
				case MAX:
					if (binValues[binIndex]==null) { binValues[binIndex] = y[valueIndex]; }
						else { if (binValues[binIndex]<y[valueIndex]) { binValues[binIndex] = y[valueIndex]; } }
					break;
				case MIN:
					if (binValues[binIndex]==null) { binValues[binIndex] = y[valueIndex]; }
						else { if (binValues[binIndex]>y[valueIndex]) { binValues[binIndex] = y[valueIndex]; } }
					break;
			}

			if (binValues[binIndex]==null) {
				binValues[binIndex] = y[valueIndex];
				continue;
			}

			if (binValues[binIndex]<y[valueIndex]) {
				binValues[binIndex] = y[valueIndex];
			}

		}

		// Interpolation
		if (interpolate) {

			for (int binIndex=0; binIndex<binValues.length; binIndex++) {
				if (binValues[binIndex]==null) {

					// Find exisiting left neighbour
					double leftNeighbourValue = beforeY;
					int leftNeighbourBinIndex = (int)java.lang.Math.floor((beforeX-firstBinStart)/binWidth);
					for (int anotherBinIndex=binIndex-1; anotherBinIndex>=0; anotherBinIndex--) {
						if (binValues[anotherBinIndex]!=null) {
							leftNeighbourValue = binValues[anotherBinIndex];
							leftNeighbourBinIndex = anotherBinIndex;
							break;
						}
					}

					// Find existing right neighbour
					double rightNeighbourValue = afterY;
					int rightNeighbourBinIndex = (binValues.length-1)+(int)java.lang.Math.ceil((afterX-lastBinStop)/binWidth);
					for (int anotherBinIndex=binIndex+1; anotherBinIndex<binValues.length; anotherBinIndex++) {
						if (binValues[anotherBinIndex]!=null) {
							rightNeighbourValue = binValues[anotherBinIndex];
							rightNeighbourBinIndex = anotherBinIndex;
							break;
						}
					}

					double slope = (rightNeighbourValue-leftNeighbourValue)/(rightNeighbourBinIndex-leftNeighbourBinIndex);
					binValues[binIndex] = new Double(leftNeighbourValue + slope * (binIndex-leftNeighbourBinIndex));

				}

			}

		}

		double[] res = new double[binValues.length];
		for (int binIndex=0; binIndex<binValues.length; binIndex++) {
			res[binIndex] = binValues[binIndex] == null ? 0 : binValues[binIndex];
		}
		return res;

	}


	/**
	 * This method fills missing values in an array by interpolation.
	 * It is assumed that value
	 * @param	values	Original values, null if value is missing
	 * @param	valueBefore	Value before the first element in "values". If null, then method tries to extrapolate if interpolation is not possible
	 * @param	valueAfter	Value after the last element in "values". If null, then method tries to extrapolate if interpolation is not possible
	 */
/*
	public static void interpolateMissingValues(double x[], Double[] y) {

		for (int index=0; index<y.length; index++) {

			if (y[index]==null) {

				// Find nearest existing left neighbour
				Double leftY = null;
				double leftX = 0.0;
				int leftIndex = -1;
				for (int anotherIndex=index; anotherIndex>=0; anotherIndex--) {
					if (y[anotherIndex]!=null) {
						leftY = y[anotherIndex];
						leftX = x[anotherIndex];
						leftIndex = anotherIndex;
						break;
					}
				}

				// Find nearest existing right neighbour
				Double rightY = null;
				double rightX = 0.0;
				int rightIndex = -1;
				for (int anotherIndex=index; anotherIndex<y.length; anotherIndex++) {
					if (y[anotherIndex]!=null) {
						rightY = y[anotherIndex];
						rightX = x[anotherIndex];
						rightIndex = anotherIndex;
						break;
					}
				}

				// If both left and right neighbour are missing, then it is not possible to interpolate or extrapolate
				if ((leftY==null) && (rightY==null)) { continue; }

				// If both neighbours are present, then interpolate
				if ((leftY!=null) && (rightY!=null)) {
					double deltaY = rightY-leftY;
					double deltaX = rightX-leftX;
					y[index] = new Double(leftY + deltaY/deltaX * (x[index]-leftX) );
					continue;
				}

				// If only left neighbour is present, then must look for another left neighbour
				if ((leftY!=null) && (rightY==null)) {
					// Find second nearest existing left neighbour
					Double secondLeftY = null;
					double secondLeftX = 0.0;
					for (int anotherIndex=leftIndex-1; anotherIndex>=0; anotherIndex--) {
						if (y[anotherIndex]!=null) {
							secondLeftY = y[anotherIndex];
							secondLeftX = x[anotherIndex];
							break;
						}
					}

					// If did not find another left neightbour, can't interpolate
					if (secondLeftY==null) continue;

					double deltaY = leftY-secondLeftY;
					double deltaX = leftX-secondLeftX;
					y[index] = new Double(leftY + deltaY/deltaX * (x[index]-leftX) );

				}

				// If only right neighbour is present, then must look for another left neighbour
				if ((leftY==null) && (rightY!=null)) {

					// Find second nearest existing left neighbour
					Double secondRightY = null;
					double secondRightX = 0.0;
					for (int anotherIndex=rightIndex+1; anotherIndex<y.length; anotherIndex++) {
						if (y[anotherIndex]!=null) {
							secondRightY = y[anotherIndex];
							secondRightX = x[anotherIndex];
							break;
						}
					}

					// If did not find another left neightbour, can't interpolate
					if (secondRightY==null) continue;

					double deltaY = secondRightY-rightY;
					double deltaX = secondRightX-rightX;
					y[index] = new Double(rightY - deltaY/deltaX * (rightX-x[index]) );

				}

			}

		}

	}
*/

}