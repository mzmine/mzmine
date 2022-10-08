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

package io.github.mzmine.modules.dataprocessing.featdet_shoulderpeaksfilter;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.masslist.SimpleMassList;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.logging.Logger;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class ShoulderPeaksFilterTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());
  private RawDataFile dataFile;

  // scan counter
  private int processedScans = 0, totalScans;
  private ObservableList<Scan> scanNumbers;

  // User parameters
  private ParameterSet parameters;

  /**
   * @param dataFile
   * @param parameters
   * @param storage
   */
  public ShoulderPeaksFilterTask(RawDataFile dataFile, ParameterSet parameters,
      MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);
    this.dataFile = dataFile;
    this.parameters = parameters;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
   */
  public String getTaskDescription() {
    return "Filtering shoulder peaks in " + dataFile;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  public double getFinishedPercentage() {
    if (totalScans == 0)
      return 0;
    else
      return (double) processedScans / totalScans;
  }

  public RawDataFile getDataFile() {
    return dataFile;
  }

  /**
   * @see Runnable#run()
   */
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    logger.info("Started mass filter on " + dataFile);

    scanNumbers = dataFile.getScans();
    totalScans = scanNumbers.size();

    // Check if we have at least one scan with a mass list of given name
    boolean haveMassList = false;
    for (int i = 0; i < totalScans; i++) {
      Scan scan = scanNumbers.get(i);
      MassList massList = scan.getMassList();
      if (massList != null) {
        haveMassList = true;
        break;
      }
    }
    if (!haveMassList) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage(dataFile.getName() + " has no mass list");
      return;
    }

    // Process all scans
    for (int i = 0; i < totalScans; i++) {

      if (isCanceled())
        return;

      Scan scan = scanNumbers.get(i);

      MassList massList = scan.getMassList();

      // Skip those scans which do not have a mass list of given name
      if (massList == null) {
        processedScans++;
        continue;
      }

      DataPoint mzPeaks[] = massList.getDataPoints();

      DataPoint newMzPeaks[] = ShoulderPeaksFilter.filterMassValues(mzPeaks, parameters);

      MassList newMassList =
          SimpleMassList.create(storage, newMzPeaks);
      scan.addMassList(newMassList);

      processedScans++;
    }

    dataFile.getAppliedMethods().add(new SimpleFeatureListAppliedMethod(
        ShoulderPeaksFilterModule.class, parameters, getModuleCallDate()));

    setStatus(TaskStatus.FINISHED);

    logger.info("Finished shoulder peaks filter on " + dataFile);

  }

}
