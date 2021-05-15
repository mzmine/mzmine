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

package io.github.mzmine.datamodel;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.impl.masslist.MobilityScanMassList;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Mass spectrum acquired during an ion mobility experiment. Note that this class does not extend
 * {@link Scan} but just {@link MassSpectrum}.
 *
 * @author https://github.com/SteffenHeu
 */
public interface MobilityScan extends MassSpectrum, Scan {

  static final double DEFAULT_MOBILITY = -1.0d;

  @Nonnull
  RawDataFile getDataFile();

  /**
   * @return The mobility of this sub-spectrum. The unit will depend on the respective mass
   * spectrometer and can be checked via {@link MobilityScan#getMobilityType()}.
   */
  double getMobility();

  /**
   * See {@link MobilityType}
   *
   * @return The type of mobility acquired in this mass spectrum.
   */
  MobilityType getMobilityType();

  /**
   * @return THe frame this spectrum belongs to.
   */
  Frame getFrame();

  /**
   * @return The retention time of the frame when this spectrum was acquired.
   */
  float getRetentionTime();

  /**
   * @return The index of this mobility subscan.
   */
  int getMobilityScanNumber();

  @Nullable
  ImsMsMsInfo getMsMsInfo();

  @Nullable
  MassList getMassList();

  void setMassList(final @Nonnull MassList massList);

  @Override
  default int compareTo(@Nonnull Scan s) {
    int result = Integer.compare(this.getScanNumber(), s.getScanNumber());
    if (result != 0) {
      return result;
    }
    result = Float.compare(this.getRetentionTime(), s.getRetentionTime());
    if (result != 0 || !(s instanceof MobilityScan ms)) {
      return result;
    }

    return Integer.compare(this.getMobilityScanNumber(), ms.getMobilityScanNumber());
  }

  @Override
  default int getScanNumber() {
    return getFrame().getFrameId();
  }

  @Nonnull
  @Override
  default String getScanDefinition() {
    return getFrame().getScanDefinition() + " - Mobility scan #" + getMobilityScanNumber();
  }

  @Nonnull
  @Override
  default Range<Double> getScanningMZRange() {
    return getFrame().getScanningMZRange();
  }

  @Override
  default double getPrecursorMZ() {
    return getMsMsInfo() != null ? getMsMsInfo().getLargestPeakMz() : 0d;
  }

  @Nonnull
  @Override
  default PolarityType getPolarity() {
    return getFrame().getPolarity();
  }

  @Override
  default int getPrecursorCharge() {
    return getMsMsInfo() != null ? getMsMsInfo().getPrecursorCharge() : 0;
  }

  @Override
  default void addMassList(@Nonnull MassList massList) {
    if (!(massList instanceof MobilityScanMassList)) {
      throw new IllegalArgumentException(
          "Cannot mass lists of type " + massList.getClass().getName() + " to MobilityScan");
    }
    setMassList(massList);
  }

  @Override
  default int getMSLevel() {
    return getFrame().getMSLevel();
  }
}
