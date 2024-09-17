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

package io.github.mzmine.datamodel.impl.masslist;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.impl.MobilityScanStorage;
import java.util.Iterator;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StoredMobilityScanMassList implements MassList {

  private final MobilityScanStorage storage;
  private final int index;

  /**
   * @param mobilityScanIndex The mobility scan index starting with 0.
   */
  public StoredMobilityScanMassList(int mobilityScanIndex,
      @NotNull final MobilityScanStorage storage) {
    this.storage = storage;
    this.index = mobilityScanIndex;
  }

  private int getMobilityScanNumber() {
    return index;
  }

  @Override
  public int getNumberOfDataPoints() {
    return storage.getNumberOfMassListDatapoints(index);
  }

  @Override
  public MassSpectrumType getSpectrumType() {
    return storage.getFrame().getSpectrumType();
  }

  @Override
  public double[] getMzValues(@NotNull double[] dst) {
    if (dst.length < getNumberOfDataPoints()) {
      dst = new double[getNumberOfDataPoints()];
    }
    storage.getMassListMzValues(getMobilityScanNumber(), dst);
    return dst;
  }

  @Override
  public double[] getIntensityValues(@NotNull double[] dst) {
    if (dst.length < getNumberOfDataPoints()) {
      dst = new double[getNumberOfDataPoints()];
    }
    storage.getMassListIntensityValues(getMobilityScanNumber(), dst);
    return dst;
  }

  @Override
  public double getMzValue(int index) {
    return storage.getMassListMzValue(getMobilityScanNumber(), index);
  }

  @Override
  public double getIntensityValue(int index) {
    return storage.getMassListIntensityValue(getMobilityScanNumber(), index);
  }

  @Nullable
  @Override
  public Double getBasePeakMz() {
    final int basePeakIndex = storage.getMassListBasePeakIndex(getMobilityScanNumber());
    return basePeakIndex != -1 ? getMzValue(basePeakIndex) : null;
  }

  @Nullable
  @Override
  public Double getBasePeakIntensity() {
    final int basePeakIndex = storage.getMassListBasePeakIndex(getMobilityScanNumber());
    return basePeakIndex != -1 ? getIntensityValue(basePeakIndex) : null;
  }

  @Nullable
  @Override
  public Integer getBasePeakIndex() {
    int basePeakIndex = storage.getMassListBasePeakIndex(getMobilityScanNumber());
    return basePeakIndex != -1 ? basePeakIndex : null;
  }

  @Nullable
  @Override
  public Range<Double> getDataPointMZRange() {
    return getNumberOfDataPoints() > 1 ? Range.closed(getMzValue(0),
        getMzValue(getNumberOfDataPoints() - 1))
        : Range.singleton(getNumberOfDataPoints() == 0 ? 0d : getMzValue(0));
  }

  @Nullable
  @Override
  public Double getTIC() {
    throw new UnsupportedOperationException("Intentionally unimplemented to safe RAM.");
  }

  @Override
  public Iterator<DataPoint> iterator() {
    return new DataPointIterator(this);
  }

  private class DataPointIterator implements Iterator<DataPoint>, DataPoint {

    private final MassSpectrum spectrum;
    // We start at -1 so the first call to next() moves us to index 0
    private int cursor = -1;

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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StoredMobilityScanMassList that = (StoredMobilityScanMassList) o;
    return index == that.index
        && getNumberOfDataPoints() == ((StoredMobilityScanMassList) o).getNumberOfDataPoints()
        && Objects.equals(storage.getFrame(), ((StoredMobilityScanMassList) o).storage.getFrame())
        && Objects.equals(storage.getFrame().getDataFile(),
        ((StoredMobilityScanMassList) o).storage.getFrame().getDataFile());
  }

  @Override
  public int hashCode() {
    return Objects.hash(index, storage.getFrame().getFrameId(), storage.getFrame().getDataFile(),
        getNumberOfDataPoints());
  }
}

