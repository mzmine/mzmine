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

package io.github.mzmine.modules.dataprocessing.filter_merge;

import io.github.msdk.MSDKRuntimeException;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Merge multiple raw data files into one. For example one positive, one negative and multiple with
 * MS2
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
class RawFileMergeTask extends AbstractTask {

  private Logger logger = Logger.getLogger(getClass().getName());

  private double perc = 0;
  private ParameterSet parameters;
  private RawDataFile[] raw;
  private String suffix;
  private boolean useMS2Marker;
  private String ms2Marker;
  private MZmineProject project;

  RawFileMergeTask(MZmineProject project, ParameterSet parameters, RawDataFile[] raw,
      @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);
    this.project = project;
    this.parameters = parameters;
    this.raw = raw;
    suffix = parameters.getParameter(RawFileMergeParameters.suffix).getValue();
    useMS2Marker = parameters.getParameter(RawFileMergeParameters.MS2_marker).getValue();
    ms2Marker = parameters.getParameter(RawFileMergeParameters.MS2_marker).getEmbeddedParameter()
        .getValue();
    if (ms2Marker.isEmpty())
      useMS2Marker = false;
  }

  @Override
  public double getFinishedPercentage() {
    return perc;
  }

  @Override
  public String getTaskDescription() {
    return "Merging raw data files";
  }

  @Override
  public void run() {
    try {
      setStatus(TaskStatus.PROCESSING);

      // total number of scans
      StringBuilder s = new StringBuilder();
      s.append("Merge files: ");
      for (RawDataFile r : raw) {
        s.append(r.getName());
        s.append(", ");
      }

      logger.info(s.toString());

      // put all in a list and sort by rt
      List<Scan> scans = new ArrayList<>();
      for (RawDataFile r : raw) {
        // some files are only for MS2
        boolean isMS2Only = useMS2Marker && r.getName().contains(ms2Marker);
        ObservableList<Scan> snarray = r.getScans();
        for (Scan scan : snarray) {
          if (isCanceled())
            return;

          if (!isMS2Only || scan.getMSLevel() > 1) {
            scans.add(scan);
          }
        }
      }

      // sort by rt
      scans.sort(Comparator.comparingDouble(Scan::getRetentionTime));

      // create new file
      RawDataFile newFile = MZmineCore.createNewFile(raw[0].getName() + " " + suffix, null,
          getMemoryMapStorage());

      int i = 0;
      for (Scan scan : scans) {
        if (isCanceled())
          return;
        // copy, reset scan number
        SimpleScan scanCopy = new SimpleScan(newFile, scan, scan.getMzValues(new double[0]),
            scan.getIntensityValues(new double[0]));
        scanCopy.setScanNumber(i);
        newFile.addScan(scanCopy);
        i++;
      }

      // TODO is this correct?
      for (FeatureListAppliedMethod appliedMethod : raw[0].getAppliedMethods()) {
        newFile.getAppliedMethods().add(appliedMethod);
      }
      newFile.getAppliedMethods().add(new SimpleFeatureListAppliedMethod(
          RawFileMergeModule.class, parameters, getModuleCallDate()));

      project.addFile(newFile);

      if (getStatus() == TaskStatus.PROCESSING)
        setStatus(TaskStatus.FINISHED);
    } catch (IOException e) {
      throw new MSDKRuntimeException(e);
    }
  }

}
