/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
