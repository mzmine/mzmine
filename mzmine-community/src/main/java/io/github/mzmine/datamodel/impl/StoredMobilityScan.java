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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.msms.PasefMsMsInfo;
import io.github.mzmine.util.scans.ScanUtils;
import java.util.Iterator;
import java.util.Objects;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StoredMobilityScan implements MobilityScan {

  private static final Logger logger = Logger.getLogger(StoredMobilityScan.class.getName());

  private final MobilityScanStorage storage;
  private final int index;

  /**
   * @param mobilityScanIndex The mobility scan index starting with 0.
   */
  public StoredMobilityScan(int mobilityScanIndex, @NotNull final MobilityScanStorage storage) {
    this.storage = storage;
    this.index = mobilityScanIndex;
  }

  @Override
  public int getNumberOfDataPoints() {
    return storage.getNumberOfRawDatapoints(index);
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
    storage.getRawMobilityScanMzValues(getMobilityScanNumber(), dst);
    return dst;
  }

  @Override
  public double[] getIntensityValues(@NotNull double[] dst) {
    if (dst.length < getNumberOfDataPoints()) {
      dst = new double[getNumberOfDataPoints()];
    }
    storage.getRawMobilityScanIntensityValues(getMobilityScanNumber(), dst);
    return dst;
  }

  @Override
  public double getMzValue(int index) {
    return storage.getRawMobilityScanMzValue(getMobilityScanNumber(), index);
  }

  @Override
  public double getIntensityValue(int index) {
    return storage.getRawMobilityScanIntensityValue(getMobilityScanNumber(), index);
  }

  @Nullable
  @Override
  public Double getBasePeakMz() {
    final int basePeakIndex = storage.getRawBasePeakIndex(getMobilityScanNumber());
    return basePeakIndex != -1 ? getMzValue(basePeakIndex) : null;
  }

  @Nullable
  @Override
  public Double getBasePeakIntensity() {
    final int basePeakIndex = storage.getRawBasePeakIndex(getMobilityScanNumber());
    return basePeakIndex != -1 ? getIntensityValue(basePeakIndex) : null;
  }

  @Nullable
  @Override
  public Integer getBasePeakIndex() {
    int rawBasePeakIndex = storage.getRawBasePeakIndex(getMobilityScanNumber());
    return rawBasePeakIndex != -1 ? rawBasePeakIndex : null;
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
    double tic = 0d;
    for (int i = 0; i < getNumberOfDataPoints(); i++) {
      tic += getIntensityValue(i);
    }
    return tic;
  }

  @NotNull
  @Override
  public RawDataFile getDataFile() {
    return getFrame().getDataFile();
  }

  @Override
  public double getMobility() {
    return storage.getFrame().getMobilityForMobilityScanNumber(getMobilityScanNumber());
  }

  @Override
  public MobilityType getMobilityType() {
    return storage.getFrame().getMobilityType();
  }

  @Override
  public Frame getFrame() {
    return storage.getFrame();
  }

  @Override
  public float getRetentionTime() {
    return storage.getFrame().getRetentionTime();
  }

  @Override
  public int getMobilityScanNumber() {
    return index;
  }

  @Nullable
  @Override
  public PasefMsMsInfo getMsMsInfo() {
    return storage.getFrame().getImsMsMsInfoForMobilityScan(getMobilityScanNumber());
  }

  @Override
  public String toString() {
    return ScanUtils.scanToString(this);
  }

  @Override
  public void addMassList(@NotNull MassList massList) {
    throw new UnsupportedOperationException(
        "Mass lists should be added to the mobility scan storage.");
  }

  @Override
  public MassList getMassList() {
    return storage.getMassList(getMobilityScanNumber());
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
    StoredMobilityScan that = (StoredMobilityScan) o;
    return index == that.index && Objects.equals(getFrame(), ((StoredMobilityScan) o).getFrame())
           && Objects.equals(getDataFile(), ((StoredMobilityScan) o).getDataFile());
  }

  @Override
  public int hashCode() {
    return Objects.hash(index, getFrame().getFrameId(), getDataFile());
  }

  @Override
  public @Nullable Float getInjectionTime() {
    return getFrame().getInjectionTime();
  }
}
