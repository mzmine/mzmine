package io.github.mzmine.modules.io.import_rawdata_mzml.spectral_processor;

import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.impl.SimpleMassSpectrum;
import io.github.mzmine.datamodel.impl.builders.SimpleBuildingScan;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectors;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.auto.AutoMassDetectorParameters;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.factor_of_lowest.FactorOfLowestMassDetector;
import io.github.mzmine.modules.io.import_rawdata_all.AdvancedSpectraImportParameters;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.MsProcessor;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.MsProcessorList;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.SimpleSpectralArrays;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.processors.CropMzMsProcessor;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.processors.MassDetectorMsProcessor;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.processors.SortByMzMsProcessor;
import io.github.mzmine.parameters.ParameterSet;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Processing all mobility scans of a frame at once ({@link MsProcessor#processMobilityScans},
 * {@link MassDetector#getMobilityScanMassValues}) must reproduce the per spectrum processing.
 */
class MobilityScanProcessingTest {

  private static final int NUM_MOBILITY_SCANS = 5;

  /**
   * Profile spectrum with gaussian peaks on an equidistant m/z grid
   */
  private static @NotNull SimpleSpectralArrays profileSpectrum(final double scale) {
    final double[] centers = {150.0712, 301.1404, 302.1437, 520.3398};
    final int numPoints = 4000;
    final double[] mzs = new double[numPoints];
    final double[] intensities = new double[numPoints];
    for (int i = 0; i < numPoints; i++) {
      final double mz = 100 + i * 0.125;
      mzs[i] = mz;
      double intensity = 0;
      for (final double center : centers) {
        final double delta = (mz - center) / 0.05;
        intensity += scale * center * Math.exp(-0.5 * delta * delta);
      }
      intensities[i] = intensity;
    }
    return new SimpleSpectralArrays(mzs, intensities);
  }

  private static @NotNull List<SimpleSpectralArrays> mobilityScans() {
    final List<SimpleSpectralArrays> scans = new ArrayList<>();
    for (int i = 0; i < NUM_MOBILITY_SCANS; i++) {
      scans.add(profileSpectrum(1 + i));
    }
    return scans;
  }

  private static @NotNull SimpleBuildingScan profileMetadata() {
    return new SimpleBuildingScan(0, 1, PolarityType.POSITIVE, MassSpectrumType.PROFILE, 1f, 0d,
        0);
  }

  private static @NotNull MsProcessorList processorList() {
    final ParameterSet advanced = new AdvancedSpectraImportParameters().cloneParameterSet();
    advanced.setParameter(AdvancedSpectraImportParameters.msMassDetection, true);
    final ParameterSet mdParam = MassDetectors.AUTO.getModuleParameters().cloneParameterSet();
    mdParam.setParameter(AutoMassDetectorParameters.noiseLevel, 10d);
    advanced.getParameter(AdvancedSpectraImportParameters.msMassDetection).getEmbeddedParameter()
        .setValue(MassDetectors.AUTO, mdParam);

    return new MsProcessorList(
        List.of(new CropMzMsProcessor(120d, 600d), new SortByMzMsProcessor(),
            new MassDetectorMsProcessor(advanced)));
  }

  @Test
  void processMobilityScansEqualsProcessScan() {
    final MsProcessorList processor = processorList();
    final List<SimpleSpectralArrays> mobilityScans = mobilityScans();

    // reference: every spectrum with its own metadata, the mass detector sets it to centroided
    final List<SimpleSpectralArrays> expected = new ArrayList<>();
    for (final SimpleSpectralArrays mobilityScan : mobilityScans) {
      expected.add(processor.processScan(profileMetadata(), mobilityScan));
    }

    // one shared metadata instance for all mobility scans, as in the importers
    final SimpleBuildingScan metadata = profileMetadata();
    final List<SimpleSpectralArrays> result = processor.processMobilityScans(metadata,
        mobilityScans);

    Assertions.assertEquals(expected, result);
    // profile data was centroided (4 peaks) in all scans, not just noise filtered
    for (final SimpleSpectralArrays scan : result) {
      Assertions.assertEquals(4, scan.getNumberOfDataPoints());
    }
    Assertions.assertEquals(MassSpectrumType.CENTROIDED, metadata.getSpectrumType());
  }

  @Test
  void processMobilityScansWithoutMassDetection() {
    // ms2 metadata, only ms1 mass detection is active
    final SimpleBuildingScan metadata = new SimpleBuildingScan(0, 2, PolarityType.POSITIVE,
        MassSpectrumType.PROFILE, 1f, 0d, 0);
    final List<SimpleSpectralArrays> result = processorList().processMobilityScans(metadata,
        mobilityScans());

    Assertions.assertEquals(NUM_MOBILITY_SCANS, result.size());
    // not centroided, crop only applies to ms1
    Assertions.assertEquals(4000, result.getFirst().getNumberOfDataPoints());
    Assertions.assertEquals(MassSpectrumType.PROFILE, metadata.getSpectrumType());
  }

  @Test
  void defaultMobilityScanMassValuesEqualSpectrumMassValues() {
    final MassDetector detector = new FactorOfLowestMassDetector(3);
    final List<MassSpectrum> spectra = new ArrayList<>();
    for (final SimpleSpectralArrays arrays : mobilityScans()) {
      spectra.add(
          new SimpleMassSpectrum(arrays.mzs(), arrays.intensities(), MassSpectrumType.CENTROIDED));
    }

    final List<double[][]> values = detector.getMobilityScanMassValues(spectra);
    Assertions.assertEquals(spectra.size(), values.size());
    for (int i = 0; i < spectra.size(); i++) {
      Assertions.assertArrayEquals(detector.getMassValues(spectra.get(i)), values.get(i));
    }
  }

  @Test
  void factorOfLowestArraysEqualSpectrum() {
    // zero intensities must not define the noise level, same for arrays and spectra
    final double[] mzs = {100, 101, 102, 103, 104};
    final double[] intensities = {0, 5, 20, 0, 12};
    final FactorOfLowestMassDetector detector = new FactorOfLowestMassDetector(2);
    final double[][] fromSpectrum = detector.getMassValues(
        new SimpleMassSpectrum(mzs, intensities, MassSpectrumType.CENTROIDED));
    final double[][] fromArrays = detector.getMassValues(mzs, intensities,
        MassSpectrumType.CENTROIDED);

    Assertions.assertArrayEquals(new double[]{102, 104}, fromSpectrum[0]);
    Assertions.assertArrayEquals(fromSpectrum, fromArrays);
  }
}
