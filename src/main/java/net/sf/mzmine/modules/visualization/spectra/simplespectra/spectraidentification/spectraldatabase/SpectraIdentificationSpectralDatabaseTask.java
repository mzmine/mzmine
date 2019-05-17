/*
 * Copyright 2006-2019 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.spectraldatabase;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.impl.HeadLessDesktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.spectraldb.entry.SpectralDBEntry;
import net.sf.mzmine.util.spectraldb.parser.AutoLibraryParser;
import net.sf.mzmine.util.spectraldb.parser.LibraryEntryProcessor;
import net.sf.mzmine.util.spectraldb.parser.UnsupportedFormatException;

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

  private SpectraIdentificationResultsWindow resultWindow;

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
   * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (totalTasks == 0 || tasks == null)
      return 0;
    return ((double) totalTasks - tasks.size()) / totalTasks;
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
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

    // add result frame
    resultWindow = new SpectraIdentificationResultsWindow();
    resultWindow.setVisible(true);

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
    resultWindow.setTitle("Matched " + count + " compunds for scan#" + currentScan.getScanNumber());
    resultWindow.revalidate();
    resultWindow.repaint();
    // Repaint the window
    Desktop desktop = MZmineCore.getDesktop();
    if (!(desktop instanceof HeadLessDesktop))
      desktop.getMainWindow().repaint();

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
      if (parser.parse(this, dataBaseFile))
        return tasks;
      else
        return new ArrayList<>();
    } catch (UnsupportedFormatException | IOException e) {
      logger.log(Level.WARNING, "Library parsing error for file " + dataBaseFile.getAbsolutePath(),
          e);
      return new ArrayList<>();
    }
  }

  public SpectraIdentificationResultsWindow getResultWindow() {
    return resultWindow;
  }

  public void setResultWindow(SpectraIdentificationResultsWindow resultWindow) {
    this.resultWindow = resultWindow;
  }

}
