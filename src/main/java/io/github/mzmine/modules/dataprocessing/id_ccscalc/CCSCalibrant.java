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

import io.github.mzmine.datamodel.features.FeatureListRow;
import org.jetbrains.annotations.NotNull;

public record CCSCalibrant(@NotNull FeatureListRow row, double libraryMass, float libraryMobility,
                           float libraryCCS, int libraryCharge) {

  static final double n2 = 28.006148008;

  @Override
  public String toString() {
    return String.format("Found: m/z %.4f mobility %.4f - Expected: mz %4f mobility %.4f ccs %.2f",
        row.getAverageMZ(), row.getAverageMobility(), libraryMass(), libraryMobility(),
        libraryCCS());
  }

  public double getN2Gamma() {
    return 1 / (double) libraryCharge() * Math.sqrt(
        libraryMass * libraryCharge / (libraryMass * libraryCharge + n2));
  }
}
