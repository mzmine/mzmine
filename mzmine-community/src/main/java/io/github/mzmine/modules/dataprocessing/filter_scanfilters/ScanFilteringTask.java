/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.filter_scanfilters;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.IOException;
import java.time.Instant;
import java.util.logging.Logger;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class ScanFilteringTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private final MZmineProject project;
  private RawDataFile dataFile, newFile;

  // scan counter
  private int processedScans = 0, totalScans;
  private ObservableList<Scan> scanNumbers;

  // User parameters
  private String suffix;
  private boolean removeOriginal;

  // Raw Data Filter
  private ScanFilter rawDataFilter;

  private ScanSelection select;
  private ParameterSet parameters;

  /**
   * @param dataFile
   * @param parameters
   * @param storage
   */
  ScanFilteringTask(MZmineProject project, RawDataFile dataFile, ParameterSet parameters,
      @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    this.project = project;
    this.dataFile = dataFile;

    var filterParam = parameters.getParameter(ScanFiltersParameters.filter)
        .getValueWithParameters();
    rawDataFilter = ScanFilters.createFilter(filterParam);

    suffix = parameters.getParameter(ScanFiltersParameters.suffix).getValue();
    select = parameters.getParameter(ScanFiltersParameters.scanSelect).getValue();

    this.parameters = parameters;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return "Filtering scans in " + dataFile;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (totalScans == 0) {
      return 0;
    } else {
      return (double) processedScans / totalScans;
    }
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

    logger.info("Started filtering scans on " + dataFile);

    scanNumbers = dataFile.getScans();
    totalScans = scanNumbers.size();

    try {

      // Create new raw data file

      String newName = dataFile.getName() + " " + suffix;
      newFile = MZmineCore.createNewFile(newName, null, getMemoryMapStorage());

      for (int i = 0; i < totalScans; i++) {

        if (isCanceled()) {
          return;
        }

        Scan scan = scanNumbers.get(i);
        Scan newScan = null;
        if (select.matches(scan)) {
          newScan = rawDataFilter.filterScan(newFile, scan);
        } else {
          newScan = scan; // TODO need to create a copy of the scan
        }

        if (newScan != null) {
          newFile.addScan(newScan);
        }

        processedScans++;
      }

      // Finalize writing
      try {
        for (FeatureListAppliedMethod appliedMethod : dataFile.getAppliedMethods()) {
          newFile.getAppliedMethods().add(appliedMethod);
        }
        newFile.getAppliedMethods().add(
            new SimpleFeatureListAppliedMethod(ScanFiltersModule.class, parameters,
                getModuleCallDate()));

        project.addFile(newFile);

        // Remove the original file if requested
        if (removeOriginal) {
          project.removeFile(dataFile);
        }
      } catch (Exception exception) {
        exception.printStackTrace();
      }

      setStatus(TaskStatus.FINISHED);
      logger.info("Finished scan filter on " + dataFile);

    } catch (IOException e) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage(e.toString());
      return;
    }
  }

}
