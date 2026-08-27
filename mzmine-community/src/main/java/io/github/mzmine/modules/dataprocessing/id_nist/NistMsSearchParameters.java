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
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.SpectraMergeSelectParameter;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.options.SpectraMergeSelectPresets;
import io.github.mzmine.modules.dataprocessing.id_nist_mspepsearch.NistSearchConfig;
import io.github.mzmine.modules.dataprocessing.id_nist_mspepsearch.NistSearchMode;
import io.github.mzmine.modules.presets.ModulePreset;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.filenames.DirectoryComponent;
import io.github.mzmine.parameters.parametertypes.filenames.DirectoryParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
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
 * Deliberately small: a search type picks one of the presets NIST recommends, and both the
 * libraries to search and everything else the preset implies follow from it, see
 * {@link NistSearchConfig}.
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
      The NIST installation directory, for example C:\\NIST26. Use the button to detect it \
      automatically.
      It must contain the MSPepSearch sub directory with MSPepSearch64.exe and the library sub \
      directories such as mainlib or hr_msms_nist.""", NistMsSearchParameters::addAutoDetectButton);

  public static final ComboParameter<NistSearchMode> SEARCH_MODE = new ComboParameter<>(
      "Search type", """
      The NIST search preset to use, which also picks the libraries.
      The GC-EI searches run on unit mass EI spectra against all EI libraries of the installation \
      (mainlib, replib); identity finds the compound itself, similarity also finds related \
      compounds that are not in the library. MS/MS runs on accurate mass spectra against all \
      tandem libraries (hr_msms_nist, lr_msms_nist, apci_msms_nist).""", SEARCH_MODES,
      NistSearchMode.MSMS_HIRES);

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
        PEAK_LISTS, NIST_DIRECTORY, SEARCH_MODE, spectraMergeSelect, DOT_PRODUCT,
        PRECURSOR_TOLERANCE, FRAGMENT_TOLERANCE, INTEGER_MZ);
  }

  /**
   * Adds the auto detect button next to the installation directory field.
   */
  private static void addAutoDetectButton(@NotNull final DirectoryComponent component) {

    component.addRightControl(FxIconUtil.newIconButton(FxIcons.SEARCH,
        "Detect the NIST installation, for example C:\\NIST26 or D:\\NIST26", () -> {

          final File discovered = NistSearchConfig.discoverInstallation();
          if (discovered != null) {
            component.setValue(discovered);
          } else {
            DialogLoggerUtil.showMessageDialog("No NIST installation found",
                "No directory with MSPepSearch was found in the drive roots or program folders. "
                    + "Select the NIST installation directory, for example D:\\NIST26, manually.");
          }
        }));
  }

  /**
   * @return the search configuration these parameters describe.
   */
  public @NotNull NistSearchConfig toConfig() {

    final Double minSimilarity = getValue(DOT_PRODUCT);
    final IntegerMode integerMz = Boolean.TRUE.equals(getValue(INTEGER_MZ)) ? getParameter(
        INTEGER_MZ).getEmbeddedParameter().getValue() : null;

    final NistSearchMode mode = getValue(SEARCH_MODE);

    return new NistSearchConfig(getValue(NIST_DIRECTORY),
        mode == null ? NistSearchMode.MSMS_HIRES : mode,
        // MSPepSearch filters on the match factor, which is the score on a 0 to 999 scale. Capped
        // at 999, because a score of 1.0 would otherwise ask for a match factor no hit can reach.
        minSimilarity == null ? 0
            : Math.min(NistSearchConfig.MAX_MATCH_FACTOR, (int) Math.round(minSimilarity * 1000)),
        getValue(PRECURSOR_TOLERANCE), getValue(FRAGMENT_TOLERANCE), integerMz);
  }

  /**
   * The two presets NIST's own workflows come down to: unit mass EI spectra against the EI
   * libraries, and accurate mass MS/MS spectra against the tandem libraries. They are offered by
   * the presets button of the setup dialog.
   */
  @Override
  public @NotNull List<ModulePreset> createDefaultPresets() {

    return List.of( //
        new ModulePreset("GC-EI (low resolution)", NistMsSearchModule.UNIQUE_ID,
            createPreset(NistSearchMode.GC_EI_IDENTITY, 0.75, MZTolerance.UNIT_MASS_TOLERANCE,
                MZTolerance.UNIT_MASS_TOLERANCE, IntegerMode.SUM,
                SpectraMergeSelectPresets.SINGLE_MERGED_SCAN, MZTolerance.UNIT_MASS_TOLERANCE)), //
        new ModulePreset("MS/MS (high resolution)", NistMsSearchModule.UNIQUE_ID,
            createPreset(NistSearchMode.MSMS_HIRES, 0.7, MZTolerance.WIDE_25_PPM_OR_10_MDA,
                new MZTolerance(0.01, 40), null, SpectraMergeSelectPresets.REPRESENTATIVE_SCANS,
                MZTolerance.WIDE_25_PPM_OR_10_MDA)));
  }

  /**
   * Builds one default preset on top of the current values, so that applying a preset keeps the
   * installation directory, which belongs to the machine rather than to the preset. The feature
   * list selection is reset instead: carrying the lists of whatever project the preset happened to
   * be created in would only overwrite the selection of the next one.
   *
   * @param mode               the search type, which also picks the libraries.
   * @param minSimilarity      the minimum similarity on mzmine's 0 to 1 scale.
   * @param precursorTolerance precursor m/z tolerance, ignored by the GC-EI searches.
   * @param fragmentTolerance  fragment m/z tolerance, ignored by the GC-EI searches.
   * @param integerMz          the unit mass merging to switch on, or null to leave it off.
   * @param mergePreset        how the spectra of a row are merged and selected.
   * @param mergeTolerance     the m/z tolerance used while merging.
   */
  private @NotNull NistMsSearchParameters createPreset(@NotNull final NistSearchMode mode,
      final double minSimilarity, @NotNull final MZTolerance precursorTolerance,
      @NotNull final MZTolerance fragmentTolerance, @Nullable final IntegerMode integerMz,
      @NotNull final SpectraMergeSelectPresets mergePreset,
      @NotNull final MZTolerance mergeTolerance) {

    final NistMsSearchParameters preset = (NistMsSearchParameters) cloneParameterSet();

    // a preset is shared between machines, so fall back to whatever installation this one has
    if (preset.getValue(NIST_DIRECTORY) == null) {
      preset.setParameter(NIST_DIRECTORY, NistSearchConfig.discoverInstallation());
    }

    // back to the default: the lists selected in the GUI, or whatever the batch step sets
    preset.setParameter(PEAK_LISTS, new FeatureListsSelection());

    preset.setParameter(SEARCH_MODE, mode);
    preset.setParameter(DOT_PRODUCT, minSimilarity);
    preset.setParameter(PRECURSOR_TOLERANCE, precursorTolerance);
    preset.setParameter(FRAGMENT_TOLERANCE, fragmentTolerance);

    preset.setParameter(INTEGER_MZ, integerMz != null);
    if (integerMz != null) {
      preset.getParameter(INTEGER_MZ).getEmbeddedParameter().setValue(integerMz);
    }

    preset.getParameter(spectraMergeSelect).setSimplePreset(mergePreset, mergeTolerance);

    return preset;
  }

  @Override
  public ExitCode showSetupDialog(final boolean valueCheckRequired) {

    // prefill the installation directory, so that the dialog is usually ready to go
    if (getValue(NIST_DIRECTORY) == null) {
      final File discovered = NistSearchConfig.discoverInstallation();
      if (discovered != null) {
        getParameter(NIST_DIRECTORY).setValue(discovered);
      }
    }

    return super.showSetupDialog(valueCheckRequired);
  }

  @Override
  public boolean checkParameterValues(final Collection<String> errorMessages) {

    final boolean valid = super.checkParameterValues(errorMessages);

    // super already reports a missing directory so return here before config
    if (getValue(NIST_DIRECTORY) == null) {
      return false;
    }

    // the installation and library rules all live on the config
    final List<String> problems = toConfig().validate();
    errorMessages.addAll(problems);

    return valid && problems.isEmpty();
  }

  @Override
  public int getVersion() {
    return 5;
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
          user interface. Please set the NIST installation directory.""";
      case 5 -> """
          The NIST libraries are no longer selected by hand. The search type now decides which \
          libraries of the installation are searched: all EI libraries for GC-EI and all tandem \
          libraries for MS/MS.""";
      default -> null;
    };
  }

  @Override
  public @Nullable Region getMessage() {
    return FxTextFlows.newTextFlowInAccordion("Information", true,
        text("Runs NIST's command line program "), italicText("MSPepSearch"),
        text(", so the search also works in batch mode and on a headless machine. "),
        boldText("Requires a licensed NIST installation"), text(" of NIST 17 or newer.\n"),
        text("The "), boldText("Presets"), text(
            " button fills in defaults for GC-EI (low resolution) and for MS/MS (high resolution).\n"),
        text("NIST returns no library spectra, so the mirror plot of a hit stays empty."));
  }
}
