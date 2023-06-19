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

public class  MobilityScanMergerTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(MobilityScanMergerTask.class.getName());

  private final ScanSelection scanSelection;
  private final IMSRawDataFile rawDataFile;
  private final MZTolerance mzTolerance;
  private final Weighting weighting;
  private final ParameterSet parameters;
  private final double noiseLevel;
  private final IntensityMergingType mergingType;
  private int totalFrames;
  private int processedFrames;

  public MobilityScanMergerTask(final IMSRawDataFile file, ParameterSet parameters,
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
            mergingType, cf, null, noiseLevel, null);

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
