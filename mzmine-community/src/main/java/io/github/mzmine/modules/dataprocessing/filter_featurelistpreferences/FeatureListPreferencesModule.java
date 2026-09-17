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

package io.github.mzmine.modules.dataprocessing.filter_featurelistpreferences;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.impl.TaskPerFeatureListModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FeatureListPreferencesModule extends TaskPerFeatureListModule {

  public FeatureListPreferencesModule() {
    super("Redefine feature list preferences", FeatureListPreferencesParameters.class,
        MZmineModuleCategory.FEATURELISTFILTERING, false,
        "Redefine preferences of a feature list, e.g., the sample types used for the RSD columns.");
  }

  @Override
  public @NotNull Task createTask(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Instant moduleCallDate, @Nullable MemoryMapStorage storage,
      @NotNull FeatureList featureList) {
    return new FeatureListPreferencesTask(storage, moduleCallDate,
        (FeatureListPreferencesParameters) parameters, this.getClass(), featureList);
  }

  /**
   * Opens the parameter setup dialog preloaded with the preferences of the first feature list and
   * applies the result to all of them. Called from the feature list context menu and from the
   * feature list summary.
   *
   * @param featureLists the feature lists to redefine, nothing happens if empty
   */
  public static void showSetupAndApply(@NotNull final List<? extends FeatureList> featureLists) {
    final List<ModularFeatureList> modular = featureLists.stream()
        .filter(ModularFeatureList.class::isInstance).map(ModularFeatureList.class::cast).distinct()
        .toList();
    if (modular.isEmpty()) {
      return;
    }

    // the dialog starts on the preferences that are currently in effect for the first list
    final FeatureListPreferencesParameters param = FeatureListPreferencesParameters.fromPreferences(
        modular.getFirst().getPreferences());
    param.setParameter(FeatureListPreferencesParameters.flists, new FeatureListsSelection(modular));

    if (param.showSetupDialog(true) == ExitCode.OK) {
      MZmineCore.runMZmineModule(FeatureListPreferencesModule.class, param);
    }
  }
}
