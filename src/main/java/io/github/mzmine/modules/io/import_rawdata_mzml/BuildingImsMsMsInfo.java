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

package io.github.mzmine.modules.io.import_rawdata_mzml;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.impl.PasefMsMsInfoImpl;

public class BuildingImsMsMsInfo {

  private final double precursorMz;
  private final float collisionEnergy;
  private final int precursorCharge;
  private final int fragmentFrameNumber;
  private final int firstSpectrumNumber;
  private int parentFrameNumber;
  private int lastSpectrumNumber;

  public BuildingImsMsMsInfo(final double precursorMz, final float collisionEnergy,
      final int precursorCharge, final int fragmentFrameNumber, final int firstSpectrumNumber) {
    this.precursorMz = precursorMz;
    this.collisionEnergy = collisionEnergy;
    this.precursorCharge = precursorCharge;
    this.firstSpectrumNumber = firstSpectrumNumber;
    this.fragmentFrameNumber = fragmentFrameNumber;
    this.lastSpectrumNumber = firstSpectrumNumber;
    parentFrameNumber = 0;
  }

  public double getPrecursorMz() {
    return precursorMz;
  }

  public int getFragmentFrameNumber() {
    return fragmentFrameNumber;
  }

  public int getFirstSpectrumNumber() {
    return firstSpectrumNumber;
  }

  public int getLastSpectrumNumber() {
    return lastSpectrumNumber;
  }

  public void setLastSpectrumNumber(int lastSpectrumNumber) {
    this.lastSpectrumNumber = lastSpectrumNumber;
  }

  public double getLargestPeakMz() {
    return precursorMz;
  }

  public Range<Integer> getSpectrumNumberRange() {
    throw new UnsupportedOperationException();
  }

  public float getCollisionEnergy() {
    return collisionEnergy;
  }

  public int getPrecursorCharge() {
    return precursorCharge;
  }

  public int getParentFrameNumber() {
    return parentFrameNumber;
  }

  public int getFrameNumber() {
    return fragmentFrameNumber;
  }

  public PasefMsMsInfoImpl build(Frame parentFrame, Frame thisFragmentFrame) {
    return new PasefMsMsInfoImpl(precursorMz, Range.closed(firstSpectrumNumber, lastSpectrumNumber),
        collisionEnergy, precursorCharge, parentFrame, thisFragmentFrame, null);
  }
}
