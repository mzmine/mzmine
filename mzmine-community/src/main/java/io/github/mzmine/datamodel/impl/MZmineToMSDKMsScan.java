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

package io.github.mzmine.datamodel.impl;

import com.google.common.collect.Range;
import io.github.msdk.datamodel.ActivationInfo;
import io.github.msdk.datamodel.IsolationInfo;
import io.github.msdk.datamodel.MsScan;
import io.github.msdk.datamodel.MsSpectrumType;
import io.github.msdk.datamodel.PolarityType;
import io.github.msdk.datamodel.RawDataFile;
import io.github.msdk.datamodel.SimpleIsolationInfo;
import io.github.msdk.util.tolerances.MzTolerance;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.msms.DDAMsMsInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Simple implementation of the Scan interface.
 */
public class MZmineToMSDKMsScan implements MsScan {

  private final Scan mzmineScan;
  private final List<IsolationInfo> isolations = new ArrayList<>();

  /**
   * Clone constructor
   */
  public MZmineToMSDKMsScan(Scan mzmineScan) {
    this.mzmineScan = mzmineScan;

    int precursorCharge = -1;
    double precursorMz = 0d;

    if(mzmineScan.getMsMsInfo() instanceof DDAMsMsInfo info) {
      precursorCharge =  Objects.requireNonNullElse(info.getPrecursorCharge(), -1);
      precursorMz = info.getIsolationMz();
    }

    if (precursorMz != 0d) {
      Range<Double> isolationMzRange = Range.singleton(precursorMz);
      isolations.add(new SimpleIsolationInfo(isolationMzRange, 0f, precursorMz, precursorCharge,
          null, null));
    }
  }

  @Override
  public MsSpectrumType getSpectrumType() {
    return MsSpectrumType.valueOf(mzmineScan.getSpectrumType().name());
  }

  @Override
  public Integer getNumberOfDataPoints() {
    return mzmineScan.getNumberOfDataPoints();
  }

  @Override
  public double[] getMzValues(double[] array) {
    if (array == null || array.length < mzmineScan.getNumberOfDataPoints())
      array = new double[mzmineScan.getNumberOfDataPoints()];
    mzmineScan.getMzValues(array);
    return array;
  }

  @Override
  public float[] getIntensityValues(float[] array) {
    if (array == null || array.length < mzmineScan.getNumberOfDataPoints())
      array = new float[mzmineScan.getNumberOfDataPoints()];
    for (int i = 0; i < mzmineScan.getNumberOfDataPoints(); i++) {
      array[i] = (float) mzmineScan.getIntensityValue(i);
    }
    return array;
  }

  @Override
  public Float getTIC() {
    return mzmineScan.getTIC().floatValue();
  }

  @Override
  public Range<Double> getMzRange() {
    return mzmineScan.getDataPointMZRange();
  }

  @Override
  public MzTolerance getMzTolerance() {
    return null;
  }

  @Override
  public RawDataFile getRawDataFile() {
    return null;
  }

  @Override
  public Integer getScanNumber() {
    return mzmineScan.getScanNumber();
  }

  @Override
  public String getScanDefinition() {
    return mzmineScan.getScanDefinition();
  }

  @Override
  public String getMsFunction() {
    return "ms";
  }

  @Override
  public Integer getMsLevel() {
    return mzmineScan.getMSLevel();
  }

  @Override
  public Float getRetentionTime() {
    return (float) (mzmineScan.getRetentionTime() * 60d);
  }

  @Override
  public Range<Double> getScanningRange() {
    return mzmineScan.getScanningMZRange();
  }

  @Override
  public ActivationInfo getSourceInducedFragmentation() {
    return null;
  }

  @Override
  public List<IsolationInfo> getIsolations() {
    return isolations;
  }

  @Override
  public PolarityType getPolarity() {
    return PolarityType.valueOf(mzmineScan.getPolarity().name());
  }

  public Scan getMzmineScan() {
    return mzmineScan;
  }

}
