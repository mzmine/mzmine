/*
 * (C) Copyright 2015-2017 by MSDK Development Team
 *
 * This software is dual-licensed under either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1 as published by the Free
 * Software Foundation
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by the Eclipse Foundation.
 */

package io.github.msdk.datamodel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.collect.Range;

import io.github.msdk.MSDKRuntimeException;
import io.github.msdk.util.MsSpectrumUtil;
import io.github.msdk.util.tolerances.MzTolerance;

/**
 * Simple implementation of the MsSpectrum interface
 */
public abstract class AbstractMsSpectrum implements MsSpectrum {

  private @Nonnull double mzValues[];
  private @Nonnull float intensityValues[];

  private @Nonnull Integer numOfDataPoints = 0;
  private @Nullable Range<Double> mzRange;
  private @Nonnull Float totalIonCurrent = 0f;

  private @Nonnull MsSpectrumType spectrumType = MsSpectrumType.CENTROIDED;
  private @Nullable MzTolerance mzTolerance;

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
  public MsSpectrumType getSpectrumType() {
    return spectrumType;
  }

  /**
   * {@inheritDoc}
   *
   * @param spectrumType a {@link MsSpectrumType} object.
   */
  public void setSpectrumType(@Nonnull MsSpectrumType spectrumType) {
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

  /** {@inheritDoc} */
  @Override
  public MzTolerance getMzTolerance() {
    return mzTolerance;
  }

  /**
   * <p>
   * Setter for the field <code>mzTolerance</code>.
   * </p>
   *
   * @param mzTolerance a {@link MzTolerance} object.
   */
  public void setMzTolerance(MzTolerance mzTolerance) {
    this.mzTolerance = mzTolerance;
  }

}
