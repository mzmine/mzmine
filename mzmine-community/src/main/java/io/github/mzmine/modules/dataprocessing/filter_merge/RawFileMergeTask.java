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
