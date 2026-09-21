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
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectionModule;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectionParameters;
import io.github.mzmine.modules.io.import_rawdata_all.AdvancedSpectraImportParameters;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportParameters;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.MSLevel;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import org.jetbrains.annotations.NotNull;

/**
 * Accepts mass detection as a preceding batch step or as part of advanced spectral data import.
 */
public enum MassDetectionCondition implements ModuleOrderCondition {
  /**
   * Mass detection specifically on ms1.
   */
  MS1(MSLevel.MSONE),
  /**
   * Mass detection specifically on ms level 2 and/or higher
   */
  MSn(MSLevel.MSMS),
  /**
   * Mass detection executed on any ms level (1 and 2 or higher are ok).
   */
  MSany(MSLevel.MSANY);

  final MSLevel msLevel;

  MassDetectionCondition(MSLevel msLevel) {
    this.msLevel = msLevel;
  }

  @Override
  public @NotNull String description() {
    return "standalone mass detection or advanced data import with mass detection enabled";
  }

  @Override
  public boolean matches(
      @NotNull final MZmineProcessingStep<? extends MZmineProcessingModule> step) {
    return matchesMassDetectionStep(step) || matchesAdvancedImportMassDetection(step);
  }

  private boolean matchesMassDetectionStep(@NotNull final MZmineProcessingStep<?> step) {
    if (!(step.getModule() instanceof MassDetectionModule)) {
      return false;
    }
    final ParameterSet param = step.getParameterSet();
    final ScanSelection scanSelection = param.getValue(MassDetectionParameters.scanSelection);
    final MsLevelFilter msLevelFilter = scanSelection.getMsLevelFilter();

    return switch (this) {
      case MS1 -> msLevelFilter.accept(1);
      case MSn -> msLevelFilter.accept(2);
      case MSany -> msLevelFilter.accept(1) || msLevelFilter.accept(2);
    };
  }

  private boolean matchesAdvancedImportMassDetection(@NotNull final MZmineProcessingStep<?> step) {
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

    return switch (this) {
      case MS1 ->
          Boolean.TRUE.equals(advanced.getValue(AdvancedSpectraImportParameters.msMassDetection));
      case MSn ->
          Boolean.TRUE.equals(advanced.getValue(AdvancedSpectraImportParameters.ms2MassDetection));
      case MSany ->
          Boolean.TRUE.equals(advanced.getValue(AdvancedSpectraImportParameters.msMassDetection))
              || Boolean.TRUE.equals(
              advanced.getValue(AdvancedSpectraImportParameters.ms2MassDetection));
    };
  }
}
