/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.modules.dataprocessing.featdet_mobilityscanmerger;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.maths.CenterFunction;
import io.github.mzmine.util.maths.CenterMeasure;
import io.github.mzmine.util.maths.Weighting;
import io.github.mzmine.util.scans.SpectraMerging;
import io.github.mzmine.util.scans.SpectraMerging.MergingType;

public class MoblityScanMergerTask extends AbstractTask {

  private final int totalFrames;
  private final IMSRawDataFile rawDataFile;
  private final MZTolerance mzTolerance;
  private final Weighting weighting;
  private final ParameterSet parameters;
  private int processedFrames;

  public MoblityScanMergerTask(final IMSRawDataFile file, ParameterSet parameters) {
    processedFrames = 0;
    totalFrames = file.getNumberOfFrames();
    this.parameters = parameters;
    this.rawDataFile = file;

    mzTolerance = parameters.getParameter(MobilityScanMergerParameters.mzTolerance).getValue();
    weighting = parameters.getParameter(MobilityScanMergerParameters.weightingType)
        .getValue();
  }


  @Override
  public String getTaskDescription() {
    return null;
  }

  @Override
  public double getFinishedPercentage() {
    return processedFrames / (double) totalFrames;
  }

  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    CenterFunction cf = new CenterFunction(CenterMeasure.AVG, weighting);

    for (Frame f : rawDataFile.getFrames()) {
      SimpleFrame frame = (SimpleFrame) f;
      double[][] merged = SpectraMerging
          .calculatedMergedMzsAndIntensities(frame.getMobilityScans(), 0d, mzTolerance,
              MergingType.SUMMED, cf);

      frame.setDataPoints(merged[0], merged[1]);

      processedFrames++;
    }

    setStatus(TaskStatus.FINISHED);
  }
}
