/*
 * Copyright 2006-2007 The MZmine Development Team
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

package net.sf.mzmine.modules.peakpicking.recursivethreshold;

import java.util.ArrayList;

import net.sf.mzmine.data.impl.ConstructionPeak;


/**
 * This class represents a score (goodness of fit) between Peak and 1D-peak
 */
class MatchScore implements Comparable<MatchScore> {

    private double score;
    private ConstructionPeak ucPeak;
    private OneDimPeak oneDimPeak;
    private double mzTolerance, intTolerance;

    MatchScore(ConstructionPeak uc, OneDimPeak od, double mzTolerance, double intTolerance) {
        this.mzTolerance = mzTolerance;
        this.intTolerance = intTolerance;
        ucPeak = uc;
        oneDimPeak = od;
        score = calcScore(uc, od);
    }

    public double getScore() {
        return score;
    }

    public ConstructionPeak getPeak() {
        return ucPeak;
    }

    public OneDimPeak getOneDimPeak() {
        return oneDimPeak;
    }

    public int compareTo(MatchScore m) {
        int retsig = (int) java.lang.Math.signum(score - m.getScore());
        if (retsig == 0) {
            retsig = -1;
        } // Must never return 0, because treeset can't hold equal
        // elements
        return retsig;
    }

    private double calcScore(ConstructionPeak uc, OneDimPeak od) {

        double ucMZ = uc.getMZ();

        // If mz difference is too big? (do this first for optimal
        // performance)
        if (java.lang.Math.abs(ucMZ - od.mz) > mzTolerance) {
            return Double.MAX_VALUE;

        } else {

            // Calculate score components and total score
            double scoreMZComponent = java.lang.Math.abs(ucMZ - od.mz);
            double scoreRTComponent = calcScoreForRTShape(uc, od);
            double totalScore = java.lang.Math.sqrt(scoreMZComponent
                    * scoreMZComponent + scoreRTComponent
                    * scoreRTComponent);

            return totalScore;
        }

    }

    /**
     * This function check for the shape of the peak in RT direction, and
     * determines if it is possible to add given m/z peak at the end of the
     * peak.
     */
    private double calcScoreForRTShape(ConstructionPeak uc, OneDimPeak od) {

        double nextIntensity = od.intensity;
        //Hashtable<Integer, Double[]> datapoints = uc.getRawDatapoints();
        int[] scanNumbers = uc.getScanNumbers();

        // If no previous m/z peaks
        if (scanNumbers.length == 0) {
            return 0;
        }

        ArrayList<Double> intensities = uc.getConstructionIntensities();

        // If only one previous m/z peak
        if (scanNumbers.length == 1) {

            double prevIntensity = intensities.get(0);

            // If it goes up, then give minimum (best) score
            if ((nextIntensity - prevIntensity) >= 0) {
                return 0;
            }

            // If it goes too much down, then give MAX_VALUE
            double bottomMargin = prevIntensity
                    * (1 - intTolerance);
            if (nextIntensity <= bottomMargin) {
                return Double.MAX_VALUE;
            }

            // If it goes little bit down, but within marginal, then give
            // score between 0...maxScore
            // return ( (prevIntensity-nextIntensity) / (
            // prevIntensity-bottomMargin) );
            return 0;

        }

        // There are two or more previous m/z peaks in this peak

        // Determine shape of the peak

        int derSign = 1;

        for (int ind=1; ind<scanNumbers.length; ind++) {

            double prevIntensity = intensities.get(ind-1);
            double currIntensity = intensities.get(ind);

            // If peak is currently going up
            if (derSign == 1) {
                // Then next intensity must be above bottomMargin or derSign
                // changes
                double bottomMargin = prevIntensity
                        * (1 - intTolerance);

                if (currIntensity <= bottomMargin) {
                    derSign = -1;
                    continue;
                }
            }

            // If peak is currently going down
            if (derSign == -1) {
                // Then next intensity should be less than topMargin or peak
                // ends
                double topMargin = prevIntensity
                        * (1 + intTolerance);

                if (currIntensity >= topMargin) {
                    return Double.MAX_VALUE;
                }
            }

        }
        // derSign now contains information about RT peak shape at the end
        // of the peak so far

        // If peak is currently going up
        if (derSign == 1) {

            // Then give minimum (best) score in any case (peak can continue
            // going up or start going down)
            return 0;
        }

        // If peak is currently going down
        if (derSign == -1) {

            double lastIntensity = intensities.get(intensities.size()-1);

            // Then peak must not start going up again
            double topMargin = lastIntensity
                    * (1 + intTolerance);

            if (nextIntensity >= topMargin) {
                return Double.MAX_VALUE;
            }

            if (nextIntensity < lastIntensity) {
                return 0;
            }

            // return maxScore * ( 1 - ( (topMargin-nextInt) /
            // (topMargin-prevInts[usedSize-1]) ) );
            return 0;
        }

        // Should never go here
        return Double.MAX_VALUE;

    }
    
}