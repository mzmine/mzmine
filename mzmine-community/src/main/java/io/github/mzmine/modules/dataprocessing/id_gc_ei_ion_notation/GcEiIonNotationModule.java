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

package io.github.mzmine.modules.dataprocessing.id_gc_ei_ion_notation;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.batchmode.order.ModuleOrderCondition;
import io.github.mzmine.modules.batchmode.order.ModuleOrderRecommendation;
import io.github.mzmine.modules.batchmode.order.ModuleOrderRule;
import io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc.SpectralDeconvolutionGCModule;
import io.github.mzmine.modules.dataprocessing.id_nist.NistMsSearchModule;
import io.github.mzmine.modules.dataprocessing.id_spectral_library_match.SpectralLibrarySearchModule;
import io.github.mzmine.modules.impl.TaskPerFeatureListModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Calculates EI ion notations ([M]+ and [M-loss]+) for all grouped rows of GC-EI compounds from
 * the molecular formula of the compound annotation.
 */
public class GcEiIonNotationModule extends TaskPerFeatureListModule {

  public static final String NAME = "GC-EI ion notation from annotation formula";

  public GcEiIonNotationModule() {
    super(NAME, GcEiIonNotationParameters.class, MZmineModuleCategory.ION_IDENTITY_NETWORKS, false,
        """
            Positive mode GC-EI compounds only. Uses the molecular formula of the compound \
            annotation to calculate the molecular ion [M]+ and explains every grouped ion as \
            [M]+ minus a neutral loss that is a sub formula of M. The molecular ion does not \
            need to be detected.""");
  }

  @Override
  public @NotNull List<@NotNull ModuleOrderRecommendation> getModuleOrderRecommendations() {
    return List.of(ModuleOrderRecommendation.of(
            "GC-EI ion notations require the compounds of the GC-EI spectral deconvolution",
            ModuleOrderRule.mustRunAfter(SpectralDeconvolutionGCModule.class)),
        ModuleOrderRecommendation.of(
            "GC-EI ion notations use the molecular formula of the compound annotation",
            ModuleOrderRule.ifPresentShouldRunAfter(
                ModuleOrderCondition.anyOf(SpectralLibrarySearchModule.class,
                    NistMsSearchModule.class))));
  }

  @Override
  public @NotNull Task createTask(@NotNull final MZmineProject project,
      @NotNull final ParameterSet parameters, @NotNull final Instant moduleCallDate,
      @Nullable final MemoryMapStorage storage, @NotNull final FeatureList featureList) {
    return new GcEiIonNotationTask(storage, moduleCallDate, parameters, this.getClass(),
        featureList);
  }
}
