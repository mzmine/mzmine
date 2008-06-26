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

package net.sf.mzmine.modules.peakpicking.threestep.xicconstruction;


/**
 * This class represents a score (goodness of fit) between a chromatogram and m/z peak
 */
public class MatchScore implements Comparable<MatchScore> {

    private float score;
    private Chromatogram chromatogram;
    private ConnectedMzPeak cMzPeak;
    private float mzTolerance;

    public MatchScore(Chromatogram chromatogram, ConnectedMzPeak cMzPeak, float mzTolerance) {
        this.mzTolerance = mzTolerance;
        this.chromatogram = chromatogram;
        this.cMzPeak = cMzPeak;
        score = calcScore(chromatogram, cMzPeak);
    }

    public float getScore() {
        return score;
    }

    public Chromatogram getChromatogram() {
        return chromatogram;
    }

    public ConnectedMzPeak getMzPeak() {
        return cMzPeak;
    }

    public int compareTo(MatchScore m) {
        int retsig = (int) Math.signum(score - m.getScore());
        if (retsig == 0) {
            retsig = -1;
        } 
        return retsig;
    }

    private float calcScore(Chromatogram chromatogram, ConnectedMzPeak cMzPeak) {

        float chromatoMZ = chromatogram.getMZ();
        float chromatoIntensity = chromatogram.getIntensity();

        // If mz difference is too big? (do this first for optimal
        // performance)
        if (Math.abs(chromatoMZ - cMzPeak.getMzPeak().getMZ()) > mzTolerance) {
            return Float.MAX_VALUE;

        } else {

            // Calculate score components and total score
            double scoreMZComponent = (float) Math.abs(chromatoMZ - cMzPeak.getMzPeak().getMZ());
            double scoreIntensityComponent = (float) Math.abs(chromatoIntensity - cMzPeak.getMzPeak().getIntensity());
            float totalScore = (float) Math.sqrt(scoreMZComponent
                    * scoreMZComponent + scoreIntensityComponent
                    * scoreIntensityComponent);

            return totalScore;
        }

    }
}