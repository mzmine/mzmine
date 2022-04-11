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

package io.github.mzmine.modules.io.projectsave;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public enum ProjectSaveOption {
  STANDALONE("Standalone (large/flexible)", "Large flexible format that contains the raw data"), //
  REFERENCING("Referencing (small)",
      "Smaller format that points to the raw data files in their original path. "
      + "Project might be corrupted by removing, renaming, or moving files.");

  public final String name;
  public final String description;

  ProjectSaveOption(String name, String description) {
    this.name = name;
    this.description = description;
  }

  @Override
  public String toString() {
    return name;
  }

  public String getDescription() {
    return description;
  }
}
