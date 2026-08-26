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
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.SpectraMergeSelectParameter;
import io.github.mzmine.modules.dataprocessing.id_nist_mspepsearch.NistSearchConfig;
import io.github.mzmine.modules.dataprocessing.id_nist_mspepsearch.NistSearchMode;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.CheckComboParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.filenames.DirectoryParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.scans.ScanUtils.IntegerMode;
import java.io.File;
import java.util.Collection;
import java.util.List;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Parameters of the NIST MS search module.
 * <p>
 * Deliberately small: a search type picks one of the presets NIST recommends and everything the
 * preset implies is fixed in {@link NistSearchConfig} and the command builder rather than exposed
 * here.
 */
public class NistMsSearchParameters extends SimpleParameterSet {

  /**
   * The search types offered. The hybrid search is left out - it needs the molecular weight of the
   * unknown, which MSPepSearch only accepts as a single global value.
   */
  private static final NistSearchMode[] SEARCH_MODES = {NistSearchMode.GC_EI_IDENTITY,
      NistSearchMode.GC_EI_SIMILARITY, NistSearchMode.MSMS_HIRES};

  public static final FeatureListsParameter PEAK_LISTS = new FeatureListsParameter();

  public static final DirectoryParameter NIST_DIRECTORY = new DirectoryParameter(
      "NIST installation directory", """
      The NIST installation directory, for example D:\\NIST26.
      It must contain the MSPepSearch sub directory with MSPepSearch64.exe, and the library sub \
      directories such as mainlib, replib or hr_msms_nist.""");

  public static final CheckComboParameter<String> LIBRARIES = new CheckComboParameter<>("Libraries",
      """
          The libraries to search, discovered in the installation directory above. Select at least \
          one and at most 16.
          Reopen this dialog after changing the installation directory to refresh the list.
          Use the EI libraries (mainlib, replib) for GC-EI searches and the tandem libraries \
          (hr_msms_nist, lr_msms_nist, apci_msms_nist) for MS/MS searches.""", List.of(), List.of(),
      true);

  public static final ComboParameter<NistSearchMode> SEARCH_MODE = new ComboParameter<>(
      "Search type", """
      The NIST search preset to use.
      The GC-EI searches work on unit mass EI spectra and use the NIST main and replicate \
      libraries; identity finds the compound itself, similarity also finds related compounds that \
      are not in the library. MS/MS works on accurate mass spectra and uses the tandem libraries.""",
      SEARCH_MODES, NistSearchMode.MSMS_HIRES);

  public static final SpectraMergeSelectParameter spectraMergeSelect = SpectraMergeSelectParameter.createLimitedToFewScans();

  /**
   * Kept under its original name so that saved batches keep their value. MSPepSearch filters on the
   * NIST match factor, which is this score times 1000.
   */
  public static final DoubleParameter DOT_PRODUCT = new DoubleParameter("Min cosine similarity",
      """
          The minimum similarity score of a reported hit, on mzmine's 0 to 1 scale.
          This is the NIST match factor divided by 1000 (MSPepSearch /MinMF): 0.7 and above is \
          usually considered a good match, 0.9 and above an excellent one.""",
      MZmineCore.getConfiguration().getScoreFormat(), 0.7, 0.0, 1.0);

  public static final MZToleranceParameter PRECURSOR_TOLERANCE = new MZToleranceParameter(
      "Precursor m/z tolerance", """
      MS/MS only. MSPepSearch /Z or /ZPPM: the precursor ion m/z uncertainty.
      MSPepSearch takes either a ppm or an absolute value, not the maximum of both: if the ppm value \
      is greater than zero it is used, otherwise the absolute value is.""", 0.005, 20);

  public static final MZToleranceParameter FRAGMENT_TOLERANCE = new MZToleranceParameter(
      "Fragment m/z tolerance", """
      MS/MS only. MSPepSearch /M or /MPPM: the product ion m/z uncertainty.
      MSPepSearch takes either a ppm or an absolute value, not the maximum of both: if the ppm value \
      is greater than zero it is used, otherwise the absolute value is.""", 0.01, 40);

  public static final OptionalParameter<ComboParameter<IntegerMode>> INTEGER_MZ = new OptionalParameter<>(
      new ComboParameter<>("Integer m/z", """
          GC-EI only. Merge fractional m/z to unit mass before searching, as the NIST EI libraries \
          are unit mass.
          Only needed if your spectra are not already centroided to unit mass.""",
          IntegerMode.values(), IntegerMode.SUM), false);

  public NistMsSearchParameters() {
    super(
        "https://mzmine.github.io/mzmine_documentation/module_docs/id_spectra_NIST/NIST-ms-search.html",
        PEAK_LISTS, NIST_DIRECTORY, LIBRARIES, SEARCH_MODE, spectraMergeSelect, DOT_PRODUCT,
        PRECURSOR_TOLERANCE, FRAGMENT_TOLERANCE, INTEGER_MZ);
  }

  /**
   * @return the search configuration these parameters describe.
   */
  public @NotNull NistSearchConfig toConfig() {

    final List<String> selected = getValue(LIBRARIES);
    final Double minSimilarity = getValue(DOT_PRODUCT);
    final IntegerMode integerMz = Boolean.TRUE.equals(getValue(INTEGER_MZ)) ? getParameter(
        INTEGER_MZ).getEmbeddedParameter().getValue() : null;

    final NistSearchMode mode = getValue(SEARCH_MODE);

    return new NistSearchConfig(getValue(NIST_DIRECTORY),
        selected == null ? List.of() : List.copyOf(selected),
        mode == null ? NistSearchMode.MSMS_HIRES : mode,
        // MSPepSearch filters on the match factor, which is the score on a 0 to 999 scale
        minSimilarity == null ? 0 : (int) Math.round(minSimilarity * 1000),
        getValue(PRECURSOR_TOLERANCE), getValue(FRAGMENT_TOLERANCE), integerMz);
  }

  @Override
  public ExitCode showSetupDialog(final boolean valueCheckRequired) {

    // Prefill the installation directory and populate the library list before the dialog opens, so
    // that the choices always reflect the installation that is actually configured.
    if (getValue(NIST_DIRECTORY) == null) {
      final File discovered = NistSearchConfig.discoverInstallation();
      if (discovered != null) {
        getParameter(NIST_DIRECTORY).setValue(discovered);
      }
    }
    refreshLibraryChoices();

    return super.showSetupDialog(valueCheckRequired);
  }

  /**
   * Rediscovers the libraries of the configured installation and offers them as choices, keeping any
   * selection that still exists.
   */
  public void refreshLibraryChoices() {

    final List<String> names = NistSearchConfig.discoverLibraryNames(getValue(NIST_DIRECTORY));
    getParameter(LIBRARIES).setChoices(names.toArray(String[]::new));

    final List<String> selected = getValue(LIBRARIES);
    if (selected != null && !selected.isEmpty()) {
      getParameter(LIBRARIES).setValue(selected.stream().filter(names::contains).toList());
    }
  }

  @Override
  public boolean checkParameterValues(final Collection<String> errorMessages) {

    final boolean valid = super.checkParameterValues(errorMessages);

    // the installation, library and search type rules all live on the config
    final List<String> problems = toConfig().validate();
    errorMessages.addAll(problems);

    return valid && problems.isEmpty();
  }

  @Override
  public int getVersion() {
    return 4;
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }

  @Override
  public @Nullable String getVersionMessage(int version) {
    return switch (version) {
      case 3 -> "Improved spectral merging options. Please reconfigure the NIST MS search step.";
      case 4 -> """
          NIST MS search now runs NIST's command line program MSPepSearch instead of the MS Search \
          user interface. Please set the NIST installation directory and select the libraries to \
          search.""";
      default -> null;
    };
  }

  @Override
  public @Nullable Region getMessage() {
    return FxTextFlows.newTextFlowInAccordion("Information", true,
        text("This module runs NIST's command line search program "), italicText("MSPepSearch"),
        text(", which needs no visible NIST MS Search window and therefore also works in batch mode "
            + "and on a headless machine. All spectra are written to a single query file and "
            + "searched in one run.\n"), boldText("Requires a licensed NIST library installation"),
        text(" - MSPepSearch ships with NIST 17 and newer.\n"), text(
            "NIST does not return the library spectra themselves, so the mirror plot of a hit stays "
                + "empty. Open the compound in NIST MS Search to inspect the reference spectrum."));
  }
}
