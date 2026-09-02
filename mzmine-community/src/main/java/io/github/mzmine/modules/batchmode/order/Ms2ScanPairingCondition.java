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

package io.github.mzmine.modules.batchmode.order;

import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.FeatureResolverModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.GeneralResolverParameters;
import io.github.mzmine.modules.dataprocessing.filter_diams2.DiaMs2CorrModule;
import io.github.mzmine.modules.dataprocessing.filter_groupms2.GroupMS2Module;
import io.github.mzmine.parameters.ParameterSet;
import org.jetbrains.annotations.NotNull;

/**
 * Accepts MS2 scan pairing by a feature resolver or a dedicated MS2 pairing module.
 */
public enum Ms2ScanPairingCondition implements ModuleOrderCondition {
  INSTANCE;

  @Override
  public @NotNull String description() {
    return """
        MS2 spectra are paried by a feature resolver ("%s" parameter), "%s" module, or "%s" module.
        """.formatted(GeneralResolverParameters.groupMS2Parameters.getName(),
        GroupMS2Module.MODULE_NAME, DiaMs2CorrModule.NAME);
  }

  @Override
  public boolean matches(
      @NotNull final MZmineProcessingStep<? extends MZmineProcessingModule> step) {
    return step.getModule() instanceof GroupMS2Module
        || step.getModule() instanceof DiaMs2CorrModule || hasResolverMs2ScanPairing(step);
  }

  private static boolean hasResolverMs2ScanPairing(@NotNull final MZmineProcessingStep<?> step) {
    if (!(step.getModule() instanceof FeatureResolverModule)) {
      return false;
    }

    final ParameterSet parameters = step.getParameterSet();
    if (parameters == null || !parameters.hasParameter(
        GeneralResolverParameters.groupMS2Parameters)) {
      return false;
    }
    return Boolean.TRUE.equals(parameters.getValue(GeneralResolverParameters.groupMS2Parameters));
  }
}
