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
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.impl.AbstractMassSpectrum.DataPointIterator;
import java.util.Arrays;
import java.util.Iterator;
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

  public SimpleMassSpectrum(double[] mzValues, double[] intensityValues,
      MassSpectrumType spectrumType) {
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
    return i != null && i >= 0 && i < getNumberOfDataPoints() ? getMzValue(i) : null;
  }

  @Nullable
  @Override
  public Double getBasePeakIntensity() {
    Integer i = getBasePeakIndex();
    return i != null && i >= 0 && i < getNumberOfDataPoints() ? getIntensityValue(i) : null;
  }

  @Nullable
  @Override
  public Integer getBasePeakIndex() {
    if (basePeakIndex == -1) {
      double max = 0d;
      for (int i = 0; i < getNumberOfDataPoints(); i++) {
        if (basePeakIndex == -1 || max < getIntensityValue(i)) {
          max = getIntensityValue(i);
          basePeakIndex = i;
        }
      }
    }
    return basePeakIndex != -1 ? basePeakIndex : null;
  }

  @Nullable
  @Override
  public Range<Double> getDataPointMZRange() {
    if (mzRange == null) {
      if (getNumberOfDataPoints() > 1) {
        mzRange = Range.closed(getMzValue(0), getMzValue(getNumberOfDataPoints() - 1));
      } else if (getNumberOfDataPoints() == 1) {
        mzRange = Range.singleton(getMzValue(0));
      }
    }

    return mzRange;
  }

  @Nullable
  @Override
  public Double getTIC() {
    if (tic == null) {
      tic = Arrays.stream(intensityValues).sum();
    }

    return tic;
  }

  @NotNull
  @Override
  public Iterator<DataPoint> iterator() {
    return new DataPointIterator(this);
  }

}
