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

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.impl.SummedIntensityMobilitySeries;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.List;

/**
 * Stores data points from ion mobility frames (summed across the mobility axis at one retention
 * time). Used as a tag to do instanceof checks with MsTimeSeries objects in the feature model.
 *
 * @author https://github.com/SteffenHeu
 */
public interface IonMobilogramTimeSeries extends IonTimeSeries<Frame> {

  @Override
  default float getRetentionTime(int index) {
    return getSpectrum(index).getRetentionTime();
  }

  List<SimpleIonMobilitySeries> getMobilograms();

  default SimpleIonMobilitySeries getMobilogram(int index) {
    return getMobilograms().get(index);
  }

  SummedIntensityMobilitySeries getSummedMobilogram();

  /**
   * Creates a copy of this series using the same frame list, but with new mz/intensities and new
   * mobilograms, e.g. after smoothing.
   *
   * @param storage
   * @param newMzValues
   * @param newIntensityValues
   * @param newMobilograms
   * @return
   */
  IonMobilogramTimeSeries copyAndReplace(MemoryMapStorage storage, double[] newMzValues,
      double[] newIntensityValues, List<SimpleIonMobilitySeries> newMobilograms);

  /**
   * @param scan
   * @return The intensity value for the given scan or 0 if the no intensity was measured at that
   * scan.
   */
  @Override
  default double getIntensityForSpectrum(Frame scan) {
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
  default double getMzForSpectrum(Frame scan) {
    int index = getSpectra().indexOf(scan);
    if (index != -1) {
      return getMZ(index);
    }
    return 0;
  }
}
