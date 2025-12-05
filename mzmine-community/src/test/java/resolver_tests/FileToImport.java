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

import io.github.mzmine.modules.io.import_rawdata_all.AdvancedSpectraImportParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.MassDetectorWizardOptions;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import org.jetbrains.annotations.NotNull;

/**
 * Wraps a file + import param
 */
public record FileToImport(@NotNull String filePath,
                           @NotNull AdvancedSpectraImportParameters importParam) {

  private static final AdvancedSpectraImportParameters factor5 = AdvancedSpectraImportParameters.create(
      MassDetectorWizardOptions.FACTOR_OF_LOWEST_SIGNAL, 5d, 2.5d, null, ScanSelection.MS1, false);
  private static final AdvancedSpectraImportParameters centroid500 = AdvancedSpectraImportParameters.create(
      MassDetectorWizardOptions.ABSOLUTE_NOISE_LEVEL, 500d, 200d, null, ScanSelection.MS1, false);

  public static FileToImport factor5(@NotNull String fileName) {
    return new FileToImport(fileName, factor5);
  }

  public static FileToImport centroid500(@NotNull String fileName) {
    return new FileToImport(fileName, centroid500);
  }

  public static FileToImport centroid(@NotNull String fileName, double noiseLevelMs1,
      double noiseLevelMs2) {

    AdvancedSpectraImportParameters param = AdvancedSpectraImportParameters.create(
        MassDetectorWizardOptions.ABSOLUTE_NOISE_LEVEL, noiseLevelMs1, noiseLevelMs2, null,
        ScanSelection.ALL_SCANS, false);
    return new FileToImport(fileName, param);
  }


}
