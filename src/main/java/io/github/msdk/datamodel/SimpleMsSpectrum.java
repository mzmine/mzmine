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

/**
 * Simple implementation of the MsSpectrum interface.
 */
public class SimpleMsSpectrum extends AbstractMsSpectrum {

  /**
   * <p>Constructor for SimpleMsSpectrum.</p>
   */
  public SimpleMsSpectrum() {}

  /**
   * <p>
   * Constructor for SimpleMsSpectrum.
   * </p>
   *
   * @param mzValues an array of double.
   * @param intensityValues an array of float.
   * @param size a {@link Integer} object.
   * @param spectrumType a {@link MsSpectrumType} object.
   */
  public SimpleMsSpectrum(@Nonnull double mzValues[], @Nonnull float intensityValues[],
      @Nonnull Integer size, @Nonnull MsSpectrumType spectrumType) {
    setDataPoints(mzValues, intensityValues, size);
    setSpectrumType(spectrumType);
  }


}
