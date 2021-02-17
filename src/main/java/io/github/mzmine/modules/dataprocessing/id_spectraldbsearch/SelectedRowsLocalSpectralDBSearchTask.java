/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.id_spectraldbsearch;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.main.MZmineCore;
//import io.github.mzmine.modules.visualization.featurelisttable.table.PeakListTable;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.modules.visualization.spectra.spectralmatchresults.SpectraIdentificationResultsWindowFX;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.spectraldb.entry.SpectralDBEntry;
import io.github.mzmine.util.spectraldb.parser.AutoLibraryParser;
import io.github.mzmine.util.spectraldb.parser.LibraryEntryProcessor;
import io.github.mzmine.util.spectraldb.parser.UnsupportedFormatException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javax.annotation.Nonnull;

public class SelectedRowsLocalSpectralDBSearchTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private final FeatureListRow[] peakListRows;
  private final File dataBaseFile;

  private ParameterSet parameters;

  private List<RowsSpectralMatchTask> tasks;

  private SpectraIdentificationResultsWindowFX resultWindow;

  private FeatureTableFX table;

  private int totalTasks;

  public SelectedRowsLocalSpectralDBSearchTask(FeatureListRow[] peakListRows, FeatureTableFX table,
      ParameterSet parameters) {
    this.peakListRows = peakListRows;
    this.parameters = parameters;
    this.table = table;
    dataBaseFile = parameters.getParameter(LocalSpectralDBSearchParameters.dataBaseFile).getValue();
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (totalTasks == 0 || tasks == null) {
      return 0;
    }
    return ((double) totalTasks - tasks.size()) / totalTasks;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return "Spectral database identification of " + peakListRows.length
        + " feature lists using database " + dataBaseFile;
  }

  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    int count = 0;

    if (peakListRows.length == 1) {
      // add result frame
      Platform.runLater(() -> {
        resultWindow = new SpectraIdentificationResultsWindowFX();
        resultWindow.show();
      });
    } else {
      resultWindow = null;
    }

    try {
      tasks = parseFile(dataBaseFile);
      totalTasks = tasks.size();
      if (!tasks.isEmpty()) {
        // wait for the tasks to finish
        while (!isCanceled() && !tasks.isEmpty()) {
          for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).isFinished() || tasks.get(i).isCanceled()) {
              count += tasks.get(i).getCount();
              tasks.remove(i);
              i--;
            }
          }
          // wait for all sub tasks to finish
          try {
            Thread.sleep(100);
          } catch (Exception e) {
            cancel();
          }
        }
        // cancelled
        if (isCanceled()) {
          tasks.stream().forEach(AbstractTask::cancel);
        }
      } else {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("DB file was empty - or error while parsing " + dataBaseFile);
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Could not read file " + dataBaseFile, e);
      setStatus(TaskStatus.ERROR);
      setErrorMessage(e.toString());
    }
    logger.info("Added " + count + " spectral library matches");
    if (resultWindow != null) {
      resultWindow
          .setTitle("Matched " + count + " compounds for feature list row: " + peakListRows[0]);
      resultWindow.setMatchingFinished();
    }

    // work around to update feature list identities
    /* TODO:
    if (table.getRowCount() > 0) {
      table.setRowSelectionInterval(0, 0);
    }
    */
    setStatus(TaskStatus.FINISHED);

  }

  /**
   * Load all library entries from data base file
   *
   * @param dataBaseFile
   * @return
   */
  private List<RowsSpectralMatchTask> parseFile(File dataBaseFile)
      throws UnsupportedFormatException, IOException {
    //
    List<RowsSpectralMatchTask> tasks = new ArrayList<>();
    AutoLibraryParser parser = new AutoLibraryParser(100, new LibraryEntryProcessor() {
      @Override
      public void processNextEntries(List<SpectralDBEntry> list, int alreadyProcessed) {
        // start last task
        RowsSpectralMatchTask task = new RowsSpectralMatchTask(peakListRows.length + " rows",
            peakListRows, parameters, alreadyProcessed + 1, list, (match) -> {
          // one selected row -> show in dialog
          if (resultWindow != null) {
            Platform.runLater(() -> resultWindow.addMatches(match));
          }
        });
        MZmineCore.getTaskController().addTask(task);
        tasks.add(task);
      }
    });

    // return tasks
    parser.parse(this, dataBaseFile);
    return tasks;
  }

}
