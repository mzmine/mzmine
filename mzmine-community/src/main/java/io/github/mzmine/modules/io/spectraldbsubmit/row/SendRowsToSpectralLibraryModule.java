/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.io.spectraldbsubmit.row;

import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.io.spectraldbsubmit.batch.LibraryBatchGenerationSubParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.SpectralLibrarySelection;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

/**
 * Sends annotated feature list rows to a spectral library (existing or newly created). Shows a
 * two-step dialog: first the user selects or creates the target library, then configures the entry
 * generation parameters. The generated entries are added to the library which is then added/updated
 * in the current project for later export.
 */
public class SendRowsToSpectralLibraryModule implements MZmineModule {

  private static final String MODULE_NAME = "Send annotated rows to spectral library";

  /**
   * Shows the library selection dialog followed by the parameter setup dialog, then submits the
   * task to the task controller.
   *
   * @param rows           the selected annotated feature list rows to process
   * @param moduleCallDate timestamp of the module call
   */
  public static void showDialogAndSubmitTask(@NotNull final List<ModularFeatureListRow> rows,
      @NotNull final Instant moduleCallDate) {
    if (rows.isEmpty()) {
      return;
    }

    final LibraryBatchGenerationSubParameters parameters = (LibraryBatchGenerationSubParameters) MZmineCore.getConfiguration()
        .getModuleParameters(SendRowsToSpectralLibraryModule.class);
    if (parameters == null) {
      throw new IllegalStateException(MODULE_NAME + " parameter not found");
    }

    final SpectralLibrarySelection lastSelectedLib = parameters.getValue(
        LibraryBatchGenerationSubParameters.lastLibrarySelection);
    final List<SpectralLibrary> lastLibraries = lastSelectedLib.getMatchingLibraries();
    // suggest the feature list name as library name
    final String suggestedName = rows.getFirst().getFeatureList().getName();
    final SpectralLibrarySelectionDialog libDialog = new SpectralLibrarySelectionDialog(rows.size(), lastLibraries,
        suggestedName);
    final Optional<SpectralLibrary> libResult = libDialog.showAndWait();

    if (libResult.isEmpty() || libResult.get() == null) {
      return;
    }
    final SpectralLibrary targetLibrary = libResult.get();

    final ExitCode exitCode = parameters.showSetupDialog(true);
    if (exitCode != ExitCode.OK) {
      return;
    }

    final SendRowsToSpectralLibraryTask task = new SendRowsToSpectralLibraryTask(rows,
        targetLibrary, parameters, moduleCallDate);
    MZmineCore.getTaskController().addTask(task);
  }

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return LibraryBatchGenerationSubParameters.class;
  }
}
