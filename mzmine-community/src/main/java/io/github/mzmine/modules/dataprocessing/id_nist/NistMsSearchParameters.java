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
import static io.github.mzmine.javafx.components.factories.FxTexts.hyperlinkText;
import static io.github.mzmine.javafx.components.factories.FxTexts.italicText;
import static io.github.mzmine.javafx.components.factories.FxTexts.text;

import io.github.mzmine.javafx.components.factories.FxTextFlows;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.SpectraMergeSelectParameter;
import io.github.mzmine.modules.dataprocessing.id_nist_mspepsearch.NistSearchConfig;
import io.github.mzmine.modules.dataprocessing.id_nist_mspepsearch.NistSearchMode;
import io.github.mzmine.modules.presets.ModulePreset;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.filenames.DirectoryComponent;
import io.github.mzmine.parameters.parametertypes.filenames.DirectoryParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.scans.ScanUtils.IntegerMode;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
  private static final NistSearchMode[] SEARCH_MODES = {NistSearchMode.AUTO,
      NistSearchMode.GC_EI_IDENTITY, NistSearchMode.GC_EI_SIMILARITY, NistSearchMode.MSMS_HIRES};

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
      tandem libraries (hr_msms_nist, lr_msms_nist, apci_msms_nist).
      Automatic uses the GC-EI identity search if the feature list was built by a spectral \
      deconvolution module and the MS/MS search otherwise. The effective search type is written to \
      the log and shown in the task description.""", SEARCH_MODES, NistSearchMode.AUTO);

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
        // the automatic type is resolved by the task, which knows the feature list
        mode == null ? NistSearchMode.AUTO : mode,
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

    return Arrays.stream(NistSearchDefaults.values()).map(
        defaults -> new ModulePreset(defaults.presetName(), NistMsSearchModule.UNIQUE_ID,
            createPreset(defaults))).toList();
  }

  /**
   * Builds one default preset on top of the current values, so that applying a preset keeps the
   * installation directory, which belongs to the machine rather than to the preset. The feature
   * list selection is reset instead: carrying the lists of whatever project the preset happened to
   * be created in would only overwrite the selection of the next one.
   */
  private @NotNull NistMsSearchParameters createPreset(@NotNull final NistSearchDefaults defaults) {

    final NistMsSearchParameters preset = (NistMsSearchParameters) cloneParameterSet();

    // a preset is shared between machines, so fall back to whatever installation this one has
    if (preset.getValue(NIST_DIRECTORY) == null) {
      preset.setParameter(NIST_DIRECTORY, NistSearchConfig.discoverInstallation());
    }

    // back to the default: the lists selected in the GUI, or whatever the batch step sets
    preset.setParameter(PEAK_LISTS, new FeatureListsSelection());

    preset.setParameter(SEARCH_MODE, defaults.mode());
    preset.setParameter(DOT_PRODUCT, defaults.minSimilarity());
    preset.setParameter(PRECURSOR_TOLERANCE, defaults.precursorTolerance());
    preset.setParameter(FRAGMENT_TOLERANCE, defaults.fragmentTolerance());
    preset.setIntegerMz(defaults.integerMz());
    preset.getParameter(spectraMergeSelect)
        .setSimplePreset(defaults.mergePreset(), defaults.mergeTolerance());

    return preset;
  }

  /**
   * Switches the optional unit mass merging on or off in one call.
   *
   * @param integerMz the merging mode to use, or null to switch it off.
   */
  private void setIntegerMz(@Nullable final IntegerMode integerMz) {

    if (integerMz == null) {
      setParameter(INTEGER_MZ, false);
    } else {
      setParameter(INTEGER_MZ, true, integerMz);
    }
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
    return 4;
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }

  @Override
  public Map<String, Parameter<?>> getNameParameterMap() {

    final Map<String, Parameter<?>> map = super.getNameParameterMap();

    // renamed in version 4: the MS Search user interface was started from its own MSSEARCH
    // directory, MSPepSearch is addressed through the installation directory above it
    map.put("NIST MS Search directory", getParameter(NIST_DIRECTORY));

    return map;
  }

  /**
   * Fills in what a parameter set saved before version 4 cannot carry.
   * <p>
   * The search type and the two tolerances did not exist while mzmine drove the MS Search user
   * interface, and the installation directory means something else than it did. The search type
   * becomes {@link NistSearchMode#AUTO}, everything else is filled from the workflow the old
   * parameters point at. Only parameters that were not in the file are touched, so that a newer
   * batch keeps whatever it was saved with.
   */
  @Override
  public void handleLoadedParameters(final Map<String, Parameter<?>> loadedParams,
      final int loadedVersion) {

    super.handleLoadedParameters(loadedParams, loadedVersion);

    if (loadedVersion >= 4) {
      return;
    }

    // Integer m/z was the only GC-EI specific option the old parameters had, so switching it on is
    // the one hint they give about which of the two workflows was set up.
    // assumption: everything else was an MS/MS search, which is what the old default was.
    final NistSearchDefaults defaults =
        Boolean.TRUE.equals(getValue(INTEGER_MZ)) ? NistSearchDefaults.GC_EI
            : NistSearchDefaults.MSMS;

    // decision: the old parameters do not say which workflow they were set up for, so the search
    // type is left to the automatic detection rather than guessed from Integer m/z. The tolerances
    // below have no such fallback and do use the guess.
    if (!loadedParams.containsKey(SEARCH_MODE.getName())) {
      setParameter(SEARCH_MODE, NistSearchMode.AUTO);
    }
    if (!loadedParams.containsKey(PRECURSOR_TOLERANCE.getName())) {
      setParameter(PRECURSOR_TOLERANCE, defaults.precursorTolerance());
    }
    if (!loadedParams.containsKey(FRAGMENT_TOLERANCE.getName())) {
      setParameter(FRAGMENT_TOLERANCE, defaults.fragmentTolerance());
    }
    if (!loadedParams.containsKey(INTEGER_MZ.getName())) {
      setIntegerMz(defaults.integerMz());
    }
    // version 2 and older merged with a different parameter that cannot be mapped
    if (!loadedParams.containsKey(spectraMergeSelect.getName())) {
      getParameter(spectraMergeSelect).setSimplePreset(defaults.mergePreset(),
          defaults.mergeTolerance());
    }

    migrateInstallationDirectory();
  }

  /**
   * Turns the directory of the MS Search executable into the installation directory MSPepSearch is
   * addressed through.
   */
  private void migrateInstallationDirectory() {

    final File loaded = getValue(NIST_DIRECTORY);
    final File installation = NistSearchConfig.toInstallation(loaded);

    if (installation != null) {
      setParameter(NIST_DIRECTORY, installation);
    } else if (loaded == null) {
      setParameter(NIST_DIRECTORY, NistSearchConfig.discoverInstallation());
    }
    // decision: an unresolvable path is kept rather than replaced. The batch may have been saved
    // on, or be carried to, a machine that has the installation; checkParameterValues reports it.
  }

  @Override
  public @Nullable String getVersionMessage(int version) {
    return switch (version) {
      case 3 -> "Improved spectral merging options. Please reconfigure the NIST MS search step.";
      case 4 -> """
          NIST MS search now runs NIST's command line program MSPepSearch.
          Set the NIST installation directory, for example C:\\NIST26.
          The search type was set to automatic, which runs the GC-EI identity search on feature \
          lists with spectral deconvolution and the MS/MS search on all others.
          The Presets button has new presets for GC-EI and MS/MS workflows.""";
      default -> null;
    };
  }

  @Override
  public @Nullable Region getMessage() {
    return FxTextFlows.newTextFlowInAccordion("Information", true,
        text("Runs NIST's command line program "), italicText("MSPepSearch. "),
        boldText("Requires a licensed NIST installation"), text(" of NIST 17 or newer."),
        text("\nContact mzio to obtain the latest NIST library ("),
        hyperlinkText("mzio.io/nist", "https://mzio.io/nist/"), text(")."), text("\nThe "),
        text(
            "NIST returns no library spectra or structures, so the mirror plot only shows the input spectrum."));
  }
}
