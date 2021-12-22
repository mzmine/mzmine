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

package io.github.mzmine.modules.dataprocessing.id_spectral_library_match;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.SpectralLibraryMatchesType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.modules.visualization.spectra.spectralmatchresults.SpectraIdentificationResultsWindowFX;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.spectraldb.entry.SpectralDBFeatureIdentity;
import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Runs spectral library matching on selected rows
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class SelectedRowsSpectralLibrarySearchTask extends RowsSpectralMatchTask {

  private static final Logger logger = Logger
      .getLogger(SelectedRowsSpectralLibrarySearchTask.class.getName());
  private SpectraIdentificationResultsWindowFX resultWindow;
  private FeatureTableFX table;

  public SelectedRowsSpectralLibrarySearchTask(List<FeatureListRow> rows, FeatureTableFX table,
      ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(parameters, rows, moduleCallDate); // no new data stored -> null
    this.table = table;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    logger.info(() -> String
        .format("Spectral library matching of %d rows against libraries: %s",
            rows.size(), librariesJoined));

    // add type to featureLists
    for (var row : rows) {
      if (row.getFeatureList() instanceof ModularFeatureList mod) {
        mod.addRowType(DataTypes.get(SpectralLibraryMatchesType.class));
      }
    }

    if (rows.size() == 1) {
      // add result frame
      MZmineCore.runLater(() -> {
        resultWindow = new SpectraIdentificationResultsWindowFX();
        resultWindow.show();
      });
    } else {
      resultWindow = null;
    }

    // run the actual subtask
    super.run();

    // Add task description to peakList
    if (!isCanceled()) {
      setStatus(TaskStatus.FINISHED);
    }
  }

  @Override
  protected void addIdentities(FeatureListRow row, List<SpectralDBFeatureIdentity> matches) {
    super.addIdentities(row, matches);
    // one selected row -> show in dialog
    if (resultWindow != null) {
      MZmineCore.runLater(() -> resultWindow.addMatches(matches));
    }
  }

}
