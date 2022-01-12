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

package io.github.mzmine.modules.dataprocessing.featdet_massdetection;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.Scan;

/**
 * Specifies the type of scans the mass detection should be applied to in the mass detection setup
 * dialog.
 */
public enum SelectedScanTypes {
  SCANS("All scan types"), MOBLITY_SCANS("Mobility scans only"), FRAMES(
      "Frames only");

  private final String str;

  SelectedScanTypes(String str) {
    this.str = str;
  }

  @Override
  public String toString() {
    return str;
  }

  public boolean applyTo(Scan scan) {
    return switch (this) {
      case SCANS -> true;
      case FRAMES -> scan instanceof Frame;
      case MOBLITY_SCANS -> scan instanceof MobilityScan;
    };
  }
}
