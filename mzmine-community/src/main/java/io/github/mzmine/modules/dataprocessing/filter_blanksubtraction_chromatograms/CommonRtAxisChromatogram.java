/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.filter_blanksubtraction_chromatograms;

import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public record CommonRtAxisChromatogram(double mz, float minRT, float maxRT, float rtStep,
                                       double[] intensities) {

  private static final Logger logger = Logger.getLogger(CommonRtAxisChromatogram.class.getName());

  public static CommonRtAxisChromatogram create(final double mz, final int bins, final float minRT,
      final float maxRT) {
    float rtStep = (maxRT - minRT) / bins;
    return new CommonRtAxisChromatogram(mz, minRT, maxRT, rtStep, new double[bins]);
  }

  public int size() {
    return intensities.length;
  }

  public float getRT(int index) {
    return minRT + rtStep * index;
  }

  public double getIntensity(int index) {
    return intensities[index];
  }

  public int indexOf(float rt, @NotNull DefaultTo closest) {
    float index = (rt - minRT) / rtStep;
    return closest.decideFor(index, 0, size());
  }

  public int closestIndexOf(float rt) {
    int index = Math.round((rt - minRT) / rtStep);
    return MathUtils.withinBounds(index, 0, size());
  }

  /**
   * Copy with new mz and empty intensities
   */
  public CommonRtAxisChromatogram withMzAndEmpty(double mz) {
    return new CommonRtAxisChromatogram(mz, minRT, maxRT, rtStep, new double[size()]);
  }

  /**
   * set the intensity at rt to the maximum of old and new value
   */
  public void setClosestMaxIntensity(final float rt, final double intensity) {
    var index = closestIndexOf(rt);
    intensities[index] = Math.max(intensities[index], intensity);
  }

  /**
   * @return max intensity in index range
   */
  public double getMaxIntensity(final int minIndex, final int maxIndexInclusive) {
    assert maxIndexInclusive >= minIndex;
    double max = 0;
    for (int i = Math.max(minIndex, 0); i <= Math.min(maxIndexInclusive, size() - 1); i++) {
      max = Math.max(max, intensities[i]);
    }
    return max;
  }
}
