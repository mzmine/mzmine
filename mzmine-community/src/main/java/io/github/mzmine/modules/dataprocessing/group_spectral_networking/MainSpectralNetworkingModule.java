/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.group_spectral_networking;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.modified_cosine.ModifiedCosineSpectralNetworkingTask;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.ms2deepscore.MS2DeepscoreNetworkingTask;
import io.github.mzmine.modules.impl.AbstractProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

/**
 * This module runs molecular networking based on different algorithms listed in
 * {@link SpectralNetworkingOptions}.
 */
public class MainSpectralNetworkingModule extends AbstractProcessingModule {

  public MainSpectralNetworkingModule() {
    super("Spectral / Molecular Networking", MainSpectralNetworkingParameters.class,
        MZmineModuleCategory.FEATURE_GROUPING,
        "Spectral Networking (Molecular Networking) of fragmentation data");
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull final MZmineProject project,
      @NotNull final ParameterSet parameters, @NotNull final Collection<Task> tasks,
      @NotNull final Instant moduleCallDate) {

    SpectralNetworkingOptions algorithm = parameters.getValue(
        MainSpectralNetworkingParameters.algorithms);
    var featureLists = parameters.getValue(MainSpectralNetworkingParameters.featureLists)
        .getMatchingFeatureLists();
    switch (algorithm) {
      case MODIFIED_COSINE -> {
        // one task for each feature list
        for (final ModularFeatureList featureList : featureLists) {
          var task = new ModifiedCosineSpectralNetworkingTask(parameters, featureList,
              moduleCallDate, this.getClass());
          tasks.add(task);
        }
      }
      case MS2_DEEPSCORE -> {
        // one task for all
        var task = new MS2DeepscoreNetworkingTask(project, featureLists, parameters, null,
            moduleCallDate, this.getClass());
        tasks.add(task);
      }
    }
    return ExitCode.OK;
  }
}
