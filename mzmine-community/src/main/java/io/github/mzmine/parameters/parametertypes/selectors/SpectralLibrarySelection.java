/*
 * Copyright (c) 2004-2024 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.parameters.parametertypes.selectors;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

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

  public SpectralLibrarySelection(@NotNull final List<SpectralLibrary> libraries) {
    this(SpectralLibrarySelectionType.SPECIFIC,
        libraries.stream().map(SpectralLibrary::getPath).toList());
  }

  public List<SpectralLibrary> getMatchingLibraries() {
    return switch (selectionType) {
      case ALL_IMPORTED ->
          ProjectService.getProjectManager().getCurrentProject().getCurrentSpectralLibraries()
              .stream().toList();
      case SPECIFIC -> getMatchingSpecificFiles();
    };
  }

  public SpectralLibrarySelectionType getSelectionType() {
    return selectionType;
  }

  private List<SpectralLibrary> getMatchingSpecificFiles() {
    MZmineProject currentProject = ProjectService.getProjectManager().getCurrentProject();
    if (currentProject == null || specificLibraryNames == null || specificLibraryNames.isEmpty()) {
      return List.of();
    }

    return currentProject.getCurrentSpectralLibraries().stream()
        .filter(lib -> specificLibraryNames.contains(lib.getPath())).toList();
  }

  /**
   * User should also check if the selection type is {@link SpectralLibrarySelectionType#SPECIFIC}
   *
   * @return list of libraries that were defined but not yet loaded
   */
  @NotNull
  public List<File> getMissingSpecificFiles() {
    if (specificLibraryNames == null || specificLibraryNames.isEmpty()) {
      return List.of();
    }
    MZmineProject currentProject = ProjectService.getProjectManager().getCurrentProject();
    if (currentProject == null) {
      return List.copyOf(specificLibraryNames);
    }

    Set<File> currentLibs = currentProject.getCurrentSpectralLibraries().stream()
        .map(SpectralLibrary::getPath).collect(Collectors.toSet());
    return specificLibraryNames.stream().filter(file -> !currentLibs.contains(file)).toList();
  }

  public List<File> getSpecificLibraryNames() {
    return specificLibraryNames;
  }
}
