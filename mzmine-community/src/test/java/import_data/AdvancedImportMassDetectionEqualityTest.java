package import_data;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectionModule;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectionParameters;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectors;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.SelectedScanTypes;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.factor_of_lowest.FactorOfLowestMassDetectorParameters;
import io.github.mzmine.modules.io.import_rawdata_all.AdvancedSpectraImportParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import testutils.MZmineTestUtil;
import testutils.TaskResult;

/**
 * Mass detection during the advanced import must give the same mass lists as a plain import
 * followed by the mass detection module with the same detector. Covers scans, frames and mobility
 * scans.
 */
@TestInstance(Lifecycle.PER_CLASS)
@DisabledOnOs(OS.MAC)
class AdvancedImportMassDetectionEqualityTest {

  private static final double NOISE_FACTOR = 3d;

  @BeforeAll
  void initialize() {
    MZmineTestUtil.startMzmineCore();
  }

  @AfterAll
  void tearDown() {
    MZmineTestUtil.cleanProject();
  }

  private static @NotNull ParameterSet detectorParameters() {
    final ParameterSet detectorParam = MassDetectors.FACTOR_OF_LOWEST.getModuleParameters()
        .cloneParameterSet();
    detectorParam.setParameter(FactorOfLowestMassDetectorParameters.noiseFactor, NOISE_FACTOR);
    return detectorParam;
  }

  /**
   * Only mass detection and optional MSn denormalization, no cropping, so both pipelines run the
   * same steps
   */
  private static @NotNull AdvancedSpectraImportParameters advancedImportParameters(
      final boolean denormalizeMSnScans) {
    return AdvancedSpectraImportParameters.create(MassDetectors.FACTOR_OF_LOWEST,
        detectorParameters(), MassDetectors.FACTOR_OF_LOWEST, detectorParameters(), null,
        ScanSelection.ALL_SCANS, denormalizeMSnScans);
  }

  private static @NotNull MassDetectionParameters massDetectionParameters(
      @NotNull final RawDataFile raw, final boolean denormalizeMSnScans) {
    return MassDetectionParameters.create(new RawDataFilesSelection(new RawDataFile[]{raw}),
        ScanSelection.ALL_SCANS, SelectedScanTypes.SCANS, denormalizeMSnScans,
        MassDetectors.FACTOR_OF_LOWEST, detectorParameters());
  }

  private static @NotNull RawDataFile getRaw(@NotNull final String file) {
    final RawDataFile raw = MZmineTestUtil.getRawFromProject(new File(file).getName());
    Assertions.assertNotNull(raw, "File not imported: " + file);
    return raw;
  }

  /**
   * Copies all mass lists to memory, so they stay available after the project is replaced
   */
  private static @NotNull List<SpectrumMassList> extractMassLists(@NotNull final RawDataFile raw) {
    final List<SpectrumMassList> massLists = new ArrayList<>();
    for (final Scan scan : raw.getScans()) {
      final String scanId = "scan #" + scan.getScanNumber();
      massLists.add(SpectrumMassList.of(scanId, scan.getMassList()));
      if (scan instanceof Frame frame) {
        for (final MobilityScan mobilityScan : frame.getMobilityScans()) {
          massLists.add(SpectrumMassList.of(
              scanId + " mobility scan #" + mobilityScan.getMobilityScanNumber(),
              mobilityScan.getMassList()));
        }
      }
    }
    return massLists;
  }

  @ParameterizedTest
  @CsvSource({"rawdatafiles/DOM_a.mzML, false", "rawdatafiles/additional/orbi_idx_msn.mzML, true",
      "rawdatafiles/additional/lc-tims-ms-pasef-a.d, false"})
  void advancedImportEqualsMassDetection(@NotNull final String file,
      final boolean denormalizeMSnScans) throws InterruptedException {
    MZmineTestUtil.cleanProject();
    MZmineTestUtil.importFiles(List.of(file), 360,
        advancedImportParameters(denormalizeMSnScans));
    final List<SpectrumMassList> advancedImport = extractMassLists(getRaw(file));

    MZmineTestUtil.cleanProject();
    MZmineTestUtil.importFiles(List.of(file), 360);
    final RawDataFile raw = getRaw(file);
    final TaskResult result = MZmineTestUtil.callModuleWithTimeout(360, MassDetectionModule.class,
        massDetectionParameters(raw, denormalizeMSnScans));
    Assertions.assertInstanceOf(TaskResult.FINISHED.class, result, result.description());
    final List<SpectrumMassList> massDetection = extractMassLists(raw);

    Assertions.assertEquals(massDetection.size(), advancedImport.size(),
        "Number of spectra differs in " + file);
    int numDataPoints = 0;
    for (int i = 0; i < massDetection.size(); i++) {
      final SpectrumMassList expected = massDetection.get(i);
      final SpectrumMassList actual = advancedImport.get(i);
      final String msg = file + " " + expected.id();
      Assertions.assertEquals(expected.id(), actual.id(), msg);
      Assertions.assertArrayEquals(expected.mzs(), actual.mzs(), msg);
      Assertions.assertArrayEquals(expected.intensities(), actual.intensities(), msg);
      numDataPoints += expected.mzs().length;
    }
    // the comparison must not pass on empty mass lists
    Assertions.assertTrue(numDataPoints > 0, "No mass list data points in " + file);
  }
}
