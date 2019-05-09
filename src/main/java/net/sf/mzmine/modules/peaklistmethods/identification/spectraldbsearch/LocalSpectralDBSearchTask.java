/*
 * Copyright 2006-2018 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.identification.spectraldbsearch;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.impl.HeadLessDesktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.spectraldbsearch.parser.GnpsJsonParser;
import net.sf.mzmine.modules.peaklistmethods.identification.spectraldbsearch.parser.MonaJsonParser;
import net.sf.mzmine.modules.peaklistmethods.identification.spectraldbsearch.parser.NistMspParser;
import net.sf.mzmine.modules.peaklistmethods.identification.spectraldbsearch.parser.SpectralDBParser;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.files.FileTypeFilter;

class LocalSpectralDBSearchTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private static final String METHOD = "MS/MS spectral DB search";
  private static final int MAX_ERROR = 3;
  private int errorCounter = 0;
  private final PeakList peakList;
  private final @Nonnull String massListName;
  private final File dataBaseFile;
  private final MZTolerance mzTolerance;
  private final RTTolerance rtTolerance;
  private final boolean useRT;
  private int finishedRows = 0;
  private final int totalRows;

  private ParameterSet parameters;

  private final double noiseLevel;
  private final double minSimilarity;
  private final int minMatch;

  private List<SpectralMatchTask> tasks;

  private int totalTasks;

  LocalSpectralDBSearchTask(PeakList peakList, ParameterSet parameters) {
    this.peakList = peakList;
    this.parameters = parameters;
    dataBaseFile = parameters.getParameter(LocalSpectralDBSearchParameters.dataBaseFile).getValue();
    massListName = parameters.getParameter(LocalSpectralDBSearchParameters.massList).getValue();
    mzTolerance = parameters.getParameter(LocalSpectralDBSearchParameters.mzTolerance).getValue();
    noiseLevel = parameters.getParameter(LocalSpectralDBSearchParameters.noiseLevelMS2).getValue();

    useRT = parameters.getParameter(LocalSpectralDBSearchParameters.rtTolerance).getValue();
    rtTolerance = parameters.getParameter(LocalSpectralDBSearchParameters.rtTolerance)
        .getEmbeddedParameter().getValue();

    minMatch = parameters.getParameter(LocalSpectralDBSearchParameters.minMatch).getValue();
    minSimilarity = parameters.getParameter(LocalSpectralDBSearchParameters.minCosine).getValue();
    totalRows = peakList.getNumberOfRows();
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
    return "MS/MS spectral database identification of " + peakList + " using database "
        + dataBaseFile;
  }

  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    int count = 0;
    // try {
    tasks = parseFile(dataBaseFile);
    totalTasks = tasks.size();
    // if (!tasks.isEmpty()) {
    // wait for the tasks to finish
    while (!isCanceled() && !tasks.isEmpty()) {
      for (int i = 0; i < tasks.size(); i++) {
        if (tasks.get(i).isFinished() || tasks.get(i).isCanceled()) {
          count += tasks.get(i).getCount();
          tasks.remove(i);
          i--;
        }
      }
      try {
        Thread.sleep(100);
      } catch (Exception e) {
        cancel();
      }
    }
    // } else {
    // setStatus(TaskStatus.ERROR);
    // setErrorMessage("DB file was empty - or error while parsing " + dataBaseFile);
    // throw new MSDKRuntimeException("DB file was empty - or error while parsing " + dataBaseFile);
    // }
    // } catch (Exception e) {
    // logger.log(Level.SEVERE, "Could not read file " + dataBaseFile, e);
    // setStatus(TaskStatus.ERROR);
    // setErrorMessage(e.toString());
    // throw new MSDKRuntimeException(e);
    // }
    logger.info("Added " + count + " spectral library matches");

    // Add task description to peakList
    peakList.addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
        "Peak identification using MS/MS spectral database " + dataBaseFile, parameters));

    // Repaint the window to reflect the change in the peak list
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
    FileTypeFilter json = new FileTypeFilter("json", "");
    FileTypeFilter msp = new FileTypeFilter("msp", "");
    if (json.accept(dataBaseFile)) {
      // test Gnps and MONA json parser
      SpectralDBParser[] parser =
          new SpectralDBParser[] {new GnpsJsonParser(), new MonaJsonParser()};
      for (SpectralDBParser p : parser) {
        try {
          List<SpectralMatchTask> list = p.parse(this, peakList, parameters, dataBaseFile);
          if (!list.isEmpty())
            return list;
        } catch (Exception ex) {
        }
      }
    } else if (msp.accept(dataBaseFile)) {
      // load NIST msp format
      NistMspParser parser = new NistMspParser();
      try {
        List<SpectralMatchTask> list = parser.parse(this, peakList, parameters, dataBaseFile);
        if (!list.isEmpty())
          return list;
      } catch (Exception ex) {
      }
    } else {
      logger.log(Level.WARNING, "Unsupported file format: " + dataBaseFile.getAbsolutePath());
    }
    return new ArrayList<>();
  }

}
