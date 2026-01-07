/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

import static io.github.mzmine.javafx.components.factories.FxTexts.boldText;
import static io.github.mzmine.javafx.components.factories.FxTexts.italicText;
import static io.github.mzmine.javafx.components.factories.FxTexts.text;

import io.github.mzmine.javafx.components.factories.FxTextFlows;
import io.github.mzmine.javafx.components.factories.FxTexts;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.SpectraMergeSelectParameter;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.options.SpectraMergeSelectPresets;
import io.github.mzmine.modules.tools.msmsspectramerge.MsMsSpectraMergeParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.filenames.DirectoryParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.scans.ScanUtils.IntegerMode;
import java.io.File;
import java.util.Collection;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
  public static final DirectoryParameter NIST_MS_SEARCH_DIR = new DirectoryParameter(
      "NIST MS Search directory",
      "Full path of the directory containing the NIST MS Search executable (nistms$.exe)");

  /**
   * Match factor cut-off.
   */
  public static final DoubleParameter DOT_PRODUCT = new DoubleParameter("Min cosine similarity",
      "The minimum cosine similarity score (dot product) for identification",
      MZmineCore.getConfiguration().getScoreFormat(), 0.7, 0.0, 1.0);

  public static final SpectraMergeSelectParameter spectraMergeSelect = SpectraMergeSelectParameter.createLimitedToFewScans();

  /**
   * Optional MZ rounding.
   */
  public static final OptionalParameter<ComboParameter<IntegerMode>> INTEGER_MZ = new OptionalParameter<>(
      new ComboParameter<>("Integer m/z", "Merging mode for fractional m/z to unit mass",
          IntegerMode.values()), false);

  /**
   * Spectrum import option: Overwrite or Append.
   */
  public static final ComboParameter<ImportOption> IMPORT_PARAMETER = new ComboParameter<>(
      "Spectrum Import",
      "Select if the spectra shall be added to the NIST search history (Append) or if the search history shall be overwritten (Overwrite).",
      ImportOption.values(), ImportOption.OVERWRITE);

  // NIST MS Search executable.
  private static final String NIST_MS_SEARCH_EXE = "nistms$.exe";

  /**
   * Construct the parameter set.
   */
  public NistMsSearchParameters() {
    super(
        "https://mzmine.github.io/mzmine_documentation/module_docs/id_spectra_NIST/NIST-ms-search.html",
        PEAK_LISTS, NIST_MS_SEARCH_DIR, DOT_PRODUCT, spectraMergeSelect, INTEGER_MZ,
        IMPORT_PARAMETER);
  }

  /**
   * Is this a Windows OS?
   *
   * @return true/false if the os.name property does/doesn't contain "Windows".
   */
  private static boolean isWindows() {

    return System.getProperty("os.name").toUpperCase().contains("WINDOWS");
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

  @Override
  public int getVersion() {
    return 3;
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }

  @Override
  public @Nullable String getVersionMessage(int version) {
    return switch (version) {
      case 3 -> "Improved spectral merging options. Please reconfigure the NIST MS search step.";
      default -> null;
    };
  }

  @Override
  public @Nullable Region getMessage() {
    return FxTextFlows.newTextFlowInAccordion("Information", true, text("""
            You must have a valid NIST library installation and the "Automation" check box under"""),
        italicText(" Options"), text(" -> "), italicText("Library search options"), text(" -> "),
        italicText("Other options"), text(" -> "), italicText("Automation"),
        text(" must be enabled."),
        text("\nThis search may take longer than spectral library searches in mzmine."),
        text("\nWe recommend to "), boldText("not interact"),
        text(" with the MS Search interface during the search."));
  }
}
