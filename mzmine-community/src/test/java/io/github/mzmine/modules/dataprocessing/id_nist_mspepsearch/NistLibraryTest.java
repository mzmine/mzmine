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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests {@link NistLibrary#discover(File)} against the layout of a real NIST 26 installation.
 */
class NistLibraryTest {

  @TempDir
  Path root;

  /**
   * Creates a fake library directory holding the given index files.
   */
  private void library(final String name, final String... files) throws IOException {

    final Path dir = Files.createDirectory(root.resolve(name));
    for (final String file : files) {
      Files.createFile(dir.resolve(file));
    }
  }

  @Test
  @DisplayName("Libraries are classified the way MSPepSearch needs them")
  void discoversAndClassifies() throws IOException {

    // the index files these libraries actually contain in a NIST 26 installation
    library("mainlib", "alphanam.in6", "nist.db", "LIBSIGN.MSD");
    library("replib", "contrib.inr", "inchikey.inr", "LIBSIGN.MSD");
    library("hr_msms_nist", "ALPHANAM.INU", "PEAK.DBU", "LIBSIGN.MSD");
    library("lr_msms_nist", "ALPHANAM.INU", "LIBSIGN.MSD");
    // nist_ri is user format but also ships a few .inr files - the user format has to win
    library("nist_ri", "ALPHANAM.INU", "deriv.inr", "USER.DBU", "LIBSIGN.MSD");
    // not a library
    library("MSPepSearch", "MSPepSearch64.exe", "nistdl64a.dll");
    library("test-files", "readme.txt");

    final Map<String, NistLibraryKind> kinds = NistLibrary.discover(root.toFile()).stream()
        .collect(java.util.stream.Collectors.toMap(NistLibrary::name, NistLibrary::kind));

    assertEquals(5, kinds.size(), () -> "unexpected libraries: " + kinds);
    assertEquals(NistLibraryKind.MAIN, kinds.get("mainlib"));
    assertEquals(NistLibraryKind.REPLICATE, kinds.get("replib"));
    assertEquals(NistLibraryKind.USER, kinds.get("hr_msms_nist"));
    assertEquals(NistLibraryKind.USER, kinds.get("lr_msms_nist"));
    assertEquals(NistLibraryKind.USER, kinds.get("nist_ri"));
  }

  @Test
  @DisplayName("The main and replicate libraries are listed first")
  void mainAndReplicateComeFirst() throws IOException {

    library("apci_msms_nist", "ALPHANAM.INU");
    library("replib", "contrib.inr");
    library("mainlib", "alphanam.in6");
    library("hr_msms_nist", "ALPHANAM.INU");

    final List<String> names = NistLibrary.discover(root.toFile()).stream().map(NistLibrary::name)
        .toList();

    assertEquals(List.of("mainlib", "replib", "apci_msms_nist", "hr_msms_nist"), names);
  }

  @Test
  @DisplayName("A directory without libraries yields an empty list")
  void emptyInstallation() throws IOException {

    library("MSPepSearch", "MSPepSearch64.exe");
    assertTrue(NistLibrary.discover(root.toFile()).isEmpty());
  }

  @Test
  @DisplayName("A missing or null directory is not an error")
  void missingDirectory() {

    assertTrue(NistLibrary.discover(null).isEmpty());
    assertTrue(NistLibrary.discover(new File(root.toFile(), "does-not-exist")).isEmpty());
  }

  @Test
  @DisplayName("Each kind maps to its MSPepSearch switch")
  void kindArguments() {

    assertEquals("/MAIN", NistLibraryKind.MAIN.getArgument());
    assertEquals("/REPL", NistLibraryKind.REPLICATE.getArgument());
    assertEquals("/LIB", NistLibraryKind.USER.getArgument());
  }
}
