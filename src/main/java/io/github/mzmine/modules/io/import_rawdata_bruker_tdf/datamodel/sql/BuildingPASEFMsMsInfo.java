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

package io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql;

import com.google.common.collect.Range;

public class BuildingPASEFMsMsInfo {

  private final double precursorMz;
  private final Range<Integer> spectrumNumberRange;
  private final float collisionEnergy;
  private final Integer precursorCharge;
  private final Integer parentFrameNumber;
  private final Integer fragmentFrameNumber;
  private final double isolationWidth;

  public BuildingPASEFMsMsInfo(double precursorMz, Range<Integer> spectrumNumberRange,
      float collisionEnergy, Integer precursorCharge, Integer parentFrameNumber, Integer fragmentFrameNumber,
      double isolationWidth) {
    this.precursorMz = precursorMz;
    this.spectrumNumberRange = spectrumNumberRange;
    this.collisionEnergy = collisionEnergy;
    this.precursorCharge = precursorCharge;
    this.parentFrameNumber = parentFrameNumber;
    this.fragmentFrameNumber = fragmentFrameNumber;
    this.isolationWidth = isolationWidth;
  }

  public double getLargestPeakMz() {
    return precursorMz;
  }

  public Range<Integer> getSpectrumNumberRange() {
    return spectrumNumberRange;
  }

  public float getCollisionEnergy() {
    return collisionEnergy;
  }

  public Integer getPrecursorCharge() {
    return precursorCharge;
  }

  public Integer getParentFrameNumber() {
    return parentFrameNumber;
  }

  public Integer getFrameNumber() {
    return fragmentFrameNumber;
  }

  public Range<Double> getIsolationWindow() {
    return Range.closed(precursorMz - isolationWidth / 2, precursorMz + isolationWidth / 2);
  }
}
