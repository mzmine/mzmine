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

package io.github.mzmine.modules.dataprocessing.featdet_shoulderpeaksfilter.peakmodels;

import com.google.common.collect.Range;

import io.github.mzmine.modules.dataprocessing.featdet_shoulderpeaksfilter.PeakModel;

/**
 * 
 * This class represents a Gaussian model, using the formula:
 * 
 * f(x) = a * e ^ ((x-b)^2 / (-2 * c^2))
 * 
 * where
 * 
 * a... height of the model (intensityMain) b... center of the model (mzMain) c... FWHM / (2 *
 * sqrt(2 . ln(2))) FWHM... Full Width at Half Maximum
 *
 */
public class GaussPeak implements PeakModel {

  private double mzMain, intensityMain, FWHM, partC, part2C2;

  /**
   * @see io.github.mzmine.modules.masslistmethods.shoulderpeaksfilter.peakpicking.twostep.massdetection.exactmass.peakmodel.PeakModel#setParameters(double,
   *      double, double)
   */
  public void setParameters(double mzMain, double intensityMain, double resolution) {

    this.mzMain = mzMain;
    this.intensityMain = intensityMain;

    // FWFM (Full Width at Half Maximum)
    FWHM = (mzMain / resolution);
    partC = FWHM / 2.354820045f;
    part2C2 = 2f * (double) Math.pow(partC, 2);
  }

  /**
   * @see io.github.mzmine.modules.masslistmethods.shoulderpeaksfilter.peakpicking.twostep.peakmodel.PeakModel#getBasePeakWidth()
   */
  public Range<Double> getWidth(double partialIntensity) {

    // The height value must be bigger than zero.
    if (partialIntensity <= 0)
      return Range.atLeast(0.0);

    // Using the Gaussian function we calculate the peak width at intensity
    // given (partialIntensity)

    double portion = partialIntensity / intensityMain;
    double ln = -1 * (double) Math.log(portion);

    double sideRange = (double) (Math.sqrt(part2C2 * ln));

    // This range represents the width of our peak in m/z
    Range<Double> rangePeak = Range.closed(mzMain - sideRange, mzMain + sideRange);

    return rangePeak;
  }

  /**
   * @see io.github.mzmine.modules.masslistmethods.shoulderpeaksfilter.peakpicking.twostep.peakmodel.PeakModel#getIntensity(double)
   */
  public double getIntensity(double mz) {

    // Using the Gaussian function we calculate the intensity at given m/z
    double diff2 = (double) Math.pow(mz - mzMain, 2);
    double exponent = -1 * (diff2 / part2C2);
    double eX = (double) Math.exp(exponent);
    double intensity = intensityMain * eX;
    return intensity;
  }

}
