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

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.MzPeak;
import net.sf.mzmine.data.PeakStatus;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.data.impl.SimpleMzPeak;
import net.sf.mzmine.util.Range;

public class GaussianPeakModel implements ChromatographicPeak {

    private double xRight = -1, xLeft = -1;
    private double rtMain, intensityMain, FWHM, partC, part2C2;
    public double getArea() {
        // TODO Auto-generated method stub
        return 0;
    }
    public RawDataFile getDataFile() {
        // TODO Auto-generated method stub
        return null;
    }
    public double getHeight() {
        // TODO Auto-generated method stub
        return 0;
    }
    public double getMZ() {
        // TODO Auto-generated method stub
        return 0;
    }
    public int getMostIntenseFragmentScanNumber() {
        // TODO Auto-generated method stub
        return 0;
    }
    public MzPeak getMzPeak(int scanNumber) {
        // TODO Auto-generated method stub
        return null;
    }
    public PeakStatus getPeakStatus() {
        // TODO Auto-generated method stub
        return null;
    }
    public double getRT() {
        // TODO Auto-generated method stub
        return 0;
    }
    public Range getRawDataPointsIntensityRange() {
        // TODO Auto-generated method stub
        return null;
    }
    public Range getRawDataPointsMZRange() {
        // TODO Auto-generated method stub
        return null;
    }
    public Range getRawDataPointsRTRange() {
        // TODO Auto-generated method stub
        return null;
    }
    public int getRepresentativeScanNumber() {
        // TODO Auto-generated method stub
        return 0;
    }
    public int[] getScanNumbers() {
        // TODO Auto-generated method stub
        return null;
    }

    /*public ChromatographicPeak fillingPeak(
            ChromatographicPeak originalDetectedShape, double[] params) {

        xRight = -1;
        xLeft = -1;

        ConnectedMzPeak[] listMzPeaks = ((ConnectedPeak) originalDetectedShape).getAllMzPeaks();

        RawDataFile dataFile = originalDetectedShape.getDataFile();
        Range originalRange = originalDetectedShape.getRawDataPointsRTRange();
        double rangeSize = originalRange.getSize();
        double mass = originalDetectedShape.getMZ();
        Range extendedRange = new Range(originalRange.getMin() - rangeSize,
                originalRange.getMax() + rangeSize);
        int[] listScans = dataFile.getScanNumbers(1, extendedRange);

        double C, factor;

        C = params[0]; // level of excess;
        factor = (1.0f - (Math.abs(C) / 10.0f));

        intensityMain = originalDetectedShape.getHeight() * factor;
        rtMain = originalDetectedShape.getRT();
        // FWFM (Full Width at Half Maximum)

        FWHM = calculateWidth(listMzPeaks) * factor;

        if (FWHM < 0) {
            return originalDetectedShape;
        }

        partC = FWHM / 2.354820045f;
        part2C2 = 2f * (double) Math.pow(partC, 2);

        // Calculate intensity of each point in the shape.
        double t, shapeHeight;
        Scan scan;

        ConnectedMzPeak newMzPeak = null;
        ConnectedPeak filledPeak = null;

        for (int scanIndex : listScans) {

            scan = dataFile.getScan(scanIndex);
            t = scan.getRetentionTime();
            shapeHeight = getIntensity(t);

            // Level of significance
            if (shapeHeight < (intensityMain * 0.001))
                continue;

            newMzPeak = getDetectedMzPeak(listMzPeaks, scan);

            if (newMzPeak != null) {
                ((SimpleMzPeak) newMzPeak.getMzPeak()).setIntensity(shapeHeight);
            } else {
                newMzPeak = new ConnectedMzPeak(scan, new SimpleMzPeak(
                        new SimpleDataPoint(mass, shapeHeight)));
            }

            if (filledPeak == null) {
                filledPeak = new ConnectedPeak(
                        originalDetectedShape.getDataFile(), newMzPeak);
            }

            filledPeak.addMzPeak(newMzPeak);
        }

        return filledPeak;
    }

    /**
     * This method calculates the width of the chromatographic peak at half
     * intensity
     * 
     * @param listMzPeaks
     * @param height
     * @param RT
     * @return FWHM
     private double calculateWidth(ConnectedMzPeak[] listMzPeaks) {

        double halfIntensity = intensityMain / 2, intensity = 0, intensityPlus = 0, retentionTime = 0;
        ConnectedMzPeak[] rangeDataPoints = listMzPeaks; // .clone();

        for (int i = 0; i < rangeDataPoints.length - 1; i++) {

            intensity = rangeDataPoints[i].getMzPeak().getIntensity();
            intensityPlus = rangeDataPoints[i + 1].getMzPeak().getIntensity();
            retentionTime = rangeDataPoints[i].getScan().getRetentionTime();

            if (intensity > intensityMain)
                continue;

            // Left side of the curve
            if (retentionTime < rtMain) {
                if ((intensity <= halfIntensity)
                        && (intensityPlus >= halfIntensity)) {

                    // First point with intensity just less than half of total
                    // intensity
                    double leftY1 = intensity;
                    double leftX1 = retentionTime;

                    // Second point with intensity just bigger than half of
                    // total
                    // intensity
                    double leftY2 = intensityPlus;
                    double leftX2 = rangeDataPoints[i + 1].getScan().getRetentionTime();

                    // We calculate the slope with formula m = Y1 - Y2 / X1 - X2
                    double mLeft = (leftY1 - leftY2) / (leftX1 - leftX2);

                    // We calculate the desired point (at half intensity) with
                    // the
                    // linear equation
                    // X = X1 + [(Y - Y1) / m ], where Y = half of total
                    // intensity
                    xLeft = leftX1 + (((halfIntensity) - leftY1) / mLeft);
                    continue;
                }
            }

            // Right side of the curve
            if (retentionTime > rtMain) {
                if ((intensity >= halfIntensity)
                        && (intensityPlus <= halfIntensity)) {

                    // First point with intensity just bigger than half of total
                    // intensity
                    double rightY1 = intensity;
                    double rightX1 = retentionTime;

                    // Second point with intensity just less than half of total
                    // intensity
                    double rightY2 = intensityPlus;
                    double rightX2 = rangeDataPoints[i + 1].getScan().getRetentionTime();

                    // We calculate the slope with formula m = Y1 - Y2 / X1 - X2
                    double mRight = (rightY1 - rightY2) / (rightX1 - rightX2);

                    // We calculate the desired point (at half intensity) with
                    // the
                    // linear equation
                    // X = X1 + [(Y - Y1) / m ], where Y = half of total
                    // intensity
                    xRight = rightX1 + (((halfIntensity) - rightY1) / mRight);
                    break;
                }
            }
        }

        if ((xRight <= -1) && (xLeft > 0)) {
            double beginning = rangeDataPoints[0].getScan().getRetentionTime();
            double ending = rangeDataPoints[rangeDataPoints.length - 1].getScan().getRetentionTime();
            xRight = rtMain + (ending - beginning) / 4.71f;
        }

        if ((xRight > 0) && (xLeft <= -1)) {
            double beginning = rangeDataPoints[0].getScan().getRetentionTime();
            double ending = rangeDataPoints[rangeDataPoints.length - 1].getScan().getRetentionTime();
            xLeft = rtMain - (ending - beginning) / 4.71f;
        }

        boolean negative = (((xRight - xLeft)) < 0);

        if ((negative) || ((xRight == -1) && (xLeft == -1))) {
            double beginning = rangeDataPoints[0].getScan().getRetentionTime();
            double ending = rangeDataPoints[rangeDataPoints.length - 1].getScan().getRetentionTime();
            xRight = rtMain + (ending - beginning) / 9.42f;
            xLeft = rtMain - (ending - beginning) / 9.42f;
        }

        double FWHM = (xRight - xLeft);

        return FWHM;
    }

    public double getIntensity(double rt) {

        // Using the Gaussian function we calculate the intensity at given m/z
        double diff2 = (double) Math.pow(rt - rtMain, 2);
        double exponent = -1 * (diff2 / part2C2);
        double eX = (double) Math.exp(exponent);
        double intensity = intensityMain * eX;
        return intensity;
    }

    public ConnectedMzPeak getDetectedMzPeak(ConnectedMzPeak[] listMzPeaks,
            Scan scan) {
        int scanNumber = scan.getScanNumber();
        for (int i = 0; i < listMzPeaks.length; i++) {
            if (listMzPeaks[i].getScan().getScanNumber() == scanNumber)
                return listMzPeaks[i].clone();
        }
        return null;
    }
    */

}
