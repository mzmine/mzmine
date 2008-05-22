/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peakpicking.twostep.massdetection.localmaxima;

import java.util.Vector;
import java.util.logging.Logger;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.modules.peakpicking.twostep.massdetection.MassDetector;
import net.sf.mzmine.modules.peakpicking.twostep.massdetection.MzPeak;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.ScanUtils;

public class LocalMaxMassDetector implements MassDetector {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	// parameter values
    private float binSize, chromatographicThresholdLevel, noiseLevel;
    private float minimumMZPeakWidth, maximumMZPeakWidth;
    private Range mzRange;
    
    public LocalMaxMassDetector(LocalMaxMassDetectorParameters parameters, Range mzRange) {

        binSize = (Float) parameters.getParameterValue(LocalMaxMassDetectorParameters.binSize);
        chromatographicThresholdLevel = (Float) parameters.getParameterValue(LocalMaxMassDetectorParameters.chromatographicThresholdLevel);
        noiseLevel = (Float) parameters.getParameterValue(LocalMaxMassDetectorParameters.noiseLevel);
        minimumMZPeakWidth = (Float) parameters.getParameterValue(LocalMaxMassDetectorParameters.minimumMZPeakWidth);
        maximumMZPeakWidth = (Float) parameters.getParameterValue(LocalMaxMassDetectorParameters.maximumMZPeakWidth); 
        
        // Get minimum and maximum m/z values
        this.mzRange = mzRange;
        
    }
    public Vector<MzPeak> getMassValues(Scan scan) {
    	return getMassValues (scan, 0);
    }
    
    public Vector<MzPeak> getMassValues(Scan scan, int scanNumber) {
    	
        /*
         * Calculate M/Z binning
         */
    	
        int numOfBins = (int) (Math.ceil(mzRange.getSize() / binSize));
        float[] chromatographicThresholds = new float[numOfBins];

        Scan sc = scan;
        DataPoint dataPoints[] = sc.getDataPoints();
        float[] mzValues = new float[dataPoints.length];
        float[] intensityValues = new float[dataPoints.length];
        for (int dp = 0; dp < dataPoints.length; dp++) {
            mzValues[dp] = dataPoints[dp].getMZ();
            intensityValues[dp] = dataPoints[dp].getIntensity();
        }
        
        if (chromatographicThresholdLevel > 0) {

            float[] binInts = new float[numOfBins];
               
            float[] tmpInts = ScanUtils.binValues(mzValues,
                  intensityValues, mzRange, numOfBins, true,
                  ScanUtils.BinningType.MAX);
            for (int bini = 0; bini < numOfBins; bini++) {
                binInts[bini] = tmpInts[bini];
            }


            // Calculate filtering threshold from each RIC
            float initialThreshold = Float.MAX_VALUE;

            for (int bini = 0; bini < numOfBins; bini++) {
                chromatographicThresholds[bini] = binInts[bini];
                if (chromatographicThresholds[bini] < initialThreshold) {
                    initialThreshold = chromatographicThresholds[bini];
                }
            }

            binInts = null;

        } else {
            for (int bini = 0; bini < numOfBins; bini++)
                chromatographicThresholds[bini] = 0;
        }

        Vector<MzPeak> mzPeaks = new Vector<MzPeak>();

        // Find MzPeaks

        Vector<Integer> mzPeakInds = new Vector<Integer>();
        recursiveThreshold(mzValues, intensityValues, 0, mzValues.length - 1,
                    noiseLevel, minimumMZPeakWidth, maximumMZPeakWidth, mzPeakInds, 0);

            for (Integer j : mzPeakInds) {
                // Is intensity above the noise level
                if (intensityValues[j] >= noiseLevel) {

                    // Determine correct bin
                    int bin = (int) Math.floor((mzValues[j] - mzRange.getMin()) / binSize);
                    if (bin < 0) {
                        bin = 0;
                    }
                    if (bin >= numOfBins) {
                        bin = numOfBins - 1;
                    }

                    // Is intensity above the chromatographic threshold level
                    // for this bin?
                    if (intensityValues[j] >= chromatographicThresholds[bin]) {

                    	mzPeaks.add(new MzPeak(scanNumber, j, mzValues[j],
                                intensityValues[j]));
                    }

                }

            }
        return mzPeaks;
    }

    
    /**
     * This function searches for maximums from given part of a spectrum
     */
    private int recursiveThreshold(float[] masses, float intensities[],
            int startInd, int stopInd, float thresholdLevel,
            float minPeakWidthMZ, float maxPeakWidthMZ,
            Vector<Integer> CentroidInds, int recuLevel) {

        int peakStartInd;
        int peakStopInd;
        float peakWidthMZ;
        int peakMinInd;
        int peakMaxInd;

        for (int ind = startInd; ind <= stopInd; ind++) {
            // While below threshold
            while ((ind <= stopInd) && (intensities[ind] <= thresholdLevel)) {
                ind++;
            }

            if (ind >= stopInd) {
                break;
            }

            peakStartInd = ind;
            peakMinInd = peakStartInd;
            peakMaxInd = peakStartInd;

            // While peak is on
            while ((ind <= stopInd) && (intensities[ind] > thresholdLevel)) {
                // Check if this is the minimum point of the peak
                if (intensities[ind] < intensities[peakMinInd]) {
                    peakMinInd = ind;
                }

                // Check if this is the maximum poin of the peak
                if (intensities[ind] > intensities[peakMaxInd]) {
                    peakMaxInd = ind;
                }

                ind++;
            }

            if (ind == stopInd) {
                ind--;
            }
            // peakStopInd = ind - 1;
            peakStopInd = ind - 1;

            // Is this suitable peak?

            if (peakStopInd < 0) {
                peakWidthMZ = 0;
            } else {
                int tmpInd1 = peakStartInd - 1;
                if (tmpInd1 < startInd) {
                    tmpInd1 = startInd;
                }
                int tmpInd2 = peakStopInd + 1;
                if (tmpInd2 > stopInd) {
                    tmpInd2 = stopInd;
                }
                peakWidthMZ = masses[peakStopInd] - masses[peakStartInd];
            }

            if ((peakWidthMZ >= minPeakWidthMZ)
                    && (peakWidthMZ <= maxPeakWidthMZ)) {

                // Two options: define peak centroid index as maxintensity index
                // or mean index of all indices
                CentroidInds.add(new Integer(peakMaxInd));

                if (recuLevel > 0) {
                    return peakStopInd + 1;
                }
                // lastKnownGoodPeakStopInd = peakStopInd;
            }

            // Is there need for further investigation?
            if (peakWidthMZ > maxPeakWidthMZ) {
                ind = recursiveThreshold(masses, intensities, peakStartInd,
                        peakStopInd, intensities[peakMinInd], minPeakWidthMZ,
                        maxPeakWidthMZ, CentroidInds, recuLevel + 1);
            }

            if (ind == (stopInd - 1)) {
                break;
            }
        }

        // return lastKnownGoodPeakStopInd;
        return stopInd;

    }

}
