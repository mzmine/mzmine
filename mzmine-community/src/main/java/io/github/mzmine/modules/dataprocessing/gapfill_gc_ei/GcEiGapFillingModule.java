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

package io.github.mzmine.modules.dataprocessing.gapfill_gc_ei;

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
 * Gap filling for GC-EI data. A gap is only filled if the top signals of the pseudo spectrum of a
 * row are re-detected as co-eluting features with the original chromatogram builder and resolver
 * settings.
 */
public class GcEiGapFillingModule extends TaskPerFeatureListModule {

  public static final String NAME = "GC-EI gap filling";

  public GcEiGapFillingModule() {
    super(NAME, GcEiGapFillingParameters.class, MZmineModuleCategory.GAPFILLING, true, """
        Fills missing features (gaps) of GC-EI feature lists. Re-detects the most intense signals \
        of the pseudo spectrum of each row with the original chromatogram builder and resolver \
        settings and adds the quantifier feature if all signals co-elute.""");
  }

  @Override
  public @NotNull Task createTask(@NotNull final MZmineProject project,
      @NotNull final ParameterSet parameters, @NotNull final Instant moduleCallDate,
      @Nullable final MemoryMapStorage storage, @NotNull final FeatureList featureList) {
    return new GcEiGapFillingTask(project, featureList, storage, moduleCallDate, parameters,
        getClass());
  }

  @Override
  public @NotNull List<@NotNull ModuleOrderRecommendation> getModuleOrderRecommendations() {
    return List.of(ModuleOrderRecommendation.of(
            "Gap filling must run after sample alignment. Without prior alignment gap filling does not change anything.",
            ModuleOrderRule.mustRunAfter(
                ModuleCategoryOrderCondition.of(MZmineModuleCategory.ALIGNMENT))),
        ModuleOrderRecommendation.of(
            "GC-EI gap filling uses the pseudo spectra of the GC-EI spectral deconvolution.",
            ModuleOrderRule.mustRunAfter(SpectralDeconvolutionGCModule.class)));
  }
}
