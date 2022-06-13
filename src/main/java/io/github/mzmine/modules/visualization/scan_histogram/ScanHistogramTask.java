/*
 * Copyright 2006-2022 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.scan_histogram;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.scan_histogram.chart.ScanHistogramTab;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;

public class ScanHistogramTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(ScanHistogramTask.class.getName());
  private final RawDataFile[] dataFiles;
  private final ParameterSet parameters;
  private ScanHistogramTab tab;

  public ScanHistogramTask(RawDataFile[] dataFile, ParameterSet parameters,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    this.dataFiles = dataFile;
    this.parameters = parameters;
  }

  @Override
  public String getTaskDescription() {
    return "Creating scan histogram of " + Arrays.stream(dataFiles).map(Object::toString)
        .collect(Collectors.joining(","));
  }

  @Override
  public double getFinishedPercentage() {
    if (tab == null) {
      return 0;
    }
    int totalScans = tab.getTotalScans();
    if (totalScans == 0) {
      return 0;
    } else {
      return (double) tab.getProcessedScans() / totalScans;
    }
  }

  public RawDataFile[] getDataFiles() {
    return dataFiles;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    // create histogram dialog
    Platform.runLater(() -> {
      tab = new ScanHistogramTab("Scan histogram Visualizer", parameters, dataFiles);
      MZmineCore.getDesktop().addTab(tab);
    });

    setStatus(TaskStatus.FINISHED);
    logger.info("Finished creating scan histogram");
  }

}
