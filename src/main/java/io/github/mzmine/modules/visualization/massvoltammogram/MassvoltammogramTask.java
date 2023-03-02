/*
 * Copyright 2006-2022 The MZmine Development Team
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
 */

package io.github.mzmine.modules.visualization.massvoltammogram;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.massvoltammogram.io.MassvoltammogramFromFeatureListParameters;
import io.github.mzmine.modules.visualization.massvoltammogram.io.MassvoltammogramFromFileParameters;
import io.github.mzmine.modules.visualization.massvoltammogram.utils.Massvoltammogram;
import io.github.mzmine.modules.visualization.massvoltammogram.utils.ReactionMode;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import org.jetbrains.annotations.NotNull;

public class MassvoltammogramTask extends AbstractTask {

  //Raw Data
  private RawDataFile file;
  private ModularFeatureList featureList;

  //Parameter
  private ScanSelection scanSelection;
  private final ReactionMode reactionMode;
  private final double delayTime; //In s.
  private final double potentialRampSpeed; //In mV/s.
  private final Range<Double> potentialRange;
  private final double stepSize; //In mV.
  private final Range<Double> mzRange;


  public MassvoltammogramTask(@NotNull ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);

    //Setting up the parameter that depend on a raw data file or a feature list being chosen.
    if (parameters instanceof MassvoltammogramFromFileParameters) {

      file = MassvoltammogramFromFileParameters.files.getValue().getMatchingRawDataFiles()[0];
      scanSelection = parameters.getValue(MassvoltammogramFromFileParameters.scanSelection);
      featureList = null;

    } else if (parameters instanceof MassvoltammogramFromFeatureListParameters) {

      featureList = MassvoltammogramFromFeatureListParameters.featureList.getValue()
          .getMatchingFeatureLists()[0];
      file = null;
      scanSelection = null;
    }

    //Setting up the remaining parameters.
    potentialRampSpeed = parameters.getValue(MassvoltammogramFromFileParameters.potentialRampSpeed);
    stepSize = parameters.getValue(MassvoltammogramFromFileParameters.stepSize);
    potentialRange = parameters.getValue(MassvoltammogramFromFileParameters.potentialRange);
    mzRange = parameters.getValue(MassvoltammogramFromFileParameters.mzRange);
    reactionMode = parameters.getValue(MassvoltammogramFromFileParameters.reactionMode);
    delayTime = parameters.getValue(MassvoltammogramFromFileParameters.delayTime);

  }

  @Override
  public String getTaskDescription() {
    return "Creating Massvoltammogram";
  }

  @Override
  public double getFinishedPercentage() {
    return 0;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    //Creating the massvoltammogram.
    Massvoltammogram massvoltammogram;

    if (file != null) {

      massvoltammogram = new Massvoltammogram(file, scanSelection, reactionMode, delayTime,
          potentialRampSpeed, potentialRange, stepSize, mzRange);

    } else if (featureList != null) {

      massvoltammogram = new Massvoltammogram(featureList, reactionMode, delayTime,
          potentialRampSpeed, potentialRange, stepSize, mzRange);

    } else {

      setStatus(TaskStatus.ERROR);
      setErrorMessage("No data source is selected.");
      return;
    }

    //Adding the massvoltammogram to a new MZmineTab.
    final MassvoltammogramTab mvTab = new MassvoltammogramTab("Massvoltammogram", massvoltammogram);
    MZmineCore.getDesktop().addTab(mvTab);

    //Setting the task status to finished.
    setStatus(TaskStatus.FINISHED);
  }
}
