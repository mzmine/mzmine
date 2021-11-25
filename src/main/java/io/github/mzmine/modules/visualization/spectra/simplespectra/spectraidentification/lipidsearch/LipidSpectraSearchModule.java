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

package io.github.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.lipidsearch;

import java.time.Instant;
import java.util.Date;
import org.jetbrains.annotations.NotNull;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.ExitCode;

/**
 * Module to identify signals by using the lipid search module.
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster)
 */
public class LipidSpectraSearchModule implements MZmineModule {

  private static final String MODULE_NAME = "Lipid search";
  private static final String MODULE_DESCRIPTION =
      "This module attempts to annotate signals in selected mass spectra as lipids";

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  public @NotNull String getDescription() {
    return MODULE_DESCRIPTION;
  }

  /**
   * Show dialog for identifying lipids in spectra.
   * 
   */
  public static void showSpectraIdentificationDialog(final Scan scan,
      final SpectraPlot spectraPlot, @NotNull Instant moduleCallDate) {

    final SpectraIdentificationLipidSearchParameters parameters =
        (SpectraIdentificationLipidSearchParameters) MZmineCore.getConfiguration()
            .getModuleParameters(LipidSpectraSearchModule.class);;

    // Run task.
    if (parameters.showSetupDialog(true) == ExitCode.OK) {

      MZmineCore.getTaskController().addTask(new SpectraIdentificationLipidSearchTask(
          parameters.cloneParameterSet(), scan, spectraPlot, moduleCallDate));
    }
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return SpectraIdentificationLipidSearchParameters.class;
  }
}
