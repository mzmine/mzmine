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

package io.github.mzmine.datamodel.msms;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.Nullable;

/**
 * Precursor information stored in IMS MS2 frames regarding their respective sub spectra.
 *
 * @author https://github.com/SteffenHeu
 */
public interface PasefMsMsInfo extends DDAMsMsInfo {

  String XML_SPECTRUM_NUMBER_RANGE_ATTR = "spectrumnumberrange";

  public final int UNKNOWN_CHARGE = 0;
  public final double UNKNOWN_COLISSIONENERGY = -1d;

  public Frame getMsMsFrame();

  public Frame getParentFrame();

  /**
   * @return The most intense m/z of the detected precursor.
   */
  @Nullable double getIsolationMz();

  /**
   * @return The range of spectra numbers in this frame where this precursor was fragmented in.
   */
  Range<Integer> getSpectrumNumberRange();

  /**
   * @return Collision energy this precursor was fragmented at in the given range. May be null if
   * not set or 0 if unknown.
   */
  Float getActivationEnergy();

  /**
   * @return The charge of the precursor. 0 = unknown.
   */
  Integer getPrecursorCharge();

  @Nullable
  default Range<Float> getMobilityRange() {
    final Frame msMsFrame = getMsMsFrame();
    if (msMsFrame == null) {
      return null;
    }
    final Range<Integer> spectrumNumberRange = getSpectrumNumberRange();
    final double lower = msMsFrame.getMobilityForMobilityScanNumber(
        spectrumNumberRange.lowerEndpoint());
    final double upper = msMsFrame.getMobilityForMobilityScanNumber(
        spectrumNumberRange.upperEndpoint());
    return Range.closed((float) Math.min(lower, upper), (float) Math.max(lower, upper));
  }

  void writeToXML(XMLStreamWriter writer) throws XMLStreamException;
}
