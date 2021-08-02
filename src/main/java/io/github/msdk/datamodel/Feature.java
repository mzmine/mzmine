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

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A feature represents a single three-dimensional peak in an LC-MS or GC-MS dataset. It must have
 * at least an m/z value and a retention time. In addition, it can contain more detailed data about
 * the peak, such as S/N ratio or raw data points that constitute the feature (Chromatogram).
 *
 * @since 0.0.8
 */
public interface Feature {

  /**
   * <p>getMz.</p>
   *
   * @return a {@link Double} object.
   */
  @Nonnull
  Double getMz();

  /**
   * <p>getRetentionTime.</p>
   *
   * @return a {@link Float} object.
   */
  @Nonnull
  Float getRetentionTime();

  /**
   * <p>getArea.</p>
   *
   * @return a {@link Float} object.
   */
  @Nullable
  Float getArea();

  /**
   * <p>getHeight.</p>
   *
   * @return a {@link Float} object.
   */
  @Nullable
  Float getHeight();

  /**
   * Returns signal to noise ratio.
   *
   * @return S/N ratio, null if the method does not estimate it, NaN if noise was estimated to 0
   */
  @Nullable
  Float getSNRatio();

  /**
   * Returns arbitrary, dimension-less quality score for the feature
   *
   * @return Score
   */
  @Nullable
  Float getScore();

  /**
   * <p>getChromatogram.</p>
   *
   * @return a {@link Chromatogram} object.
   */
  @Nullable
  Chromatogram getChromatogram();

  /**
   * <p>getMSMSSpectra.</p>
   *
   * @return a {@link List} object.
   */
  @Nullable
  List<MsScan> getMSMSSpectra();

  /**
   * <p>getIonAnnotation.</p>
   *
   * @return a {@link IonAnnotation} object.
   */
  @Nullable
  IonAnnotation getIonAnnotation();

}
