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

package io.github.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.spectraldatabase;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
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

/**
 * Task to compare single spectra with spectral databases
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
class SpectraIdentificationSpectralDatabaseTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private final File dataBaseFile;

  private ParameterSet parameters;

  private Scan currentScan;
  private SpectraPlot spectraPlot;

  private List<SpectralMatchTask> tasks;

  private SpectraIdentificationResultsWindowFX resultWindow;

  private int totalTasks;

  SpectraIdentificationSpectralDatabaseTask(ParameterSet parameters, Scan currentScan,
      SpectraPlot spectraPlot) {

    this.parameters = parameters;
    dataBaseFile = parameters
        .getParameter(SpectraIdentificationSpectralDatabaseParameters.dataBaseFile).getValue();
    this.currentScan = currentScan;
    this.spectraPlot = spectraPlot;

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
    return "Spectral database identification of spectrum " + currentScan.getScanDefinition()
        + " using database " + dataBaseFile;
  }

  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    int count = 0;

    Platform.runLater(() -> {
      // add result frame
      resultWindow = new SpectraIdentificationResultsWindowFX();
//    resultWindow.setVisible(true);
      resultWindow.show();
    });

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
        return;
      }

    } catch (Exception e) {
      logger.log(Level.SEVERE, "Could not read file " + dataBaseFile, e);
      setStatus(TaskStatus.ERROR);
      setErrorMessage(e.toString());
    }
    logger.info("Added " + count + " spectral library matches");
    final int fcount = count;
    Platform.runLater(() -> {
      resultWindow
          .setTitle("Matched " + fcount + " compounds for scan#" + currentScan.getScanNumber());
      resultWindow.setMatchingFinished();
    });
//    resultWindow.revalidate();
//    resultWindow.repaint();

    setStatus(TaskStatus.FINISHED);

  }

  /**
   * Load all library entries from data base file
   *
   * @param dataBaseFile
   * @return
   */
  private List<SpectralMatchTask> parseFile(File dataBaseFile) {
    // one task for every 1000 entries
    List<SpectralMatchTask> tasks = new ArrayList<>();
    AutoLibraryParser parser = new AutoLibraryParser(1000, new LibraryEntryProcessor() {
      @Override
      public void processNextEntries(List<SpectralDBEntry> list, int alreadyProcessed) {
        // start last task
        SpectralMatchTask task = new SpectralMatchTask(parameters, alreadyProcessed + 1, list,
            spectraPlot, currentScan, resultWindow);
        MZmineCore.getTaskController().addTask(task);
        tasks.add(task);
      }
    });

    // return tasks
    try {
      // parse and create spectral matching tasks for batches of entries
      parser.parse(this, dataBaseFile);
      return tasks;
    } catch (UnsupportedFormatException | IOException e) {
      logger.log(Level.WARNING, "Library parsing error for file " + dataBaseFile.getAbsolutePath(),
          e);
      return new ArrayList<>();
    }
  }

  public SpectraIdentificationResultsWindowFX getResultWindow() {
    return resultWindow;
  }

  public void setResultWindow(SpectraIdentificationResultsWindowFX resultWindow) {
    this.resultWindow = resultWindow;
  }

}
