package resolver_tests;

import io.github.mzmine.modules.io.import_rawdata_all.AdvancedSpectraImportParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.MassDetectorWizardOptions;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import org.jetbrains.annotations.NotNull;

/**
 * Wraps a file + import param
 */
record FileToImport(@NotNull String filePath,
                    @NotNull AdvancedSpectraImportParameters importParam) {

  static FileToImport factor5(@NotNull String fileName) {
    return new FileToImport(fileName, factor5);
  }

  static FileToImport centroid500(@NotNull String fileName) {
    return new FileToImport(fileName, centroid500);
  }

  private static final AdvancedSpectraImportParameters factor5 = AdvancedSpectraImportParameters.create(
      MassDetectorWizardOptions.FACTOR_OF_LOWEST_SIGNAL, 5d, 2.5d, null, ScanSelection.MS1, false);

  private static final AdvancedSpectraImportParameters centroid500 = AdvancedSpectraImportParameters.create(
      MassDetectorWizardOptions.ABSOLUTE_NOISE_LEVEL, 500d, 200d, null, ScanSelection.MS1, false);


}
