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

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.logging.Logger;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;

public class SingleRowPredictionTask extends FormulaPredictionTask {

  private static final Logger logger = Logger.getLogger(SingleRowPredictionTask.class.getName());
  private final FeatureListRow peakListRow;

  SingleRowPredictionTask(ParameterSet parameters, FeatureListRow peakListRow,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate, null, null, null); // no new data stored -> null
    this.peakListRow = peakListRow;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    Platform.runLater(() -> {
      resultWindowFX = new ResultWindowFX(
          "Searching for " + MZmineCore.getConfiguration().getMZFormat().format(searchedMass),
          peakListRow, searchedMass, charge, this, parameters);
      resultWindowFX.show();

    });

    // setup
    detectedPattern = peakListRow.getBestIsotopePattern();
    Feature bestPeak = peakListRow.getBestFeature();
    Scan bestMsMs = bestPeak.getMostIntenseFragmentScan();

    if ((checkMSMS) && (bestMsMs != null)) {
      this.msmsScan = bestMsMs.getMassList();
      if (this.msmsScan == null) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage(
            "The MS/MS scan #" + bestMsMs.getScanNumber() + " in file " + bestMsMs.getDataFile()
                .getName() + " does not have a mass list");
        return;
      }
    }

    // run prediction
    super.run();

    if (isCanceled()) {
      return;
    }

    peakListRow.getFeatureList().getAppliedMethods().add(
        new SimpleFeatureListAppliedMethod(FormulaPredictionModule.class, parameters,
            getModuleCallDate()));

    setStatus(TaskStatus.FINISHED);
  }

}
