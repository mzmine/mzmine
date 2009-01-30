/*
 * Copyright 2006-2009 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peakpicking.shapemodeler.peakmodels;

import java.util.TreeMap;
import java.util.logging.Logger;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.MzPeak;
import net.sf.mzmine.data.PeakStatus;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.data.impl.SimpleMzPeak;
import net.sf.mzmine.util.PeakUtils;
import net.sf.mzmine.util.Range;

public class EMGPeakModel implements ChromatographicPeak {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private double xRight = -1, xLeft = -1;
    
	// Peak information
	private double rt, height, mz, area;
	private int[] scanNumbers;
	private RawDataFile rawDataFile;
	private PeakStatus status;
	private int representativeScan = -1, fragmentScan = -1;
	private Range rawDataPointsIntensityRange, rawDataPointsMZRange,
			rawDataPointsRTRange;
	private TreeMap<Integer, MzPeak> dataPointsMap;

	public double getArea() {
		return area;
	}

	public RawDataFile getDataFile() {
		return rawDataFile;
	}

	public double getHeight() {
		return height;
	}

	public double getMZ() {
		return mz;
	}

	public int getMostIntenseFragmentScanNumber() {
		return fragmentScan;
	}

	public MzPeak getMzPeak(int scanNumber) {
		return dataPointsMap.get(scanNumber);
	}

	public PeakStatus getPeakStatus() {
		return status;
	}

	public double getRT() {
		return rt;
	}

	public Range getRawDataPointsIntensityRange() {
		return rawDataPointsIntensityRange;
	}

	public Range getRawDataPointsMZRange() {
		return rawDataPointsMZRange;
	}

	public Range getRawDataPointsRTRange() {
		return rawDataPointsRTRange;
	}

	public int getRepresentativeScanNumber() {
		return representativeScan;
	}

	public int[] getScanNumbers() {
		return scanNumbers;
	}
	
	public String toString(){
		return "EMG peak " + PeakUtils.peakToString(this);
	}

	public EMGPeakModel(ChromatographicPeak originalDetectedShape,
			int[] scanNumbers, double[] intensities, double[] retentionTimes,
			double resolution) {

		height = originalDetectedShape.getHeight();
		rt = originalDetectedShape.getRT();
		mz = originalDetectedShape.getMZ();
		this.scanNumbers = scanNumbers;
		rawDataFile = originalDetectedShape.getDataFile();
		rawDataPointsIntensityRange = originalDetectedShape
				.getRawDataPointsIntensityRange();
		rawDataPointsMZRange = originalDetectedShape.getRawDataPointsMZRange();
		rawDataPointsRTRange = originalDetectedShape.getRawDataPointsRTRange();
		dataPointsMap = new TreeMap<Integer, MzPeak>();
		status = originalDetectedShape.getPeakStatus();

        xRight = -1;
        xLeft = -1;

		// FWFM (Full Width at Half Maximum)
		double FWHM = calculateWidth(intensities, retentionTimes, resolution, rt, mz,
				height);

        /*
         * Calculates asymmetry of the peak using the formula
         * 
         * Ap = FWHM / ([1.76 (b/a)^2] - [11.15 (b/a)] + 28)
         * 
         * This formula is a variation of Foley J.P., Dorsey J.G. "Equations for
         * calculation of chormatographic figures of Merit for Ideal and Skewed
         * Peaks", Anal. Chem. 1983, 55, 730-737.
         * 
         * */
         

        // Left side
        double beginning = xLeft;
        if (beginning <= 0)
        	beginning = retentionTimes[0];
        double a = (rt - beginning);
        
        // Right side
        double ending = xRight;
        if (ending <= 0)
        	ending = retentionTimes[retentionTimes.length - 1];
        double b = (ending - rt);

        double paramA = b / a;
        double paramB = (1.76d * Math.pow(paramA, 2))
                - (11.15d * paramA) + 28.0d;

        // Calculates Width at base of the peak.
        double paramC = (FWHM *2.355 ) / 4.71;
        double Ap = paramC / paramB;

        if ((b > a) && (Ap > 0))
            Ap *= -1;
        
		// Calculate intensity of each point in the shape.
		double shapeHeight, currentRT, previousRT, previousHeight, C = 0.5d;

		previousHeight = calculateEMGIntensity(height, rt, FWHM, Ap, C, retentionTimes[0]);
		MzPeak mzPeak = new SimpleMzPeak(
				new SimpleDataPoint(mz, previousHeight));
		dataPointsMap.put(scanNumbers[0], mzPeak);

		for (int i = 1; i < retentionTimes.length; i++) {

			shapeHeight = calculateEMGIntensity(height, rt, FWHM, Ap, C, retentionTimes[0]);
			logger.finest("Shape value " + i+ " intensity " + intensities[i] + " shape " + shapeHeight);
			mzPeak = new SimpleMzPeak(new SimpleDataPoint(mz, shapeHeight));
			dataPointsMap.put(scanNumbers[i], mzPeak);

			currentRT = retentionTimes[i];
			previousRT = retentionTimes[i - 1];
			dataPointsMap.put(scanNumbers[0], mzPeak);
			area += (currentRT - previousRT) * (shapeHeight + previousHeight)
					/ 2;
			previousHeight = shapeHeight;
		}
        
    }

    /**
     * This method calculates the width of the chromatographic peak at half
     * intensity
     * 
     * @param listMzPeaks
     * @param height
     * @param RT
     * @return FWHM
     * */
     
    private double calculateWidth(double[] intensities,
			double[] retentionTimes, double resolution, double retentionTime,
			double mass, double maxIntensity) {

        double halfIntensity = maxIntensity / 2, intensity = 0, intensityPlus = 0;
        double leftY1,leftX1,leftY2,leftX2,mLeft;
        double rightY1,rightX1,rightY2,rightX2,mRight;
        double localXLeft = -1, localXRight = -1;
        double lowestLeft = intensity/5, lowestRight = intensity/5;
        double FWHM ;
        double beginning = retentionTimes[0];
        double ending = retentionTimes[retentionTimes.length - 1];

        for (int i = 0; i < retentionTimes.length - 1; i++) {

            intensity = intensities[i];
            intensityPlus = intensities[i + 1];

            if (intensity > height)
                continue;

            // Left side of the curve
            if (retentionTimes[i] < retentionTime) {
            	if (intensity < lowestLeft){
            		lowestLeft = intensity;
            		xLeft = retentionTimes[i];
            	}
            	
            	
            	
                if ((intensity <= halfIntensity) && (retentionTimes[i] < retentionTime)
                        && (intensityPlus >= halfIntensity)) {

                    // First point with intensity just less than half of total
                    // intensity
                    leftY1 = intensity;
                    leftX1 = retentionTimes[i];

                    // Second point with intensity just bigger than half of
                    // total
                    // intensity
                    leftY2 = intensityPlus;
                    leftX2 = retentionTimes[i + 1];

                    // We calculate the slope with formula m = Y1 - Y2 / X1 - X2
                    mLeft = (leftY1 - leftY2) / (leftX1 - leftX2);

                    // We calculate the desired point (at half intensity) with
                    // the
                    // linear equation
                    // X = X1 + [(Y - Y1) / m ], where Y = half of total
                    // intensity
                    localXLeft = leftX1 + (((halfIntensity) - leftY1) / mLeft);
                    continue;
                }
            }

            // Right side of the curve
            if (retentionTime > retentionTime) {

            	if (intensity < lowestRight){
            		lowestRight = intensity;
            		xRight = retentionTimes[i];
            	}

            	
            	if ((intensity >= halfIntensity) && (retentionTimes[i] > retentionTime)
                        && (intensityPlus <= halfIntensity)) {

                    // First point with intensity just bigger than half of total
                    // intensity
                    rightY1 = intensity;
                    rightX1 = retentionTime;

                    // Second point with intensity just less than half of total
                    // intensity
                    rightY2 = intensityPlus;
                    rightX2 = retentionTimes[i + 1];

                    // We calculate the slope with formula m = Y1 - Y2 / X1 - X2
                    mRight = (rightY1 - rightY2) / (rightX1 - rightX2);

                    // We calculate the desired point (at half intensity) with
                    // the
                    // linear equation
                    // X = X1 + [(Y - Y1) / m ], where Y = half of total
                    // intensity
                    localXRight = rightX1 + (((halfIntensity) - rightY1) / mRight);
                    break;
                }
            }
        }
        
        FWHM = (localXRight - localXLeft) / 2.354820045d;
        boolean negative = (((localXRight - localXLeft)) < 0);

        if ((localXRight <= -1) && (localXLeft > 0)) {
            localXRight = retentionTime + (ending - beginning) / 4.71f;
            FWHM = (localXRight - localXLeft) / 2.354820045d;
        }

        else if ((localXRight > 0) && (localXLeft <= -1)) {
            localXLeft = retentionTime - (ending - beginning) / 4.71f;
            FWHM = (localXRight - localXLeft) / 2.354820045d;
        }

        if ((negative) || ((localXRight == -1) && (localXLeft == -1))) {
        	FWHM = (mass / resolution);
            FWHM /= 2.354820045d;
        }

        return FWHM ;

    }

    /**
     * 
     * This method calculates the height of Exponential Modified Gaussian
     * function, using the mathematical model proposed by Zs. Pápai and T. L.
     * Pap "Determination of chromatographic peak parameters by non-linear curve
     * fitting using statistical moments", The Analyst,
     * http://www.rsc.org/publishing/journals/AN/article.asp?doi=b111304f
     * 
     * @param heightMax
     * @param RT
     * @param Dp
     * @param Ap
     * @param C
     * @param t
     * @return intensity
     */
    private double calculateEMGIntensity(double heightMax, double RT, double Dp,
            double Ap, double C, double t) {
        double shapeHeight;

        double partA1 = Math.pow((t - RT), 2) / (2 * Math.pow(Dp, 2));
        double partA = heightMax * Math.exp(-1 * partA1);

        double partB1 = (t - RT) / Dp;
        double partB2 = (Ap / 6.0f) * (Math.pow(partB1, 2) - (3 * partB1));
        double partB3 = (C / 24)
                * (Math.pow(partB1, 4) - (6 * Math.pow(partB1, 2)) + 3);
        double partB = 1 + partB2 + partB3;

        shapeHeight = (double) (partA * partB);

        if (shapeHeight < 0)
            shapeHeight = 0;

        return shapeHeight;
    }

}
