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

package io.github.mzmine.modules.visualization.mzhistogram;

import io.github.mzmine.main.MZmineCore;
import java.time.Instant;
import java.util.Date;
import java.util.logging.Logger;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.visualization.mzhistogram.chart.HistogramTab;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;

public class ScanMzHistogramTask extends AbstractTask {
  private Logger logger = Logger.getLogger(this.getClass().getName());

  private MZmineProject project;
  private RawDataFile dataFile;

  private HistogramTab tab;

  private ParameterSet parameters;

  /**
   * @param dataFile
   * @param parameters
   */
  public ScanMzHistogramTask(MZmineProject project, RawDataFile dataFile,
      ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    this.project = project;
    this.dataFile = dataFile;
    this.parameters = parameters;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return "Creating m/z distribution histogram of " + dataFile;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if(tab == null) {
      return 0;
    }
    int totalScans = tab.getTotalScans();
    if (totalScans == 0)
      return 0;
    else
      return (double) tab.getProcessedScans() / totalScans;
  }

  public RawDataFile getDataFile() {
    return dataFile;
  }

  /**
   * @see Runnable#run()
   */
  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    // create histogram dialog
    Platform.runLater(() -> {
      tab = new HistogramTab(dataFile, "m/z scan histogram Visualizer", "m/z",
          parameters);
      MZmineCore.getDesktop().addTab(tab);
    });

    setStatus(TaskStatus.FINISHED);
    logger.info("Finished mz distribution histogram on " + dataFile);
  }

}
