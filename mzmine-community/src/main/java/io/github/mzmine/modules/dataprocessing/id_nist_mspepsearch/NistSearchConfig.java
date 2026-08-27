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

package io.github.mzmine.modules.dataprocessing.id_nist_mspepsearch;

import com.sun.jna.Platform;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.scans.ScanUtils.IntegerMode;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Everything the MSPepSearch engine needs for one search run.
 * <p>
 * This is the public entry point of the package: the module that offers the search builds a config
 * and hands it to {@link NistPepSearchTask}, so that nothing else of the engine has to be visible.
 * The libraries are not part of it - they follow from the search type, see {@link #libraries()}.
 *
 * @param nistDirectory      the NIST installation directory, e.g. {@code D:\NIST26}.
 * @param mode               the search type, which also decides which libraries are searched.
 * @param minMatchFactor     the minimum NIST match factor of a reported hit, 0-999
 *                           ({@code /MinMF}).
 * @param precursorTolerance precursor m/z tolerance, only used by
 *                           {@link NistSearchMode#MSMS_HIRES}.
 * @param fragmentTolerance  fragment m/z tolerance, only used by
 *                           {@link NistSearchMode#MSMS_HIRES}.
 * @param integerMz          merge fractional m/z to unit mass before searching, or null to submit
 *                           the spectra as they are. Only meaningful for the unit mass EI
 *                           searches.
 */
public record NistSearchConfig(@Nullable File nistDirectory, @NotNull NistSearchMode mode,
                               int minMatchFactor, @Nullable MZTolerance precursorTolerance,
                               @Nullable MZTolerance fragmentTolerance,
                               @Nullable IntegerMode integerMz) {

  /**
   * MSPepSearch accepts at most one main and one replicate library, and 16 libraries in total.
   */
  static final int MAX_LIBRARIES = 16;

  /**
   * How many hits MSPepSearch reports per query spectrum ({@code /HITS}).
   */
  static final int MAX_HITS = 20;

  /**
   * The highest NIST match factor there is. A {@code /MinMF} above this can never be met, so a
   * minimum similarity of 1.0 has to be capped here rather than becoming 1000.
   */
  public static final int MAX_MATCH_FACTOR = 999;

  /**
   * Relative location of the search executable inside a NIST installation. The 64 bit build is
   * preferred; the 32 bit build is the fallback for older installations.
   */
  private static final String[] EXECUTABLES = {"MSPepSearch/MSPepSearch64.exe",
      "MSPepSearch/MSPepSearch.exe"};

  /**
   * The name the NIST installer gives its directory: NIST followed by the two digit version, e.g.
   * NIST23 or NIST26.
   */
  private static final Pattern VERSIONED_DIRECTORY = Pattern.compile("NIST(\\d{2})",
      Pattern.CASE_INSENSITIVE);

  /**
   * Looks for a NIST installation, so that the directory can be prefilled and the auto detect
   * button of the setup dialog has something to do.
   * <p>
   * Searched are the roots of all drives, which is where the installer puts it ({@code C:\NIST26},
   * {@code D:\NIST26}), and the two program folders. The newest version wins.
   *
   * @return the installation directory, or null if none was found.
   */
  public static @Nullable File discoverInstallation() {

    if (!Platform.isWindows()) {
      return null;
    }

    final List<File> candidates = new ArrayList<>();
    for (final File root : File.listRoots()) {
      addInstallationCandidates(candidates, root);
      addInstallationCandidates(candidates, new File(root, "Program Files"));
      addInstallationCandidates(candidates, new File(root, "Program Files (x86)"));
    }

    // newest version first: NIST26 before NIST23, and a versioned name before an unversioned one
    candidates.sort(Comparator.comparingInt(NistSearchConfig::installationVersion).reversed()
        .thenComparing(File::getName, String.CASE_INSENSITIVE_ORDER));

    for (final File candidate : candidates) {
      if (findExecutable(candidate) != null) {
        return candidate;
      }
    }

    return null;
  }

  /**
   * Adds the sub directories of the given parent that could be a NIST installation.
   */
  private static void addInstallationCandidates(@NotNull final List<File> candidates,
      @NotNull final File parent) {

    final File[] found = parent.listFiles(
        file -> file.isDirectory() && file.getName().toUpperCase(Locale.ROOT).startsWith("NIST"));
    if (found != null) {
      candidates.addAll(List.of(found));
    }
  }

  /**
   * @return the two digit version of a {@code NISTxx} directory, or 0 for any other name.
   */
  private static int installationVersion(@NotNull final File directory) {

    final Matcher matcher = VERSIONED_DIRECTORY.matcher(directory.getName());
    return matcher.matches() ? Integer.parseInt(matcher.group(1)) : 0;
  }

  /**
   * @return the MSPepSearch executable inside the given installation, or null if there is none.
   */
  private static @Nullable File findExecutable(@Nullable final File nistDirectory) {

    if (nistDirectory == null) {
      return null;
    }

    for (final String relative : EXECUTABLES) {
      final File executable = new File(nistDirectory, relative);
      if (executable.isFile()) {
        return executable;
      }
    }

    return null;
  }

  /**
   * Everything that stops this configuration from being searched.
   * <p>
   * The single source of these rules: the setup dialog reports them while the user can still fix
   * them, and the task checks them again because a batch may have been saved against an
   * installation that has since been moved or uninstalled.
   *
   * @return the problems, or an empty list if the search can run.
   */
  public @NotNull List<String> validate() {

    if (!Platform.isWindows()) {
      return List.of("NIST MS search is only available on Windows.");
    }

    if (nistDirectory == null) {
      return List.of("The NIST installation directory is not set.");
    }

    if (findExecutable(nistDirectory) == null) {
      return List.of(
          "MSPepSearch was not found in " + nistDirectory + ". Expected " + EXECUTABLES[0]
              + " inside the NIST installation directory.");
    }

    if (libraries().isEmpty()) {
      return List.of(
          "No %s library was found in %s. The %s search needs a library such as %s.".formatted(
              mode.requiredLibraryContent(), nistDirectory, mode,
              mode.isHighResolution() ? "hr_msms_nist" : "mainlib"));
    }

    return List.of();
  }

  /**
   * @return the MSPepSearch executable of this installation, or null if it cannot be found.
   */
  @Nullable File executable() {
    return findExecutable(nistDirectory);
  }

  /**
   * The libraries of the installation that fit the search type, reduced to what MSPepSearch
   * accepts: at most one main library, at most one replicate library and at most
   * {@link #MAX_LIBRARIES} in total.
   * <p>
   * Not cached: the installation directory can be changed in the setup dialog, and a batch can be
   * run against an installation that has been updated since the batch was saved.
   */
  @NotNull List<NistLibrary> libraries() {

    final NistLibraryContent required = mode.requiredLibraryContent();

    final List<NistLibrary> result = new ArrayList<>();
    boolean mainUsed = false;
    boolean replicateUsed = false;

    for (final NistLibrary library : NistLibrary.discover(nistDirectory)) {

      if (library.content() != required) {
        continue;
      }

      switch (library.kind()) {
        case MAIN -> {
          if (!mainUsed) {
            result.add(library);
            mainUsed = true;
          }
        }
        case REPLICATE -> {
          if (!replicateUsed) {
            result.add(library);
            replicateUsed = true;
          }
        }
        case USER -> result.add(library);
      }

      if (result.size() == MAX_LIBRARIES) {
        break;
      }
    }

    return result;
  }

  /**
   * @return the names of the libraries that {@link #libraries()} searches, for the log and the
   * setup dialog.
   */
  public @NotNull List<String> libraryNames() {
    return libraries().stream().map(NistLibrary::name).toList();
  }
}
