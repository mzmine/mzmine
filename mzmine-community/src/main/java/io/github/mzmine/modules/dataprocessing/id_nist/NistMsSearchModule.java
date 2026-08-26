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

package io.github.mzmine.modules.dataprocessing.id_nist;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.SpectraMergeSelectParameter;
import io.github.mzmine.modules.dataprocessing.id_nist_mspepsearch.NistPepSearchTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Searches feature lists against NIST libraries with NIST's command line program MSPepSearch.
 */
public class NistMsSearchModule implements MZmineProcessingModule {

  private static final String MODULE_NAME = "NIST MS search";
  private static final String MODULE_DESCRIPTION =
      "Searches GC-EI or MS/MS spectra against the libraries of a licensed NIST installation, "
          + "using NIST's command line program MSPepSearch.";

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @NotNull String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.ANNOTATION;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return NistMsSearchParameters.class;
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull final MZmineProject project,
      @NotNull final ParameterSet parameters, @NotNull final Collection<Task> tasks,
      @NotNull final Instant moduleCallDate) {

    for (final FeatureList featureList : parameters.getParameter(NistMsSearchParameters.PEAK_LISTS)
        .getValue().getMatchingFeatureLists()) {

      tasks.add(createTask(featureList, null, parameters, moduleCallDate));
    }

    return ExitCode.OK;
  }

  /**
   * Searches the spectra of a single feature list row.
   *
   * @param featureList the feature list.
   * @param row         the row to search.
   */
  public static void singleRowSearch(final FeatureList featureList, final FeatureListRow row) {

    final ParameterSet parameters = MZmineCore.getConfiguration()
        .getModuleParameters(NistMsSearchModule.class);

    if (parameters.showSetupDialog(true) == ExitCode.OK) {
      MZmineCore.getTaskController()
          .addTask(createTask(featureList, row, parameters, Instant.now()));
    }
  }

  private static NistPepSearchTask createTask(final FeatureList featureList,
      @Nullable final FeatureListRow row, final ParameterSet parameters,
      final Instant moduleCallDate) {

    final SpectraMergeSelectParameter mergeSelect = parameters.getParameter(
        NistMsSearchParameters.spectraMergeSelect);

    return new NistPepSearchTask(((NistMsSearchParameters) parameters).toConfig(), featureList, row,
        mergeSelect, NistMsSearchModule.class, parameters, moduleCallDate);
  }
}
