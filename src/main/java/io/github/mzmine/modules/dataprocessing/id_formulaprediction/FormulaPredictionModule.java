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

package io.github.mzmine.modules.dataprocessing.id_formulaprediction;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import org.jetbrains.annotations.NotNull;

public class FormulaPredictionModule implements MZmineModule {

  private static final String MODULE_NAME = "Formula prediction";

  public static void showSingleRowIdentificationDialog(FeatureListRow row) {

    ParameterSet parameters =
        MZmineCore.getConfiguration().getModuleParameters(FormulaPredictionModule.class);

    double mzValue = row.getAverageMZ();
    parameters.getParameter(FormulaPredictionParameters.neutralMass).setIonMass(mzValue);

    Scan bestScan = row.getBestFeature().getRepresentativeScan();
    if (bestScan != null) {
      RawDataFile dataFile = row.getBestFeature().getRawDataFile();
      PolarityType scanPolarity = bestScan.getPolarity();
      switch (scanPolarity) {
        case POSITIVE:
          parameters.getParameter(FormulaPredictionParameters.neutralMass)
              .setIonType(IonizationType.POSITIVE_HYDROGEN);
          break;
        case NEGATIVE:
          parameters.getParameter(FormulaPredictionParameters.neutralMass)
              .setIonType(IonizationType.NEGATIVE_HYDROGEN);
          break;
        default:
          break;
      }
    }

    int charge = row.getBestFeature().getCharge();
    if (charge > 0) {
      parameters.getParameter(FormulaPredictionParameters.neutralMass).setCharge(charge);
    }

    ExitCode exitCode = parameters.showSetupDialog(true);
    if (exitCode != ExitCode.OK) {
      return;
    }

    SingleRowPredictionTask newTask =
        new SingleRowPredictionTask(parameters.cloneParameterSet(), row, Instant.now());

    // execute the sequence
    MZmineCore.getTaskController().addTask(newTask);

  }

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return FormulaPredictionParameters.class;
  }

}
