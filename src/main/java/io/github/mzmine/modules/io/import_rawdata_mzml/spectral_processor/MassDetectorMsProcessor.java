/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.io.import_rawdata_mzml.spectral_processor;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.modules.io.import_rawdata_all.AdvancedSpectraImportParameters;
import io.github.mzmine.parameters.ParameterSet;
import org.jetbrains.annotations.NotNull;

public class MassDetectorMsProcessor implements MsProcessor {

  private final MZmineProcessingStep<MassDetector> ms1Detector;
  private final MZmineProcessingStep<MassDetector> ms2Detector;

  public MassDetectorMsProcessor(@NotNull ParameterSet advancedParameters) {
    if (advancedParameters.getParameter(AdvancedSpectraImportParameters.msMassDetection)
        .getValue()) {
      this.ms1Detector = advancedParameters.getParameter(
          AdvancedSpectraImportParameters.msMassDetection).getEmbeddedParameter().getValue();
    } else {
      ms1Detector = null;
    }
    if (advancedParameters.getParameter(AdvancedSpectraImportParameters.ms2MassDetection)
        .getValue()) {
      this.ms2Detector = advancedParameters.getParameter(
          AdvancedSpectraImportParameters.ms2MassDetection).getEmbeddedParameter().getValue();
    } else {
      ms2Detector = null;
    }
  }

  private SimpleSpectralArrays applyMassDetection(MZmineProcessingStep<MassDetector> msDetector,
      final SimpleSpectralArrays spectrum) {
    // run mass detection on data object
    // [mzs, intensities]
    var values = msDetector.getModule()
        .getMassValues(spectrum.mzs(), spectrum.intensities(), msDetector.getParameterSet());
    return new SimpleSpectralArrays(values[0], values[1]);
  }


  public SimpleSpectralArrays processScan(final Scan scan, final SimpleSpectralArrays spectrum) {
    if (ms2Detector != null && scan.getMSLevel() > 1) {
      return applyMassDetection(ms2Detector, spectrum);
    } else if (ms1Detector != null) {
      return applyMassDetection(ms1Detector, spectrum);
    }
    // no mass detection return input
    return spectrum;
  }
}
