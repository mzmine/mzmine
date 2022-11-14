/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.datamodel.impl;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MassSpectrumType;
import java.nio.DoubleBuffer;
import java.util.Iterator;
import java.util.logging.Logger;
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
   *
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
    if (basePeakIndex == null) {
      return null;
    } else {
      return getMzValues().get(basePeakIndex);
    }
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
