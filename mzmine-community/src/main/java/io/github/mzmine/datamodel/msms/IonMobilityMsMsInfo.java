/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.datamodel.msms;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import org.jetbrains.annotations.Nullable;

public interface IonMobilityMsMsInfo extends MsMsInfo {

  String XML_SPECTRUM_NUMBER_RANGE_ATTR = "spectrumnumberrange";

  /**
   * @return The range of spectra numbers in this frame where this precursor was fragmented in.
   */
  Range<Integer> getSpectrumNumberRange();

  @Nullable
  default Range<Float> getMobilityRange() {
    final Frame msMsFrame = getMsMsFrame();
    if (msMsFrame == null) {
      return null;
    }
    final Range<Integer> spectrumNumberRange = getSpectrumNumberRange();
    if (spectrumNumberRange == null) {
      return null;
    }
    final double lower = msMsFrame.getMobilityForMobilityScanNumber(
        spectrumNumberRange.lowerEndpoint());
    final double upper = msMsFrame.getMobilityForMobilityScanNumber(
        spectrumNumberRange.upperEndpoint());
    return Range.closed((float) Math.min(lower, upper), (float) Math.max(lower, upper));
  }

  Frame getMsMsFrame();

  @Override
  IonMobilityMsMsInfo createCopy();
}
