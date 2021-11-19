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

package io.github.mzmine.datamodel.featuredata;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.featuredata.impl.ModifiableSpectra;
import io.github.mzmine.datamodel.featuredata.impl.SummedIntensityMobilitySeries;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stores data points from ion mobility frames (summed across the mobility axis at one retention
 * time). Used as a tag to do instanceof checks with MsTimeSeries objects in the feature model.
 *
 * @author https://github.com/SteffenHeu
 */
public interface IonMobilogramTimeSeries extends IonTimeSeries<Frame>, ModifiableSpectra<Frame> {

  @Override
  default float getRetentionTime(int index) {
    return getSpectrum(index).getRetentionTime();
  }

  IonMobilogramTimeSeries subSeries(@Nullable MemoryMapStorage storage,
      @NotNull List<Frame> subset, @NotNull BinningMobilogramDataAccess mobilogramBinning);

  List<IonMobilitySeries> getMobilograms();

  default IonMobilitySeries getMobilogram(int index) {
    return getMobilograms().get(index);
  }

  /**
   * @param frame The frame
   * @return The {@link IonMobilitySeries} for the given frame, null if there is no series for the
   * given frame.
   */
  @Nullable
  default IonMobilitySeries getMobilogram(@Nullable final Frame frame) {
    final int index = getSpectra().indexOf(frame);
    return index != -1 ? getMobilogram(index) : null;
  }

  SummedIntensityMobilitySeries getSummedMobilogram();

  /**
   * Allows creation of a new {@link IonMobilogramTimeSeries} with processed {@link
   * SummedIntensityMobilitySeries}.
   *
   * @param storage
   * @param summedMobilogram
   * @return
   */
  IonMobilogramTimeSeries copyAndReplace(@Nullable MemoryMapStorage storage,
      @NotNull SummedIntensityMobilitySeries summedMobilogram);

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

  @Override
  IonMobilogramTimeSeries subSeries(@Nullable MemoryMapStorage storage,
      @NotNull List<Frame> subset);
}
