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

package io.github.mzmine.modules.dataprocessing.filter_duplicatefilter_gc_ei;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.batchmode.order.ModuleCategoryOrderCondition;
import io.github.mzmine.modules.batchmode.order.ModuleOrderRecommendation;
import io.github.mzmine.modules.batchmode.order.ModuleOrderRule;
import io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc.SpectralDeconvolutionGCModule;
import io.github.mzmine.modules.impl.TaskPerFeatureListModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Duplicate row filter for GC-EI data. Compares retention time, quantifier m/z, and the similarity
 * of the deconvoluted pseudo spectra.
 */
public class GcEiDuplicateFilterModule extends TaskPerFeatureListModule {

  public static final String NAME = "GC-EI duplicate row filter";

  public GcEiDuplicateFilterModule() {
    super(NAME, GcEiDuplicateFilterParameters.class, MZmineModuleCategory.FEATURELISTFILTERING,
        true, """
            Removes duplicate rows of the same compound from GC-EI feature lists. Duplicates are \
            rows with similar retention time and similar pseudo spectra, which share their \
            quantifier ions.""");
  }

  @Override
  public @NotNull Task createTask(@NotNull final MZmineProject project,
      @NotNull final ParameterSet parameters, @NotNull final Instant moduleCallDate,
      @Nullable final MemoryMapStorage storage, @NotNull final FeatureList featureList) {
    return new GcEiDuplicateFilterTask(project, featureList, storage, moduleCallDate, parameters,
        getClass());
  }

  @Override
  public @NotNull List<@NotNull ModuleOrderRecommendation> getModuleOrderRecommendations() {
    return List.of(ModuleOrderRecommendation.of(
            "GC-EI duplicate filtering uses the pseudo spectra of the GC-EI spectral deconvolution.",
            ModuleOrderRule.mustRunAfter(SpectralDeconvolutionGCModule.class)),
        ModuleOrderRecommendation.of(
            "Duplicate filtering should run after Gap filling/Secondary feature finding.",
            ModuleOrderRule.ifPresentMustRunAfter(
                ModuleCategoryOrderCondition.of(MZmineModuleCategory.GAPFILLING))));
  }
}
