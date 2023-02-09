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

package io.github.mzmine.datamodel.data_access;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.util.exceptions.MissingMassListException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The intended use of this memory access is to loop over all scans and access data points via
 * {@link #getMzValue(int)} and {@link #getIntensityValue(int)}
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public abstract class ScanDataAccess implements Scan {

  protected final RawDataFile dataFile;
  protected final ScanDataType type;
  // current data
  protected final double[] mzs;
  protected final double[] intensities;
  protected Map<Scan, Integer> scanIndexMap;
  protected int currentNumberOfDataPoints = -1;
  protected int scanIndex = -1;

  /**
   * The intended use of this memory access is to loop over all scans and access data points via
   * {@link #getMzValue(int)} and {@link #getIntensityValue(int)}
   *
   * @param dataFile target data file to loop over all scans or mass lists
   * @param type     processed or raw data
   */
  protected ScanDataAccess(RawDataFile dataFile, ScanDataType type) {
    this.dataFile = dataFile;
    this.type = type;
    // might even use the maximum number of data points in the selected scans
    // but seems unnecessary
    int length = getMaxNumberOfDataPoints();
    mzs = new double[length];
    intensities = new double[length];
  }

  /**
   * @return Number of data points in the current scan depending of the defined DataType
   * (RAW/CENTROID)
   */
  @Override
  public int getNumberOfDataPoints() {
    return currentNumberOfDataPoints;
  }

  /**
   * @return the currently selected scan or null if none is selected
   */
  @Nullable
  public abstract Scan getCurrentScan();

  /**
   * @return the masslist of the current scan (if available)
   */
  @Nullable
  public MassList getMassList() {
    Scan scan = getCurrentScan();
    return scan == null ? null : scan.getMassList();
  }

  /**
   * Get mass-to-charge ratio at index
   *
   * @param index data point index
   * @return m/z value at index
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
   * @return intensity value at index
   */
  @Override
  public double getIntensityValue(int index) {
    assert index < getNumberOfDataPoints() && index >= 0;
    return intensities[index];
  }

  /**
   * Jumps to target scan. Uses hashmap that is lazily initialized. Use jumpToIndex if index is
   * available.
   *
   * @param target jump to target scan
   * @return true if target scan was available
   */
  public boolean jumpToScan(Scan target) {
    return jumpToIndex(indexOf(target));
  }

  /**
   * Uses a lazily initialized hashmap to find the index of the scan in this data access
   *
   * @param target the target scan
   * @return the scan index or -1 if not found
   */
  public int indexOf(Scan target) {
    if (scanIndexMap == null) {
      scanIndexMap = new HashMap<>(getNumberOfScans());
      int oldIndex = scanIndex;
      // fill map with indexes
      scanIndex = -1;
      int c = 0;
      while (hasNextScan()) {
        scanIndexMap.put(nextScan(), c);
        c++;
      }
      scanIndex = oldIndex;
    }
    return scanIndexMap.getOrDefault(target, -1);
  }

  /**
   * Jump to scan at index
   *
   * @param index index of scan in this data access (of all matching scans)
   * @return true if scan is available
   */
  public boolean jumpToIndex(int index) {
    scanIndex = index - 1;
    return nextScan() != null;
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
      scanIndex++;
      Scan scan = getCurrentScan();
      loadScanData(scan);
      return scan;
    }
    return null;
  }

  /**
   * Load scan data
   *
   * @param scan target scan to load data from
   * @throws MissingMassListException if mass list values were accessed but not available. Run Mass
   *                                  detection first
   */
  protected void loadScanData(Scan scan) throws MissingMassListException {
    switch (type) {
      case RAW -> {
        try {
          scan.getMzValues(mzs);
          scan.getIntensityValues(intensities);
          currentNumberOfDataPoints = scan.getNumberOfDataPoints();
        } catch (NullPointerException e) {
          // in case mass detection is performed on an IMS raw data file imported from mzml,
          // no mz values have been set.
          Arrays.fill(mzs, 0d);
          Arrays.fill(intensities, 0d);
          currentNumberOfDataPoints = 0;
        }
      }
      case CENTROID -> {
        MassList masses = scan.getMassList();
        if (masses == null) {
          throw new MissingMassListException(scan);
        }
        masses.getMzValues(mzs);
        masses.getIntensityValues(intensities);
        currentNumberOfDataPoints = masses.getNumberOfDataPoints();
      }
    }
    assert currentNumberOfDataPoints <= mzs.length;
  }

  /**
   * The current list of scans has another element
   *
   * @return true if next scan is available. Also see {@link #getNumberOfScans()}
   */
  public boolean hasNextScan() {
    return scanIndex + 1 < getNumberOfScans();
  }

  /**
   * Number of selected scans
   *
   * @return the total number of scans for this data access
   */
  public abstract int getNumberOfScans();

  public void reset() {
    currentNumberOfDataPoints = -1;
    scanIndex = -1;
  }

  /**
   * Maximum number of data points is used to create the arrays that back the data
   *
   * @return the maximum number of data points
   */
  private int getMaxNumberOfDataPoints() {
    return switch (type) {
      case CENTROID -> dataFile.getMaxCentroidDataPoints();
      case RAW -> dataFile.getMaxRawDataPoints();
    };
  }

  // ###############################################
  // general MassSpectrum methods
  @Override
  public MassSpectrumType getSpectrumType() {
    return switch (type) {
      case RAW -> {
        Scan scan = getCurrentScan();
        yield scan == null ? null : scan.getSpectrumType();
      }
      case CENTROID -> MassSpectrumType.CENTROIDED;
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
    MassSpectrum masses = getCurrentDataSource();
    return masses == null ? null : masses.getBasePeakIndex();
  }

  @Nullable
  @Override
  public Range<Double> getDataPointMZRange() {
    MassSpectrum masses = getCurrentDataSource();
    return masses == null ? null : masses.getDataPointMZRange();
  }

  @Nullable
  @Override
  public Double getTIC() {
    MassSpectrum masses = getCurrentDataSource();
    return masses == null ? null : masses.getTIC();
  }

  /**
   * Depending on selected data type, scan or mass list
   *
   * @return scan or mass list
   */
  public MassSpectrum getCurrentDataSource() {
    return switch (type) {
      case RAW -> getCurrentScan();
      case CENTROID -> getMassList();
    };
  }

  @Override
  public @NotNull RawDataFile getDataFile() {
    return dataFile;
  }

  @Override
  public int getScanNumber() {
    Scan scan = getCurrentScan();
    return scan == null ? -1 : scan.getScanNumber();
  }

  @Override
  public @NotNull String getScanDefinition() {
    Scan scan = getCurrentScan();
    return scan == null ? "" : scan.getScanDefinition();
  }

  @Override
  public int getMSLevel() {
    Scan scan = getCurrentScan();
    return scan == null ? -1 : scan.getMSLevel();
  }

  @Override
  public float getRetentionTime() {
    Scan scan = getCurrentScan();
    return scan == null ? 0f : scan.getRetentionTime();
  }

  @Override
  public @Nullable Range<Double> getScanningMZRange() {
    Scan scan = getCurrentScan();
    return (scan == null || scan.isEmptyScan()) ? Range.singleton(0d) : scan.getScanningMZRange();
  }

  @Override
  public @Nullable MsMsInfo getMsMsInfo() {
    Scan scan = getCurrentScan();
    return scan == null ? null : scan.getMsMsInfo();
  }

  @Override
  public @NotNull PolarityType getPolarity() {
    Scan scan = getCurrentScan();
    return scan == null ? PolarityType.UNKNOWN : scan.getPolarity();
  }

  @Override
  public void addMassList(@NotNull MassList massList) {
    Scan scan = getCurrentScan();
    if (scan != null) {
      scan.addMassList(massList);
    }
  }

  @Override
  public double[] getMzValues(@NotNull double[] dst) {
    throw new UnsupportedOperationException(
        "The intended use of this class is to loop over all scans and data points");
  }

  @Override
  public double[] getIntensityValues(@NotNull double[] dst) {
    throw new UnsupportedOperationException(
        "The intended use of this class is to loop over all scans and data points");
  }

  @NotNull
  @Override
  public Iterator<DataPoint> iterator() {
    throw new UnsupportedOperationException(
        "The intended use of this class is to loop over all scans and data points");
  }

  @Override
  public @Nullable Float getInjectionTime() {
    Scan scan = getCurrentScan();
    return scan == null ? null : scan.getInjectionTime();
  }
}
