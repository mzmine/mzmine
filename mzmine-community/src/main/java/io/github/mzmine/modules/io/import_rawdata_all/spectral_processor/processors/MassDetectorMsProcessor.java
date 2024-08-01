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

package io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.processors;

import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectors;
import io.github.mzmine.modules.io.import_rawdata_all.AdvancedSpectraImportParameters;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.MsProcessor;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.SimpleSpectralArrays;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MassDetectorMsProcessor implements MsProcessor {

  private final MassDetector ms1Detector;
  private final MassDetector ms2Detector;
  private final String description;

  public MassDetectorMsProcessor(@NotNull ParameterSet advanced) {
    StringBuilder descb = new StringBuilder("Applying mass detection on scans:");

    ms1Detector = buildDetector(advanced, AdvancedSpectraImportParameters.msMassDetection, descb,
        "MS1: ");
    ms2Detector = buildDetector(advanced, AdvancedSpectraImportParameters.ms2MassDetection, descb,
        "MS2: ");

    this.description = descb.toString();
  }

  @Nullable
  private MassDetector buildDetector(final @NotNull ParameterSet advanced,
      final OptionalParameter<ModuleOptionsEnumComboParameter<MassDetectors>> msMassDetection,
      final StringBuilder description, final String msLevelStr) {
    var optionalParameter = advanced.getParameter(msMassDetection);
    if (!optionalParameter.getValue()) {
      return null;
    }
    var detector = optionalParameter.getEmbeddedParameter().getValueWithParameters();

    description.append("\n  - ").append(msLevelStr).append(detector.parameters().toString());
    return MassDetectors.createMassDetector(detector);
  }

  private SimpleSpectralArrays applyMassDetection(MassDetector msDetector,
      final @Nullable Scan metadataOnlyScan, final SimpleSpectralArrays spectrum) {
    // run mass detection on data object
    // [mzs, intensities]
    MassSpectrumType type = MassSpectrumType.CENTROIDED;
    if (metadataOnlyScan != null) {
      type = metadataOnlyScan.getSpectrumType();
    }
    if (type == null) {
      type = MassSpectrumType.CENTROIDED;
    }

    var values = msDetector.getMassValues(spectrum.mzs(), spectrum.intensities(), type);
    return new SimpleSpectralArrays(values[0], values[1]);
  }


  @Override
  public @NotNull SimpleSpectralArrays processScan(final @Nullable Scan metadataOnlyScan,
      final @NotNull SimpleSpectralArrays spectrum) {
    if (isMsnActive() && metadataOnlyScan != null && metadataOnlyScan.getMSLevel() > 1) {
      return applyMassDetection(ms2Detector, metadataOnlyScan, spectrum);
    } else if (isMs1Active()) {
      return applyMassDetection(ms1Detector, metadataOnlyScan, spectrum);
    }
    // no mass detection return input
    return spectrum;
  }

  public boolean isMs1Active() {
    return ms1Detector != null;
  }

  public boolean isMsnActive() {
    return ms2Detector != null;
  }

  @Override
  public @NotNull String description() {
    return description;
  }
}
