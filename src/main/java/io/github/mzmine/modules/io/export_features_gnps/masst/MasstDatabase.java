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

package io.github.mzmine.modules.io.export_features_gnps.masst;

/**
 * Available databases to search by masst. Selection of the database in GNPS is with the enum value
 * names - not toString.
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public enum MasstDatabase {
  // value names equal the GNPS values
  ALL("ALL (Non-redundant MS/MS)"), GNPS("GNPS"), METABOLOMICSWORKBENCH(
      "Metabolomics Workbench"), METABOLIGHTS("Metabolights");

  private final String title;

  MasstDatabase(String title) {
    this.title = title;
  }

  public String getTitle() {
    return title;
  }

  @Override
  public String toString() {
    return getTitle();
  }

  public String getGnpsValue() {
    return name();
  }
}
