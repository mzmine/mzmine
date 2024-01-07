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
package io.github.mzmine.modules.dataprocessing.id_nist;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.tools.msmsspectramerge.MsMsSpectraMergeParameters;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import java.io.File;
import java.util.Collection;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.filenames.DirectoryParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.util.scans.ScanUtils.IntegerMode;

/**
 * Holds NIST MS Search parameters.
 *
 * @author $Author$
 * @version 2.0
 */
public class NistMsSearchParameters extends SimpleParameterSet {

  /**
   * Feature lists to operate on.
   */
  public static final FeatureListsParameter PEAK_LISTS = new FeatureListsParameter();

  /**
   * NIST MS Search path.
   */
  public static final DirectoryParameter NIST_MS_SEARCH_DIR =
      new DirectoryParameter("NIST MS Search directory",
          "Full path of the directory containing the NIST MS Search executable (nistms$.exe)");

  /**
   * MS Level for search.
   */
  public static final IntegerParameter MS_LEVEL = new IntegerParameter("MS level",
      "Choose MS level for spectal matching. Enter \"1\" for MS1 spectra or ADAP-GC Cluster Spectra.",
      2, 1, 1000);

  /**
   * Match factor cut-off.
   */
  public static final DoubleParameter DOT_PRODUCT = new DoubleParameter("Min cosine similarity",
      "The minimum cosine similarity score (dot product) for identification",
      MZmineCore.getConfiguration().getScoreFormat(),
      0.7, 0.0, 1.0);

  /**
   * Optional MS/MS merging parameters.
   */
  public static final OptionalModuleParameter<MsMsSpectraMergeParameters> MERGE_PARAMETER =
      new OptionalModuleParameter<>("Merge MS/MS (experimental)",
          "Merge high-quality MS/MS instead of exporting just the most intense one.",
          new MsMsSpectraMergeParameters(), false);

  /**
   * Optional MZ rounding.
   */
  public static final OptionalParameter<ComboParameter<IntegerMode>> INTEGER_MZ =
      new OptionalParameter<>(
          new ComboParameter<>("Integer m/z", "Merging mode for fractional m/z to unit mass",
              IntegerMode.values()), false);

  /**
   * Spectrum import option: Overwrite or Append.
   */
  public static final ComboParameter<ImportOption> IMPORT_PARAMETER =
      new ComboParameter<>("Spectrum Import", "Import Options", ImportOption
          .values(), ImportOption.OVERWRITE);

  // NIST MS Search executable.
  private static final String NIST_MS_SEARCH_EXE = "nistms$.exe";

  /**
   * Construct the parameter set.
   */
  public NistMsSearchParameters() {
    super(new Parameter[] {PEAK_LISTS, NIST_MS_SEARCH_DIR, MS_LEVEL, DOT_PRODUCT,
        MERGE_PARAMETER, INTEGER_MZ, IMPORT_PARAMETER},
        "https://mzmine.github.io/mzmine_documentation/module_docs/id_spectra_NIST/NIST-ms-search.html");
  }

  @Override
  public boolean checkParameterValues(final Collection<String> errorMessages) {

    // Unsupported OS.
    if (!isWindows()) {
      errorMessages.add("NIST MS Search is only supported on Windows operating systems.");
      return false;
    }

    boolean result = super.checkParameterValues(errorMessages);

    // NIST MS Search home directory and executable.
    final File executable = getNistMsSearchExecutable();

    // Executable missing.
    if (executable == null || !executable.exists()) {

      errorMessages.add("NIST MS Search executable (" + NIST_MS_SEARCH_EXE
          + ") not found.  Please set the to the full path of the directory containing the NIST MS Search executable.");
      result = false;
    }

    return result;
  }

  /**
   * Gets the full path to the NIST MS Search executable.
   *
   * @return the path.
   */
  public File getNistMsSearchExecutable() {

    final File dir = getParameter(NIST_MS_SEARCH_DIR).getValue();
    return dir == null ? null : new File(dir, NIST_MS_SEARCH_EXE);
  }

  /**
   * Is this a Windows OS?
   *
   * @return true/false if the os.name property does/doesn't contain "Windows".
   */
  private static boolean isWindows() {

    return System.getProperty("os.name").toUpperCase().contains("WINDOWS");
  }
}
