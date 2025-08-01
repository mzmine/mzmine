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

package io.github.mzmine.modules.io.import_spectral_library;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import io.github.mzmine.util.spectraldb.parser.AutoLibraryParser;
import io.github.mzmine.util.spectraldb.parser.UnsupportedFormatException;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class SpectralLibraryImportTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(SpectralLibraryImportTask.class.getName());

  private final MZmineProject project;
  private final File dataBaseFile;
  private AutoLibraryParser parser;

  public SpectralLibraryImportTask(MZmineProject project, File dataBaseFile,
      @NotNull Instant moduleCallDate) {
    super(MemoryMapStorage.forMassList(), moduleCallDate);
    this.project = project;
    this.dataBaseFile = dataBaseFile;
  }

  @Override
  public double getFinishedPercentage() {
    return parser == null ? 0 : parser.getProgress();
  }

  @Override
  public String getTaskDescription() {
    if (parser != null) {
      return "Import spectral library from %s (%d)".formatted(dataBaseFile,
          parser.getProcessedEntries());
    }
    return "Import spectral library from " + dataBaseFile;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    try {
      // will block until all library spectra are added to entries list
      SpectralLibrary library = parseFile(dataBaseFile);
      // remove empty or 0 intensity spectra
      library.removeif(this::checkRemoveEntry);
      library.trim(); // trim to save memory
      final List<SpectralLibraryEntry> entries = library.getEntries();
      if (!entries.isEmpty()) {
        project.addSpectralLibrary(library);

        logger.log(Level.INFO,
            () -> String.format("Library %s successfully added with %d entries", dataBaseFile,
                entries.size()));
      } else {
        logger.log(Level.WARNING, "Library was empty or there was an error while reading");
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE,
          "Could not read file %s. The file/path may not exist.".formatted(dataBaseFile), e);
      setStatus(TaskStatus.ERROR);
      setErrorMessage(e.toString());
      return;
    }
    setStatus(TaskStatus.FINISHED);
  }

  private boolean checkRemoveEntry(SpectralLibraryEntry entry) {
    if (entry.getNumberOfDataPoints() == 0) {
      return true;
    }
    Double basePeak = entry.getBasePeakIntensity();
    return basePeak == null || basePeak <= 0;
  }

  /**
   * Load all library entries from data base file
   *
   * @param dataBaseFile the target database file
   */
  private SpectralLibrary parseFile(File dataBaseFile)
      throws UnsupportedFormatException, IOException {
    //
    SpectralLibrary library = new SpectralLibrary(MemoryMapStorage.forMassList(), dataBaseFile);
    parser = new AutoLibraryParser(1000, (list, alreadyProcessed) -> library.addEntries(list));
    // return tasks
    parser.parse(this, dataBaseFile, library);
    return library;
  }

}
