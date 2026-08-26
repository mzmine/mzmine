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

import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.scans.ScanUtils.IntegerMode;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Everything the MSPepSearch engine needs for one search run.
 * <p>
 * This is the public entry point of the package: the module that offers the search builds a config
 * and hands it to {@link NistPepSearchTask}, so that nothing else of the engine has to be visible.
 * The static helpers exist for the same reason - they let a parameter set discover an installation
 * and its libraries without seeing {@link NistLibrary}.
 *
 * @param nistDirectory     the NIST installation directory, e.g. {@code D:\NIST26}.
 * @param libraryNames      the names of the library sub directories to search, as returned by
 *                          {@link #discoverLibraryNames(File)}.
 * @param mode              the search type.
 * @param minMatchFactor    the minimum NIST match factor of a reported hit, 0-999
 *                          ({@code /MinMF}).
 * @param precursorTolerance precursor m/z tolerance, only used by {@link NistSearchMode#MSMS_HIRES}.
 * @param fragmentTolerance fragment m/z tolerance, only used by {@link NistSearchMode#MSMS_HIRES}.
 * @param integerMz         merge fractional m/z to unit mass before searching, or null to submit
 *                          the spectra as they are. Only meaningful for the unit mass EI searches.
 */
public record NistSearchConfig(@Nullable File nistDirectory, @NotNull List<String> libraryNames,
                               @NotNull NistSearchMode mode, int minMatchFactor,
                               @Nullable MZTolerance precursorTolerance,
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
   * Relative location of the search executable inside a NIST installation. The 64 bit build is
   * preferred; the 32 bit build is the fallback for older installations.
   */
  private static final String[] EXECUTABLES = {"MSPepSearch/MSPepSearch64.exe",
      "MSPepSearch/MSPepSearch.exe"};

  /**
   * @return true if the os.name property contains "Windows". MSPepSearch ships as a Windows
   * executable only.
   */
  static boolean isWindows() {
    return System.getProperty("os.name").toUpperCase(Locale.ROOT).contains("WINDOWS");
  }

  /**
   * Looks for a NIST installation in the usual places, so that the directory can be prefilled the
   * first time a setup dialog is opened.
   *
   * @return the installation directory, or null if none was found.
   */
  public static @Nullable File discoverInstallation() {

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
      Arrays.sort(candidates, Comparator.comparing(File::getName).reversed());

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
   * The names of the libraries of an installation, main and replicate first and the rest
   * alphabetically.
   *
   * @return the names, or an empty list if the directory is null, missing or holds no library.
   */
  public static @NotNull List<String> discoverLibraryNames(@Nullable final File nistDirectory) {
    return NistLibrary.discover(nistDirectory).stream().map(NistLibrary::name).toList();
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

    if (!isWindows()) {
      return List.of("NIST MS search is only available on Windows.");
    }

    final List<String> problems = new ArrayList<>();

    if (findExecutable(nistDirectory) == null) {
      problems.add("MSPepSearch was not found in " + nistDirectory + ". Expected " + EXECUTABLES[0]
          + " inside the NIST installation directory.");
    }

    if (libraryNames.isEmpty()) {
      problems.add("Select at least one NIST library to search.");
      return problems;
    }
    if (libraryNames.size() > MAX_LIBRARIES) {
      problems.add("MSPepSearch can search at most " + MAX_LIBRARIES + " libraries at once, but "
          + libraryNames.size() + " are selected.");
    }

    // The EI libraries and the tandem libraries are not interchangeable, and mixing them wastes a
    // lot of time rather than failing outright, so complain about it up front.
    final Set<String> eiLibraries = NistLibrary.discover(nistDirectory).stream()
        .filter(library -> library.kind() != NistLibraryKind.USER).map(NistLibrary::name)
        .collect(Collectors.toUnmodifiableSet());
    final boolean anyEi = libraryNames.stream().anyMatch(eiLibraries::contains);
    final boolean anyOther = libraryNames.stream().anyMatch(name -> !eiLibraries.contains(name));

    if (mode.isHighResolution() && anyEi && !anyOther) {
      problems.add("The MS/MS search needs a tandem library such as hr_msms_nist, but only the EI "
          + "libraries are selected.");
    } else if (!mode.isHighResolution() && anyOther && !anyEi) {
      problems.add("The GC-EI searches need an EI library such as mainlib or replib, but none is "
          + "selected.");
    }

    return problems;
  }

  /**
   * @return the MSPepSearch executable of this installation, or null if it cannot be found.
   */
  @Nullable File executable() {
    return findExecutable(nistDirectory);
  }

  /**
   * The selected libraries, resolved against the installation and reduced to what MSPepSearch
   * accepts: at most one main library, at most one replicate library and at most
   * {@link #MAX_LIBRARIES} in total.
   */
  @NotNull List<NistLibrary> libraries() {

    final List<NistLibrary> result = new ArrayList<>();
    boolean mainUsed = false;
    boolean replicateUsed = false;

    for (final NistLibrary library : NistLibrary.discover(nistDirectory)) {

      if (!libraryNames.contains(library.name())) {
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
}
