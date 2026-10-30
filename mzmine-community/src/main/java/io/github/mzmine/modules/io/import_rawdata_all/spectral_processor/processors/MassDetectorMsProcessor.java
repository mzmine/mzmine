/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MetadataOnlyScan;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.SimpleMassSpectrum;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectors;
import io.github.mzmine.modules.io.import_rawdata_all.AdvancedSpectraImportParameters;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.MsProcessor;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.SimpleSpectralArrays;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter;
import java.util.ArrayList;
import java.util.List;
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
    final MassSpectrumType type = getSpectrumType(metadataOnlyScan);
    var values = msDetector.getMassValues(spectrum.mzs(), spectrum.intensities(), type);
    setCentroided(metadataOnlyScan);
    return new SimpleSpectralArrays(values[0], values[1]);
  }

  /**
   * @return the spectrum type of the metadata scan or centroided if unknown
   */
  private static @NotNull MassSpectrumType getSpectrumType(final @Nullable Scan metadataOnlyScan) {
    if (metadataOnlyScan == null || metadataOnlyScan.getSpectrumType() == null) {
      return MassSpectrumType.CENTROIDED;
    }
    return metadataOnlyScan.getSpectrumType();
  }

  /**
   * update the info so we can just rely on the metadata scan when building the actual scan
   */
  private static void setCentroided(final @Nullable Scan metadataOnlyScan) {
    if (metadataOnlyScan instanceof MetadataOnlyScan ms) {
      ms.setSpectrumType(MassSpectrumType.CENTROIDED);
    }
  }

  /**
   * @return the mass detector for the ms level of this scan or null if mass detection is not active
   * for this ms level or the scan is null
   */
  private @Nullable MassDetector getDetector(final @Nullable Scan metadataOnlyScan) {
    if (metadataOnlyScan == null) {
      return null;
    }
    return metadataOnlyScan.getMSLevel() > 1 ? ms2Detector : ms1Detector;
  }

  @Override
  public @NotNull SimpleSpectralArrays processScan(final @Nullable Scan metadataOnlyScan,
      final @NotNull SimpleSpectralArrays spectrum) {
    final MassDetector detector = getDetector(metadataOnlyScan);
    if (detector == null) {
      // no mass detection return input
      return spectrum;
    }
    return applyMassDetection(detector, metadataOnlyScan, spectrum);
  }

  @Override
  public @NotNull List<SimpleSpectralArrays> processMobilityScans(
      final @Nullable Scan metadataOnlyScan,
      final @NotNull List<SimpleSpectralArrays> mobilityScans) {
    final MassDetector detector = getDetector(metadataOnlyScan);
    if (detector == null || mobilityScans.isEmpty()) {
      return mobilityScans;
    }

    // type is read once before the shared metadata is set to centroided
    final MassSpectrumType type = getSpectrumType(metadataOnlyScan);
    final List<MassSpectrum> spectra = new ArrayList<>(mobilityScans.size());
    for (final SimpleSpectralArrays mobilityScan : mobilityScans) {
      spectra.add(new SimpleMassSpectrum(mobilityScan.mzs(), mobilityScan.intensities(), type));
    }

    final List<double[][]> values = detector.getMobilityScanMassValues(spectra);
    setCentroided(metadataOnlyScan);

    final List<SimpleSpectralArrays> processed = new ArrayList<>(values.size());
    for (final double[][] mobilityScanValues : values) {
      processed.add(new SimpleSpectralArrays(mobilityScanValues[0], mobilityScanValues[1]));
    }
    return processed;
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
