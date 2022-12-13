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
