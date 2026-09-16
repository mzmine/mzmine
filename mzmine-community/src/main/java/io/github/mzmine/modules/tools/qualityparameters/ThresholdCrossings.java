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

package io.github.mzmine.modules.tools.qualityparameters;

/**
 * The x values (retention time or mobility) at which a peak crosses a given intensity threshold.
 * The crossings are searched from the outer edges of the peak inwards towards the apex, see
 * {@link QualityParameters#findThresholdCrossings(double, double[], double[])}.
 * <p>
 * A side that never drops below the threshold within the data has no crossing. The edge and
 * extrapolated values are provided so that callers can apply their own fallback for such sides.
 *
 * @param apexX              x value of the most intense data point
 * @param leftX              crossing left of the apex, NaN if the peak never drops below the
 *                           threshold on the left
 * @param rightX             crossing right of the apex, NaN if the peak never drops below the
 *                           threshold on the right
 * @param leftEdgeX          x value of the first data point, i.e. the width that was actually
 *                           observed on the left
 * @param rightEdgeX         x value of the last data point, i.e. the width that was actually
 *                           observed on the right
 * @param leftExtrapolatedX  linear extrapolation of the two leftmost data points outwards down to
 *                           the threshold, NaN if that segment does not rise towards the apex
 * @param rightExtrapolatedX same as {@code leftExtrapolatedX} for the two rightmost data points
 */
public record ThresholdCrossings(double apexX, double leftX, double rightX, double leftEdgeX,
                                 double rightEdgeX, double leftExtrapolatedX,
                                 double rightExtrapolatedX) {

  public boolean hasLeftCrossing() {
    return !Double.isNaN(leftX);
  }

  public boolean hasRightCrossing() {
    return !Double.isNaN(rightX);
  }
}
