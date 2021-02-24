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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.datamodel.data_access;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.util.exceptions.MissingMassListException;
import java.util.Iterator;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The intended use of this memory access is to loop over all scans and access data points via
 * {@link #getMzValue(int)} and {@link #getIntensityValue(int)}
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class ScanDataAccess implements MassSpectrum {

  public enum DataType {
    RAW, CENTROID
  }

  protected final RawDataFile dataFile;
  protected final DataType type;
  protected final Scan[] scans;

  // current data
  protected final double[] mzs;
  protected final double[] intensities;
  protected int currentScan = -1;
  protected int currentNumberOfDataPoints = -1;

  /**
   * The intended use of this memory access is to loop over all scans and access data points via
   * {@link #getMzValue(int)} and {@link #getIntensityValue(int)}
   *
   * @param dataFile  target data file to loop over all scans or mass lists
   * @param type      processed or raw data
   * @param selection processed or raw data
   */
  protected ScanDataAccess(RawDataFile dataFile,
      DataType type, ScanSelection selection) {
    this.dataFile = dataFile;
    this.type = type;
    scans = selection.getMatchingScans(dataFile);
    // might even use the maximum number of data points in the selected scans
    // but seems unnecessary
    int length = getMaxNumberOfDataPoints();
    mzs = new double[length];
    intensities = new double[length];
  }

  /**
   *
   *
   * @return Number of data points in the current scan depending of the defined DataType (RAW/CENTROID)
   */
  @Override
  public int getNumberOfDataPoints() {
    return currentNumberOfDataPoints;
  }

  public Scan getCurrentScan() {
    assert currentScan >= 0 && hasNextScan();
    return scans[currentScan];
  }

  /**
   * @return
   */
  private MassList getMassList() {
    return getCurrentScan().getMassList();
  }

  /**
   * Get mass-to-charge ratio at index
   *
   * @param index data point index
   * @return
   */
  @Override
  public double getMzValue(int index) {
    assert index < getNumberOfDataPoints() && index >= 0;
    return mzs[index];
  }

  /**
   * Get intensity at index
   *
   * @param index data point index
   * @return
   */
  @Override
  public double getIntensityValue(int index) {
    assert index < getNumberOfDataPoints() && index >= 0;
    return intensities[index];
  }

  /**
   * Set the data to the next scan, if available. Returns the scan for additional data access. m/z
   * and intensity values should be accessed from this data class via {@link #getMzValue(int)} and
   * {@link #getIntensityValue(int)}
   *
   * @return the scan or null
   * @throws MissingMassListException if DataType.CENTROID is selected and mass list is missing in
   *                                  the current scan
   */
  @Nullable
  public Scan nextScan() throws MissingMassListException {
    if (hasNextScan()) {
      currentScan++;
      switch (type) {
        case RAW -> {
          scans[currentScan].getMzValues(mzs);
          scans[currentScan].getIntensityValues(intensities);
          currentNumberOfDataPoints = scans[currentScan].getNumberOfDataPoints();
        }
        case CENTROID -> {
          MassList masses = scans[currentScan].getMassList();
          if (masses == null) {
            throw new MissingMassListException(scans[currentScan]);
          }
          masses.getMzValues(mzs);
          masses.getIntensityValues(intensities);
          currentNumberOfDataPoints = masses.getNumberOfDataPoints();
        }
      }
      return scans[currentScan];
    }
    return null;
  }

  /**
   * The current list of scans has another element
   *
   * @return
   */
  public boolean hasNextScan() {
    return currentScan < getNumberOfScans();
  }

  /**
   * Number of selected scans
   *
   * @return
   */
  public int getNumberOfScans() {
    return scans.length;
  }

  /**
   * Maximum number of data points is used to create the arrays that back the data
   *
   * @return
   */
  private int getMaxNumberOfDataPoints() {
    return switch (type) {
      case CENTROID -> dataFile.getMaxCentroidDataPoints();
      case RAW -> dataFile.getMaxRawDataPoints();
      default -> throw new IllegalStateException("Unexpected value: " + type);
    };
  }

  // ###############################################
  // general MassSpectrum methods
  @Override
  public MassSpectrumType getSpectrumType() {
    return switch (type) {
      case RAW -> scans[currentScan].getSpectrumType();
      case CENTROID -> MassSpectrumType.CENTROIDED;
      default -> throw new IllegalStateException("Unexpected value: " + type);
    };
  }

  @Nullable
  @Override
  public Double getBasePeakMz() {
    Integer index = getBasePeakIndex();
    return index != null && index >= 0 ? getMzValue(index) : null;
  }

  @Nullable
  @Override
  public Double getBasePeakIntensity() {
    Integer index = getBasePeakIndex();
    return index != null && index >= 0 ? getIntensityValue(index) : null;
  }

  @Nullable
  @Override
  public Integer getBasePeakIndex() {
    switch (type) {
      case RAW:
        return getCurrentScan().getBasePeakIndex();
      case CENTROID:
        MassList masses = getMassList();
        return masses == null ? null : masses.getBasePeakIndex();
      default:
        throw new IllegalStateException("Unexpected value: " + type);
    }
  }

  @Nullable
  @Override
  public Range<Double> getDataPointMZRange() {
    switch (type) {
      case RAW:
        return getCurrentScan().getDataPointMZRange();
      case CENTROID:
        MassList masses = getMassList();
        return masses == null ? null : masses.getDataPointMZRange();
      default:
        throw new IllegalStateException("Unexpected value: " + type);
    }
  }

  @Nullable
  @Override
  public Double getTIC() {
    switch (type) {
      case RAW:
        return getCurrentScan().getTIC();
      case CENTROID:
        MassList masses = getMassList();
        return masses == null ? null : masses.getTIC();
      default:
        throw new IllegalStateException("Unexpected value: " + type);
    }
  }

  @Override
  public double[] getMzValues(@Nonnull double[] dst) {
    throw new UnsupportedOperationException(
        "The intended use of this class is to loop over all scans and data points");
  }

  @Override
  public double[] getIntensityValues(@Nonnull double[] dst) {
    throw new UnsupportedOperationException(
        "The intended use of this class is to loop over all scans and data points");
  }

  @Override
  public Stream<DataPoint> stream() {
    throw new UnsupportedOperationException(
        "The intended use of this class is to loop over all scans and data points");
  }

  @Nonnull
  @Override
  public Iterator<DataPoint> iterator() {
    throw new UnsupportedOperationException(
        "The intended use of this class is to loop over all scans and data points");
  }
}
