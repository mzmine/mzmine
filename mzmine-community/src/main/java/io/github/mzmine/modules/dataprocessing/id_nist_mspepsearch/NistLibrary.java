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

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A NIST mass spectral library: one sub directory of a NIST installation.
 *
 * @param name the directory name, which is what the user selects and what is stored in the batch
 *             file.
 * @param dir  the directory itself. MSPepSearch is handed the absolute path.
 * @param kind determines whether the library goes to {@code /MAIN}, {@code /REPL} or {@code /LIB}.
 */
public record NistLibrary(@NotNull String name, @NotNull File dir, @NotNull NistLibraryKind kind) {

  /**
   * MSPepSearch accepts at most one main and one replicate library, and 16 libraries in total.
   */
  public static final int MAX_LIBRARIES = 16;

  /**
   * Name index files. The extension also tells the library format apart: {@code .in6} is the NIST
   * main library format, {@code .inr} the replicate format and {@code .INU} the user format used by
   * every other shipped library (MS/MS, RI, APCI).
   */
  private static final FilenameFilter INDEX_FILTER = (dir, name) -> {
    final String lower = name.toLowerCase(Locale.ROOT);
    return lower.endsWith(".in6") || lower.endsWith(".inr") || lower.endsWith(".inu")
        || lower.equals("libsign.msd");
  };

  /**
   * Finds the libraries of a NIST installation.
   *
   * @param nistRoot the installation directory, e.g. {@code D:\NIST26}.
   * @return the libraries found, sorted with the main and replicate libraries first, or an empty
   * list if the directory is null, missing or holds no library.
   */
  public static @NotNull List<NistLibrary> discover(@Nullable final File nistRoot) {

    if (nistRoot == null || !nistRoot.isDirectory()) {
      return List.of();
    }

    final File[] candidates = nistRoot.listFiles(File::isDirectory);
    if (candidates == null) {
      return List.of();
    }

    final List<NistLibrary> libraries = new ArrayList<>();
    for (final File candidate : candidates) {

      final NistLibraryKind kind = determineKind(candidate);
      if (kind != null) {
        libraries.add(new NistLibrary(candidate.getName(), candidate, kind));
      }
    }

    // main and replicate first, then alphabetically: matches how users think about the libraries
    // and puts the two libraries that need a dedicated switch at the top of the selection list.
    libraries.sort(Comparator.comparingInt((NistLibrary l) -> l.kind().ordinal())
        .thenComparing(NistLibrary::name, String.CASE_INSENSITIVE_ORDER));
    return libraries;
  }

  /**
   * Classifies a directory by the name index files it contains.
   *
   * @return the kind, or null if the directory is not a NIST library.
   */
  private static @Nullable NistLibraryKind determineKind(final File dir) {

    final String[] indexFiles = dir.list(INDEX_FILTER);
    if (indexFiles == null || indexFiles.length == 0) {
      return null;
    }

    boolean main = false;
    boolean replicate = false;
    boolean user = false;

    for (final String indexFile : indexFiles) {
      final String lower = indexFile.toLowerCase(Locale.ROOT);
      if (lower.endsWith(".in6")) {
        main = true;
      } else if (lower.endsWith(".inr")) {
        replicate = true;
      } else if (lower.endsWith(".inu")) {
        user = true;
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

  @Override
  public String toString() {
    return name;
  }
}
