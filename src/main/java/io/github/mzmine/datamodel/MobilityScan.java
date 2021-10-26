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

package io.github.mzmine.datamodel;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.msms.PasefMsMsInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Mass spectrum acquired during an ion mobility experiment. Note that this class does not extend
 * {@link Scan} but just {@link MassSpectrum}.
 *
 * @author https://github.com/SteffenHeu
 */
public interface MobilityScan extends MassSpectrum, Scan {

  static final double DEFAULT_MOBILITY = -1.0d;

  @NotNull RawDataFile getDataFile();

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

  @Nullable PasefMsMsInfo getMsMsInfo();

  @Nullable MassList getMassList();

  @Override
  default int compareTo(@NotNull Scan s) {
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

  /**
   * Returns the frame id. The mobility scan number can be accessed via {@link
   * MobilityScan#getMobilityScanNumber()}.
   *
   * @return The frame Id. {@link Frame#getScanNumber()}.
   */
  @Override
  default int getScanNumber() {
    return getFrame().getFrameId();
  }

  @NotNull
  @Override
  default String getScanDefinition() {
    return getFrame().getScanDefinition() + " - Mobility scan #" + getMobilityScanNumber();
  }

  @NotNull
  @Override
  default Range<Double> getScanningMZRange() {
    return getFrame().getScanningMZRange();
  }

  @NotNull
  @Override
  default PolarityType getPolarity() {
    return getFrame().getPolarity();
  }

  @Override
  default int getMSLevel() {
    return getFrame().getMSLevel();
  }

}
