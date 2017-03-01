/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.qualityparameters;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.RawDataFile;

/**
 * Calculates quality parameters for each peak in a peak list: - Full width at
 * half maximum (FWHM) - Tailing Factor - Asymmetry factor
 */
public class QualityParameters {

    public static void calculateQualityParameters(PeakList peakList) {

        Feature peak;
        double height, rt;

        for (int i = 0; i < peakList.getNumberOfRows(); i++) {
            for (int x = 0; x < peakList.getNumberOfRawDataFiles(); x++) {

                peak = peakList.getPeak(i, peakList.getRawDataFile(x));
                if (peak != null) {
                    height = peak.getHeight();
                    rt = peak.getRT();

                    // FWHM
                    double rtValues[] = PeakFindRTs(height / 2, rt, peak);
                    Double fwhm = rtValues[1] - rtValues[0];
                    if (fwhm <= 0 || Double.isNaN(fwhm) || Double.isInfinite(fwhm)) {
                        fwhm = null;
                    }
                    peak.setFWHM(fwhm);

                    // Tailing Factor - TF
                    double rtValues2[] = PeakFindRTs(height * 0.05, rt, peak);
                    Double tf = (rtValues2[1] - rtValues2[0])
                            / (2 * (rt - rtValues2[0]));
                    if (tf <= 0 || Double.isNaN(tf) || Double.isInfinite(tf)) {
                        tf = null;
                    }
                    peak.setTailingFactor(tf);

                    // Asymmetry factor - AF
                    double rtValues3[] = PeakFindRTs(height * 0.1, rt, peak);
                    Double af = (rtValues3[1] - rt) / (rt - rtValues3[0]);
                    if (af <= 0 || Double.isNaN(af) || Double.isInfinite(af)) {
                        af = null;
                    }
                    peak.setAsymmetryFactor(af);

                }
            }
        }

    }

    private static double[] PeakFindRTs(double intensity, double rt, Feature peak) {

        double x1 = 0, x2 = 0, x3 = 0, x4 = 0, y1 = 0, y2 = 0, y3 = 0, y4 = 0,
                lastDiff1 = intensity, lastDiff2 = intensity, currentDiff, currentRT;
        int[] scanNumbers = peak.getScanNumbers();
        RawDataFile dataFile = peak.getDataFile();

        // Find the data points closet to input intensity on both side of the
        // peak apex
        for (int i = 1; i < scanNumbers.length - 1; i++) {

            if (peak.getDataPoint(scanNumbers[i]) != null) {
                currentDiff = Math.abs(intensity
                        - peak.getDataPoint(scanNumbers[i]).getIntensity());
                currentRT = dataFile.getScan(scanNumbers[i]).getRetentionTime();
                if (currentDiff < lastDiff1 & currentDiff > 0 & currentRT <= rt
                        & peak.getDataPoint(scanNumbers[i + 1]) != null) {
                    x1 = dataFile.getScan(scanNumbers[i]).getRetentionTime();
                    y1 = peak.getDataPoint(scanNumbers[i]).getIntensity();
                    x2 = dataFile.getScan(scanNumbers[i + 1])
                            .getRetentionTime();
                    y2 = peak.getDataPoint(scanNumbers[i + 1]).getIntensity();
                    lastDiff1 = currentDiff;
                } else if (currentDiff < lastDiff2 & currentDiff > 0
                        & currentRT >= rt
                        & peak.getDataPoint(scanNumbers[i - 1]) != null) {
                    x3 = dataFile.getScan(scanNumbers[i - 1])
                            .getRetentionTime();
                    y3 = peak.getDataPoint(scanNumbers[i - 1]).getIntensity();
                    x4 = dataFile.getScan(scanNumbers[i]).getRetentionTime();
                    y4 = peak.getDataPoint(scanNumbers[i]).getIntensity();
                    lastDiff2 = currentDiff;
                }
            }
        }

        // Calculate RT value for input intensity based on linear regression
        double slope, intercept, rt1, rt2;
        if (y1 > 0) {
            slope = (y2 - y1) / (x2 - x1);
            intercept = y1 - (slope * x1);
            rt1 = (intensity - intercept) / slope;
        } else if (x2 > 0) { // Straight drop of peak to 0 intensity
            rt1 = x2;
        } else {
            rt1 = peak.getRawDataPointsRTRange().lowerEndpoint();
        }
        if (y4 > 0) {
            slope = (y4 - y3) / (x4 - x3);
            intercept = y3 - (slope * x3);
            rt2 = (intensity - intercept) / slope;
        } else if (x3 > 0){ // Straight drop of peak to 0 intensity
            rt2 = x3;
        } else {
            rt2 = peak.getRawDataPointsRTRange().upperEndpoint();
        }

        return new double[] { rt1, rt2 };
    }

}
