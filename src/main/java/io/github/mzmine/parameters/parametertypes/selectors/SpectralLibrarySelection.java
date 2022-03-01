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

package io.github.mzmine.parameters.parametertypes.selectors;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import java.io.File;
import java.util.List;

public class SpectralLibrarySelection {

  private final SpectralLibrarySelectionType selectionType;
  // only used when asked for specific libraries
  private final List<File> specificLibraryNames;

  public SpectralLibrarySelection() {
    this(SpectralLibrarySelectionType.ALL_IMPORTED, List.of());
  }

  public SpectralLibrarySelection(SpectralLibrarySelectionType selectionType,
      List<File> specificLibraryNames) {
    this.selectionType = selectionType;
    this.specificLibraryNames = specificLibraryNames;
  }

  public List<SpectralLibrary> getMatchingLibraries() {
    return switch (selectionType) {
      case ALL_IMPORTED -> MZmineCore.getProjectManager().getCurrentProject()
          .getCurrentSpectralLibraries().stream().toList();
      case SPECIFIC -> getMatchingSpecificFiles();
    };
  }

  public SpectralLibrarySelectionType getSelectionType() {
    return selectionType;
  }

  private List<SpectralLibrary> getMatchingSpecificFiles() {
    MZmineProject currentProject = MZmineCore.getProjectManager().getCurrentProject();
    if (currentProject == null || specificLibraryNames == null || specificLibraryNames.isEmpty()) {
      return List.of();
    }

    return currentProject.getCurrentSpectralLibraries().stream()
        .filter(lib -> specificLibraryNames.contains(lib.getPath())).toList();
  }

  public List<File> getSpecificLibraryNames() {
    return specificLibraryNames;
  }
}
