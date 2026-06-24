/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.io.spectraldbsubmit.row;

import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.io.spectraldbsubmit.batch.LibraryBatchGenerationSubParameters;
import io.github.mzmine.modules.io.spectraldbsubmit.batch.LibraryBatchGenerationTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.SpectralLibrarySelection;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Generates spectral library entries for the given feature list rows using the same logic as
 * {@link LibraryBatchGenerationTask} and adds the entries to the specified {@link SpectralLibrary}.
 * The library is then added to (or updated in) the current project so that the user can
 * subsequently export it.
 */
public class SendRowsToSpectralLibraryTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(
      SendRowsToSpectralLibraryTask.class.getName());

  private final List<ModularFeatureListRow> rows;
  private final SpectralLibrary targetLibrary;
  private final LibraryBatchGenerationSubParameters parameters;
  private final long totalRows;
  private final AtomicInteger finishedRows = new AtomicInteger(0);

  public SendRowsToSpectralLibraryTask(@NotNull final List<ModularFeatureListRow> rows,
      @NotNull final SpectralLibrary targetLibrary,
      @NotNull final LibraryBatchGenerationSubParameters parameters,
      @NotNull final Instant moduleCallDate) {
    super(null, moduleCallDate);
    this.rows = rows;
    this.targetLibrary = targetLibrary;
    this.parameters = parameters;
    this.totalRows = rows.size();
  }

  @Override
  public double getFinishedPercentage() {
    return totalRows == 0 ? 0 : (double) finishedRows.get() / totalRows;
  }

  @Override
  public String getTaskDescription() {
    return "Sending %d rows to spectral library '%s'".formatted(rows.size(),
        targetLibrary.getName());
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    // delegate entry creation to the batch task (secondary constructor, no file output)
    final LibraryBatchGenerationTask entryCreator = new LibraryBatchGenerationTask(parameters,
        targetLibrary.getName(), getModuleCallDate());

    // decision: start ENTRY_ID after the maximum existing integer ID in the library
    int nextId = computeNextEntryId();
    int addedEntries = 0;

    for (final ModularFeatureListRow row : rows) {
      if (isCanceled()) {
        return;
      }

      if (!row.isIdentified()) {
        finishedRows.getAndIncrement();
        continue;
      }

      final List<SpectralLibraryEntry> entries = entryCreator.processRow(row);
      for (final SpectralLibraryEntry entry : entries) {
        entry.putIfNotNull(DBEntryField.ENTRY_ID, String.valueOf(nextId++));
        targetLibrary.addEntry(entry);
        addedEntries++;
      }
      finishedRows.incrementAndGet();
    }

    // add/update the library in the current project so it is visible and exportable
    ProjectService.getProjectManager().getCurrentProject().addSpectralLibrary(targetLibrary);

    // make sure to track the last selected library in the config parameters
    saveLastSelectedLibraryToConfig();

    logger.info("Added %d new spectral library entries to library '%s'".formatted(addedEntries,
        targetLibrary.getName()));
    setStatus(TaskStatus.FINISHED);
  }

  private void saveLastSelectedLibraryToConfig() {
    final ParameterSet configParam = ConfigService.getConfiguration()
        .getModuleParameters(SendRowsToSpectralLibraryModule.class);
    SpectralLibrarySelection newSelection = new SpectralLibrarySelection(List.of(targetLibrary));
    configParam.setParameter(LibraryBatchGenerationSubParameters.lastLibrarySelection,
        newSelection);
  }

  /**
   * Finds the highest integer-parseable ENTRY_ID among all existing entries in the target library
   * and returns that value + 1. Non-numeric IDs are ignored. Returns 1 when the library is empty or
   * contains no numeric IDs.
   */
  private int computeNextEntryId() {
    return targetLibrary.getEntries().stream()
        .map(e -> e.getAsString(DBEntryField.ENTRY_ID).orElse("")).mapToInt(id -> {
          try {
            return Integer.parseInt(id.trim());
          } catch (NumberFormatException ex) {
            return 0;
          }
        }).max().orElse(0) + 1;
  }
}
