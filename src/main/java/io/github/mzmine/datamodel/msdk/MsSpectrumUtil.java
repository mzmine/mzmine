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

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.collect.Range;

import io.github.mzmine.datamodel.msdk.DataPointSorter.SortingDirection;
import io.github.mzmine.datamodel.msdk.DataPointSorter.SortingProperty;

/**
 * <p>
 * MsSpectrumUtil class.
 * </p>
 */
public class MsSpectrumUtil {

  /**
   * Returns the m/z range of given data points. Can return null if the data point list is empty.
   *
   * @return a {@link Range} object.
   * @param mzValues an array of double.
   * @param size a {@link Integer} object.
   */
  @Nullable
  public static Range<Double> getMzRange(@Nonnull double mzValues[], @Nonnull Integer size) {

    // Parameter check
    Preconditions.checkNotNull(mzValues);
    Preconditions.checkNotNull(size);
    Preconditions.checkPositionIndex(size, mzValues.length);

    if (size == 0)
      return null;

    double min = mzValues[0];
    double max = mzValues[size - 1];
    return Range.closed(min, max);
  }

  /**
   * Calculates the total ion current (=sum of all intensity values)
   *
   * @return a {@link Float} object.
   * @param intensityValues an array of float.
   * @param size a {@link Integer} object.
   */
  public static @Nonnull Float getTIC(@Nonnull float intensityValues[], @Nonnull Integer size) {

    // Parameter check
    Preconditions.checkNotNull(intensityValues);
    Preconditions.checkNotNull(size);
    Preconditions.checkPositionIndex(size, intensityValues.length);

    float tic = 0f;
    for (int i = 0; i < size; i++) {
      tic += intensityValues[i];
    }
    return tic;
  }

  /**
   * Calculates the total ion current (=sum of all intensity values)
   *
   * @return a {@link Float} object.
   * @param intensityValues an array of float.
   * @param size a {@link Integer} object.
   * @param mzValues an array of double.
   * @param mzRange a {@link Range} object.
   */
  public static @Nonnull Float getTIC(@Nonnull double mzValues[], @Nonnull float intensityValues[],
      @Nonnull Integer size, @Nonnull Range<Double> mzRange) {

    // Parameter check
    Preconditions.checkNotNull(mzValues);
    Preconditions.checkNotNull(intensityValues);
    Preconditions.checkNotNull(size);
    Preconditions.checkPositionIndex(size, mzValues.length);
    Preconditions.checkPositionIndex(size, intensityValues.length);
    Preconditions.checkNotNull(mzRange);

    float tic = 0f;
    for (int i = 0; i < size; i++) {
      if (mzRange.contains(mzValues[i]))
        tic += intensityValues[i];
    }
    return tic;
  }

  /**
   * Returns the highest intensity value. Returns 0 if the list has no data points.
   *
   * @return a {@link Float} object.
   * @param intensityValues an array of float.
   * @param size a {@link Integer} object.
   */
  public static @Nonnull Float getMaxIntensity(@Nonnull float intensityValues[],
      @Nonnull Integer size) {

    // Parameter check
    Preconditions.checkNotNull(intensityValues);
    Preconditions.checkNotNull(size);
    Preconditions.checkPositionIndex(size, intensityValues.length);

    Integer topIndex = getBasePeakIndex(intensityValues, size);
    if (topIndex == null)
      return 0f;
    return intensityValues[topIndex];
  }

  /**
   * Returns the highest intensity value. Returns 0 if the list has no data points.
   *
   * @return a {@link Float} object.
   * @param intensityValues an array of float.
   * @param size a {@link Integer} object.
   * @param mzValues an array of double.
   * @param mzRange a {@link Range} object.
   */
  public static @Nonnull Float getMaxIntensity(@Nonnull double mzValues[],
      @Nonnull float intensityValues[], @Nonnull Integer size, @Nonnull Range<Double> mzRange) {

    // Parameter check
    Preconditions.checkNotNull(mzValues);
    Preconditions.checkNotNull(intensityValues);
    Preconditions.checkNotNull(size);
    Preconditions.checkPositionIndex(size, mzValues.length);
    Preconditions.checkPositionIndex(size, intensityValues.length);
    Preconditions.checkNotNull(mzRange);

    Integer topIndex = getBasePeakIndex(mzValues, intensityValues, size, mzRange);
    if (topIndex == null)
      return 0f;
    return intensityValues[topIndex];
  }

  /**
   * Returns the index of the highest intensity value. Returns null if the list has no data points.
   *
   * @return a {@link Integer} object.
   * @param intensityValues an array of float.
   * @param size a {@link Integer} object.
   */
  public static @Nullable Integer getBasePeakIndex(@Nonnull float intensityValues[],
      @Nonnull Integer size) {

    // Parameter check
    Preconditions.checkNotNull(intensityValues);
    Preconditions.checkNotNull(size);
    Preconditions.checkPositionIndex(size, intensityValues.length);

    Integer topIndex = null;
    for (int i = 0; i < size; i++) {
      if ((topIndex == null) || ((intensityValues[i] > intensityValues[topIndex])))
        topIndex = i;
    }
    return topIndex;
  }

  /**
   * Returns the index of the highest intensity value. Returns null if the list has no data points
   * or if no data point was found within the mz range.
   *
   * @param mzRange a {@link Range} object.
   * @return a {@link Integer} object.
   * @param mzValues an array of double.
   * @param intensityValues an array of float.
   * @param size a {@link Integer} object.
   */
  public static @Nullable Integer getBasePeakIndex(@Nonnull double mzValues[],
      @Nonnull float intensityValues[], @Nonnull Integer size, @Nonnull Range<Double> mzRange) {

    // Parameter check
    Preconditions.checkNotNull(intensityValues);
    Preconditions.checkNotNull(mzValues);
    Preconditions.checkNotNull(size);
    Preconditions.checkPositionIndex(size, mzValues.length);
    Preconditions.checkPositionIndex(size, intensityValues.length);
    Preconditions.checkNotNull(mzRange);

    Integer topIndex = null;
    for (int i = 0; i < size; i++) {
      if ((topIndex == null || intensityValues[i] > intensityValues[topIndex])
          && mzRange.contains(mzValues[i]))
        topIndex = i;
    }
    return topIndex;
  }

  /**
   * <p>
   * normalizeIntensity.
   * </p>
   *
   * @param intensityValues an array of float.
   * @param size a {@link Integer} object.
   * @param scale a {@link Float} object.
   */
  public static void normalizeIntensity(@Nonnull float intensityValues[], @Nonnull Integer size,
      @Nonnull Float scale) {

    // Parameter check
    Preconditions.checkNotNull(intensityValues);
    Preconditions.checkNotNull(size);
    Preconditions.checkPositionIndex(size, intensityValues.length);
    Preconditions.checkNotNull(scale);

    final float max = getMaxIntensity(intensityValues, size);

    for (int i = 0; i < intensityValues.length; i++) {
      intensityValues[i] = intensityValues[i] / max * scale;
    }

  }

  /**
   * <p>
   * msSpectrumToString.
   * </p>
   *
   * @param spectrum a {@link MsSpectrum} object.
   * @return a {@link String} object.
   */
  public static @Nonnull String msSpectrumToString(MsSpectrum spectrum) {

    return msSpectrumToString(spectrum.getMzValues(), spectrum.getIntensityValues(),
        spectrum.getNumberOfDataPoints());

  }

  /**
   * <p>
   * msSpectrumToString.
   * </p>
   *
   * @param mzValues an array of double.
   * @param intensityValues an array of float.
   * @param size a {@link Integer} object.
   * @return a {@link String} object.
   */
  public static @Nonnull String msSpectrumToString(@Nonnull double mzValues[],
      @Nonnull float intensityValues[], @Nonnull Integer size) {

    // Parameter check
    Preconditions.checkNotNull(mzValues);
    Preconditions.checkNotNull(intensityValues);
    Preconditions.checkNotNull(size);
    Preconditions.checkPositionIndex(size, mzValues.length);
    Preconditions.checkPositionIndex(size, intensityValues.length);

    StringBuilder b = new StringBuilder();
    for (int i = 0; i < size; i++) {
      b.append(mzValues[i]);
      b.append(" ");
      b.append(intensityValues[i]);
      if (i < size - 1)
        b.append("\n");
    }

    return b.toString();

  }

  /**
   * Filters with only N most intense elements.
   * 
   * @param spectrum - ms spectrum
   * @param pairsLimit - maximum amount of items in a new Spectrum
   * @return
   */
  public static @Nonnull MsSpectrum filterMsSpectrum(@Nonnull MsSpectrum ms,
      @Nonnull Integer pairsLimit) {

    TreeMap<Float, Double> intesityMzSorted = new TreeMap<>(Comparator.reverseOrder());
    double mz[] = ms.getMzValues();
    float intensity[] = ms.getIntensityValues();
    if (mz.length <= pairsLimit)
      return ms;

    double[] newMz = new double[pairsLimit];
    float[] newIntensity = new float[pairsLimit];

    /* Sort intensity pairs */
    for (int i = 0; i < mz.length; i++) {
      intesityMzSorted.put(intensity[i], mz[i]);
    }

    /* Create new arrays with filtered intensity pairs */
    for (int i = 0; i < pairsLimit; i++) {
      Entry<Float, Double> pair = intesityMzSorted.firstEntry();
      intesityMzSorted.remove(pair.getKey());
      newMz[i] = pair.getValue();
      newIntensity[i] = pair.getKey();
    }
    intesityMzSorted.clear();

    /* Sort ascending by mz */
    DataPointSorter.sortDataPoints(newMz, newIntensity, pairsLimit, SortingProperty.MZ,
        SortingDirection.ASCENDING);

    /* Create new Spectrum object */
    MsSpectrumType type = ms.getSpectrumType();
    SimpleMsSpectrum filteredMs = new SimpleMsSpectrum(newMz, newIntensity, pairsLimit, type);
    return filteredMs;
  }

  /**
   * Method preprocesses list of spectra, limits its amount
   * Filtering of Spectrum objects is done by retrieving top N Spectrum objects with largest
   * intensity values
   *
   * @param spectra - list of spectrum to be preprocessed
   * @param listLimit - maximum amount of items to be in a new list
   * @return filtered lists
   */
  public static List<MsSpectrum> filterMsSpectra(List<MsSpectrum> spectra, int listLimit) {
    if (spectra == null)
      return null;

    /* Small optimization */
    if (spectra.size() < listLimit) {
      return spectra;
    }

    TreeMap<Float, MsSpectrum> orderedSpectra = new TreeMap<>(Comparator.reverseOrder());

    /* Order by largest intensity value */
    for (MsSpectrum ms : spectra) {
      float biggest = 0;
      for (float temp : ms.getIntensityValues()) {
        if (temp > biggest)
          biggest = temp;
      }

      orderedSpectra.put(biggest, ms);
    }

    /* Retrieve only top N items */
    List<MsSpectrum> ordered = new LinkedList<>();
    for (int i = 0; i < listLimit; i++) {
      Entry<Float, MsSpectrum> pair = orderedSpectra.firstEntry();
      if (pair == null) break;
      orderedSpectra.remove(pair.getKey());
      MsSpectrum ms = pair.getValue();
      ordered.add(ms);
    }

    return ordered;
  }
}
