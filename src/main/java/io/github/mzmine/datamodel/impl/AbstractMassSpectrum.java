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
import java.nio.DoubleBuffer;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A basic implementation of the MassSpectrum interface.
 */
public abstract class AbstractMassSpectrum implements MassSpectrum {

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  protected @Nullable Range<Double> mzRange;
  protected @Nullable Integer basePeakIndex = null;
  protected double totalIonCurrent = 0.0;
  private MassSpectrumType spectrumType = MassSpectrumType.CENTROIDED;


  protected synchronized void updateMzRangeAndTICValues() {

    final DoubleBuffer mzValues = getMzValues();
    final DoubleBuffer intensityValues = getIntensityValues();

    assert mzValues != null;
    assert intensityValues != null;
    assert mzValues.capacity() == intensityValues.capacity();

    totalIonCurrent = 0.0;

    if (mzValues.capacity() == 0) {
      mzRange = null;
      basePeakIndex = null;
      return;
    }

    totalIonCurrent = 0.0;
    basePeakIndex = 0;
    mzRange = Range.closed(mzValues.get(0), mzValues.get(mzValues.capacity() - 1));

    for (int i = 0; i < mzValues.capacity() - 1; i++) {

      // Check the order of the m/z values
      if ((i < mzValues.capacity() - 1) && (mzValues.get(i) > mzValues.get(i + 1))) {
        throw new IllegalArgumentException("The m/z values must be sorted in ascending order");
      }

      // Update base peak index
      if (intensityValues.get(i) > intensityValues.get(basePeakIndex)) {
        basePeakIndex = i;
      }

      // Update TIC
      totalIonCurrent += intensityValues.get(i);
    }

    totalIonCurrent += intensityValues.get(intensityValues.capacity() - 1);
  }


  /**
   * @see io.github.mzmine.datamodel.Scan#getNumberOfDataPoints()
   */
  @Override
  public int getNumberOfDataPoints() {
    return getMzValues().capacity();
  }

  /**
   * @see io.github.mzmine.datamodel.Scan#
   */
  @Override
  @Nullable
  public Range<Double> getDataPointMZRange() {
    return mzRange;
  }

  /**
   */
  @Override
  public @Nullable Integer getBasePeakIndex() {
    return basePeakIndex;
  }

  @Override
  public @NotNull Double getTIC() {
    return totalIonCurrent;
  }

  /**
   * @see io.github.mzmine.datamodel.Scan#getSpectrumType()
   */
  @Override
  public MassSpectrumType getSpectrumType() {
    return spectrumType;
  }

  public void setSpectrumType(MassSpectrumType spectrumType) {
    this.spectrumType = spectrumType;
  }

  @Override
  public double getMzValue(int index) {
    return getMzValues().get(index);
  }

  @Override
  public double getIntensityValue(int index) {
    return getIntensityValues().get(index);
  }

  @Override
  @Nullable
  public Double getBasePeakMz() {
    if (basePeakIndex == null)
      return null;
    else
      return getMzValues().get(basePeakIndex);
  }

  @Override
  @Nullable
  public Double getBasePeakIntensity() {
    if (basePeakIndex == null) {
      return null;
    } else {
      return getIntensityValues().get(basePeakIndex);
    }
  }

  abstract DoubleBuffer getMzValues();

  abstract DoubleBuffer getIntensityValues();

  @Override
  public Iterator<DataPoint> iterator() {
    return new DataPointIterator(this);
  }

  @Override
  public Stream<DataPoint> stream() {
    return Streams.stream(this);
  }

  public static class DataPointIterator implements Iterator<DataPoint>, DataPoint {

    // We start at -1 so the first call to next() moves us to index 0
    private int cursor = -1;
    private final MassSpectrum spectrum;

    DataPointIterator(MassSpectrum spectrum) {
      this.spectrum = spectrum;
    }

    @Override
    public boolean hasNext() {
      return (cursor + 1) < spectrum.getNumberOfDataPoints();
    }

    @Override
    public DataPoint next() {
      cursor++;
      return this;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

    @Override
    public double getMZ() {
      return spectrum.getMzValue(cursor);
    }

    @Override
    public double getIntensity() {
      return spectrum.getIntensityValue(cursor);
    }

  }
}
