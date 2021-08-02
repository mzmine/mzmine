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

package io.github.mzmine.modules.visualization.networking.visual;

public enum EdgeAtt {

  SCORE, SIM_N, TYPE, LABEL, GNPS_SCORE, DIFF_SCORE, SIM_SCORE, DIFF_N, DELTA_MZ, NUMBER_OF_COLLAPSED_EDGES;

  @Override
  public String toString() {
    return super.toString().replaceAll("_", " ");
  }

  public boolean isNumber() {
    return switch (this) {
      case TYPE, LABEL -> false;
      case SCORE, SIM_N, GNPS_SCORE, DIFF_SCORE, SIM_SCORE, DIFF_N, DELTA_MZ, NUMBER_OF_COLLAPSED_EDGES -> true;
    };
  }
}
