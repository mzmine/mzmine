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
import io.github.mzmine.gui.Desktop;
import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.gui.MZmineDesktop;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_spectral_library.SpectralLibraryImportModule;
import io.github.mzmine.modules.io.import_spectral_library.SpectralLibraryImportTask;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskService;
import io.github.mzmine.taskcontrol.threadpools.FixedThreadPoolTask;
import io.github.mzmine.util.StringUtils;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
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
      case AS_SELECTED_IN_MAIN_WINDOW -> {
        final Desktop desktop = DesktopService.getDesktop();
        if(desktop instanceof MZmineDesktop desk) {
          yield List.of(desk.getSelectedSpectralLibraries());
        }
        yield List.of();
      }
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

  /**
   * Checks if all spectral libraries are available, otherwise ask for import, finally return
   * selected list of libraries.
   *
   * @return all selected spectral libraries
   * @throws SpectralLibrarySelectionException when specific libraries are provided but are not
   *                                           imported (GUI: ask to import, CLI: throw exception).
   */
  public List<SpectralLibrary> getMatchingLibrariesAndCheckAvailability()
      throws SpectralLibrarySelectionException {
    if (this.getSelectionType() == SpectralLibrarySelectionType.SPECIFIC) {
      List<File> missing = this.getMissingSpecificFiles();
      if (!missing.isEmpty()) {
        if (DesktopService.isGUI() && DialogLoggerUtil.showDialogYesNo(
            "Import missing spectral libraries?", """
                Some library files were not imported before spectral library matching - should mzmine import them now?
                It is recommended to import spectral libraries during the initial data import with \
                the %s or %s modules. Then set this library selection to use all imported libraries.
                Missing library files specifically defined:
                %s""".formatted(AllSpectralDataImportModule.MODULE_NAME,
                SpectralLibraryImportModule.MODULE_NAME,
                StringUtils.join(missing, "\n", File::getAbsolutePath)))) {
          // user wants libraries to be imported
          Instant now = Instant.now();
          List<Task> tasks = missing.stream().map(
                  file -> (Task) new SpectralLibraryImportTask(ProjectService.getProject(), file, now))
              .toList();
          FixedThreadPoolTask masterImportTask = new FixedThreadPoolTask(
              "Import missing spectral libraries", tasks.size(), tasks);
          // block until finished import
          TaskService.getController().runTaskOnThisThreadBlocking(masterImportTask);
          // call this method to redo checks
          return getMatchingLibrariesAndCheckAvailability();
        } else {
          throw new SpectralLibrarySelectionException();
        }
      }
    }
    return getMatchingLibraries();
  }


  /**
   * Checks if all spectral libraries are available, otherwise ask for import, finally return
   * selected list of libraries. Combines all spectral libraries
   *
   * @return all selected spectral libraries
   * @throws SpectralLibrarySelectionException when specific libraries are provided but are not
   *                                           imported (GUI: ask to import, CLI: throw exception).
   */
  public @NotNull List<SpectralLibraryEntry> getMatchingLibraryEntriesAndCheckAvailability()
      throws SpectralLibrarySelectionException {
    List<SpectralLibrary> libraries = getMatchingLibrariesAndCheckAvailability();
    if (libraries.isEmpty()) {
      throw SpectralLibrarySelectionException.forNoLibraries();
    }

    List<SpectralLibraryEntry> entries = new ArrayList<>();
    for (var lib : libraries) {
      entries.addAll(lib.getEntries());
    }

    if (entries.isEmpty()) {
      throw SpectralLibrarySelectionException.forEmptyLibraries(libraries);
    }
    return entries;
  }


  public List<File> getSpecificLibraryNames() {
    return specificLibraryNames;
  }
}
