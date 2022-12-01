/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.featdet_chromatogrambuilder;

import io.github.mzmine.datamodel.Scan;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import com.google.common.collect.Range;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;

public class HighestDataPointConnector {

  private final MZTolerance mzTolerance;
  private final double minimumTimeSpan, minimumHeight;
  private final RawDataFile dataFile;
  private final Scan allScanNumbers[];

  // Mapping of last data point m/z --> chromatogram
  private Set<Chromatogram> buildingChromatograms;

  public HighestDataPointConnector(RawDataFile dataFile, Scan allScanNumbers[],
      double minimumTimeSpan, double minimumHeight, MZTolerance mzTolerance) {

    this.mzTolerance = mzTolerance;
    this.minimumHeight = minimumHeight;
    this.minimumTimeSpan = minimumTimeSpan;
    this.dataFile = dataFile;
    this.allScanNumbers = allScanNumbers;

    // We use LinkedHashSet to maintain a reproducible ordering. If we use
    // plain HashSet, the resulting feature list row IDs will have different
    // order every time the method is invoked.
    buildingChromatograms = new LinkedHashSet<Chromatogram>();

  }

  public void addScan(Scan scanNumber, DataPoint mzValues[]) {

    // Sort m/z peaks by descending intensity
    Arrays.sort(mzValues,
        new DataPointSorter(SortingProperty.Intensity, SortingDirection.Descending));

    // Set of already connected chromatograms in each iteration
    Set<Chromatogram> connectedChromatograms = new LinkedHashSet<Chromatogram>();

    // TODO: these two nested cycles should be optimized for speed
    for (DataPoint mzPeak : mzValues) {

      // Search for best chromatogram, which has highest last data point
      Chromatogram bestChromatogram = null;

      for (Chromatogram testChrom : buildingChromatograms) {

        DataPoint lastMzPeak = testChrom.getLastMzPeak();
        Range<Double> toleranceRange = mzTolerance.getToleranceRange(lastMzPeak.getMZ());
        if (toleranceRange.contains(mzPeak.getMZ())) {
          if ((bestChromatogram == null) || (testChrom.getLastMzPeak()
              .getIntensity() > bestChromatogram.getLastMzPeak().getIntensity())) {
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
        bestChromatogram = new Chromatogram(dataFile, allScanNumbers);
      }

      // Add this mzPeak to the chromatogram
      bestChromatogram.addMzPeak(scanNumber, mzPeak);

      // Move the chromatogram to the set of connected chromatograms
      connectedChromatograms.add(bestChromatogram);

    }

    // Process those chromatograms which were not connected to any m/z peak
    for (Chromatogram testChrom : buildingChromatograms) {

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

  public Chromatogram[] finishChromatograms() {

    // Iterate through current chromatograms and remove those which do not
    // contain any committed segment nor long-enough building segment

    Iterator<Chromatogram> chromIterator = buildingChromatograms.iterator();
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
    Chromatogram[] chromatograms = buildingChromatograms.toArray(new Chromatogram[0]);
    return chromatograms;
  }

}
