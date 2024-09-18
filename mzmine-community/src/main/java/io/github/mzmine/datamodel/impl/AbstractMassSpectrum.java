/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.featuredata.impl.StorageUtils;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.scans.ScanUtils;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.foreign.ValueLayout.OfDouble;
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

    if (getNumberOfDataPoints() == 0) {
      totalIonCurrent = 0.0;
      mzRange = null;
      basePeakIndex = null;
      return;
    }

    basePeakIndex = 0;

    double lastMz = getMzValue(0);
    double maxIntensity = getIntensityValue(0);
    totalIonCurrent = maxIntensity;
    for (int i = 1; i < getNumberOfDataPoints(); i++) {

      // Check the order of the m/z values
      double mz = getMzValue(i);
      if (lastMz > mz) {
        throw new IllegalArgumentException("The m/z values must be sorted in ascending order");
      }

      // Update base peak index
      double intensity = getIntensityValue(i);
      if (intensity > maxIntensity) {
        basePeakIndex = i;
        maxIntensity = intensity;
      }

      // Update TIC
      totalIonCurrent += intensity;
      //
      lastMz = mz;
    }
    // set range after checking the order
    mzRange = Range.closed(getMzValue(0), getMzValue(getNumberOfDataPoints() - 1));
  }


  /**
   * @see io.github.mzmine.datamodel.Scan#getNumberOfDataPoints()
   */
  @Override
  public int getNumberOfDataPoints() {
    return (int) StorageUtils.numDoubles(getMzValues());
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
    if (spectrumType == null) {
      spectrumType = ScanUtils.detectSpectrumType(
          DataPointUtils.getDoubleBufferAsArray(getMzValues()),
          DataPointUtils.getDoubleBufferAsArray(getIntensityValues()));
    }

    return spectrumType;
  }

  public void setSpectrumType(MassSpectrumType spectrumType) {
    this.spectrumType = spectrumType;
  }

  @Override
  public double getMzValue(int index) {
    return getMzValues().getAtIndex(JAVA_DOUBLE, index);
  }

  @Override
  public double getIntensityValue(int index) {
    return getIntensityValues().getAtIndex(JAVA_DOUBLE, index);
  }

  @Override
  @Nullable
  public Double getBasePeakMz() {
    if (basePeakIndex == null) {
      return null;
    } else {
      return getMzValues().getAtIndex(JAVA_DOUBLE, basePeakIndex);
    }
  }

  @Override
  @Nullable
  public Double getBasePeakIntensity() {
    if (basePeakIndex == null) {
      return null;
    } else {
      return getIntensityValues().getAtIndex(JAVA_DOUBLE, basePeakIndex);
    }
  }

  abstract MemorySegment getMzValues();

  abstract MemorySegment getIntensityValues();

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
