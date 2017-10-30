/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.datamodel.impl;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Range;

import io.github.msdk.datamodel.ActivationInfo;
import io.github.msdk.datamodel.IsolationInfo;
import io.github.msdk.datamodel.MsScan;
import io.github.msdk.datamodel.MsSpectrumType;
import io.github.msdk.datamodel.PolarityType;
import io.github.msdk.datamodel.RawDataFile;
import io.github.msdk.datamodel.SimpleIsolationInfo;
import io.github.msdk.util.tolerances.MzTolerance;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Scan;

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
    if (mzmineScan.getPrecursorMZ() != 0) {
      Range<Double> isolationMzRange = Range.singleton(mzmineScan.getPrecursorMZ());
      double precursorMz = mzmineScan.getPrecursorMZ();
      int precursorCharge = mzmineScan.getPrecursorCharge();
      ActivationInfo activationInfo = null;
      isolations.add(new SimpleIsolationInfo(isolationMzRange, 0f, precursorMz, precursorCharge,
          activationInfo));
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
    DataPoint dp[] = mzmineScan.getDataPoints();
    for (int i = 0; i < mzmineScan.getNumberOfDataPoints(); i++) {
      array[i] = dp[i].getMZ();
    }
    return array;
  }

  @Override
  public float[] getIntensityValues(float[] array) {
    if (array == null || array.length < mzmineScan.getNumberOfDataPoints())
      array = new float[mzmineScan.getNumberOfDataPoints()];
    DataPoint dp[] = mzmineScan.getDataPoints();
    for (int i = 0; i < mzmineScan.getNumberOfDataPoints(); i++) {
      array[i] = (float) dp[i].getIntensity();
    }
    return array;
  }

  @Override
  public Float getTIC() {
    return (float) mzmineScan.getTIC();
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
