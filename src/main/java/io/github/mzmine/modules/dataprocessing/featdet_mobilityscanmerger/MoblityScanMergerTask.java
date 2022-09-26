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

package io.github.mzmine.modules.dataprocessing.featdet_mobilityscanmerger;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.datamodel.impl.masslist.ScanPointerMassList;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.maths.CenterFunction;
import io.github.mzmine.util.maths.Weighting;
import io.github.mzmine.util.scans.SpectraMerging;
import io.github.mzmine.util.scans.SpectraMerging.IntensityMergingType;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class MoblityScanMergerTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(MoblityScanMergerTask.class.getName());

  private final ScanSelection scanSelection;
  private final IMSRawDataFile rawDataFile;
  private final MZTolerance mzTolerance;
  private final Weighting weighting;
  private final ParameterSet parameters;
  private final double noiseLevel;
  private final IntensityMergingType mergingType;
  private int totalFrames;
  private int processedFrames;

  public MoblityScanMergerTask(final IMSRawDataFile file, ParameterSet parameters,
      @NotNull Instant moduleCallDate) {
    super(null,
        moduleCallDate); // for now, the merged data points are added to the frame on the raw data
    // level. In the future, we will generate a mass list.
    processedFrames = 0;
    totalFrames = 1;
    this.parameters = parameters;
    this.rawDataFile = file;

    mzTolerance = parameters.getParameter(MobilityScanMergerParameters.mzTolerance).getValue();
    mergingType = parameters.getParameter(MobilityScanMergerParameters.mergingType).getValue();
    weighting = parameters.getParameter(MobilityScanMergerParameters.weightingType).getValue();
    scanSelection = parameters.getParameter(MobilityScanMergerParameters.scanSelection).getValue();
    noiseLevel = parameters.getParameter(MobilityScanMergerParameters.noiseLevel).getValue();
  }

  @Override
  public String getTaskDescription() {
    return "Merging mobility scans for frame " + processedFrames + "/" + totalFrames;
  }

  @Override
  public double getFinishedPercentage() {
    return processedFrames / (double) totalFrames;
  }

  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    final CenterFunction cf = new CenterFunction(SpectraMerging.DEFAULT_CENTER_MEASURE, weighting);

    List<? extends Frame> frames = scanSelection.getMatchingScans(rawDataFile.getFrames());
    totalFrames = frames.size();

    try {
      for (Frame f : frames) {
        SimpleFrame frame = (SimpleFrame) f;
        double[][] merged = SpectraMerging.calculatedMergedMzsAndIntensities(
            frame.getMobilityScans().stream().map(MobilityScan::getMassList).toList(), mzTolerance,
            mergingType, cf, noiseLevel, null);

        frame.setDataPoints(merged[0], merged[1]);
        frame.addMassList(new ScanPointerMassList(frame));

        processedFrames++;
      }
    } catch (NullPointerException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
      setErrorMessage("No mass list present in " + rawDataFile.getName()
          + ".\nPlease run mass detection first.");
      setStatus(TaskStatus.ERROR);
      return;
    }

    rawDataFile.getAppliedMethods().add(
        new SimpleFeatureListAppliedMethod(MobilityScanMergerModule.class, parameters,
            getModuleCallDate()));

    setStatus(TaskStatus.FINISHED);
  }
}
