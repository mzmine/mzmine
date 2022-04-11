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
import com.google.common.collect.Streams;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.impl.AbstractMassSpectrum.DataPointIterator;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Simple implementation of MassSpectrum that stores all data in memory.
 */
public class SimpleMassSpectrum implements MassSpectrum {

  private final double[] mzValues;
  private final double[] intensityValues;
  private final MassSpectrumType spectrumType;
  private int basePeakIndex = -1;
  private Range<Double> mzRange = null;
  private Double tic = null;

  public SimpleMassSpectrum(double[] mzValues, double[] intensityValues) {
    this(mzValues, intensityValues, MassSpectrumType.CENTROIDED);
  }

  public SimpleMassSpectrum(double[] mzValues, double[] intensityValues, MassSpectrumType spectrumType) {
    this.spectrumType = spectrumType;
    assert mzValues.length == intensityValues.length;
    this.mzValues = mzValues;
    this.intensityValues = intensityValues;
  }


  @Override
  public int getNumberOfDataPoints() {
    return mzValues.length;
  }

  @Override
  public MassSpectrumType getSpectrumType() {
    return spectrumType;
  }

  @Override
  public double[] getMzValues(@NotNull double[] dst) {
    return mzValues;
  }

  @Override
  public double[] getIntensityValues(@NotNull double[] dst) {
    return intensityValues;
  }

  @Override
  public double getMzValue(int index) {
    return mzValues[index];
  }

  @Override
  public double getIntensityValue(int index) {
    return intensityValues[index];
  }

  @Nullable
  @Override
  public Double getBasePeakMz() {
    Integer i = getBasePeakIndex();
    return i!=null && i>=0 && i<getNumberOfDataPoints()? getMzValue(i) : null;
  }

  @Nullable
  @Override
  public Double getBasePeakIntensity() {
    Integer i = getBasePeakIndex();
    return i!=null && i>=0 && i<getNumberOfDataPoints()? getIntensityValue(i) : null;
  }

  @Nullable
  @Override
  public Integer getBasePeakIndex() {
  if(basePeakIndex == -1) {
    double max = 0d;
    for (int i = 0; i < getNumberOfDataPoints(); i++) {
      if (basePeakIndex == -1 || max < getIntensityValue(i)) {
        max = getIntensityValue(i);
        basePeakIndex = i;
      }
    }
  }
  return basePeakIndex != -1? basePeakIndex : null;
  }

  @Nullable
  @Override
  public Range<Double> getDataPointMZRange() {
    if(mzRange == null) {
      if(getNumberOfDataPoints()>1) {
        mzRange = Range.closed(getMzValue(0), getMzValue(getNumberOfDataPoints()-1));
      }
      else if(getNumberOfDataPoints()==1) {
        mzRange = Range.singleton(getMzValue(0));
      }
    }

    return mzRange;
  }

  @Nullable
  @Override
  public Double getTIC() {
    if(tic == null) {
      tic = Arrays.stream(intensityValues).sum();
    }

    return tic;
  }

  @Override
  public Stream<DataPoint> stream() {
    return Streams.stream(this);
  }

  @NotNull
  @Override
  public Iterator<DataPoint> iterator() {
    return new DataPointIterator(this);
  }

}
