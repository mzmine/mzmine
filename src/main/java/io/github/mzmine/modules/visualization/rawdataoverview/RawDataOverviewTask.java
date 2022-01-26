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

package io.github.mzmine.modules.visualization.rawdataoverview;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.Arrays;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;

/*
 * Raw data overview task class
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster)
 */
public class RawDataOverviewTask extends AbstractTask {

  private RawDataFile rawDataFiles[];

  private int totalSteps = 0;
  private int appliedSteps = 0;

  public RawDataOverviewTask(ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null

    this.rawDataFiles = parameters.getParameter(RawDataOverviewParameters.rawDataFiles).getValue()
        .getMatchingRawDataFiles();

    this.totalSteps = rawDataFiles.length;
  }

  @Override
  public String getTaskDescription() {
    return "Create raw data overview for " + rawDataFiles.length + " raw data files";
  }

  @Override
  public double getFinishedPercentage() {
    return totalSteps == 0 ? 0 : (double) appliedSteps / totalSteps;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    if (isCanceled()) {
      return;
    }

    RawDataOverviewPane rdop = new RawDataOverviewPane(true, false);
    MZmineCore.getDesktop().addTab(rdop);
    Platform.runLater(() -> rdop.onRawDataFileSelectionChanged(Arrays.asList(rawDataFiles)));

    setStatus(TaskStatus.FINISHED);
  }
}
