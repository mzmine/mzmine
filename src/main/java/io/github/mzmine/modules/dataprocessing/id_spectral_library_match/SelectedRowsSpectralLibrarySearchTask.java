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
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Runs spectral library matching on selected rows
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class SelectedRowsSpectralLibrarySearchTask extends RowsSpectralMatchTask {

  private static final Logger logger = Logger.getLogger(
      SelectedRowsSpectralLibrarySearchTask.class.getName());
  private SpectraIdentificationResultsWindowFX resultWindow;
  private final FeatureTableFX table;
  private final List<SpectralDBAnnotation> syncList = Collections.synchronizedList(
      new ArrayList<>());

  public SelectedRowsSpectralLibrarySearchTask(List<FeatureListRow> rows, FeatureTableFX table,
      ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(parameters, rows, moduleCallDate); // no new data stored -> null
    this.table = table;
    MZmineCore.runLater(() -> {
      resultWindow = new SpectraIdentificationResultsWindowFX();
      resultWindow.addMatches(syncList);
      MZmineCore.getDesktop().addTab(resultWindow);
    });
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    logger.info(() -> String.format("Spectral library matching of %d rows against libraries: %s",
        rows.size(), librariesJoined));

    // add type to featureLists
    for (var row : rows) {
      if (row.getFeatureList() instanceof ModularFeatureList mod) {
        mod.addRowType(DataTypes.get(SpectralLibraryMatchesType.class));
      }
    }

    // run the actual subtask
    super.run();
    if (resultWindow != null) {
      MZmineCore.runLater(() -> resultWindow.setMatchingFinished());
    }
    // Add task description to peakList
    if (!isCanceled()) {
      setStatus(TaskStatus.FINISHED);
    }
  }

  @Override
  protected void addIdentities(FeatureListRow row, List<SpectralDBAnnotation> matches) {
    super.addIdentities(row, matches);
    // one selected row -> show in dialog
      if (resultWindow != null) {
        MZmineCore.runLater(() -> resultWindow.addMatches(matches));
      }
      else {
        syncList.addAll(matches);
      }
  }

}
