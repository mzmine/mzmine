/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_massdetection.frequency;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.exactmass.ExactMassDetector;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.scans.ScanUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class FrequencyMassDetector implements MassDetector {

  private final int maxSignalsInBin;
  private final double intensityPercentile;

  /**
   * required to create a default instance via reflection
   */
  public FrequencyMassDetector() {
    maxSignalsInBin = 3;
    intensityPercentile = 0.999;
  }

  public FrequencyMassDetector(ParameterSet parameters) {
    maxSignalsInBin = parameters.getValue(FrequencyMassDetectorParameters.maxSignalsInBin);
    intensityPercentile = parameters.getValue(FrequencyMassDetectorParameters.intensityPercentile);
  }


  public static double[][] getMassValues(MassSpectrum spectrum, int maxSignalsInBin,
      double intensityPercentile) {
    final DataPoint[] dataPoints;
    // need to apply centroiding to profile data first
    if (spectrum.getSpectrumType() == MassSpectrumType.PROFILE) {
      double[][] centroided = ExactMassDetector.getMassValues(spectrum, 0);
      dataPoints = new DataPoint[centroided[0].length];
    } else {
      dataPoints = ScanUtils.extractDataPoints(spectrum);
    }
    return getMassValues(dataPoints, MassSpectrumType.CENTROIDED, maxSignalsInBin,
        intensityPercentile);
  }

  public static double[][] getMassValues(DataPoint[] dps, MassSpectrumType type,
      int maxSignalsInBin, double intensityPercentile) {
    // sort datapoints by intensity
    Arrays.sort(dps, DataPointSorter.DEFAULT_INTENSITY);

    int index = 0;
    var total = dps.length;
    double lowerBound = dps[0].getIntensity() * intensityPercentile;

    List<DataPoint> keep = new ArrayList<>();

    for (int i = 1; i < total; i++) {
      double intensity = dps[i].getIntensity();
      if (intensity < lowerBound) {
        // stops collecting
        if (i - index <= maxSignalsInBin) {
          for (int j = index; j < i; j++) {
            keep.add(dps[j]);
          }
        }
        // set to next upper bound
        index = i;
        lowerBound = intensity * intensityPercentile;
      }
    }

    if (total - index <= maxSignalsInBin) {
      for (int j = index; j < total; j++) {
        keep.add(dps[j]);
      }
    }

    keep.sort(DataPointSorter.DEFAULT_MZ_ASCENDING);
    return DataPointUtils.getDataPointsAsDoubleArray(keep);
  }

  @Override
  public double[][] getMassValues(double[] mzs, double[] intensities,
      final @NotNull MassSpectrumType type) {
    return getMassValues(DataPointUtils.getDataPoints(mzs, intensities), type, maxSignalsInBin,
        intensityPercentile);
  }

  @Override
  public FrequencyMassDetector create(ParameterSet parameters) {
    return new FrequencyMassDetector(parameters);
  }

  @Override
  public boolean filtersActive() {
    return true; // profile to centroid so always active
  }

  @Override
  public double[][] getMassValues(MassSpectrum spectrum) {
    return getMassValues(spectrum, maxSignalsInBin, intensityPercentile);
  }

  @Override
  public @NotNull String getName() {
    return "Frequency";
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return FrequencyMassDetectorParameters.class;
  }

}
