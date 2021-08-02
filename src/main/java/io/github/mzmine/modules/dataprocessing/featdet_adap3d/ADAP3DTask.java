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

package io.github.mzmine.modules.dataprocessing.featdet_adap3d;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;
import io.github.mzmine.datamodel.msdk.MsScan;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.MZmineToMSDKMsScan;
import io.github.mzmine.datamodel.impl.MZmineToMSDKRawDataFile;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import org.jetbrains.annotations.Nullable;

public class ADAP3DTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private final MZmineProject project;
  private final RawDataFile dataFile;
  private final ScanSelection scanSelection;
  private final String suffix;
  private ADAP3DFeatureDetectionMethod msdkADAP3DMethod;
  private final ParameterSet parameters;

  /**
   * @param dataFile
   * @param parameters
   */
  public ADAP3DTask(MZmineProject project, RawDataFile dataFile, ParameterSet parameters, @Nullable
      MemoryMapStorage storage) {
    super(storage);

    this.project = project;
    this.dataFile = dataFile;
    this.scanSelection = parameters.getParameter(ADAP3DParameters.scanSelection).getValue();
    this.suffix = parameters.getParameter(ADAP3DParameters.suffix).getValue();
    this.parameters = parameters;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return "Running ADAP3D on file " + dataFile;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (msdkADAP3DMethod == null)
      return 0.0;
    Float msdkProgress = msdkADAP3DMethod.getFinishedPercentage();
    if (msdkProgress == null)
      return 0.0;
    return msdkProgress.doubleValue();
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
    logger.info("Started ADAP3D on " + dataFile);

    List<Scan> selectedScans = Arrays.asList(scanSelection.getMatchingScans(dataFile));

    // Check if we have any scans
    if (selectedScans.size() == 0) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("No scans match the selected criteria");
      return;
    }

    // Check if the scans are properly ordered by RT
    double prevRT = Double.NEGATIVE_INFINITY;
    for (Scan s : selectedScans) {
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

    // Run MSDK module
    MZmineToMSDKRawDataFile msdkRawDataFile = new MZmineToMSDKRawDataFile(dataFile);
    Predicate<MsScan> scanSelectionPredicate =
        scan -> selectedScans.contains(((MZmineToMSDKMsScan) scan).getMzmineScan());
    msdkADAP3DMethod = new ADAP3DFeatureDetectionMethod(msdkRawDataFile, scanSelectionPredicate,
        new ADAP3DFeatureDetectionParameters());
    List<ModularFeature> features = null;
    try {
      if (isCanceled())
        return;
      features = msdkADAP3DMethod.execute();
      if (isCanceled())
        return;
    } catch (Exception e) {
      e.printStackTrace();
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Error in ADAP3D: " + e.getMessage());
    }

    if (features == null)
      features = new ArrayList<>(0);

    logger.info("ADAP3D detected " + features.size() + " features in " + dataFile
        + ", converting to MZmine peaklist");

    // Create new MZmine feature list
    ModularFeatureList newPeakList = new ModularFeatureList(dataFile + " " + suffix,
        getMemoryMapStorage(), dataFile);

    int rowId = 1;
    for (ModularFeature mzmineFeature : features) {
      if (isCanceled())
        return;
      FeatureListRow row = new ModularFeatureListRow(newPeakList, rowId);
      row.addFeature(dataFile, mzmineFeature);
      newPeakList.addRow(row);
      rowId++;
    }

    newPeakList.getAppliedMethods().add(new SimpleFeatureListAppliedMethod(
        ADAP3DModule.class, parameters));
    // Add new peaklist to the project
    project.addFeatureList(newPeakList);

    // Add quality parameters to peaks
    //QualityParameters.calculateQualityParameters(newPeakList);

    setStatus(TaskStatus.FINISHED);

    logger.info("Finished ADAP3D feature detection on " + dataFile);

  }

  @Override
  public void cancel() {
    super.cancel();
    if (msdkADAP3DMethod != null)
      msdkADAP3DMethod.cancel();
  }

}
