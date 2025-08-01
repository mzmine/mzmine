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
import io.github.mzmine.datamodel.msms.ActivationMethod;

public class BuildingImsMsMsInfo {

  private final double precursorMz;
  private final Float collisionEnergy;
  private final int precursorCharge;
  private final int fragmentFrameNumber;
  private final int firstSpectrumNumber;
  private int parentFrameNumber;
  private int lastSpectrumNumber;
  private Double lowerIsolationMz = null;
  private Double upperIsolationMz = null;
  private ActivationMethod activationMethod = null;

  public BuildingImsMsMsInfo(final double precursorMz, final Float collisionEnergy,
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

  public Float getCollisionEnergy() {
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
    // todo read mz isolation from ims-mzml and add it here. then either create pasef or dia msms info
    return new PasefMsMsInfoImpl(precursorMz, Range.closed(firstSpectrumNumber, lastSpectrumNumber),
        collisionEnergy, precursorCharge, parentFrame, thisFragmentFrame,
        lowerIsolationMz != null && upperIsolationMz != null ? Range.closed(lowerIsolationMz,
            upperIsolationMz) : null);
  }

  public Double getLowerIsolationMz() {
    return lowerIsolationMz;
  }

  public void setLowerIsolationMz(Double lowerIsolationMz) {
    this.lowerIsolationMz = lowerIsolationMz;
  }

  public Double getUpperIsolationMz() {
    return upperIsolationMz;
  }

  public void setUpperIsolationMz(Double upperIsolationMz) {
    this.upperIsolationMz = upperIsolationMz;
  }

  public ActivationMethod getActivationMethod() {
    return activationMethod;
  }

  public void setActivationMethod(ActivationMethod activationMethod) {
    this.activationMethod = activationMethod;
  }
}
