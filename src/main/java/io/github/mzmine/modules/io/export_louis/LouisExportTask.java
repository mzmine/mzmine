/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.io.export_louis;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.util.logging.Logger;
import javax.annotation.Nullable;

class LouisExportTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private final MZmineProject project;
  private FeatureList featureList;
  private ModularFeatureList resultFeatureList;

  // features counter
  private int processedFeatures;
  private int totalRows;

  // parameter values
  private ParameterSet parameters;

  // our data type from the parameters
  private final Double noise_level;
  private final File outfile;

  /**
   * Constructor to set all parameters and the project
   */
  public LouisExportTask(MZmineProject project, FeatureList featureList, ParameterSet parameters,
      @Nullable
          MemoryMapStorage storage) {
    super(storage);
    this.project = project;
    this.featureList = featureList;
    this.parameters = parameters;
    // Get parameter values for easier use
    noise_level = parameters.getParameter(LouisExportParameters.noiseLevel).getValue();
    outfile = parameters.getParameter(LouisExportParameters.filename).getValue();
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return "Learner task on " + featureList;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0) {
      return 0;
    }
    return (double) processedFeatures / (double) totalRows;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    logger.info("Louis' export on " + featureList.getName());

    // if aligned feature list - stop the method!
    if (featureList.getRawDataFiles().size() > 1) {
      setStatus(TaskStatus.ERROR);
      return;
    }

    NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
    NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();

    // create writer
    try (BufferedWriter writer = Files
        .newBufferedWriter(outfile.toPath(), StandardCharsets.UTF_8)) {
      // write content to file
      writer.append("m/z,rt_start,rt_end");
      writer.newLine();


      for (FeatureListRow row : featureList.getRows()) {
        // initialize some variables
        boolean foundStart = false;
        double startRT = -1;
        double endRT = -1;

        // initial check
        if (row.getAverageHeight() >= noise_level) {
          Feature feature = row.getFeatures().get(0);
          var chromatogramData = feature.getFeatureData();

          int datapoints = chromatogramData.getNumberOfValues();
          // loop over all data points in a chromatogram (or resolved feature)
          for (int dp = 0; dp < datapoints; dp++) {
            double intensity = chromatogramData.getIntensity(dp);
            double rt = chromatogramData.getRetentionTime(dp);

            if (intensity >= noise_level) {
              if (foundStart == false) {
                foundStart = true;
                startRT = rt;
              } else {
                // start was found do nothing
              }
            } else {
              // intesnity below noise
              if (foundStart) {
                // we found the end
                endRT = rt;
                foundStart = false;
                // export to file
                String line = mzFormat.format(row.getAverageMZ()) + "," + rtFormat.format(startRT) + "," + rtFormat.format(endRT);
                writer.append(line);
                writer.newLine();
              }
            }

            // check last data point
            if(dp == datapoints-1) {
              if(foundStart) {
                endRT = rt;

                String line = mzFormat.format(row.getAverageMZ()) + "," + rtFormat.format(startRT) + "," + rtFormat.format(endRT);
                writer.append(line);
                writer.newLine();
              }
            }
          } // end of data points loop
        }
      } // end of row loop
    } catch (IOException e) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Could not open file " + outfile + " for writing.");
      return;
    }

    logger.info("Finished on " + featureList);
    setStatus(TaskStatus.FINISHED);
  }

}
