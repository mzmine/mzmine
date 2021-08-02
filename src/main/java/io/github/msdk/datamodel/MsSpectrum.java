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

import com.google.common.collect.Range;

import io.github.msdk.util.tolerances.MzTolerance;

/**
 * A mass spectrum. This is a base interface typically extended by other, more specialized
 * interfaces. It may represent a single scan in raw MS data, a calculated isotope pattern, a
 * predicted fragmentation spectrum of a molecule, etc.
 *
 */
public interface MsSpectrum {

  /**
   * Returns the type of this mass spectrum. For spectra that are loaded from raw data files, the
   * type is detected automatically. For calculated spectra, the type depends on the method of
   * calculation.
   *
   * @return spectrum type (profile, centroided, thresholded)
   */
  @Nonnull
  MsSpectrumType getSpectrumType();

  /**
   * <p>
   * getNumberOfDataPoints.
   * </p>
   *
   * @return a {@link Integer} object.
   */
  @Nonnull
  Integer getNumberOfDataPoints();

  /**
   * <p>
   * Returns the m/z values of this spectrum.
   *
   * Note: this method may need to read data from disk, therefore it may be quite slow.
   * </p>
   *
   * @return an array of double.
   */
  @Nonnull
  default double[] getMzValues() {
    return getMzValues(null);
  }

  /**
   * <p>
   * Returns the m/z values of this spectrum.
   *
   * Note: this method may need to read data from disk, therefore it may be quite slow.
   * </p>
   *
   * @return an array of double.
   */
  @Nonnull
  double[] getMzValues(double array[]);

  /**
   * <p>
   * Returns the intensity values of this spectrum.
   *
   * Note: this method may need to read data from disk, therefore it may be quite slow.
   * </p>
   *
   * @return an array of float.
   */
  @Nonnull
  default float[] getIntensityValues() {
    return getIntensityValues(null);
  }

  /**
   * <p>
   * Returns the intensity values of this spectrum.
   *
   * Note: this method may need to read data from disk, therefore it may be quite slow.
   * </p>
   *
   * @return an array of float.
   */
  @Nonnull
  float[] getIntensityValues(float array[]);

  /**
   * Returns the sum of intensities of all data points (total ion current or TIC).
   *
   * @return total ion current
   */
  @Nonnull
  Float getTIC();

  /**
   * Returns the range of m/z values for the current spectrum. This can return null if the spectrum
   * has no data points.
   *
   * @return m/z range
   */
  @Nullable
  Range<Double> getMzRange();

  /**
   * Returns an object that implements the MzTolerance interface. If not MzTolernace is available,
   * then null is returned.
   *
   * @return m/z tolerance for the scan
   */
  @Nullable
  MzTolerance getMzTolerance();
}
