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

package io.github.mzmine.datamodel.impl;

import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.util.exceptions.MissingMassListException;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class MemoryEfficientDataAccess {

  public enum DataType {
    RAW, CENTROID;
  }

  protected final RawDataFile dataFile;
  protected final DataType type;
  private final Scan[] scans;

  // current data
  protected final double[] mzs;
  protected final double[] intensities;
  protected int currentScan = -1;
  protected int currentNumberOfDataPoints = -1;

  public MemoryEfficientDataAccess(RawDataFile dataFile,
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
   * Number of data points in the current scan depending of the defined DataType (RAW/CENTROID)
   * @return
   */
  public int getNumberOfDataPoints() {
    return currentNumberOfDataPoints;
  }

  /**
   * Get mass-to-charge ratio at index
   *
   * @param index
   * @return
   */
  public double getMz(int index) {
    assert index < getNumberOfDataPoints() && index >= 0;
    return mzs[index];
  }

  /**
   * Get intensity at index
   *
   * @param index
   * @return
   */
  public double getIntensity(int index) {
    assert index < getNumberOfDataPoints() && index >= 0;
    return intensities[index];
  }

  /**
   * Set the data to the next scan, if available
   *
   * @throws MissingMassListException if DataType.CENTROID is selected and mass list is missing in
   *                                  the current scan
   */
  public void nextScan() throws MissingMassListException {
    if (hasNextScan()) {
      currentScan++;
      switch (type) {
        case RAW:
          scans[currentScan].getMzValues(mzs);
          scans[currentScan].getIntensityValues(intensities);
          currentNumberOfDataPoints = scans[currentScan].getNumberOfDataPoints();
          break;
        case CENTROID:
          MassList masses = scans[currentScan].getMassList();
          if (masses == null) {
            throw new MissingMassListException(scans[currentScan]);
          }
          masses.getMzValues(mzs);
          masses.getIntensityValues(intensities);
          currentNumberOfDataPoints = masses.getNumberOfDataPoints();
          break;
      }
    }
  }

  /**
   * The current list of scans has another element
   * @return
   */
  public boolean hasNextScan() {
    return currentScan < scans.length;
  }

  /**
   * Maximum number of data points is used to create the arrays that back the data
   * @return
   */
  private int getMaxNumberOfDataPoints() {
    return switch (type) {
      case CENTROID -> dataFile.getMaxCentroidDataPoints();
      case RAW -> dataFile.getMaxRawDataPoints();
      default -> throw new IllegalStateException("Unexpected value: " + type);
    };
  }
}
