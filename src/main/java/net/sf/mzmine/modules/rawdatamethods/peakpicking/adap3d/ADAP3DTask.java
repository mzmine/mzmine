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

package net.sf.mzmine.modules.rawdatamethods.peakpicking.adap3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;

import io.github.msdk.datamodel.Feature;
import io.github.msdk.datamodel.MsScan;
import io.github.msdk.featuredetection.adap3d.ADAP3DFeatureDetectionMethod;
import io.github.msdk.featuredetection.adap3d.ADAP3DFeatureDetectionParameters;
import net.sf.mzmine.datamodel.Feature.FeatureStatus;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.MZmineToMSDKMsScan;
import net.sf.mzmine.datamodel.impl.MZmineToMSDKRawDataFile;
import net.sf.mzmine.datamodel.impl.SimpleFeature;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.modules.peaklistmethods.qualityparameters.QualityParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

public class ADAP3DTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private final MZmineProject project;
  private final RawDataFile dataFile;
  private final ScanSelection scanSelection;
  private final String suffix;
  private ADAP3DFeatureDetectionMethod msdkADAP3DMethod;


  /**
   * @param dataFile
   * @param parameters
   */
  public ADAP3DTask(MZmineProject project, RawDataFile dataFile, ParameterSet parameters) {

    this.project = project;
    this.dataFile = dataFile;
    this.scanSelection = parameters.getParameter(ADAP3DParameters.scanSelection).getValue();
    this.suffix = parameters.getParameter(ADAP3DParameters.suffix).getValue();

  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
   */
  public String getTaskDescription() {
    return "Running ADAP3D on file " + dataFile;
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
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
    msdkADAP3DMethod = new ADAP3DFeatureDetectionMethod(msdkRawDataFile, scanSelectionPredicate, new ADAP3DFeatureDetectionParameters());
    List<Feature> features = null;
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

    // Create new MZmine 2 peak list
    SimplePeakList newPeakList = new SimplePeakList(dataFile + " " + suffix, dataFile);

    int rowId = 1;
    for (Feature msdkFeature : features) {
      if (isCanceled())
        return;
      SimpleFeature mzmineFeature =
          new SimpleFeature(dataFile, FeatureStatus.DETECTED, msdkFeature);
      PeakListRow row = new SimplePeakListRow(rowId);
      row.addPeak(dataFile, mzmineFeature);
      newPeakList.addRow(row);
      rowId++;
    }

    // Add new peaklist to the project
    project.addPeakList(newPeakList);

    // Add quality parameters to peaks
    QualityParameters.calculateQualityParameters(newPeakList);

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
