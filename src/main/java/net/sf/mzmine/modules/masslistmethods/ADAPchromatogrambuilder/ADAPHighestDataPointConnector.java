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
 *
 * Edited and modified by Owen Myers (Oweenm@gmail.com)
 */

package net.sf.mzmine.modules.masslistmethods.ADAPchromatogrambuilder;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import com.google.common.collect.Range;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.util.DataPointSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

public class ADAPHighestDataPointConnector {

    private final MZTolerance mzTolerance;
    private final double minimumTimeSpan, minimumHeight;
    private final RawDataFile dataFile;
    private final int allScanNumbers[];

    // Mapping of last data point m/z --> chromatogram
    private Set<ADAPChromatogram> buildingChromatograms;

    public ADAPHighestDataPointConnector(RawDataFile dataFile, int allScanNumbers[],
            double minimumTimeSpan, double minimumHeight,
            MZTolerance mzTolerance) {

        this.mzTolerance = mzTolerance;
        this.minimumHeight = minimumHeight;
        this.minimumTimeSpan = minimumTimeSpan;
        this.dataFile = dataFile;
        this.allScanNumbers = allScanNumbers;

        // We use LinkedHashSet to maintain a reproducible ordering. If we use
        // plain HashSet, the resulting peak list row IDs will have different
        // order every time the method is invoked.
        buildingChromatograms = new LinkedHashSet<ADAPChromatogram>();

    }

    public void addScan(int scanNumber, DataPoint mzValues[]) {

        // Sort m/z peaks by descending intensity
        Arrays.sort(mzValues, new DataPointSorter(SortingProperty.Intensity,
                SortingDirection.Descending));

        // Set of already connected chromatograms in each iteration
        Set<ADAPChromatogram> connectedChromatograms = new LinkedHashSet<ADAPChromatogram>();

        // TODO: these two nested cycles should be optimized for speed
        for (DataPoint mzPeak : mzValues) {

            // Search for best chromatogram, which has highest last data point
            ADAPChromatogram bestChromatogram = null;

            for (ADAPChromatogram testChrom : buildingChromatograms) {

                DataPoint lastMzPeak = testChrom.getLastMzPeak();
                Range<Double> toleranceRange = mzTolerance
                        .getToleranceRange(lastMzPeak.getMZ());
                if (toleranceRange.contains(mzPeak.getMZ())) {
                    if ((bestChromatogram == null) || (testChrom.getLastMzPeak()
                            .getIntensity() > bestChromatogram.getLastMzPeak()
                                    .getIntensity())) {
                        bestChromatogram = testChrom;
                    }
                }

            }

            // If we found best chromatogram, check if it is already connected.
            // In such case, we may discard this mass and continue. If we
            // haven't found a chromatogram, we may create a new one.
            if (bestChromatogram != null) {
                if (connectedChromatograms.contains(bestChromatogram)) {
                    continue;
                }
            } else {
                bestChromatogram = new ADAPChromatogram(dataFile, allScanNumbers);
            }

            // Add this mzPeak to the chromatogram
            bestChromatogram.addMzPeak(scanNumber, mzPeak);

            // Move the chromatogram to the set of connected chromatograms
            connectedChromatograms.add(bestChromatogram);

        }

        // Process those chromatograms which were not connected to any m/z peak
        for (ADAPChromatogram testChrom : buildingChromatograms) {

            // Skip those which were connected
            if (connectedChromatograms.contains(testChrom)) {
                continue;
            }

            // Check if we just finished a long-enough segment
            if (testChrom.getBuildingSegmentLength() >= minimumTimeSpan) {
                testChrom.commitBuildingSegment();

                // Move the chromatogram to the set of connected chromatograms
                connectedChromatograms.add(testChrom);
                continue;
            }

            // Check if we have any committed segments in the chromatogram
            if (testChrom.getNumberOfCommittedSegments() > 0) {
                testChrom.removeBuildingSegment();

                // Move the chromatogram to the set of connected chromatograms
                connectedChromatograms.add(testChrom);
                continue;
            }

        }

        // All remaining chromatograms in buildingChromatograms are discarded
        // and buildingChromatograms is replaced with connectedChromatograms
        buildingChromatograms = connectedChromatograms;

    }

    public ADAPChromatogram[] finishChromatograms() {

        // Iterate through current chromatograms and remove those which do not
        // contain any committed segment nor long-enough building segment

        Iterator<ADAPChromatogram> chromIterator = buildingChromatograms.iterator();
        while (chromIterator.hasNext()) {

            ADAPChromatogram chromatogram = chromIterator.next();

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
        ADAPChromatogram[] chromatograms = buildingChromatograms
                .toArray(new ADAPChromatogram[0]);
        return chromatograms;
    }

}
