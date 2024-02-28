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
 * This class is using two Lorentzian peak models. One is used to model the actual peak, and one is
 * used to model a peak with 5% of the intensity of the main peak, and 5% of its resolution. This
 * broader and smaller peak should cover side regions, for example FTMS shoulder peaks.
 * 
 */
public class ExtendedLorentzianPeak implements PeakModel {

  /**
   * This constant defines at what percentage of the intensity we set as the border between the main
   * and the broad (shoulder) peak models. Default is 5%.
   */
  public static final double shoulderIntensityRatio = 0.05;

  /**
   * This constant defines what percentage of the resolution shall we use to build the broad
   * (shoulder) peak model. Default is 5%.
   */
  public static final double shoulderResolutionRatio = 0.05;

  private LorentzianPeak mainPeak, shoulderPeak;
  private Range<Double> mainPeakRange;
  private double shoulderIntensity;

  public ExtendedLorentzianPeak() {
    mainPeak = new LorentzianPeak();
    shoulderPeak = new LorentzianPeak();
  }

  /**
   * @see io.github.mzmine.modules.masslistmethods.shoulderpeaksfilter.peakpicking.twostep.massdetection.exactmass.peakmodel.PeakModel#setParameters(double,
   *      double, double)
   */
  public void setParameters(double mzMain, double intensityMain, double resolution) {

    mainPeak.setParameters(mzMain, intensityMain, resolution);
    shoulderPeak.setParameters(mzMain, intensityMain * shoulderIntensityRatio,
        resolution * shoulderResolutionRatio);

    this.shoulderIntensity = intensityMain * shoulderIntensityRatio;
    this.mainPeakRange = mainPeak.getWidth(shoulderIntensity);

  }

  /**
   * @see io.github.mzmine.modules.masslistmethods.shoulderpeaksfilter.peakpicking.twostep.peakmodel.PeakModel#getBasePeakWidth()
   */
  public Range<Double> getWidth(double partialIntensity) {

    // The height value must be bigger than zero.
    if (partialIntensity <= 0)
      return Range.atLeast(0.0);

    if (partialIntensity < shoulderIntensity)
      return shoulderPeak.getWidth(partialIntensity);
    else
      return mainPeak.getWidth(partialIntensity);

  }

  /**
   * @see io.github.mzmine.modules.masslistmethods.shoulderpeaksfilter.peakpicking.twostep.peakmodel.PeakModel#getIntensity(double)
   */
  public double getIntensity(double mz) {

    if (mainPeakRange.contains(mz))
      return mainPeak.getIntensity(mz);
    else
      return shoulderPeak.getIntensity(mz);

  }

}
