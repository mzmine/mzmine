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

import org.jetbrains.annotations.Nullable;

/**
 * The x values (retention time or mobility) at which a peak crosses the intensity thresholds of all
 * peak shape quality parameters. All of them are collected in a single pass over the peak, see
 * {@link QualityParameters#findThresholdCrossings(float[], double[])}.
 * <p>
 * The crossings are searched from the outer edges of the peak inwards towards the apex. A side that
 * never drops below a threshold within the data has no crossing at that threshold. The edge and
 * extrapolated values are provided so that callers can apply their own fallback for such sides.
 * <p>
 * decision: a crossing that does not exist is null rather than NaN. A primitive field is therefore
 * one that always has a value, and a missing one cannot silently travel through arithmetic.
 *
 * @param apexX                x value of the most intense data point
 * @param firstX               x value of the first data point, i.e. the width that was actually
 *                             observed on the left
 * @param lastX                x value of the last data point, i.e. the width that was actually
 *                             observed on the right
 * @param leftX50              crossing of 50 % of the apex intensity left of the apex, null if the
 *                             peak never drops that low on the left
 * @param rightX50             same as {@code leftX50} right of the apex
 * @param leftX10              crossing of 10 % of the apex intensity left of the apex, null if the
 *                             peak never drops that low on the left
 * @param rightX10             same as {@code leftX10} right of the apex
 * @param leftX5               crossing of 5 % of the apex intensity left of the apex, null if the
 *                             peak never drops that low on the left
 * @param rightX5              same as {@code leftX5} right of the apex
 * @param leftExtrapolatedX50  estimated crossing of 50 % of the apex intensity on the left flank if
 *                             the peak were not cut off, from a least squares fit over that flank
 *                             and capped at a multiple of the observed half width, null if the
 *                             flank is too flat or does not rise towards the apex. Only the FWHM
 *                             extrapolates, so the lower thresholds carry no such estimate.
 * @param rightExtrapolatedX50 same as {@code leftExtrapolatedX50} for the right flank
 */
public record ThresholdCrossings(float apexX, float firstX, float lastX, @Nullable Float leftX50,
                                 @Nullable Float rightX50, @Nullable Float leftX10,
                                 @Nullable Float rightX10, @Nullable Float leftX5,
                                 @Nullable Float rightX5, @Nullable Float leftExtrapolatedX50,
                                 @Nullable Float rightExtrapolatedX50) {

}
