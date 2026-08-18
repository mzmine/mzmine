/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 * SPDX-License-Identifier: MIT
 */
package io.github.mzmine.modules.dataprocessing.id_nist;

import static io.github.mzmine.javafx.components.factories.FxTexts.boldText;
import static io.github.mzmine.javafx.components.factories.FxTexts.hyperlinkText;
import static io.github.mzmine.javafx.components.factories.FxTexts.text;

import io.github.mzmine.javafx.components.factories.FxTextFlows;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.filenames.DirectoryParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Parameters for the headless NIST MSPepSearch EI library search. */
public class NistMsSearchParameters extends SimpleParameterSet {

  /** Where users obtain MSPepSearch. It is not redistributed with mzmine. */
  private static final List<String> MSPEPSEARCH_EXECUTABLE_NAMES =
      List.of("MSPepSearch64.exe", "MSPepSearch.exe");

  /** Where users obtain MSPepSearch. It is not redistributed with mzmine. */
  public static final String MSPEPSEARCH_DOWNLOAD_URL =
      "https://chemdata.nist.gov/dokuwiki/doku.php?id=peptidew:mspepsearch";

  public static final FeatureListsParameter PEAK_LISTS = new FeatureListsParameter();

  public static final DirectoryParameter NIST_LIBRARY_DIR = new DirectoryParameter(
      "NIST library directory",
      "Licensed NIST MSSEARCH directory containing mainlib/replib. Browse if NIST is not installed at C:\\NIST23.",
      "C:\\NIST23\\MSSEARCH");

  public static final DirectoryParameter MSPEPSEARCH_DIR = new DirectoryParameter(
      "MSPepSearch directory",
      "Folder containing MSPepSearch64.exe and its DLLs, or any folder above it such as a NIST "
          + "root. MSPepSearch is a free NIST download and is not part of a standard NIST MS "
          + "Search installation. Download it from " + MSPEPSEARCH_DOWNLOAD_URL
          + " and extract it anywhere, then browse to that folder.",
      "C:\\NIST23");

  public static final ComboParameter<NistLibrarySelection> LIBRARIES = new ComboParameter<>(
      "EI libraries", "Licensed NIST EI libraries to search", NistLibrarySelection.values(),
      NistLibrarySelection.MAIN_AND_REPLICATE);

  public static final IntegerParameter MIN_MATCH_FACTOR = new IntegerParameter(
      "Minimum match factor", "Minimum NIST match factor (0-999) retained as an annotation", 650,
      0, 999);

  public static final OptionalParameter<DoubleParameter> LIMIT_RAW_APEX_RETRIES =
      new OptionalParameter<>(new DoubleParameter(
          "Retry raw apex when peak is above baseline (%)",
          "If a deconvoluted pseudo-spectrum returns no qualifying hit, retry its raw apex only "
              + "when the feature apex is at least this percentage above the local baseline. "
              + "This speeds up batch searches without filtering the initial pseudo-spectrum search.",
          new DecimalFormat("0.0"), 25d, 0d, 10_000d), true);

  public static final IntegerParameter MAX_HITS = new IntegerParameter("Maximum hits",
      "Maximum number of NIST hits stored per feature row", 10, 1, 100);

  public NistMsSearchParameters() {
    super(new Parameter[]{PEAK_LISTS, NIST_LIBRARY_DIR, MSPEPSEARCH_DIR, LIBRARIES,
            MIN_MATCH_FACTOR, LIMIT_RAW_APEX_RETRIES, MAX_HITS},
        "https://mzmine.github.io/mzmine_documentation/module_docs/id_spectra_NIST/NIST-ms-search.html");
  }

  static File normalizeMsSearchDirectory(@Nullable File selected) {
    if (selected == null) {
      return null;
    }
    File nested = new File(selected, "MSSEARCH");
    return nested.isDirectory() ? nested : selected;
  }

  public File getNistLibraryDirectory() {
    return normalizeMsSearchDirectory(getValue(NIST_LIBRARY_DIR));
  }

  /**
   * Resolves MSPepSearch64.exe from the selected folder. Falls back to an {@code external_tools}
   * copy so a packaged build that ships the runtime still works without configuration, but
   * mzmine does not redistribute MSPepSearch itself.
   */
  public @Nullable File getMsPepSearchExecutable() {
    final File selected = getValue(MSPEPSEARCH_DIR);
    if (selected != null && selected.isDirectory()) {
      final File exe = findMsPepSearchExecutable(selected);
      if (exe != null) {
        return exe;
      }
    }
    final File bundled = FileAndPathUtil.resolveInExternalToolsDir(
        "nist_mspepsearch/MSPepSearch64.exe");
    return bundled.isFile() ? bundled : null;
  }

  /**
   * Finds MSPepSearch under {@code folder}, accepting the layouts users actually have: the folder
   * holding the executable itself, an mzmine-style {@code nist_mspepsearch} subfolder, or any
   * immediate subfolder. NIST ships the tool as a date-stamped archive that extracts to names like
   * {@code 2024_03_15_MSPepSearch_x64}, so pointing at a NIST root such as C:\NIST23 has to work
   * too. When several copies exist the newest-looking folder wins, which for date-stamped names is
   * the most recent release.
   */
  static @Nullable File findMsPepSearchExecutable(@Nullable File folder) {
    if (folder == null || !folder.isDirectory()) {
      return null;
    }
    final File direct = executableIn(folder);
    if (direct != null) {
      return direct;
    }
    final File[] children = folder.listFiles(File::isDirectory);
    if (children == null) {
      return null;
    }
    Arrays.sort(children,
        Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER).reversed());
    // Prefer mzmine's own bundling location when present, then any other subfolder.
    for (File child : children) {
      if (child.getName().equalsIgnoreCase("nist_mspepsearch")) {
        final File exe = executableIn(child);
        if (exe != null) {
          return exe;
        }
      }
    }
    for (File child : children) {
      final File exe = executableIn(child);
      if (exe != null) {
        return exe;
      }
    }
    return null;
  }

  private static @Nullable File executableIn(File folder) {
    for (String candidate : MSPEPSEARCH_EXECUTABLE_NAMES) {
      final File exe = new File(folder, candidate);
      if (exe.isFile()) {
        return exe;
      }
    }
    return null;
  }

  @Override
  public boolean checkParameterValues(final Collection<String> errors) {
    if (!System.getProperty("os.name").toUpperCase().contains("WINDOWS")) {
      errors.add("NIST MSPepSearch is only supported on Windows.");
      return false;
    }
    fillInMsPepSearchDirectory();
    boolean valid = super.checkParameterValues(errors);
    return checkNistInstallation(errors, valid);
  }

  /**
   * Populates an empty MSPepSearch folder before generic validation runs.
   *
   * <p>Configurations saved by an earlier version have no value for this parameter, so it loads
   * blank and {@code DirectoryParameter} then rejects it as "not set properly" - blocking the
   * dialog even though a bundled runtime would have served the search perfectly well. Filling the
   * field in also shows the user which copy is actually going to be used.</p>
   */
  private void fillInMsPepSearchDirectory() {
    if (getValue(MSPEPSEARCH_DIR) != null) {
      return;
    }
    final File bundled = FileAndPathUtil.resolveInExternalToolsDir(
        "nist_mspepsearch/MSPepSearch64.exe");
    getParameter(MSPEPSEARCH_DIR).setValue(
        bundled.isFile() ? bundled.getParentFile() : new File("C:" + File.separator + "NIST23"));
  }

  /** Validates a single-row search whose feature list has already been resolved by the caller. */
  public boolean checkParameterValuesForExplicitSearch(final Collection<String> errors) {
    if (!System.getProperty("os.name").toUpperCase().contains("WINDOWS")) {
      errors.add("NIST MSPepSearch is only supported on Windows.");
      return false;
    }
    fillInMsPepSearchDirectory();
    final boolean valid = super.checkParameterValues(errors, true);
    return checkNistInstallation(errors, valid);
  }

  private boolean checkNistInstallation(final Collection<String> errors, boolean valid) {
    File executable = getMsPepSearchExecutable();
    if (executable == null || !executable.isFile()) {
      errors.add(
          "MSPepSearch64.exe was not found. MSPepSearch is a free NIST download and is not included "
              + "with mzmine or with a standard NIST MS Search installation. Download it from "
              + MSPEPSEARCH_DOWNLOAD_URL
              + ", extract the archive, and set \"MSPepSearch directory\" to the extracted folder.");
      valid = false;
    }
    File libraryDir = getNistLibraryDirectory();
    if (libraryDir == null || !libraryDir.isDirectory()) {
      errors.add("The selected NIST library directory does not exist.");
      return false;
    }
    NistLibrarySelection selection = getValue(LIBRARIES);
    if (selection.usesMain() && !new File(libraryDir, "mainlib").isDirectory()) {
      errors.add("NIST mainlib was not found under " + libraryDir);
      valid = false;
    }
    if (selection.usesReplicate() && !new File(libraryDir, "replib").isDirectory()) {
      errors.add("NIST replib was not found under " + libraryDir);
      valid = false;
    }
    return valid;
  }

  @Override
  public int getVersion() {
    return 6;
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }

  @Override
  public @Nullable String getVersionMessage(int version) {
    return version < 4 ? "NIST search now uses headless MSPepSearch. Reconfigure the NIST library path."
        : version < 5 ? "Raw-apex retries can now be limited by local peak prominence."
            : version < 6
                ? "MSPepSearch is no longer bundled. Download it from " + MSPEPSEARCH_DOWNLOAD_URL
                    + " and set the new \"MSPepSearch directory\" parameter."
                : null;
  }

  @Override
  public @Nullable Region getMessage() {
    return FxTextFlows.newTextFlowInAccordion("Headless NIST EI search", true,
        text("mzmine sends each GC-EI pseudo-spectrum (or the apex scan if no pseudo-spectrum exists) to the "),
        boldText("MSPepSearch"), text(" command line tool. No NIST window opens."),
        text("\n\nTwo separate things are needed, and neither ships with mzmine:"),
        text("\n1. The "), boldText("MSPepSearch program"),
        text(" - a free NIST download, and not part of a standard NIST MS Search installation. Get it from "),
        hyperlinkText(MSPEPSEARCH_DOWNLOAD_URL),
        text(", extract it, then set \"MSPepSearch directory\" to the extracted folder."),
        text("\n2. Your "), boldText("licensed NIST EI libraries"),
        text(" (mainlib/replib), typically under C:\\NIST23\\MSSEARCH."),
        text("\n\nLibrary data stays on this workstation and is never copied into mzmine."));
  }
}
