/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.visualization.mzhistogram;

import java.util.Arrays;
import java.util.logging.Logger;
import com.google.common.collect.Range;
import io.github.msdk.MSDKRuntimeException;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.modules.visualization.mzhistogram.chart.EHistogramDialog;
import net.sf.mzmine.modules.visualization.mzhistogram.chart.HistogramData;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

public class MZDistributionHistoTask extends AbstractTask {
  private Logger logger = Logger.getLogger(this.getClass().getName());

  private MZmineProject project;
  private RawDataFile dataFile;

  // scan counter
  private int processedScans = 0, totalScans;
  private ScanSelection scanSelection;
  private Scan[] scans;

  // User parameters
  private String suffix, massListName;
  private Range<Double> mzRange;
  private boolean useRTRange;
  private Range<Double> rtRange;
  private double binWidth;

  /**
   * @param dataFile
   * @param parameters
   */
  public MZDistributionHistoTask(MZmineProject project, RawDataFile dataFile,
      ParameterSet parameters) {

    this.project = project;
    this.dataFile = dataFile;
    this.scanSelection =
        parameters.getParameter(MZDistributionHistoParameters.scanSelection).getValue();
    this.massListName = parameters.getParameter(MZDistributionHistoParameters.massList).getValue();

    this.mzRange = parameters.getParameter(MZDistributionHistoParameters.mzRange).getValue();
    this.useRTRange = parameters.getParameter(MZDistributionHistoParameters.rtRange).getValue();
    if (useRTRange)
      this.rtRange = parameters.getParameter(MZDistributionHistoParameters.rtRange)
          .getEmbeddedParameter().getValue();
    this.binWidth = parameters.getParameter(MZDistributionHistoParameters.binWidth).getValue();
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
   */
  public String getTaskDescription() {
    return "Detecting images in " + dataFile;
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
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
    logger.info("Starting to build mz distribution histogram for " + dataFile);

    // all selected scans
    scans = scanSelection.getMatchingScans(dataFile);
    totalScans = scans.length;

    // histo data
    DoubleArrayList data = new DoubleArrayList();

    for (Scan scan : scans) {
      if (isCanceled())
        return;

      // retention time in range
      if (!useRTRange || rtRange.contains(scan.getRetentionTime())) {
        // go through all mass lists
        MassList massList = scan.getMassList(massListName);
        if (massList == null) {
          setStatus(TaskStatus.ERROR);
          setErrorMessage("Scan " + dataFile + " #" + scan.getScanNumber()
              + " does not have a mass list " + massListName);
          return;
        }
        DataPoint mzValues[] = massList.getDataPoints();

        // insert all mz in order and count them
        Arrays.stream(mzValues).mapToDouble(dp -> dp.getMZ()).filter(mz -> mzRange.contains(mz))
            .forEach(mz -> data.add(mz));
        processedScans++;
      }
    }

    if (!data.isEmpty()) {
      // to array
      double[] histo = new double[data.size()];
      for (int i = 0; i < data.size(); i++)
        histo[i] = data.get(i);

      // create histogram dialog
      EHistogramDialog dialog =
          new EHistogramDialog("m/z distribution", new HistogramData(histo), binWidth);
      dialog.setVisible(true);
    } else {
      throw new MSDKRuntimeException("Data was empty. Review your selected filters.");
    }
    setStatus(TaskStatus.FINISHED);
    logger.info("Finished mz distribution histogram on " + dataFile);
  }

}
