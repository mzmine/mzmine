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
 * A NIST mass spectral library: one sub directory of a NIST installation.
 *
 * @param name    the directory name, which is what is written to the log.
 * @param dir     the directory itself. MSPepSearch is handed the absolute path.
 * @param kind    determines whether the library goes to {@code /MAIN}, {@code /REPL} or
 *                {@code /LIB}.
 * @param content decides for which search type the library can be used.
 */
record NistLibrary(@NotNull String name, @NotNull File dir, @NotNull NistLibraryKind kind,
                   @NotNull NistLibraryContent content) {

  /**
   * Name index extensions, which also tell the library format apart: {@code .in6} is the NIST main
   * library format, {@code .inr} the replicate format and {@code .INU} the user format used by
   * every other shipped library (MS/MS, RI, APCI).
   */
  private static final Set<String> INDEX_EXTENSIONS = Set.of(".in6", ".inr", ".inu");

  /**
   * Index file name prefixes that only a library of MS/MS spectra has, because only those entries
   * carry a precursor m/z. Present in hr_msms_nist, lr_msms_nist and apci_msms_nist and absent from
   * every EI library.
   */
  private static final Set<String> PRECURSOR_INDEX_PREFIXES = Set.of("precmz", "peak_pm");

  /**
   * Files that only the NIST retention index library has. It ships in the same format as the tandem
   * libraries but holds no spectra, so it must never be searched.
   */
  private static final Set<String> RETENTION_INDEX_FILES = Set.of("ri.idx", "riref.idx",
      "ri_spec.idx");

  /**
   * Finds the libraries of a NIST installation.
   *
   * @param nistRoot the installation directory, e.g. {@code D:\NIST26}.
   * @return the libraries found, sorted with the main and replicate libraries first, or an empty
   * list if the directory is null, missing or holds no library.
   */
  static @NotNull List<NistLibrary> discover(@Nullable final File nistRoot) {

    if (nistRoot == null || !nistRoot.isDirectory()) {
      return List.of();
    }

    final File[] candidates = nistRoot.listFiles(File::isDirectory);
    if (candidates == null) {
      return List.of();
    }

    final List<NistLibrary> libraries = new ArrayList<>();
    for (final File candidate : candidates) {

      final String[] files = candidate.list();
      if (files == null) {
        continue;
      }

      // lower cased once, both classifications below are case insensitive
      final Set<String> lowerCase = Arrays.stream(files).map(file -> file.toLowerCase(Locale.ROOT))
          .collect(Collectors.toUnmodifiableSet());

      final NistLibraryKind kind = determineKind(lowerCase);
      if (kind != null) {
        libraries.add(new NistLibrary(candidate.getName(), candidate, kind,
            determineContent(kind, lowerCase)));
      }
    }

    // main and replicate first, then alphabetically: matches how users think about the libraries
    // and puts the two libraries that need a dedicated switch at the top.
    libraries.sort(Comparator.comparingInt((NistLibrary l) -> l.kind().ordinal())
        .thenComparing(NistLibrary::name, String.CASE_INSENSITIVE_ORDER));
    return libraries;
  }

  /**
   * Classifies the format of a directory by the name index files it contains.
   *
   * @param files the lower cased file names of the directory.
   * @return the kind, or null if the directory is not a NIST library.
   */
  private static @Nullable NistLibraryKind determineKind(@NotNull final Set<String> files) {

    boolean main = false;
    boolean replicate = false;
    boolean user = false;

    for (final String file : files) {

      final int dot = file.lastIndexOf('.');
      final String extension = dot < 0 ? "" : file.substring(dot);
      if (!INDEX_EXTENSIONS.contains(extension)) {
        continue;
      }

      switch (extension) {
        case ".in6" -> main = true;
        case ".inr" -> replicate = true;
        default -> user = true;
      }
    }

    // A user library may ship a few .inr files next to its .INU index (nist_ri does), so the user
    // format wins. Only a library without any .INU index is a true main or replicate library.
    if (user) {
      return NistLibraryKind.USER;
    }
    if (main) {
      return NistLibraryKind.MAIN;
    }
    if (replicate) {
      return NistLibraryKind.REPLICATE;
    }
    return null;
  }

  /**
   * Classifies what a library holds, so that a search type can pick the libraries that fit it.
   * <p>
   * The main and replicate libraries are EI by definition. Everything else is in the user format
   * and is told apart by its index files: a precursor m/z index means MS/MS spectra, the retention
   * index files mean no spectra at all, and anything else is taken to be an EI library so that
   * custom libraries built with Lib2NIST are searched as well.
   *
   * @param files the lower cased file names of the directory.
   */
  private static @NotNull NistLibraryContent determineContent(@NotNull final NistLibraryKind kind,
      @NotNull final Set<String> files) {

    if (kind != NistLibraryKind.USER) {
      return NistLibraryContent.EI;
    }

    for (final String file : files) {
      if (PRECURSOR_INDEX_PREFIXES.stream().anyMatch(file::startsWith)) {
        return NistLibraryContent.MSMS;
      }
    }

    if (files.containsAll(RETENTION_INDEX_FILES)) {
      return NistLibraryContent.NON_SPECTRAL;
    }

    return NistLibraryContent.EI;
  }

  @Override
  public String toString() {
    return name;
  }
}
