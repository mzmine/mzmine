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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massconnection.highestdatapoint;

import java.util.Arrays;
import java.util.Iterator;
import java.util.TreeMap;

import net.sf.mzmine.data.MzPeak;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massconnection.Chromatogram;
import net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massconnection.MassConnector;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.DataPointSorter;

public class HighestDataPointConnector implements MassConnector {

    private double mzTolerance, minimumTimeSpan, minimumHeight;

    // Mapping of last data point m/z --> chromatogram
    private TreeMap<Double, Chromatogram> buildingChromatograms;

    public HighestDataPointConnector(
            HighestDataPointConnectorParameters parameters) {

        minimumTimeSpan = (Double) parameters.getParameterValue(HighestDataPointConnectorParameters.minimumTimeSpan);
        minimumHeight = (Double) parameters.getParameterValue(HighestDataPointConnectorParameters.minimumHeight);
        mzTolerance = (Double) parameters.getParameterValue(HighestDataPointConnectorParameters.mzTolerance);

        buildingChromatograms = new TreeMap<Double, Chromatogram>();
    }

    public void addScan(RawDataFile dataFile, int scanNumber, MzPeak[] mzValues) {

        // Sort m/z peaks by descending intensity
        Arrays.sort(mzValues, new DataPointSorter(false, false));

        // Create an empty set of updated chromatograms
        TreeMap<Double, Chromatogram> connectedChromatograms = new TreeMap<Double, Chromatogram>();

        for (MzPeak mzPeak : mzValues) {

            // Use binary search to find chromatograms within m/z tolerance
            double mzKeys[] = CollectionUtils.toDoubleArray(buildingChromatograms.keySet());
            int index = Arrays.binarySearch(mzKeys, mzPeak.getMZ()
                    - mzTolerance);
            if (index < 0)
                index = (index + 1) * -1;

            // Search for best chromatogram, which has highest last data point
            Chromatogram bestChromatogram = null;
            double bestKey = Double.NaN;

            while (index < mzKeys.length) {

                if (mzKeys[index] > mzPeak.getMZ() + mzTolerance)
                    break;

                Chromatogram chrom = buildingChromatograms.get(mzKeys[index]);
                if ((bestChromatogram == null)
                        || (chrom.getLastMzPeak().getIntensity() > bestChromatogram.getLastMzPeak().getIntensity())) {
                    bestChromatogram = chrom;
                    bestKey = mzKeys[index];
                }

                index++;

            }

            // If we found best chromatogram, remove it from
            // buildingChromatograms. If we haven't, create a new one.
            if (bestChromatogram != null) {
                buildingChromatograms.remove(bestKey);
            } else {
                bestChromatogram = new Chromatogram(dataFile);
            }

            // Add this mzPeak to the chromatogram
            bestChromatogram.addMzPeak(scanNumber, mzPeak);

            // Move the chromatogram to the set of connected chromatograms
            connectedChromatograms.put(mzPeak.getMZ(), bestChromatogram);

        }

        // Process those chromatograms which were not connected to any m/z peak
        for (Chromatogram testChrom : buildingChromatograms.values()) {

            // Check if we just finished a long-enough segment
            if (testChrom.getBuildingSegmentLength() >= minimumTimeSpan) {
                testChrom.commitBuildingSegment();

                // Move the chromatogram to the set of connected chromatograms
                connectedChromatograms.put(testChrom.getLastMzPeak().getMZ(),
                        testChrom);
                continue;
            }

            // Check if we have any committed segments in the chromatogram
            if (testChrom.getNumberOfCommittedSegments() > 0) {
                testChrom.removeBuildingSegment();

                // Move the chromatogram to the set of connected chromatograms
                connectedChromatograms.put(testChrom.getLastMzPeak().getMZ(),
                        testChrom);
            }

        }

        // All remaining chromatograms in buildingChromatograms are discarded
        // and buildingChromatograms is replaced with connectedChromatograms
        buildingChromatograms = connectedChromatograms;

    }

    public Chromatogram[] finishChromatograms() {

        // Iterate through current chromatograms and remove those which do not
        // contain any committed segment nor long-enough building segment
        Iterator<Chromatogram> chromIterator = buildingChromatograms.values().iterator();
        while (chromIterator.hasNext()) {

            Chromatogram chromatogram = chromIterator.next();

            if (chromatogram.getBuildingSegmentLength() >= minimumTimeSpan) {
                chromatogram.commitBuildingSegment();
                chromatogram.finishChromatogram();
            } else {
                if (chromatogram.getNumberOfCommittedSegments() == 0) {
                    chromIterator.remove();
                    continue;
                } else {
                    chromatogram.removeBuildingSegment();
                    chromatogram.finishChromatogram();
                }
            }

            // Remove chromatograms smaller then minimum height
            if (chromatogram.getHeight() < minimumHeight)
                chromIterator.remove();

        }

        // All remaining chromatograms are good, so we can return them
        Chromatogram[] chromatograms = buildingChromatograms.values().toArray(
                new Chromatogram[0]);
        return chromatograms;
    }

}
