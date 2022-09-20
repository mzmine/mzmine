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
 */

package io.github.mzmine.modules.visualization.scan_histogram;

/**
 * The data types that can be displayed in a scan histogram
 */
public enum ScanHistogramType {
  MZ, INTENSITY, INTENSITY_RECAL, MASS_DEFECT;

  @Override
  public String toString() {
    return switch (this) {
      case MZ -> "m/z";
      case INTENSITY -> "Intensity";
      case INTENSITY_RECAL -> "Intensity (noise recalibrated)";
      case MASS_DEFECT -> "Mass defect";
    };
  }
}
