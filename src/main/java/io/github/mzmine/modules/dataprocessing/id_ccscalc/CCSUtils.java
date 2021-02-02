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
import java.util.logging.Logger;
import javax.annotation.Nonnull;

/**
 * @see CCSCalcModule
 */
public class CCSUtils {

  private static final Logger logger = Logger.getLogger(CCSUtils.class.getName());

  /**
   * @return
   */
  public static Float calcCCS(double mz, @Nonnull Float mobility,
      @Nonnull MobilityType mobilityType, int charge) {
    return switch (mobilityType) {
      case TIMS -> calcCCSFromTimsMobility(mobility.doubleValue(), charge, mz);
      case DRIFT_TUBE -> logUnsupportedMobilityUnit();
      case TRAVELING_WAVE -> logUnsupportedMobilityUnit();
      case FAIMS -> logUnsupportedMobilityUnit();
      default -> logUnsupportedMobilityUnit();
    };
  }

  /**
   * Uses Bruker's library to calculate the ccs for a given tims mobility.
   *
   * @param mobility
   * @param charge
   * @param mz
   * @return
   * @author https://github.com/SteffenHeu
   */
  public static Float calcCCSFromTimsMobility(double mobility, int charge, double mz) {
    return TDFUtils.calculateCCS(mobility, charge, mz).floatValue();
  }

  public static Float logUnsupportedMobilityUnit() {
    logger.fine("Unsupported mobility unit");
    return null;
  }
}
