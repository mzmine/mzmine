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
