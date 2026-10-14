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
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.dialogs.GroupedParameterSetupDialog;
import io.github.mzmine.parameters.dialogs.GroupedParameterSetupPane.GroupView;
import io.github.mzmine.parameters.dialogs.GroupedParameterSetupPane.ParameterGroup;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.filenames.DirectoryComponent;
import io.github.mzmine.parameters.parametertypes.filenames.DirectoryParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MzToleranceUnit;
import io.github.mzmine.parameters.parametertypes.tolerances.SingleMzToleranceParameter;
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
      The NIST installation directory, for example C:\\NIST26. Use the button to detect it automatically.
      It must contain the MSPepSearch sub directory with MSPepSearch64.exe and the library sub \
      directories such as mainlib or hr_msms_nist.""", NistMsSearchParameters::addAutoDetectButton);

  public static final ComboParameter<NistSearchMode> SEARCH_MODE = new ComboParameter<>(
      "Search type", """
      The NIST search preset to use. It also picks the libraries and decides which of the parameters below apply.
      GC-EI identity: unit mass EI spectra against the EI libraries (mainlib, replib), to find the compound itself.
      GC-EI similarity: the same, but also finds related compounds that are not in the library themselves.
      MS/MS: accurate mass spectra against the tandem libraries (hr_msms_nist, lr_msms_nist, apci_msms_nist).
      Automatic: GC-EI identity if the feature list was built by a spectral deconvolution module, MS/MS otherwise.
      The search type that was actually used is written to the log and shown in the task description.""",
      SEARCH_MODES, NistSearchMode.AUTO);

  public static final SpectraMergeSelectParameter spectraMergeSelect = SpectraMergeSelectParameter.createLimitedToFewScans();

  /**
   * Kept under its original name so that saved batches keep their value. MSPepSearch filters on the
   * NIST match factor, which is this score times 1000.
   */
  public static final DoubleParameter DOT_PRODUCT = new DoubleParameter("Min cosine similarity", """
      The minimum similarity score of a reported hit, on mzmine's 0 to 1 scale.
      This is the NIST match factor divided by 1000 (MSPepSearch /MinMF): 0.7 and above is \
      usually considered a good match, 0.9 and above an excellent one.""",
      MZmineCore.getConfiguration().getScoreFormat(), 0.7, 0.0, 1.0);

  /**
   * Single unit rather than the usual pair: MSPepSearch takes either {@code /Z} or {@code /ZPPM}
   * and silently uses whichever came last, so a maximum of the two cannot be expressed.
   */
  public static final SingleMzToleranceParameter PRECURSOR_TOLERANCE = new SingleMzToleranceParameter(
      "Precursor m/z tolerance", """
      MS/MS searches only, the GC-EI searches ignore it.
      How far the precursor m/z of a library entry may differ from the one of the searched spectrum.
      It only decides which library entries are compared and does not enter the match factor itself.
      MSPepSearch takes a single value, so this is either an absolute or a relative tolerance, never the maximum of both.""",
      MzToleranceUnit.PPM, 0.005, 20);

  /**
   * Single unit for the same reason as {@link #PRECURSOR_TOLERANCE}, here {@code /M} and
   * {@code /MPPM}.
   */
  public static final SingleMzToleranceParameter FRAGMENT_TOLERANCE = new SingleMzToleranceParameter(
      "Fragment m/z tolerance", """
      MS/MS searches only. The GC-EI searches ignore it and always match on unit mass, see Integer m/z.
      The product ion m/z uncertainty. Unlike the precursor tolerance this one decides \
      which signals count as matched and therefore the match factor itself. NIST recommends 20 ppm or less.
      MSPepSearch takes a single value, so this is either an absolute or a relative tolerance, never the maximum of both.""",
      MzToleranceUnit.PPM, 0.01, 20);

  /**
   * Not optional: MSPepSearch bins to unit mass whether or not mzmine does, so the only question is
   * how the signals of a nominal mass are combined, see {@link IntegerMode}.
   */
  public static final ComboParameter<IntegerMode> INTEGER_MZ = new ComboParameter<>("Integer m/z",
      """
          GC-EI searches only, the MS/MS search ignores it.
          How the signals of the same nominal mass are combined before searching, because the NIST EI libraries are unit mass.
          Sum: adds their intensities, which is how a unit mass library spectrum reports a nominal mass, / 
          and is what accurate mass GC data (GC-QTOF, GC-Orbitrap) needs.
          Maximum: keeps only the most intense of them.
          Unit mass quadrupole data has one signal per nominal mass, so there both options are the same.""",
      IntegerMode.values(), IntegerMode.SUM);

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
    final IntegerMode integerMz = getValue(INTEGER_MZ);
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
    preset.setParameter(INTEGER_MZ, defaults.integerMz());
    preset.getParameter(spectraMergeSelect)
        .setSimplePreset(defaults.mergePreset(), defaults.mergeTolerance());

    return preset;
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    return showSetupDialog(valueCheckRequired, "");
  }

  public ExitCode showSetupDialog(boolean valueCheckRequired, String filterParameters) {
    // prefill the installation directory, so that the dialog is usually ready to go
    if (getValue(NIST_DIRECTORY) == null) {
      final File discovered = NistSearchConfig.discoverInstallation();
      if (discovered != null) {
        getParameter(NIST_DIRECTORY).setValue(discovered);
      }
    }

    // no parameter is pinned above the groups, everything is grouped by the search type it applies to
    final List<UserParameter<?, ? extends Region>> fixed = List.of();

    final List<ParameterGroup> groups = List.of( //
        new ParameterGroup("General", PEAK_LISTS, NIST_DIRECTORY, SEARCH_MODE, DOT_PRODUCT,
            spectraMergeSelect), //
        new ParameterGroup("MS/MS-specific", PRECURSOR_TOLERANCE, FRAGMENT_TOLERANCE), //
        new ParameterGroup("GC-EI-MS-specific", INTEGER_MZ) //
    );

    final GroupedParameterSetupDialog dialog = new GroupedParameterSetupDialog(valueCheckRequired,
        this, false, fixed, groups, GroupView.SINGLE_LIST);
    dialog.setTitle(NistMsSearchModule.MODULE_NAME);
    dialog.setFilterText(filterParameters);
    dialog.setWidth(800);
    dialog.setHeight(800);

    dialog.showAndWait();
    return dialog.getExitCode();
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

    // assumption: an old parameter set was an MS/MS search, which is what the old default was.
    // Integer m/z used to be the one hint about the workflow, because only the GC-EI one switched
    // it on - but it was an optional parameter whose on/off state lived in an XML attribute that
    // the plain combo box of this version does not read, so that hint is gone. What actually
    // decides the workflow is the search type below, which is left to the automatic detection.
    final NistSearchDefaults defaults = NistSearchDefaults.MSMS;

    if (!loadedParams.containsKey(SEARCH_MODE.getName())) {
      setParameter(SEARCH_MODE, NistSearchMode.AUTO);
    }
    if (!loadedParams.containsKey(PRECURSOR_TOLERANCE.getName())) {
      setParameter(PRECURSOR_TOLERANCE, defaults.precursorTolerance());
    }
    if (!loadedParams.containsKey(FRAGMENT_TOLERANCE.getName())) {
      setParameter(FRAGMENT_TOLERANCE, defaults.fragmentTolerance());
    }
    // the old on/off state cannot be read back, so the merging always starts from the default
    setParameter(INTEGER_MZ, defaults.integerMz());
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
        hyperlinkText("mzio.io/nist", "https://mzio.io/nist/"), text(")."), text(
            "\nNIST returns no library spectra or structures, so the mirror plot only shows the input spectrum."));
  }
}
