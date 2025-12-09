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

package resolver_tests;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.preferences.VendorImportParameters;
import io.github.mzmine.gui.preferences.WatersLockmassParameters;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectors;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.exactmass.ExactMassDetectorParameters;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.tof.TofMassDetectorParameters;
import io.github.mzmine.modules.io.import_rawdata_all.AdvancedSpectraImportParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.MassDetectorWizardOptions;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.project.ProjectService;
import java.io.File;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import testutils.MZmineTestUtil;
import testutils.TaskResult;
import testutils.TaskResult.FINISHED;

/**
 * Wraps a file + import param
 */
public record FilesToImport(@NotNull List<String> filePaths,
                            @NotNull AdvancedSpectraImportParameters advancedParam,
                            @NotNull VendorImportParameters vendorParam) {

  private static final AdvancedSpectraImportParameters factor5 = AdvancedSpectraImportParameters.create(
      MassDetectorWizardOptions.FACTOR_OF_LOWEST_SIGNAL, 5d, 2.5d, null, ScanSelection.MS1, false);
  private static final AdvancedSpectraImportParameters centroid500 = AdvancedSpectraImportParameters.create(
      MassDetectorWizardOptions.ABSOLUTE_NOISE_LEVEL, 500d, 200d, null, ScanSelection.MS1, false);
  private static final VendorImportParameters defaultVendorParam = VendorImportParameters.createDefault();
  private static VendorImportParameters vendorParamNoCentroid = VendorImportParameters.create(false,
      VendorImportParameters.DEFAULT_WATERS_OPTION,
      VendorImportParameters.DEFAULT_WATERS_LOCKMASS_ENABLED,
      WatersLockmassParameters.createDefault(),
      VendorImportParameters.DEFAULT_THERMO_EXCEPTION_SIGNALS);

  public static FilesToImport factor5(@NotNull String fileName) {
    return factor5(List.of(fileName));
  }

  public static FilesToImport factor5(@NotNull List<String> fileNames) {
    return new FilesToImport(fileNames, factor5, defaultVendorParam);
  }

  public static FilesToImport centroid500(@NotNull String fileName) {
    return centroid500(List.of(fileName));
  }

  public static FilesToImport centroid500(@NotNull List<String> fileNames) {
    return new FilesToImport(fileNames, centroid500, defaultVendorParam);
  }

  public static FilesToImport centroid(@NotNull String fileName, double noiseLevelMs1,
      double noiseLevelMs2) {
    final AdvancedSpectraImportParameters param = AdvancedSpectraImportParameters.create(
        MassDetectorWizardOptions.ABSOLUTE_NOISE_LEVEL, noiseLevelMs1, noiseLevelMs2, null,
        ScanSelection.ALL_SCANS, false);
    return new FilesToImport(List.of(fileName), param, defaultVendorParam);
  }

  public static FilesToImport tof(@NotNull String fileName, double noiseLevelMs1,
      double noiseLevelMs2, @NotNull AbundanceMeasure intensityCalc) {
    return tof(List.of(fileName), noiseLevelMs1, noiseLevelMs2, intensityCalc);
  }

  public static FilesToImport tof(@NotNull List<String> fileName, double noiseLevelMs1,
      double noiseLevelMs2, @NotNull AbundanceMeasure intensityCalc) {

    AdvancedSpectraImportParameters advancedParam = AdvancedSpectraImportParameters.create(
        MassDetectors.TOF_MASS_DETECTOR,
        TofMassDetectorParameters.create(noiseLevelMs1, intensityCalc),
        MassDetectors.TOF_MASS_DETECTOR,
        TofMassDetectorParameters.create(noiseLevelMs2, intensityCalc), null,
        ScanSelection.ALL_SCANS, false);

    return new FilesToImport(fileName, advancedParam, vendorParamNoCentroid);
  }

  public static FilesToImport exactMass(String mzmineFile, double noiseLevelMs1,
      double noiseLevelMs2) {

    ExactMassDetectorParameters paramMs1 = (ExactMassDetectorParameters) new ExactMassDetectorParameters().cloneParameterSet();
    paramMs1.setParameter(ExactMassDetectorParameters.noiseLevel, noiseLevelMs1);

    ExactMassDetectorParameters paramMs2 = (ExactMassDetectorParameters) new ExactMassDetectorParameters().cloneParameterSet();
    paramMs2.setParameter(ExactMassDetectorParameters.noiseLevel, noiseLevelMs2);

    AdvancedSpectraImportParameters param = AdvancedSpectraImportParameters.create(
        MassDetectors.EXACT, paramMs1, MassDetectors.EXACT, paramMs2, null, ScanSelection.ALL_SCANS,
        false);
    return new FilesToImport(List.of(mzmineFile), param, vendorParamNoCentroid);
  }

  public List<RawDataFile> runImport() {
    try {
      TaskResult taskResult = MZmineTestUtil.importFiles(filePaths, 5_000, vendorParam,
          advancedParam);
      if (taskResult instanceof FINISHED) {
        return filePaths.stream().map(File::new)
            .map(f -> ProjectService.getProject().getDataFileByName(f.getName()))
            .filter(Objects::nonNull).toList();
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    return List.of();
  }
}
