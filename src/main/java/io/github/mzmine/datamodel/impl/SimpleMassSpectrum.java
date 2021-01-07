/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.datamodel.impl;

import java.nio.DoubleBuffer;
import javax.annotation.Nonnull;

/**
 * Simple implementation of MassSpectrum that stores all data in memory.
 */
public class SimpleMassSpectrum extends AbstractMassSpectrum {

  private static final DoubleBuffer EMPTY_BUFFER = DoubleBuffer.wrap(new double[0]);

  private DoubleBuffer mzValues;
  private DoubleBuffer intensityValues;

  @Override
  public DoubleBuffer getMzValues() {
    if (mzValues == null)
      return EMPTY_BUFFER;
    else
      return mzValues;
  }

  @Override
  public DoubleBuffer getIntensityValues() {
    if (intensityValues == null)
      return EMPTY_BUFFER;
    else
      return intensityValues;
  }

  public synchronized void setDataPoints(@Nonnull double mzValues[],
      @Nonnull double intensityValues[]) {

    assert mzValues != null;
    assert intensityValues != null;
    assert mzValues.length == intensityValues.length;

    for (int i = 0; i < mzValues.length - 1; i++) {
      if (mzValues[i] > mzValues[i + 1]) {
        throw new IllegalArgumentException("The m/z values must be sorted in ascending order");
      }
    }

    this.mzValues = DoubleBuffer.wrap(mzValues);
    this.intensityValues = DoubleBuffer.wrap(intensityValues);

    updateMzRangeAndTICValues();
  }

  public synchronized void setDataPoints(@Nonnull DoubleBuffer mzValues,
      DoubleBuffer intensityValues) {

    assert mzValues != null;
    assert intensityValues != null;
    assert mzValues.capacity() == intensityValues.capacity();

    for (int i = 0; i < mzValues.capacity() - 1; i++) {
      if (mzValues.get(i) > mzValues.get(i + 1)) {
        throw new IllegalArgumentException("The m/z values must be sorted in ascending order");
      }
    }

    this.mzValues = mzValues;
    this.intensityValues = intensityValues;

    updateMzRangeAndTICValues();
  }

}
