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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.datamodel.impl;

import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.MemoryMapStorage;
import javax.annotation.Nonnull;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.Scan;

/**
 * This class represent detected masses (ions) in one mass spectrum
 */
public class SimpleMassList extends MassList {
  private final Scan scan;

  public SimpleMassList(Scan scan, @Nonnull MemoryMapStorage storage, @Nonnull double[] mzValues,
      @Nonnull double[] intensityValues) {
    super(storage, mzValues, intensityValues);
    this.scan = scan;
  }

  /**
   * Use mzValues and intensityValues constructor
   * @param scan
   * @param storageMemoryMap
   * @param dps
   */
  @Deprecated
  public static MassList create(Scan scan, MemoryMapStorage storageMemoryMap, DataPoint[] dps) {
    double[][] mzIntensity = DataPointUtils.getDataPointsAsDoubleArray(dps);
    return new SimpleMassList(scan, storageMemoryMap, mzIntensity[0], mzIntensity[1]);
  }


  @Override
  public @Nonnull Scan getScan() {
    return scan;
  }

}
