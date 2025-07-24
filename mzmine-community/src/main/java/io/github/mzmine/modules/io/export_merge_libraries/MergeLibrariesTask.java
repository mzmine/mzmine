/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.io.export_merge_libraries;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.modules.io.export_scans_modular.ExportScansFeatureTask;
import io.github.mzmine.modules.io.import_spectral_library.SpectralLibraryImportTask;
import io.github.mzmine.modules.io.spectraldbsubmit.batch.SpectralLibraryExportFormats;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.IntensityNormalizer;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.io.WriterOptions;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralDBEntry;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class MergeLibrariesTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(MergeLibrariesTask.class.getName());

  private final ParameterSet params;
  private final MZmineProject project;
  private final List<SpectralLibrary> libs;
  private final boolean removeAndImport;
  private final IdHandlingOption idHandling;
  private SpectralLibraryImportTask importTask;
  private long totalEntries = 0;
  private long exportedEntries = 0;

  protected MergeLibrariesTask(@NotNull Instant moduleCallDate, ParameterSet params,
      MZmineProject project) {
    super(moduleCallDate);
    this.params = params;
    this.project = project;

    libs = params.getValue(MergeLibrariesParameters.speclibs).getMatchingLibraries();
    totalEntries = libs.stream().mapToLong(SpectralLibrary::getNumEntries).sum();
    removeAndImport = params.getValue(MergeLibrariesParameters.removeAndImport);
    idHandling = params.getValue(MergeLibrariesParameters.idHandling);
  }

  @Override
  public String getTaskDescription() {
    return "Merging spectral libraries " + libs.stream().map(SpectralLibrary::getName)
        .collect(Collectors.joining(", "));
  }

  @Override
  public double getFinishedPercentage() {
    final double importProgress =
        removeAndImport ? (importTask != null ? importTask.getFinishedPercentage() : 0) : 1;
    return totalEntries == 0 ? 0
        : (exportedEntries / (double) totalEntries) * 0.5 + importProgress * 0.5;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    final SpectralLibraryExportFormats format = params.getValue(
        MergeLibrariesParameters.exportFormat);
    final File newFile = new File(
        FileAndPathUtil.getRealFileName(params.getValue(MergeLibrariesParameters.newLibraryFile),
            format.getExtension()));
    final IntensityNormalizer intensityNormalizer = params.getValue(
        MergeLibrariesParameters.normalizer);

    if (libs.isEmpty()) {
      error("No spectral libraries selected.");
      return;
    }

    if (!FileAndPathUtil.createDirectory(newFile.getParentFile())) {
      error("Cannot create directory %s.".formatted(newFile.getParentFile().getAbsolutePath()));
      return;
    }

    // maybe used for renumbering IDs
    final String libraryName = FileAndPathUtil.eraseFormat(newFile.getName());

    final AtomicLong entryId = new AtomicLong(0);

    // always add the used IDs to duplicate IDs so that other ids will never have the same value
    final Set<String> duplicateIds = getDuplicateIds(libs);

    try (var w = Files.newBufferedWriter(newFile.toPath(), WriterOptions.REPLACE.toOpenOption())) {
      for (final SpectralLibrary lib : libs) {
        for (SpectralLibraryEntry entry : lib.getEntries()) {

          final String currentId = entry.getOrElse(DBEntryField.ENTRY_ID, null);
          final boolean isDuplicate = currentId == null || duplicateIds.contains(currentId);

          // loop until a new ID is found that is not yet used
          String newEntryId = null;
          do {
            newEntryId = idHandling.getNewEntryId(libraryName, entry, isDuplicate,
                () -> entryId.incrementAndGet() + "_id"); // add suffix to not end with number
          } while (duplicateIds.contains(newEntryId));

          final SpectralDBEntry copy = new SpectralDBEntry((SpectralDBEntry) entry);
          duplicateIds.add(newEntryId); // add to duplicates to avoid another one
          copy.putIfNotNull(DBEntryField.ENTRY_ID, newEntryId);
          ExportScansFeatureTask.exportEntry(w, copy, format, intensityNormalizer);
          exportedEntries++;
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    if (removeAndImport) {
      FxThread.runLater(() -> ProjectService.getProject()
          .removeSpectralLibrary(libs.toArray(SpectralLibrary[]::new)));
      importTask = new SpectralLibraryImportTask(project, newFile, getModuleCallDate());
      importTask.run();
    }
    setStatus(TaskStatus.FINISHED);
  }

  private Set<String> getDuplicateIds(List<SpectralLibrary> libs) {

    // hash map supports null key
    final Map<String, Boolean> allIds = new HashMap<>();

    for (final SpectralLibrary lib : libs) {
      for (final SpectralLibraryEntry entry : lib.getEntries()) {
        final String id = entry.getAsString(DBEntryField.ENTRY_ID).orElse(null);
        if (allIds.containsKey(id)) {
          allIds.put(id, true);
        } else {
          allIds.put(id, false);
        }
      }
    }

    return allIds.entrySet().stream().filter(Entry::getValue).map(Entry::getKey)
        .filter(Objects::nonNull).collect(Collectors.toSet());
  }
}
