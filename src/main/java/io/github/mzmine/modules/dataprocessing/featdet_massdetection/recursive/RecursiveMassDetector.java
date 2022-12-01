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

package io.github.mzmine.modules.dataprocessing.featdet_massdetection.recursive;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import org.jetbrains.annotations.NotNull;

public class RecursiveMassDetector implements MassDetector {

  @Override
  public double[][] getMassValues(MassSpectrum scan, ParameterSet parameters) {
    double noiseLevel =
        parameters.getParameter(RecursiveMassDetectorParameters.noiseLevel).getValue();
    double minimumMZPeakWidth =
        parameters.getParameter(RecursiveMassDetectorParameters.minimumMZPeakWidth).getValue();
    double maximumMZPeakWidth =
        parameters.getParameter(RecursiveMassDetectorParameters.maximumMZPeakWidth).getValue();

    TreeSet<DataPoint> mzPeaks =
        new TreeSet<DataPoint>(new DataPointSorter(SortingProperty.MZ, SortingDirection.Ascending));

    // Find MzPeaks
    recursiveThreshold(mzPeaks, scan, 1, scan.getNumberOfDataPoints() - 1, noiseLevel,
        minimumMZPeakWidth, maximumMZPeakWidth, 0);

    // convert to double[][] TODO remove use of DataPoint
    int size = mzPeaks.size();
    double[] mzs = new double[size];
    double[] intensities = new double[size];
    int i = 0;
    for (DataPoint mzPeak : mzPeaks) {
      mzs[i] = mzPeak.getMZ();
      intensities[i] = mzPeak.getIntensity();
      i++;
    }
    return new double[][]{mzs, intensities};
  }

  /**
   * This function searches for maxima from given part of a spectrum
   */
  private int recursiveThreshold(TreeSet<DataPoint> mzPeaks, MassSpectrum scan, int startInd,
      int stopInd, double curentNoiseLevel, double minimumMZPeakWidth, double maximumMZPeakWidth,
      int recuLevel) {

    // logger.finest(" Level of recursion " + recuLevel);

    List<DataPoint> RawDataPointsInds = new ArrayList<DataPoint>();
    int peakStartInd, peakStopInd, peakMaxInd;
    double peakWidthMZ;

    for (int ind = startInd; ind < stopInd; ind++) {

      boolean currentIsBiggerNoise = scan.getIntensityValue(ind) > curentNoiseLevel;
      double localMinimum = Double.MAX_VALUE;

      // Ignore intensities below curentNoiseLevel
      if (!currentIsBiggerNoise) {
        continue;
      }

      // Add initial point of the peak
      peakStartInd = ind;
      peakMaxInd = peakStartInd;

      // While peak is on
      while ((ind < stopInd) && (scan.getIntensityValue(ind) > curentNoiseLevel)) {

        boolean isLocalMinimum =
            (scan.getIntensityValue(ind - 1) > scan.getIntensityValue(ind))
                && (scan.getIntensityValue(ind) < scan.getIntensityValue(ind + 1));

        // Check if this is the minimum point of the peak
        if (isLocalMinimum && (scan.getIntensityValue(ind) < localMinimum))
          localMinimum = scan.getIntensityValue(ind);

        // Check if this is the maximum point of the peak
        if (scan.getIntensityValue(ind) > scan.getIntensityValue(peakMaxInd))
          peakMaxInd = ind;

        // Forming the DataPoint array that defines this peak
        RawDataPointsInds.add(new SimpleDataPoint(scan.getMzValue(ind), scan.getIntensityValue(ind)));
        ind++;
      }

      // Add ending point of the peak
      peakStopInd = ind;

      peakWidthMZ = scan.getMzValue(peakStopInd) - scan.getMzValue(peakStartInd);

      // Verify width of the peak
      if ((peakWidthMZ >= minimumMZPeakWidth) && (peakWidthMZ <= maximumMZPeakWidth)) {

        // Declare a new MzPeak with intensity equal to max intensity
        // data point
        mzPeaks.add(new SimpleDataPoint(scan.getMzValue(peakMaxInd), scan.getIntensityValue(peakMaxInd)));

        if (recuLevel > 0) {
          // return stop index and beginning of the next peak
          return ind;
        }
      }
      RawDataPointsInds.clear();

      // If the peak is still too big applies the same method until find a
      // peak of the right size
      if (peakWidthMZ > maximumMZPeakWidth) {
        if (localMinimum < Double.MAX_VALUE) {
          ind = recursiveThreshold(mzPeaks, scan, peakStartInd, peakStopInd, localMinimum,
              minimumMZPeakWidth, maximumMZPeakWidth, recuLevel + 1);
        }

      }

    }

    // return stop index
    return stopInd;

  }

  @Override
  public @NotNull String getName() {
    return "Recursive threshold";
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return RecursiveMassDetectorParameters.class;
  }

}
