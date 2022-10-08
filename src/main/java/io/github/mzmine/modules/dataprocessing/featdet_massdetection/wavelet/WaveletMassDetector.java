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

package io.github.mzmine.modules.dataprocessing.featdet_massdetection.wavelet;

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

/**
 * This class implements the Continuous Wavelet Transform (CWT), Mexican Hat, over raw datapoints of
 * a certain spectrum. After get the spectrum in the wavelet's time domain, we use the local maxima
 * to detect possible peaks in the original raw datapoints.
 */
public class WaveletMassDetector implements MassDetector {

  /**
   * Parameters of the wavelet, NPOINTS is the number of wavelet values to use The WAVELET_ESL &
   * WAVELET_ESL indicates the Effective Support boundaries
   */
  private static final double NPOINTS = 60000;
  private static final int WAVELET_ESL = -5;
  private static final int WAVELET_ESR = 5;

  @Override
  public double[][] getMassValues(MassSpectrum scan, ParameterSet parameters) {

    double noiseLevel = parameters.getParameter(WaveletMassDetectorParameters.noiseLevel)
        .getValue();
    int scaleLevel = parameters.getParameter(WaveletMassDetectorParameters.scaleLevel).getValue();
    double waveletWindow = parameters.getParameter(WaveletMassDetectorParameters.waveletWindow)
        .getValue();

    DataPoint waveletDataPoints[] = performCWT(scan, waveletWindow, scaleLevel);

    DataPoint detected[] = getMzPeaks(noiseLevel, scan, waveletDataPoints);

    // convert to double[][] TODO remove use of DataPoint
    int size = detected.length;
    double[] mzs = new double[size];
    double[] intensities = new double[size];
    for (int i = 0; i < size; i++) {
      mzs[i] = detected[i].getMZ();
      intensities[i] = detected[i].getIntensity();
    }
    return new double[][]{mzs, intensities};
  }

  /**
   * Perform the CWT over raw data points in the selected scale level
   *
   * @param scan
   */
  private SimpleDataPoint[] performCWT(MassSpectrum scan, double waveletWindow, int scaleLevel) {
    int length = scan.getNumberOfDataPoints();
    SimpleDataPoint[] cwtDataPoints = new SimpleDataPoint[length];
    double wstep = ((WAVELET_ESR - WAVELET_ESL) / NPOINTS);
    double[] W = new double[(int) NPOINTS];

    double waveletIndex = WAVELET_ESL;
    for (int j = 0; j < NPOINTS; j++) {
      // Pre calculate the values of the wavelet
      W[j] = cwtMEXHATreal(waveletIndex, waveletWindow, 0.0);
      waveletIndex += wstep;
    }

    /*
     * We only perform Translation of the wavelet in the selected scale
     */
    int d = (int) NPOINTS / (WAVELET_ESR - WAVELET_ESL);
    int a_esl = scaleLevel * WAVELET_ESL;
    int a_esr = scaleLevel * WAVELET_ESR;
    double sqrtScaleLevel = Math.sqrt(scaleLevel);
    for (int dx = 0; dx < length; dx++) {

      /* Compute wavelet boundaries */
      int t1 = a_esl + dx;
      if (t1 < 0) {
        t1 = 0;
      }
      int t2 = a_esr + dx;
      if (t2 >= length) {
        t2 = (length - 1);
      }

      /* Perform convolution */
      double intensity = 0.0;
      for (int i = t1; i <= t2; i++) {
        int ind = (int) (NPOINTS / 2) - ((d * (i - dx) / scaleLevel) * (-1));
        if (ind < 0) {
          ind = 0;
        }
        if (ind >= NPOINTS) {
          ind = (int) NPOINTS - 1;
        }
        intensity += scan.getIntensityValue(i) * W[ind];
      }
      intensity /= sqrtScaleLevel;
      // Eliminate the negative part of the wavelet map
      if (intensity < 0) {
        intensity = 0;
      }
      cwtDataPoints[dx] = new SimpleDataPoint(scan.getMzValue(dx), intensity);
    }

    return cwtDataPoints;
  }

  /**
   * This function calculates the wavelets's coefficients in Time domain
   *
   * @param x Step of the wavelet
   * @param a Window Width of the wavelet
   * @param b Offset from the center of the peak
   */
  private double cwtMEXHATreal(double x, double a, double b) {
    /* c = 2 / ( sqrt(3) * pi^(1/4) ) */
    double c = 0.8673250705840776;
    double TINY = 1E-200;
    double x2;

    if (a == 0.0) {
      a = TINY;
    }
    x = (x - b) / a;
    x2 = x * x;
    return c * (1.0 - x2) * Math.exp(-x2 / 2);
  }

  /**
   * This function searches for maximums from wavelet data points
   */
  private DataPoint[] getMzPeaks(double noiseLevel, MassSpectrum scan,
      DataPoint[] waveletDataPoints) {

    TreeSet<DataPoint> mzPeaks = new TreeSet<>(
        new DataPointSorter(SortingProperty.MZ, SortingDirection.Ascending));

    List<DataPoint> rawDataPoints = new ArrayList<>();
    int peakMaxInd = 0;
    int stopInd = waveletDataPoints.length - 1;

    for (int ind = 0; ind <= stopInd; ind++) {

      while ((ind <= stopInd) && (waveletDataPoints[ind].getIntensity() == 0)) {
        ind++;
      }
      peakMaxInd = ind;
      if (ind >= stopInd) {
        break;
      }

      // While peak is on
      while ((ind <= stopInd) && (waveletDataPoints[ind].getIntensity() > 0)) {
        // Check if this is the maximum point of the peak
        if (waveletDataPoints[ind].getIntensity() > waveletDataPoints[peakMaxInd].getIntensity()) {
          peakMaxInd = ind;
        }
        rawDataPoints.add(new SimpleDataPoint(scan.getMzValue(ind), scan.getIntensityValue(ind)));
        ind++;
      }

      if (ind >= stopInd) {
        break;
      }

      rawDataPoints.add(new SimpleDataPoint(scan.getMzValue(ind), scan.getIntensityValue(ind)));

      if (scan.getIntensityValue(peakMaxInd) > noiseLevel) {
        SimpleDataPoint peakDataPoint = new SimpleDataPoint(scan.getMzValue(peakMaxInd),
            calcAproxIntensity(rawDataPoints));

        mzPeaks.add(peakDataPoint);

      }
      rawDataPoints.clear();
    }

    return mzPeaks.toArray(new DataPoint[0]);

  }

  private double calcAproxIntensity(List<DataPoint> rawDataPoints) {

    double aproxIntensity = 0;

    for (DataPoint d : rawDataPoints) {
      if (d.getIntensity() > aproxIntensity) {
        aproxIntensity = d.getIntensity();
      }
    }
    return aproxIntensity;
  }

  @Override
  public @NotNull String getName() {
    return "Wavelet transform";
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return WaveletMassDetectorParameters.class;
  }

}
