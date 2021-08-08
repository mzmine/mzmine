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

import io.github.mzmine.datamodel.Chromatogram;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.ChromatogramType;
import io.github.mzmine.util.ChromatogramUtils;
import io.github.mzmine.datamodel.IsolationInfo;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.collect.Range;

/**
 * Simple implementation of the Chromatogram interface.
 */
public class SimpleChromatogram implements Chromatogram {

  private @Nullable RawDataFile dataFile;
  private @NotNull Integer chromatogramNumber, numOfDataPoints = 0;
  private @NotNull ChromatogramType chromatogramType;
  private @Nullable Double mz;
  private @NotNull float rtValues[];
  private @Nullable double mzValues[];
  private @NotNull float intensityValues[];
  private @NotNull Range<Float> rtRange;

  private final @NotNull List<IsolationInfo> isolations = new ArrayList<>();

  /** {@inheritDoc} */
  @Override
  @Nullable
  public RawDataFile getRawDataFile() {
    return dataFile;
  }

  /**
   * {@inheritDoc}
   *
   * @param newRawDataFile a {@link RawDataFile} object.
   */
  public void setRawDataFile(@NotNull RawDataFile newRawDataFile) {
    this.dataFile = newRawDataFile;
  }

  /** {@inheritDoc} */
  @Override
  @NotNull
  public Integer getChromatogramNumber() {
    return chromatogramNumber;
  }

  /**
   * {@inheritDoc}
   *
   * @param chromatogramNumber a {@link Integer} object.
   */
  public void setChromatogramNumber(@NotNull Integer chromatogramNumber) {
    Preconditions.checkNotNull(chromatogramNumber);
    this.chromatogramNumber = chromatogramNumber;
  }

  /** {@inheritDoc} */
  @Override
  @NotNull
  public ChromatogramType getChromatogramType() {
    return chromatogramType;
  }

  /**
   * {@inheritDoc}
   *
   * @param newChromatogramType a {@link ChromatogramType}
   *        object.
   */
  public void setChromatogramType(@NotNull ChromatogramType newChromatogramType) {
    this.chromatogramType = newChromatogramType;
  }

  /** {@inheritDoc} */
  @Override
  public @NotNull Integer getNumberOfDataPoints() {
    return numOfDataPoints;
  }

  /** {@inheritDoc} */
  @Override
  public @NotNull float[] getRetentionTimes() {
    return getRetentionTimes(null);
  }

  /**
   * {@inheritDoc}
   *
   * @param array an array of float.
   * @return an array of float.
   */
  public @NotNull float[] getRetentionTimes(@Nullable float[] array) {
    if ((array == null) || (array.length < numOfDataPoints))
      array = new float[numOfDataPoints];
    if (rtValues != null)
      System.arraycopy(rtValues, 0, array, 0, numOfDataPoints);
    return array;
  }

  /** {@inheritDoc} */
  @Override
  public @NotNull double[] getMzValues() {
    return getMzValues(null);
  }

  /**
   * {@inheritDoc}
   *
   * @param array an array of float.
   * @return an array of float.
   */
  public @NotNull double[] getMzValues(@Nullable double[] array) {
    if ((array == null) || (array.length < numOfDataPoints))
      array = new double[numOfDataPoints];
    if (mzValues != null)
      System.arraycopy(mzValues, 0, array, 0, numOfDataPoints);
    return array;
  }

  /** {@inheritDoc} */
  @Override
  public @NotNull float[] getIntensityValues() {
    return getIntensityValues(null);
  }

  /** {@inheritDoc} */
  @Override
  public @NotNull float[] getIntensityValues(@Nullable float array[]) {
    if ((array == null) || (array.length < numOfDataPoints))
      array = new float[numOfDataPoints];
    if (intensityValues != null)
      System.arraycopy(intensityValues, 0, array, 0, numOfDataPoints);
    return array;
  }

  /**
   * {@inheritDoc}
   *
   * @param rtValues an array of float.
   * @param mzValues an array of double.
   * @param intensityValues an array of float.
   * @param size a {@link Integer} object.
   */
  public synchronized void setDataPoints(@NotNull float rtValues[], @Nullable double mzValues[],
      @NotNull float intensityValues[], @NotNull Integer size) {

    Preconditions.checkNotNull(rtValues);
    Preconditions.checkNotNull(intensityValues);
    Preconditions.checkArgument(rtValues.length >= size);
    Preconditions.checkArgument(intensityValues.length >= size);
    if (mzValues != null)
      Preconditions.checkArgument(mzValues.length >= size);

    // Make a copy of the data, instead of saving a reference to the provided array
    if ((this.rtValues == null) || (this.rtValues.length < size))
      this.rtValues = new float[size];
    System.arraycopy(rtValues, 0, this.rtValues, 0, size);

    if ((this.intensityValues == null) || (this.intensityValues.length < size))
      this.intensityValues = new float[size];
    System.arraycopy(intensityValues, 0, this.intensityValues, 0, size);

    if (mzValues != null) {
      if ((this.mzValues == null) || (this.mzValues.length < size))
        this.mzValues = new double[size];
      System.arraycopy(mzValues, 0, this.mzValues, 0, size);
    } else {
      this.mzValues = null;
    }

    // Save the size of the arrays
    this.numOfDataPoints = size;

    // Update the RT range
    this.rtRange = ChromatogramUtils.getRtRange(rtValues, size);

  }

  /** {@inheritDoc} */
  @Override
  @NotNull
  public List<IsolationInfo> getIsolations() {
    return isolations;
  }

  /** {@inheritDoc} */
  @Override
  @Nullable
  public Double getMz() {
    return mz;
  }

  /**
   * {@inheritDoc}
   *
   * @param newMz a {@link Double} object.
   */
  public void setMz(@Nullable Double newMz) {
    this.mz = newMz;
  }


  /** {@inheritDoc} */
  @Override
  @Nullable
  public Range<Float> getRtRange() {
    return rtRange;
  }

  /**
   * Adds a data point to the end of the chromatogram
   *
   * @param rt a {@link Float} object.
   * @param mz a {@link Double} object.
   * @param intensity a {@link Float} object.
   */
  public synchronized void addDataPoint(@NotNull Float rt, @Nullable Double mz,
      @NotNull Float intensity) {
    Preconditions.checkNotNull(rt);
    Preconditions.checkNotNull(intensity);
    if (mzValues != null)
      Preconditions.checkNotNull(mz);

    // Adding the first data point?
    if (numOfDataPoints == 0) {
      rtValues = new float[128];
      rtValues[0] = rt;
      intensityValues = new float[128];
      intensityValues[0] = intensity;
      if (mz != null) {
        mzValues = new double[128];
        mzValues[0] = mz;
      }
      numOfDataPoints = 1;
      return;
    }


    if (rtValues.length <= numOfDataPoints) {
      float newRtValues[] = new float[Math.min(128, numOfDataPoints * 2)];
      System.arraycopy(rtValues, 0, newRtValues, 0, numOfDataPoints);
      rtValues = newRtValues;
    }
    rtValues[numOfDataPoints] = rt;

    if (intensityValues.length <= numOfDataPoints) {
      float newIntensityValues[] = new float[Math.min(128, numOfDataPoints * 2)];
      System.arraycopy(intensityValues, 0, newIntensityValues, 0, numOfDataPoints);
      intensityValues = newIntensityValues;
    }
    rtValues[numOfDataPoints] = rt;

    if (mzValues != null && mz != null) {
      if (mzValues.length <= numOfDataPoints) {
        double newMzValues[] = new double[Math.min(128, numOfDataPoints * 2)];
        System.arraycopy(mzValues, 0, newMzValues, 0, numOfDataPoints);
        mzValues = newMzValues;
      }
      mzValues[numOfDataPoints] = mz;
    }

    numOfDataPoints++;

  }



}
