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

package io.github.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.spectraldatabase;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import org.jetbrains.annotations.NotNull;

/**
 * Module to compare single spectra with spectral libraries
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class SingleSpectrumLibrarySearchModule implements MZmineModule {

  public static final String MODULE_NAME = "Single spectra spectral library search";
  private static final String MODULE_DESCRIPTION =
      "This method compares a scan with a spectral libraries";

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  public @NotNull String getDescription() {
    return MODULE_DESCRIPTION;
  }

  /**
   * Show dialog for spectral db matching for the selected spectra
   * 
   */
  public static void showSpectraIdentificationDialog(final Scan scan,
      final SpectraPlot spectraPlot, @NotNull Instant moduleCallDate) {

    final SingleSpectrumLibrarySearchParameters parameters =
        (SingleSpectrumLibrarySearchParameters) MZmineCore.getConfiguration()
            .getModuleParameters(SingleSpectrumLibrarySearchModule.class);

    // Run task.
    if (parameters.showSetupDialog(scan, null, true) == ExitCode.OK) {

      MZmineCore.getTaskController().addTask(new SingleSpectrumLibrarySearchTask(
          parameters.cloneParameterSet(), scan, spectraPlot, moduleCallDate));
    }
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return SingleSpectrumLibrarySearchParameters.class;
  }

}
