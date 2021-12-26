/*
 * Copyright 2006-2022 The MZmine Development Team
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
 *
 */

package io.github.mzmine.modules.dataprocessing.id_ccscalc;

import org.apache.commons.math3.stat.regression.SimpleRegression;

public class CCSCalibration {

  SimpleRegression calibration;

  public float getCCS(double mz, float mobility, int charge) {
    // t_D = ß * gamma * ccs + t_fix
    // ccs = (t_D - t_fix) / ß * gamma

    return (float) ((calibration.getIntercept() - mobility) / (calibration.getSlope() * getN2Gamma(
        mz, charge)));
  }

  private double getN2Gamma(double mz, int charge) {
    return 1 / (double) charge * Math.sqrt(mz * charge / (mz * charge + CCSCalibrant.n2));
  }
}
