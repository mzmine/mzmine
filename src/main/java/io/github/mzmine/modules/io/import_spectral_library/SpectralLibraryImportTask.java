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

package io.github.mzmine.modules.io.import_spectral_library;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.spectraldb.entry.SpectralDBEntry;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import io.github.mzmine.util.spectraldb.parser.AutoLibraryParser;
import io.github.mzmine.util.spectraldb.parser.UnsupportedFormatException;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class SpectralLibraryImportTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(SpectralLibraryImportTask.class.getName());

  private final MZmineProject project;
  private final File dataBaseFile;
  private final List<SpectralDBEntry> entries = new ArrayList<>();
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
    return "Import spectral library from " + dataBaseFile;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    try {
      // will block until all library spectra are added to entries list
      parseFile(dataBaseFile);
      if (entries.size() > 0) {
        project.addSpectralLibrary(new SpectralLibrary(dataBaseFile, entries));
        logger.log(Level.INFO, () -> String
            .format("Library %s successfully added with %d entries", dataBaseFile, entries.size()));
      } else {
        logger.log(Level.WARNING, "Library was empty or there was an error while reading");
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Could not read file " + dataBaseFile, e);
      setStatus(TaskStatus.ERROR);
      setErrorMessage(e.toString());
      return;
    }
    setStatus(TaskStatus.FINISHED);
  }

  /**
   * Load all library entries from data base file
   *
   * @param dataBaseFile the target database file
   */
  private void parseFile(File dataBaseFile) throws UnsupportedFormatException, IOException {
    //
    parser = new AutoLibraryParser(1000, (list, alreadyProcessed) -> entries.addAll(list));

    // return tasks
    parser.parse(this, dataBaseFile);
  }

}
