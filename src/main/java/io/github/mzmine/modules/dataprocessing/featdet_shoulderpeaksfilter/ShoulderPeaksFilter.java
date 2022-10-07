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

package io.github.mzmine.modules.dataprocessing.featdet_shoulderpeaksfilter;

import java.util.Arrays;
import java.util.Iterator;
import java.util.TreeSet;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;

public class ShoulderPeaksFilter {

  public static DataPoint[] filterMassValues(DataPoint[] mzPeaks, ParameterSet parameters) {

    double resolution =
        parameters.getParameter(ShoulderPeaksFilterParameters.resolution).getValue();

    PeakModel peakModel = null;

    // Try to create an instance of the peak model
    try {
      PeakModelType type =
          parameters.getParameter(ShoulderPeaksFilterParameters.peakModel).getValue();
      if (type == null)
        type = PeakModelType.GAUSS;
      Class<?> modelClass = type.getModelClass();
      peakModel = (PeakModel) modelClass.newInstance();
    } catch (Exception e) {
      e.printStackTrace();
    }

    // If peakModel is null, just don't do any filtering
    if (peakModel == null)
      return mzPeaks;

    // Create a tree set of detected mzPeaks sorted by MZ in ascending order
    TreeSet<DataPoint> finalMZPeaks =
        new TreeSet<DataPoint>(new DataPointSorter(SortingProperty.MZ, SortingDirection.Ascending));

    // Create a tree set of candidate mzPeaks sorted by intensity in
    // descending order.
    TreeSet<DataPoint> candidatePeaks = new TreeSet<DataPoint>(
        new DataPointSorter(SortingProperty.Intensity, SortingDirection.Descending));
    candidatePeaks.addAll(Arrays.asList(mzPeaks));

    while (candidatePeaks.size() > 0) {

      // Always take the biggest (intensity) peak
      DataPoint currentCandidate = candidatePeaks.first();

      // Add this candidate to the final tree set sorted by MZ and remove
      // from tree set sorted by intensity
      finalMZPeaks.add(currentCandidate);
      candidatePeaks.remove(currentCandidate);

      // Remove from tree set sorted by intensity all FTMS shoulder peaks,
      // taking as a main peak the current candidate
      removeLateralPeaks(currentCandidate, candidatePeaks, peakModel, resolution);

    }

    return finalMZPeaks.toArray(new DataPoint[0]);
  }

  /**
   * This function remove peaks encountered in the lateral of a main peak (currentCandidate) that
   * are considered as garbage, for example FTMS shoulder peaks.
   * 
   * First calculates a peak model (Gauss, Lorenzian, etc) defined by peakModelName parameter, with
   * the same position (m/z) and height (intensity) of the currentCandidate, and the defined
   * resolution (resolution parameter). Second search and remove all the lateral peaks that are
   * under the curve of the modeled peak.
   * 
   */
  private static void removeLateralPeaks(DataPoint currentCandidate, TreeSet<DataPoint> candidates,
      PeakModel peakModel, double resolution) {

    // We set our peak model with same position(m/z), height(intensity) and
    // resolution of the current peak
    peakModel.setParameters(currentCandidate.getMZ(), currentCandidate.getIntensity(), resolution);

    // We search over all peak candidates and remove all of them that are
    // under the curve defined by our peak model
    Iterator<DataPoint> candidatesIterator = candidates.iterator();
    while (candidatesIterator.hasNext()) {

      DataPoint lateralCandidate = candidatesIterator.next();

      // Condition in x domain (m/z)
      if ((lateralCandidate.getIntensity() < peakModel.getIntensity(lateralCandidate.getMZ()))) {
        candidatesIterator.remove();
      }
    }

  }

}
