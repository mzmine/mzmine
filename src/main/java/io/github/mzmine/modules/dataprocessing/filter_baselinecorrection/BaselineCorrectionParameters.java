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

package io.github.mzmine.modules.dataprocessing.filter_baselinecorrection;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.filter_baselinecorrection.correctors.AsymmetryCorrector;
import io.github.mzmine.modules.dataprocessing.filter_baselinecorrection.correctors.LocMinLoessCorrector;
import io.github.mzmine.modules.dataprocessing.filter_baselinecorrection.correctors.PeakDetectionCorrector;
import io.github.mzmine.modules.dataprocessing.filter_baselinecorrection.correctors.RollingBallCorrector;
import io.github.mzmine.modules.dataprocessing.filter_baselinecorrection.correctors.RubberBandCorrector;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.ModuleComboParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.R.REngineType;

/**
 * Holds baseline correction module COMMON parameters. See
 * "io.github.mzmine.modules.rawdatamethods.filtering.baselinecorrection.correctors" sub-package for
 * method specific parameters.
 * 
 */
public class BaselineCorrectionParameters extends SimpleParameterSet {

  // Keep access to those parameters (use only from child ParametersDialogs).
  private static BaselineCorrectionParameters thisParameters = null;

  protected static BaselineCorrectionParameters getBaselineCorrectionParameters() {
    return thisParameters;
  }

  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

  /**
   * Raw data file suffix.
   */
  public static final StringParameter SUFFIX = new StringParameter("Filename suffix",
      "Suffix to be appended to raw data file names.", "baseline-corrected");

  /**
   * Chromatogram type.
   */
  public static final ComboParameter<ChromatogramType> CHROMOTAGRAM_TYPE =
      new ComboParameter<ChromatogramType>("Chromatogram type",
          "The type of chromatogram from which infer a baseline to be corrected.",
          ChromatogramType.values(), ChromatogramType.TIC);

  /**
   * List of available baseline correctors
   */
  public static final BaselineCorrector baselineCorrectors[] = {new AsymmetryCorrector(), // (Package
                                                                                          // R "ptw"
                                                                                          // -
                                                                                          // http://cran.r-project.org/web/packages/ptw/ptw.pdf)
      new RollingBallCorrector(), // (Package R "baseline" -
                                  // http://cran.r-project.org/web/packages/baseline/baseline.pdf)
      new PeakDetectionCorrector(), // (Package R "baseline" -
                                    // http://cran.r-project.org/web/packages/baseline/baseline.pdf)
      new RubberBandCorrector(), // (Package R "hyperSpec" -
                                 // http://cran.r-project.org/web/packages/hyperSpec/vignettes/baseline.pdf)
      new LocMinLoessCorrector() // (Package R/Bioc. "PROcess" -
                                 // http://bioconductor.org/packages/release/bioc/manuals/PROcess/man/PROcess.pdf)
  };

  public static final ModuleComboParameter<BaselineCorrector> BASELINE_CORRECTORS =
      new ModuleComboParameter<BaselineCorrector>("Correction method",
          "Alternative baseline correction methods", baselineCorrectors, baselineCorrectors[0]);

  /**
   * Apply in bins.
   */
  public static final BooleanParameter USE_MZ_BINS = new BooleanParameter("Use m/z bins",
      "If checked, then full m/z range will be divided into bins a baseline correction applied to each bin (see m/z bin width).",
      true);

  /**
   * M/Z bin width.
   */
  public static final DoubleParameter MZ_BIN_WIDTH =
      new DoubleParameter("m/z bin width",
          "The m/z bin size (>= 0.001) to use when the \"" + USE_MZ_BINS.getName()
              + "\" option is enabled.",
          MZmineCore.getConfiguration().getMZFormat(), 1.0, 0.001, null);

  /**
   * Scans
   */
  public static final IntegerParameter MS_LEVEL =
      new IntegerParameter("MS level", "MS level of scans to apply this method to", 1, 1, null);

  /**
   * Remove original data file.
   */
  public static final BooleanParameter REMOVE_ORIGINAL =
      new BooleanParameter("Remove source file after baseline correction",
          "If checked, original file will be replaced by the corrected version", true);

  /**
   * R engine type.
   */
  public static final ComboParameter<REngineType> RENGINE_TYPE = new ComboParameter<REngineType>(
      "R engine", "The R engine to be used for communicating with R.", REngineType.values(),
      REngineType.RCALLER);

  /**
   * Create the parameter set.
   */
  public BaselineCorrectionParameters() {
    super(new Parameter[] {dataFiles, SUFFIX, CHROMOTAGRAM_TYPE, MS_LEVEL, USE_MZ_BINS,
        MZ_BIN_WIDTH, BASELINE_CORRECTORS, RENGINE_TYPE, REMOVE_ORIGINAL},
        "https://mzmine.github.io/mzmine_documentation/module_docs/filter_raw_data/baseline-corrections.html");
    thisParameters = null;
  }

  /**
   * Use an InstantUpdateSetupDialog setup dialog instead of the regular one.
   */
  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    Parameter<?>[] parameters = this.getParameters();
    if ((parameters == null) || (parameters.length == 0))
      return ExitCode.OK;

    thisParameters = this;
    ParameterSetupDialog dialog = new InstantUpdateSetupDialog(valueCheckRequired, this);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

}
