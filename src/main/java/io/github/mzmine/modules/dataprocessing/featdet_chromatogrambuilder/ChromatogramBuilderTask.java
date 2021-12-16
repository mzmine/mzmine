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

package io.github.mzmine.modules.dataprocessing.featdet_chromatogrambuilder;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureConvertors;
import io.github.mzmine.util.FeatureSorter;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import java.time.Instant;
import java.util.Arrays;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChromatogramBuilderTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private MZmineProject project;
  private RawDataFile dataFile;

  // scan counter
  private int processedScans = 0, totalScans;
  private ScanSelection scanSelection;
  private int newPeakID = 1;
  private Scan[] scans;

  // User parameters
  private String suffix;
  private MZTolerance mzTolerance;
  private double minimumTimeSpan, minimumHeight;

  private ModularFeatureList newPeakList;
  private ParameterSet parameters;

  /**
   * @param dataFile
   * @param parameters
   */
  public ChromatogramBuilderTask(MZmineProject project, RawDataFile dataFile,
      ParameterSet parameters, @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    this.project = project;
    this.dataFile = dataFile;
    this.scanSelection =
        parameters.getParameter(ChromatogramBuilderParameters.scanSelection).getValue();

    this.mzTolerance =
        parameters.getParameter(ChromatogramBuilderParameters.mzTolerance).getValue();
    this.minimumTimeSpan =
        parameters.getParameter(ChromatogramBuilderParameters.minimumTimeSpan).getValue();
    this.minimumHeight =
        parameters.getParameter(ChromatogramBuilderParameters.minimumHeight).getValue();

    this.suffix = parameters.getParameter(ChromatogramBuilderParameters.suffix).getValue();
    this.parameters = parameters;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
   */
  public String getTaskDescription() {
    return "Detecting chromatograms in " + dataFile;
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

    logger.info("Started chromatogram builder on " + dataFile);

    scans = scanSelection.getMatchingScans(dataFile);
    totalScans = scans.length;

    // Check if the scans are properly ordered by RT
    double prevRT = Double.NEGATIVE_INFINITY;
    for (Scan s : scans) {
      if (s.getRetentionTime() < prevRT) {
        setStatus(TaskStatus.ERROR);
        final String msg = "Retention time of scan #" + s.getScanNumber()
            + " is smaller then the retention time of the previous scan."
            + " Please make sure you only use scans with increasing retention times."
            + " You can restrict the scan numbers in the parameters, or you can use the Crop filter module";
        setErrorMessage(msg);
        return;
      }
      prevRT = s.getRetentionTime();
    }

    // Create new feature list
    newPeakList = new ModularFeatureList(dataFile + " " + suffix, getMemoryMapStorage(), dataFile);

    Chromatogram[] chromatograms;
    HighestDataPointConnector massConnector = new HighestDataPointConnector(dataFile,
        scans, minimumTimeSpan, minimumHeight, mzTolerance);

    for (Scan scan : scans) {

      if (isCanceled())
        return;

      MassList massList = scan.getMassList();
      if (massList == null) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Scan " + dataFile + " #" + scan.getScanNumber()
            + " does not have a mass list");
        return;
      }

      DataPoint mzValues[] = massList.getDataPoints();

      if (mzValues == null) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Mass list does not contain m/z values for scan #"
            + scan.getScanNumber() + " of file " + dataFile);
        return;
      }

      massConnector.addScan(scan, mzValues);
      processedScans++;
    }

    chromatograms = massConnector.finishChromatograms();

    // Convert chromatograms to modular features
    Feature[] features = Arrays.stream(chromatograms)
        .map(f -> FeatureConvertors.ChromatogramToModularFeature(newPeakList, f))
        .toArray(ModularFeature[]::new);

    // Sort the final features by m/z
    Arrays.sort(features, new FeatureSorter(SortingProperty.MZ, SortingDirection.Ascending));

    // Add the features to the new feature list
    for (Feature finishedPeak : features) {
      ModularFeatureListRow newRow = new ModularFeatureListRow(newPeakList, newPeakID);
      newPeakID++;
      newRow.addFeature(dataFile, finishedPeak);
      newPeakList.addRow(newRow);
    }

    dataFile.getAppliedMethods().forEach(m -> newPeakList.getAppliedMethods().add(m));
    // Add new peaklist to the project
    newPeakList.getAppliedMethods().add(new SimpleFeatureListAppliedMethod(
        ChromatogramBuilderModule.class, parameters, getModuleCallDate()));
    project.addFeatureList(newPeakList);

    // Add quality parameters to peaks
    //QualityParameters.calculateQualityParameters(newPeakList);

    setStatus(TaskStatus.FINISHED);

    logger.info("Finished chromatogram builder on " + dataFile);

  }

}
