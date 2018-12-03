/*
 * Copyright 2006-2018 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.rawdatamethods.merge;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import io.github.msdk.MSDKRuntimeException;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.RawDataFileWriter;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleScan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

/**
 * Merge multiple raw data files into one. For example one positive, one negative and multiple with
 * MS2
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
class RawFileMergeTask extends AbstractTask {

  private Logger LOG = Logger.getLogger(getClass().getName());

  private double perc = 0;
  private RawDataFile[] raw;
  private String suffix;
  private boolean useMS2Marker;
  private String ms2Marker;
  private MZmineProject project;

  RawFileMergeTask(MZmineProject project, ParameterSet parameters, RawDataFile[] raw) {
    this.project = project;
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

      LOG.info(s.toString());

      // put all in a list and sort by rt
      List<Scan> scans = new ArrayList<>();
      for (RawDataFile r : raw) {
        // some files are only for MS2
        boolean isMS2Only = useMS2Marker && r.getName().contains(ms2Marker);
        int[] snarray = r.getScanNumbers();
        for (int sn : snarray) {
          if (isCanceled())
            return;

          Scan scan = r.getScan(sn);
          if (!isMS2Only || scan.getMSLevel() > 1) {
            scans.add(scan);
          }
        }
      }

      // sort by rt
      scans.sort(new Comparator<Scan>() {
        @Override
        public int compare(Scan a, Scan b) {
          return Double.compare(a.getRetentionTime(), b.getRetentionTime());
        }
      });

      // create new file
      RawDataFileWriter rawDataFileWriter =
          MZmineCore.createNewFile(raw[0].getName() + " " + suffix);

      int i = 0;
      for (Scan scan : scans) {
        if (isCanceled())
          return;
        // copy, reset scan number
        SimpleScan scanCopy = new SimpleScan(scan);
        scanCopy.setScanNumber(i);
        rawDataFileWriter.addScan(scanCopy);
        i++;
      }

      RawDataFile filteredRawDataFile = rawDataFileWriter.finishWriting();
      project.addFile(filteredRawDataFile);

      if (getStatus() == TaskStatus.PROCESSING)
        setStatus(TaskStatus.FINISHED);
    } catch (IOException e) {
      throw new MSDKRuntimeException(e);
    }
  }

}
