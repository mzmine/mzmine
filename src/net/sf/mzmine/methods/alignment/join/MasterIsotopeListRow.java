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

package net.sf.mzmine.methods.alignment.join;

import java.util.Hashtable;
import java.util.Vector;

import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.util.IsotopePatternUtils;
import net.sf.mzmine.util.MathUtils;


/**
 * This class represent one row of the master isotope list
 */
class MasterIsotopeListRow {

    private Hashtable<OpenedRawDataFile, IsotopePattern> isotopePatterns;

    private double monoMZ;
    private double monoRT;

    private int chargeState;

    private Vector<Double> mzVals;
    private Vector<Double> rtVals;

    private boolean alreadyJoined = false;

    private int numberOfPeaksOnRow;

    public MasterIsotopeListRow() {
        isotopePatterns = new Hashtable<OpenedRawDataFile, IsotopePattern>();
        mzVals = new Vector<Double>();
        rtVals = new Vector<Double>();
    }

    public void addIsotopePattern(OpenedRawDataFile dataFile, IsotopePattern isotopePattern, IsotopePatternUtils util) {

        isotopePatterns.put(dataFile, isotopePattern);

        // Get monoisotopic peak
        Peak[] peaks = util.getPeaksInPattern(isotopePattern);
        Peak monoPeak = peaks[0];

        if (numberOfPeaksOnRow<peaks.length) numberOfPeaksOnRow = peaks.length;

        // Add M/Z and RT
        mzVals.add(monoPeak.getNormalizedMZ());
        rtVals.add(monoPeak.getNormalizedRT());

        // Update medians
        Double[] mzValsArray = mzVals.toArray(new Double[0]);
        double[] mzValsArrayN = new double[mzValsArray.length];
        for (int i=0; i<mzValsArray.length; i++)
            mzValsArrayN[i] = mzValsArray[i];
        monoMZ = MathUtils.calcQuantile(mzValsArrayN, 0.5);

        Double[] rtValsArray = rtVals.toArray(new Double[0]);
        double[] rtValsArrayN = new double[rtValsArray.length];
        for (int i=0; i<rtValsArray.length; i++)
            rtValsArrayN[i] = rtValsArray[i];
        monoRT = MathUtils.calcQuantile(rtValsArrayN, 0.5);


        // Set charge state
        chargeState = isotopePattern.getChargeState();

    }


    public double getMonoisotopicMZ() { return monoMZ; }

    public double getMonoisotopicRT() { return monoRT; }

    public int getChargeState() { return chargeState; }

    public int getNumberOfPeaksOnRow() { return numberOfPeaksOnRow; }

    public IsotopePattern getIsotopePattern(OpenedRawDataFile dataFile) {
        return isotopePatterns.get(dataFile);
    }

    public void setJoined(boolean b) { alreadyJoined = b; }

    public boolean isAlreadyJoined() { return alreadyJoined; }

}


