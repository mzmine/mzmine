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
 * This class represents a Lorentzian function model, using the formula:
 * 
 * f(x) = a / (1 + ((x-b)^2 / (HWHM^2)))
 * 
 * where
 * 
 * a... height of the model (intensityMain) b... center of the model (mzMain) HWHM... Half Width at
 * Half Maximum
 * 
 */

public class LorentzianPeak implements PeakModel {

  private double mzMain, intensityMain, squareHWHM;

  /**
   * @see io.github.mzmine.modules.masslistmethods.shoulderpeaksfilter.peakpicking.twostep.massdetection.exactmass.peakmodel.PeakModel#setParameters(double,
   *      double, double)
   */
  public void setParameters(double mzMain, double intensityMain, double resolution) {

    this.mzMain = mzMain;
    this.intensityMain = intensityMain;

    // HWFM (Half Width at Half Maximum) ^ 2
    squareHWHM = (double) Math.pow((mzMain / resolution) / 2, 2);
  }

  /**
   * @see io.github.mzmine.modules.masslistmethods.shoulderpeaksfilter.peakpicking.twostep.peakmodel.PeakModel#getBasePeakWidth()
   */
  public Range<Double> getWidth(double partialIntensity) {

    // The height value must be bigger than zero.
    if (partialIntensity <= 0)
      return Range.atLeast(0.0);

    // Using the Lorentzian function we calculate the peak width
    double squareX = ((intensityMain / partialIntensity) - 1) * squareHWHM;

    double sideRange = (double) Math.sqrt(squareX);

    // This range represents the width of our peak in m/z terms
    Range<Double> rangePeak = Range.closed(mzMain - sideRange, mzMain + sideRange);

    return rangePeak;
  }

  /**
   * @see io.github.mzmine.modules.masslistmethods.shoulderpeaksfilter.peakpicking.twostep.peakmodel.PeakModel#getIntensity(double)
   */
  public double getIntensity(double mz) {

    // Using the Lorentzian function we calculate the intensity at given
    // m/z
    double squareX = (double) Math.pow((mz - mzMain), 2);
    double ratio = squareX / squareHWHM;
    double intensity = intensityMain / (1 + ratio);
    return intensity;
  }

}
