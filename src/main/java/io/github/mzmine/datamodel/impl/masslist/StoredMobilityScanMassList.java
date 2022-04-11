/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.datamodel.impl.masslist;

import com.google.common.collect.Range;
import com.google.common.collect.Streams;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.impl.MobilityScanStorage;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Stream;
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
    storage.getMassListMzValues(getMobilityScanNumber(), dst, 0);
    return dst;
  }

  @Override
  public double[] getIntensityValues(@NotNull double[] dst) {
    if (dst.length < getNumberOfDataPoints()) {
      dst = new double[getNumberOfDataPoints()];
    }
    storage.getMassListIntensityValues(getMobilityScanNumber(), dst, 0);
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

  @Override
  public Stream<DataPoint> stream() {
    return Streams.stream(this);
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

