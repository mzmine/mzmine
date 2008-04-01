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

package net.sf.mzmine.modules.peakpicking.centroid;

import java.util.ArrayList;


/**
 * This class represents a score (goodness of fit) between Peak and 1D-peak
 */
class MatchScore implements Comparable<MatchScore> {

    private float score;
    private CentroidPeak ucPeak;
    private OneDimPeak oneDimPeak;
    private float mzTolerance, intTolerance;

    MatchScore(CentroidPeak uc, OneDimPeak od, float mzTolerance, float intTolerance) {
        this.mzTolerance = mzTolerance;
        this.intTolerance = intTolerance;
        ucPeak = uc;
        oneDimPeak = od;
        score = calcScore(uc, od);
    }

    public float getScore() {
        return score;
    }

    public CentroidPeak getPeak() {
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

    private float calcScore(CentroidPeak uc, OneDimPeak od) {

        float ucMZ = uc.getMZ();

        // If mz difference is too big? (do this first for optimal
        // performance)
        if (java.lang.Math.abs(ucMZ - od.mz) > mzTolerance) {
            return Float.MAX_VALUE;

        } else {

            // Calculate score components and total score
            float scoreMZComponent = Math.abs(ucMZ - od.mz);
            float scoreRTComponent = calcScoreForRTShape(uc, od);
            float totalScore = (float) Math.sqrt(scoreMZComponent
                    * scoreMZComponent + scoreRTComponent
                    * scoreRTComponent);

            return totalScore;
        }

    }

    /**
     * This function check for the shape of the peak in RT direction, and
     * determines if it is possible to add given m/z peak at the end of the
     * peak.
     *
     */
    private float calcScoreForRTShape(CentroidPeak uc, OneDimPeak od) {

        float nextIntensity = od.intensity;
        //Hashtable<Integer, Float[]> datapoints = uc.getRawDatapoints();

        int[] scanNumbers = uc.getScanNumbers();

        // If no previous m/z peaks
        if (scanNumbers.length == 0) {
            return 0;
        }

        ArrayList<Float> intensities = uc.getConstructionIntensities();
        
        // If only one previous m/z peak
        if (scanNumbers.length == 1) {

            float prevIntensity = intensities.get(0);

            // If it goes up, then give minimum (best) score
            if ((nextIntensity - prevIntensity) >= 0) {
                return 0;
            }

            // If it goes too much down, then give MAX_VALUE
            float bottomMargin = prevIntensity
                    * (1 - intTolerance);
            if (nextIntensity <= bottomMargin) {
                return Float.MAX_VALUE;
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

            float prevIntensity = intensities.get(ind-1);
            float currIntensity = intensities.get(ind);

            // If peak is currently going up
            if (derSign == 1) {
                // Then next intensity must be above bottomMargin or derSign
                // changes
                float bottomMargin = prevIntensity
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
                float topMargin = prevIntensity
                        * (1 + intTolerance);

                if (currIntensity >= topMargin) {
                    return Float.MAX_VALUE;
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

            float lastIntensity = intensities.get(intensities.size()-1);

            // Then peak must not start going up again
            float topMargin = lastIntensity
                    * (1 + intTolerance);

            if (nextIntensity >= topMargin) {
                return Float.MAX_VALUE;
            }

            if (nextIntensity < lastIntensity) {
                return 0;
            }

            // return maxScore * ( 1 - ( (topMargin-nextInt) /
            // (topMargin-prevInts[usedSize-1]) ) );
            return 0;
        }

        // Should never go here
        return Float.MAX_VALUE;

    }

}