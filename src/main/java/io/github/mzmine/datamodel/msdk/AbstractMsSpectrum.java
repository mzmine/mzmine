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

package io.github.mzmine.datamodel.msdk;

import io.github.mzmine.datamodel.MassSpectrumType;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.collect.Range;

/**
 * Simple implementation of the MsSpectrum interface
 */
public abstract class AbstractMsSpectrum implements MsSpectrum {

  private @Nonnull double mzValues[];
  private @Nonnull float intensityValues[];

  private @Nonnull Integer numOfDataPoints = 0;
  private @Nullable Range<Double> mzRange;
  private @Nonnull Float totalIonCurrent = 0f;

  private @Nonnull MassSpectrumType spectrumType = MassSpectrumType.CENTROIDED;

  /** {@inheritDoc} */
  @Override
  public @Nonnull Integer getNumberOfDataPoints() {
    return numOfDataPoints;
  }

  /** {@inheritDoc} */
  @Override
  public @Nonnull double[] getMzValues(@Nullable double[] array) {
    if ((array == null) || (array.length < numOfDataPoints))
      array = new double[numOfDataPoints];
    if (mzValues != null)
      System.arraycopy(mzValues, 0, array, 0, numOfDataPoints);
    return array;
  }

  /** {@inheritDoc} */
  @Override
  public @Nonnull float[] getIntensityValues(@Nullable float array[]) {
    if ((array == null) || (array.length < numOfDataPoints))
      array = new float[numOfDataPoints];
    if (intensityValues != null)
      System.arraycopy(intensityValues, 0, array, 0, numOfDataPoints);
    return array;
  }


  /**
   * {@inheritDoc}
   *
   * @param mzValues an array of double.
   * @param intensityValues an array of float.
   * @param size a {@link Integer} object.
   */
  public synchronized void setDataPoints(@Nonnull double mzValues[],
      @Nonnull float intensityValues[], @Nonnull Integer size) {

    Preconditions.checkNotNull(mzValues);
    Preconditions.checkNotNull(intensityValues);

    // Make sure the spectrum is sorted
    for (int i = 0; i < size - 1; i++) {
      if (mzValues[i] > mzValues[i + 1])
        throw new MSDKRuntimeException("m/z values must be sorted in ascending order");
    }

    // Make a copy of the data, instead of saving a reference to the provided array
    if ((this.mzValues == null) || (this.mzValues.length < size))
      this.mzValues = new double[size];
    System.arraycopy(mzValues, 0, this.mzValues, 0, size);

    if ((this.intensityValues == null) || (this.intensityValues.length < size))
      this.intensityValues = new float[size];
    System.arraycopy(intensityValues, 0, this.intensityValues, 0, size);

    // Save the size of the arrays
    this.numOfDataPoints = size;

    // Calculate values
    this.mzRange = MsSpectrumUtil.getMzRange(mzValues, size);
    this.totalIonCurrent = MsSpectrumUtil.getTIC(intensityValues, size);

  }

  /** {@inheritDoc} */
  @Override
  @Nonnull
  public MassSpectrumType getSpectrumType() {
    return spectrumType;
  }

  /**
   * {@inheritDoc}
   *
   * @param spectrumType a {@link MassSpectrumType} object.
   */
  public void setSpectrumType(@Nonnull MassSpectrumType spectrumType) {
    this.spectrumType = spectrumType;
  }

  /**
   * <p>
   * getTIC.
   * </p>
   *
   * @return a {@link Float} object.
   */
  @Nonnull
  public Float getTIC() {
    return totalIonCurrent;
  }

  /** {@inheritDoc} */
  @Override
  public Range<Double> getMzRange() {
    return mzRange;
  }


}
