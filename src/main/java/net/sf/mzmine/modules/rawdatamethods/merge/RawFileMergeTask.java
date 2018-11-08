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

import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.project.impl.RawDataFileImpl;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

class RawFileMergeTask extends AbstractTask {

  private double perc = 0;
  private RawDataFile[] raw;
  private String suffix;
  private boolean useMS2Marker;
  private String ms2Marker;

  RawFileMergeTask(ParameterSet parameters, RawDataFile[] raw) {
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
    setStatus(TaskStatus.PROCESSING);

    // total number of scans
    int totalScans = 0;
    for (RawDataFile r : raw) {
      totalScans += r.getNumOfScans();
    }

    RawDataFileImpl f = (RawDataFileImpl) raw[0];

    RawDataFileImpl result =
        new RawDataFileImpl(f.getName() + suffix, f.getOriginalFile(), f.getRawDataFileType());

    for (RawDataFile r : raw) {
      boolean isMS2Only = useMS2Marker && r.getName().contains(ms2Marker);
      r.getScan(scan)

    }

    if (getStatus() == TaskStatus.PROCESSING)
      setStatus(TaskStatus.FINISHED);
  }

}
