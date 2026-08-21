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

package io.github.mzmine.modules.dataprocessing.id_nist_mspepsearch;

import static io.github.mzmine.javafx.components.factories.FxTexts.boldText;
import static io.github.mzmine.javafx.components.factories.FxTexts.italicText;
import static io.github.mzmine.javafx.components.factories.FxTexts.text;

import io.github.mzmine.javafx.components.factories.FxTextFlows;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.SpectraMergeSelectParameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.CheckComboParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.filenames.DirectoryParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.AdvancedParametersParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ParameterSetParameter;
import io.github.mzmine.util.ExitCode;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Parameters of the NIST MSPepSearch module.
 */
public class NistPepSearchParameters extends SimpleParameterSet {

  /**
   * Relative location of the search executable inside a NIST installation. The 64 bit build is
   * preferred; the 32 bit build is the fallback for older installations.
   */
  private static final String[] EXECUTABLES = {"MSPepSearch/MSPepSearch64.exe",
      "MSPepSearch/MSPepSearch.exe"};

  public static final FeatureListsParameter featureLists = new FeatureListsParameter();

  public static final DirectoryParameter nistDirectory = new DirectoryParameter(
      "NIST installation directory", """
      The NIST installation directory, for example D:\\NIST26.
      It must contain the MSPepSearch sub directory with MSPepSearch64.exe, and the library sub \
      directories such as mainlib, replib or hr_msms_nist.""");

  public static final CheckComboParameter<String> libraries = new CheckComboParameter<>("Libraries",
      """
          The libraries to search, discovered in the installation directory above. Select at least \
          one and at most 16.
          Reopen this dialog after changing the installation directory to refresh the list.
          Use the EI libraries (mainlib, replib) for GC-EI searches and the tandem libraries \
          (hr_msms_nist, lr_msms_nist, apci_msms_nist) for MS/MS searches.""", List.of(), List.of(),
      true);

  public static final ComboParameter<NistSearchMode> searchMode = new ComboParameter<>(
      "Search type", """
      The NIST search algorithm to use.
      GC-EI identity is the standard workflow for unit mass EI spectra; high resolution MS/MS is the \
      standard workflow for accurate mass LC-MS/MS spectra. The similarity and hybrid searches find \
      related compounds that are not themselves in the library.""", NistSearchMode.values(),
      NistSearchMode.MSMS_HIRES);

  public static final SpectraMergeSelectParameter spectraMergeSelect = SpectraMergeSelectParameter.createLimitedToFewScans();

  public static final IntegerParameter minMatchFactor = new IntegerParameter("Min match factor", """
      MSPepSearch /MinMF: the minimum NIST match factor of a reported hit, on the NIST scale of 0 to \
      999. 700 and above is usually considered a good match, 900 and above an excellent one.
      mzmine divides this by 1000 to obtain the similarity score shown in the feature table.""", 400,
      0, 999);

  public static final IntegerParameter maxHits = new IntegerParameter("Max hits per spectrum",
      "MSPepSearch /HITS: how many hits to report for each query spectrum.", 10, 1, 100);

  public static final ParameterSetParameter<NistEiSearchParameters> eiParameters = new ParameterSetParameter<>(
      "GC-EI options", "Options that apply to the low resolution GC-EI search types.",
      new NistEiSearchParameters());

  public static final ParameterSetParameter<NistMsMsSearchParameters> msmsParameters = new ParameterSetParameter<>(
      "MS/MS options", "Options that apply to the high resolution MS/MS search type.",
      new NistMsMsSearchParameters());

  public static final AdvancedParametersParameter<NistPepSearchAdvancedParameters> advanced = new AdvancedParametersParameter<>(
      new NistPepSearchAdvancedParameters());

  public NistPepSearchParameters() {
    super(
        "https://mzmine.github.io/mzmine_documentation/module_docs/id_spectra_NIST/NIST-ms-search.html",
        featureLists, nistDirectory, libraries, searchMode, spectraMergeSelect, minMatchFactor,
        maxHits, eiParameters, msmsParameters, advanced);
  }

  /**
   * @return true if the os.name property contains "Windows".
   */
  private static boolean isWindows() {
    return System.getProperty("os.name").toUpperCase(Locale.ROOT).contains("WINDOWS");
  }

  /**
   * Looks for a NIST installation in the usual places, so that the directory is prefilled the first
   * time the dialog is opened.
   *
   * @return the installation directory, or null if none was found.
   */
  public static @Nullable File discoverNistDirectory() {

    if (!isWindows()) {
      return null;
    }

    final List<File> roots = new ArrayList<>();
    for (final File fileSystemRoot : File.listRoots()) {
      roots.add(fileSystemRoot);
      roots.add(new File(fileSystemRoot, "Program Files"));
      roots.add(new File(fileSystemRoot, "Program Files (x86)"));
    }

    for (final File root : roots) {

      final File[] candidates = root.listFiles(
          file -> file.isDirectory() && file.getName().toUpperCase(Locale.ROOT).startsWith("NIST"));
      if (candidates == null) {
        continue;
      }

      // newest installation first: NIST26 before NIST23
      java.util.Arrays.sort(candidates, java.util.Comparator.comparing(File::getName).reversed());

      for (final File candidate : candidates) {
        if (findExecutable(candidate) != null) {
          return candidate;
        }
      }
    }

    return null;
  }

  /**
   * @return the MSPepSearch executable inside the given installation, or null if there is none.
   */
  private static @Nullable File findExecutable(@Nullable final File nistDir) {

    if (nistDir == null) {
      return null;
    }

    for (final String relative : EXECUTABLES) {
      final File executable = new File(nistDir, relative);
      if (executable.isFile()) {
        return executable;
      }
    }

    return null;
  }

  /**
   * @return the MSPepSearch executable of the configured installation, or null if it cannot be
   * found.
   */
  public @Nullable File getExecutable() {
    return findExecutable(getValue(nistDirectory));
  }

  /**
   * @return the libraries the user selected, resolved against the configured installation.
   */
  public @NotNull List<NistLibrary> getSelectedLibraries() {

    final List<String> selected = getValue(libraries);
    if (selected == null || selected.isEmpty()) {
      return List.of();
    }

    final List<NistLibrary> available = NistLibrary.discover(getValue(nistDirectory));
    return available.stream().filter(library -> selected.contains(library.name())).toList();
  }

  @Override
  public ExitCode showSetupDialog(final boolean valueCheckRequired) {

    // Prefill the installation directory and populate the library list before the dialog opens, so
    // that the choices always reflect the installation that is actually configured.
    if (getValue(nistDirectory) == null) {
      final File discovered = discoverNistDirectory();
      if (discovered != null) {
        getParameter(nistDirectory).setValue(discovered);
      }
    }
    refreshLibraryChoices();

    return super.showSetupDialog(valueCheckRequired);
  }

  /**
   * Rediscovers the libraries of the configured installation and offers them as choices, keeping
   * any selection that still exists.
   */
  public void refreshLibraryChoices() {

    final List<String> names = NistLibrary.discover(getValue(nistDirectory)).stream()
        .map(NistLibrary::name).toList();
    getParameter(libraries).setChoices(names.toArray(String[]::new));

    final List<String> selected = getValue(libraries);
    if (selected != null && !selected.isEmpty()) {
      getParameter(libraries).setValue(selected.stream().filter(names::contains).toList());
    }
  }

  @Override
  public boolean checkParameterValues(final Collection<String> errorMessages) {

    if (!isWindows()) {
      errorMessages.add("NIST MSPepSearch is only available on Windows.");
      return false;
    }

    boolean result = super.checkParameterValues(errorMessages);

    final File nistDir = getValue(nistDirectory);
    if (getExecutable() == null) {
      errorMessages.add(
          "MSPepSearch was not found in " + nistDir + ". Expected " + EXECUTABLES[0]
              + " inside the NIST installation directory.");
      result = false;
    }

    final List<NistLibrary> selected = getSelectedLibraries();
    if (selected.isEmpty()) {
      errorMessages.add("Select at least one NIST library to search.");
      result = false;
    } else if (selected.size() > NistLibrary.MAX_LIBRARIES) {
      errorMessages.add("MSPepSearch can search at most " + NistLibrary.MAX_LIBRARIES
          + " libraries at once, but " + selected.size() + " are selected.");
      result = false;
    }

    // The EI libraries and the tandem libraries are not interchangeable, and mixing them wastes a
    // lot of time rather than failing outright, so warn about it while the dialog is still open.
    final NistSearchMode mode = getValue(searchMode);
    if (mode != null) {

      final boolean anyUserFormat = selected.stream()
          .anyMatch(library -> library.kind() == NistLibraryKind.USER);
      final boolean anyEi = selected.stream()
          .anyMatch(library -> library.kind() != NistLibraryKind.USER);

      if (mode.isHighResolution() && anyEi && !anyUserFormat) {
        errorMessages.add(
            "The high resolution MS/MS search needs a tandem library such as hr_msms_nist, but only "
                + "the EI libraries are selected.");
        result = false;
      }
      if (!mode.isHighResolution() && anyUserFormat && !anyEi) {
        errorMessages.add("The GC-EI searches need an EI library such as mainlib or replib, but "
            + "none is selected.");
        result = false;
      }
    }

    return result;
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }

  @Override
  public @Nullable Region getMessage() {
    return FxTextFlows.newTextFlowInAccordion("Information", true,
        text("This module runs NIST's command line search program "),
        italicText("MSPepSearch"), text(
            ", which needs no visible NIST MS Search window and therefore also works in batch mode "
                + "and on a headless machine. All spectra are written to a single query file and "
                + "searched in one run.\n"), boldText("Requires a licensed NIST library installation"),
        text(" - MSPepSearch ships with NIST 17 and newer.\n"), text(
            "NIST does not return the library spectra themselves, so the mirror plot of a hit stays "
                + "empty. Open the compound in NIST MS Search to inspect the reference spectrum."));
  }

  @Override
  public int getVersion() {
    return 1;
  }
}
