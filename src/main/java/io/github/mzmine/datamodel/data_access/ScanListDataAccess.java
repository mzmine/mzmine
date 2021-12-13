/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.datamodel.data_access;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The intended use of this memory access is to loop over all scans and access data points via
 * {@link #getMzValue(int)} and {@link #getIntensityValue(int)}
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class ScanListDataAccess extends ScanDataAccess {

  protected final int totalScans;
  @NotNull
  private final Scan[] scans;

  /**
   * The intended use of this memory access is to loop over all scans and access data points via
   * {@link #getMzValue(int)} and {@link #getIntensityValue(int)}
   *
   * @param dataFile target data file to loop over all scans or mass lists
   * @param type     processed or raw data
   * @param scans    the list of scans
   */
  protected ScanListDataAccess(RawDataFile dataFile, ScanDataType type, @NotNull Scan[] scans) {
    super(dataFile, type);
    this.scans = scans;
    totalScans = scans.length;
  }

  /**
   * The intended use of this memory access is to loop over all scans and access data points via
   * {@link #getMzValue(int)} and {@link #getIntensityValue(int)}
   *
   * @param dataFile target data file to loop over all scans or mass lists
   * @param type     processed or raw data
   * @param scans    the list of scans
   */
  protected ScanListDataAccess(RawDataFile dataFile, ScanDataType type,
      @NotNull List<? extends Scan> scans) {
    super(dataFile, type);
    this.scans = scans.toArray(Scan[]::new);
    totalScans = scans.size();
  }

  @Override
  @Nullable
  public Scan getCurrentScan() {
    return scanIndex >= 0 && scanIndex < scans.length ? scans[scanIndex] : null;
  }

  @Override
  public int getNumberOfScans() {
    return totalScans;
  }

}
