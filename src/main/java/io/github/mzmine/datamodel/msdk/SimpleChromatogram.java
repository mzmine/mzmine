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

package io.github.mzmine.datamodel.msdk;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.collect.Range;

/**
 * Simple implementation of the Chromatogram interface.
 */
public class SimpleChromatogram implements Chromatogram {

  private @Nullable RawDataFile dataFile;
  private @Nonnull Integer chromatogramNumber, numOfDataPoints = 0;
  private @Nonnull ChromatogramType chromatogramType;
  private @Nullable Double mz;
  private @Nonnull SeparationType separationType;
  private @Nonnull float rtValues[];
  private @Nullable double mzValues[];
  private @Nonnull float intensityValues[];
  private @Nonnull Range<Float> rtRange;
  private @Nullable IonAnnotation ionAnnotation;

  private final @Nonnull List<IsolationInfo> isolations = new ArrayList<>();

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
  public void setRawDataFile(@Nonnull RawDataFile newRawDataFile) {
    this.dataFile = newRawDataFile;
  }

  /** {@inheritDoc} */
  @Override
  @Nonnull
  public Integer getChromatogramNumber() {
    return chromatogramNumber;
  }

  /**
   * {@inheritDoc}
   *
   * @param chromatogramNumber a {@link Integer} object.
   */
  public void setChromatogramNumber(@Nonnull Integer chromatogramNumber) {
    Preconditions.checkNotNull(chromatogramNumber);
    this.chromatogramNumber = chromatogramNumber;
  }

  /** {@inheritDoc} */
  @Override
  @Nonnull
  public ChromatogramType getChromatogramType() {
    return chromatogramType;
  }

  /**
   * {@inheritDoc}
   *
   * @param newChromatogramType a {@link ChromatogramType}
   *        object.
   */
  public void setChromatogramType(@Nonnull ChromatogramType newChromatogramType) {
    this.chromatogramType = newChromatogramType;
  }

  /** {@inheritDoc} */
  @Override
  public @Nonnull Integer getNumberOfDataPoints() {
    return numOfDataPoints;
  }

  /** {@inheritDoc} */
  @Override
  public @Nonnull float[] getRetentionTimes() {
    return getRetentionTimes(null);
  }

  /**
   * {@inheritDoc}
   *
   * @param array an array of float.
   * @return an array of float.
   */
  public @Nonnull float[] getRetentionTimes(@Nullable float[] array) {
    if ((array == null) || (array.length < numOfDataPoints))
      array = new float[numOfDataPoints];
    if (rtValues != null)
      System.arraycopy(rtValues, 0, array, 0, numOfDataPoints);
    return array;
  }

  /** {@inheritDoc} */
  @Override
  public @Nonnull double[] getMzValues() {
    return getMzValues(null);
  }

  /**
   * {@inheritDoc}
   *
   * @param array an array of float.
   * @return an array of float.
   */
  public @Nonnull double[] getMzValues(@Nullable double[] array) {
    if ((array == null) || (array.length < numOfDataPoints))
      array = new double[numOfDataPoints];
    if (mzValues != null)
      System.arraycopy(mzValues, 0, array, 0, numOfDataPoints);
    return array;
  }

  /** {@inheritDoc} */
  @Override
  public @Nonnull float[] getIntensityValues() {
    return getIntensityValues(null);
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
   * @param rtValues an array of float.
   * @param mzValues an array of double.
   * @param intensityValues an array of float.
   * @param size a {@link Integer} object.
   */
  public synchronized void setDataPoints(@Nonnull float rtValues[], @Nullable double mzValues[],
      @Nonnull float intensityValues[], @Nonnull Integer size) {

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
    this.rtRange = ChromatogramUtil.getRtRange(rtValues, size);

  }

  /** {@inheritDoc} */
  @Override
  @Nonnull
  public List<IsolationInfo> getIsolations() {
    return isolations;
  }

  /** {@inheritDoc} */
  @Override
  @Nonnull
  public SeparationType getSeparationType() {
    return separationType;
  }

  /**
   * {@inheritDoc}
   *
   * @param separationType a {@link SeparationType} object.
   */
  public void setSeparationType(@Nonnull SeparationType separationType) {
    this.separationType = separationType;
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

  /**
   * {@inheritDoc}
   *
   * @param ionAnnotation a {@link IonAnnotation} object.
   */
  public void setIonAnnotation(@Nonnull IonAnnotation ionAnnotation) {
    this.ionAnnotation = ionAnnotation;
  }

  /** {@inheritDoc} */
  @Override
  public IonAnnotation getIonAnnotation() {
    return ionAnnotation;
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
  public synchronized void addDataPoint(@Nonnull Float rt, @Nullable Double mz,
      @Nonnull Float intensity) {
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

    if (mz != null) {
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
