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

package io.github.mzmine.modules.tools.qualityparameters;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.Date;
import org.jetbrains.annotations.NotNull;

public class QualityParametersTask extends AbstractTask {

  private final FeatureList featureList;
  private final ParameterSet parameterSet;

  private double finishedPercentage;
  public QualityParametersTask(FeatureList featureList, ParameterSet parameterSet, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    this.parameterSet = parameterSet;
    this.featureList = featureList;
    setStatus(TaskStatus.WAITING);
    finishedPercentage = 0.d;
  }

  @Override
  public String getTaskDescription() {
    return "Calculating quality parameters for feature list " + featureList.getName();
  }

  @Override
  public double getFinishedPercentage() {
    return finishedPercentage;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.WAITING);

    QualityParameters.calculateAndSetModularQualityParameters((ModularFeatureList) featureList);
    finishedPercentage = 1.d;
    setStatus(TaskStatus.FINISHED);
  }

}
