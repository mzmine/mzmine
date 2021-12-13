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

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * The intended use of this memory access is to loop over all scans and access data points via
 * {@link #getMzValue(int)} and {@link #getIntensityValue(int)}
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class FilteredScanDataAccess extends ScanDataAccess {

  @NotNull
  protected final ScanSelection selection;
  protected final List<Integer> filteredScanIndexesInFile;
  protected final int totalScans;

  /**
   * The intended use of this memory access is to loop over all scans and access data points via
   * {@link #getMzValue(int)} and {@link #getIntensityValue(int)}
   *
   * @param dataFile  target data file to loop over all scans or mass lists
   * @param type      processed or raw data
   * @param selection processed or raw data
   */
  protected FilteredScanDataAccess(RawDataFile dataFile,
      ScanDataType type, @NotNull ScanSelection selection) {
    super(dataFile, type);
    this.selection = selection;
    // list of filtered indexes
    filteredScanIndexesInFile = new ArrayList<>();
    // count matching scans
    for (int i = 0; i < dataFile.getNumOfScans(); i++) {
      if (selection.matches(dataFile.getScan(i))) {
        filteredScanIndexesInFile.add(i);
      }
    }
    totalScans = filteredScanIndexesInFile.size();
  }

  public Scan getCurrentScan() {
    return scanIndex >= 0 && scanIndex < totalScans ?
        dataFile.getScan(filteredScanIndexesInFile.get(scanIndex)) : null;
  }

  @Override
  public int getNumberOfScans() {
    return totalScans;
  }

}
