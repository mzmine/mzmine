/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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
