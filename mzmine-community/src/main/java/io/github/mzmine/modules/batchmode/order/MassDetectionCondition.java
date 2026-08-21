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

import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectionModule;
import io.github.mzmine.modules.io.import_rawdata_all.AdvancedSpectraImportParameters;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportParameters;
import io.github.mzmine.parameters.ParameterSet;
import org.jetbrains.annotations.NotNull;

/**
 * Accepts mass detection as a preceding batch step or as part of advanced spectral data import.
 */
public enum MassDetectionCondition implements ModuleOrderCondition {
  INSTANCE;

  @Override
  public @NotNull String description() {
    return "run after standalone mass detection or advanced data import with mass detection enabled";
  }

  @Override
  public boolean isSatisfied(@NotNull final ModuleOrderEvaluationContext context) {
    return context.stepsBefore().stream().anyMatch(
        step -> step.getModule() instanceof MassDetectionModule || hasAdvancedImportMassDetection(
            step));
  }

  private static boolean hasAdvancedImportMassDetection(
      @NotNull final MZmineProcessingStep<?> step) {
    if (!(step.getModule() instanceof AllSpectralDataImportModule)) {
      return false;
    }

    final ParameterSet parameters = step.getParameterSet();
    if (!AllSpectralDataImportParameters.isParameterSetClass(parameters)) {
      return false;
    }

    final ParameterSet advanced = parameters.getEmbeddedParametersIfSelectedOrElse(
        AllSpectralDataImportParameters.advancedImport, null);
    if (advanced == null) {
      return false;
    }

    // decision: Either MS1 or MSn mass detection establishes a configured import-time source.
    return Boolean.TRUE.equals(advanced.getValue(AdvancedSpectraImportParameters.msMassDetection))
        || Boolean.TRUE.equals(advanced.getValue(AdvancedSpectraImportParameters.ms2MassDetection));
  }
}
