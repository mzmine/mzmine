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

package io.github.mzmine.datamodel.impl.masslist;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.impl.AbstractStorableSpectrum;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.MemoryMapStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class represent detected masses (ions) in one mass spectrum
 */
public class SimpleMassList extends AbstractStorableSpectrum implements MassList {

  public SimpleMassList(@Nullable MemoryMapStorage storage, @NotNull double[] mzValues,
      @NotNull double[] intensityValues) {
    super(storage, mzValues, intensityValues);
  }

  /**
   * Use mzValues and intensityValues constructor
   *
   * @param storageMemoryMap
   * @param dps
   */
  @Deprecated
  public static MassList create(MemoryMapStorage storageMemoryMap, DataPoint[] dps) {
    double[][] mzIntensity = DataPointUtils.getDataPointsAsDoubleArray(dps);
    return new SimpleMassList(storageMemoryMap, mzIntensity[0], mzIntensity[1]);
  }

  @Override
  public DataPoint[] getDataPoints() {
    final double[][] mzIntensity = new double[2][];
    final int numDp = getNumberOfDataPoints();

    mzIntensity[0] = new double[numDp];
    mzIntensity[1] = new double[numDp];
    getMzValues(mzIntensity[0]);
    getIntensityValues(mzIntensity[1]);

    DataPoint[] dps = new DataPoint[numDp];
    for (int i = 0; i < numDp; i++) {
      dps[i] = new SimpleDataPoint(mzIntensity[0][i], mzIntensity[1][i]);
    }

    return dps;
  }
}
