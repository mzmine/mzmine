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

package io.github.mzmine.modules.dataprocessing.id_ccscalc;

import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.modules.io.import_bruker_tdf.TDFUtils;
import javax.annotation.Nonnull;

public class CCSUtils {

  /**
   * @return
   */
  public static Float calcCCS(double mz, @Nonnull Float mobility,
      @Nonnull MobilityType mobilityType, int charge) {
    switch (mobilityType) {
      case TIMS -> {
        return calcCCSFromTimsMobility(mobility.doubleValue(), charge, mz);
      }
      case DRIFT_TUBE -> {
        return null;
      }
      case TRAVELING_WAVE -> {
        return null;
      }
      case FAIMS -> {
        return null;
      }
      default -> {
        return null;
      }
    }
  }

  /**
   * Uses Bruker's library to calculate the ccs for a given tims mobility.
   *
   * @param mobility
   * @param charge
   * @param mz
   * @return
   */
  public static Float calcCCSFromTimsMobility(double mobility, int charge, double mz) {
    return TDFUtils.calculateCCS(mobility, charge, mz).floatValue();
  }
}
