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

package io.github.mzmine.datamodel.featuredata;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.List;

/**
 * Used to store a consecutive number of data points (mz and intensity values).
 *
 * @param <T>
 * @author https://github.com/SteffenHeu
 */
public interface IonTimeSeries<T extends Scan> extends IonSpectrumSeries<T>, TimeSeries {

  /**
   * @param scan
   * @return The intensity value for the given scan or 0 if the no intensity was measured at that
   * scan.
   */
  @Override
  default double getIntensityForSpectrum(Scan scan) {
    int index = getSpectra().indexOf(scan);
    if (index != -1) {
      return getIntensity(index);
    }
    return 0;
  }

  /**
   * @param scan
   * @return The mz for the given scan or 0 if no intensity was measured at that scan.
   */
  @Override
  default double getMzForSpectrum(Scan scan) {
    int index = getSpectra().indexOf(scan);
    if (index != -1) {
      return getMZ(index);
    }
    return 0;
  }

  IonTimeSeries<T> copyAndReplace(MemoryMapStorage storage, double[] newMzValues,
      double[] newIntensityValues, List<T> newScans);
}
