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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests how {@link NistSearchConfig} picks the libraries of an installation and
 * {@link NistSearchConfig#validate()}, which is the single source of the rules that both the setup
 * dialog and {@link NistPepSearchTask} enforce.
 * <p>
 * Windows only, because MSPepSearch is a Windows executable and validate() says so before it looks
 * at anything else.
 */
@EnabledOnOs(OS.WINDOWS)
class NistSearchConfigTest {

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

  @BeforeEach
  void setUp() throws IOException {

    Files.createDirectories(root.resolve("MSPepSearch"));
    Files.createFile(root.resolve("MSPepSearch/MSPepSearch64.exe"));

    library("mainlib", "alphanam.in6");
    library("replib", "contrib.inr");
    library("hr_msms_nist", "ALPHANAM.INU", "PEAK_PM0.INU");
    library("lr_msms_nist", "ALPHANAM.INU", "precmz.inu");
    // holds no spectra and must never be searched
    library("nist_ri", "ALPHANAM.INU", "ri.idx", "riref.idx", "ri_spec.idx");
  }

  private NistSearchConfig config(final File nistDirectory, final NistSearchMode mode) {
    return new NistSearchConfig(nistDirectory, mode, 400, new MZTolerance(0.005, 20),
        new MZTolerance(0.01, 40), null);
  }

  private List<String> librariesOf(final NistSearchMode mode) {
    return config(root.toFile(), mode).libraryNames();
  }

  @Test
  @DisplayName("The GC-EI searches use every EI library and nothing else")
  void eiSearchesUseTheEiLibraries() {

    assertEquals(List.of("mainlib", "replib"), librariesOf(NistSearchMode.GC_EI_IDENTITY));
    assertEquals(List.of("mainlib", "replib"), librariesOf(NistSearchMode.GC_EI_SIMILARITY));
  }

  @Test
  @DisplayName("The MS/MS search uses every tandem library and nothing else")
  void msmsUsesTheTandemLibraries() {
    assertEquals(List.of("hr_msms_nist", "lr_msms_nist"), librariesOf(NistSearchMode.MSMS_HIRES));
  }

  @Test
  @DisplayName("A custom user library is searched with the EI libraries")
  void customLibrariesAreSearchedWithTheEiLibraries() throws IOException {

    library("my_gc_library", "ALPHANAM.INU", "PEAK.DBU");

    assertEquals(List.of("mainlib", "replib", "my_gc_library"),
        librariesOf(NistSearchMode.GC_EI_IDENTITY));
  }

  @Test
  @DisplayName("At most one main and one replicate library are passed")
  void onlyOneMainAndOneReplicateLibrary() throws IOException {

    library("mainlib_2020", "alphanam.in6");
    library("replib_2020", "contrib.inr");

    assertEquals(List.of("mainlib", "replib"), librariesOf(NistSearchMode.GC_EI_IDENTITY));
  }

  @Test
  @DisplayName("No more libraries than MSPepSearch accepts are passed")
  void atMostSixteenLibraries() throws IOException {

    for (int i = 0; i < 20; i++) {
      library("user_msms_" + i, "ALPHANAM.INU", "precmz.inu");
    }

    assertEquals(NistSearchConfig.MAX_LIBRARIES, librariesOf(NistSearchMode.MSMS_HIRES).size());
  }

  @Test
  @DisplayName("A complete configuration has no problems")
  void validConfiguration() {

    assertTrue(config(root.toFile(), NistSearchMode.GC_EI_IDENTITY).validate().isEmpty());
    assertTrue(config(root.toFile(), NistSearchMode.MSMS_HIRES).validate().isEmpty());
  }

  @Test
  @DisplayName("A directory without MSPepSearch is reported")
  void missingExecutable() {

    final List<String> problems = config(new File(root.toFile(), "not-an-install"),
        NistSearchMode.MSMS_HIRES).validate();

    assertEquals(1, problems.size());
    assertTrue(problems.getFirst().contains("MSPepSearch was not found"),
        () -> "unexpected problem: " + problems);
  }

  @Test
  @DisplayName("A null directory is reported rather than throwing")
  void nullDirectory() {

    final List<String> problems = config(null, NistSearchMode.MSMS_HIRES).validate();

    // the message must name the unset directory, not print "null" as if it were a path
    assertTrue(problems.stream().anyMatch(problem -> problem.contains("is not set")),
        () -> "unexpected problems: " + problems);
    assertFalse(problems.stream().anyMatch(problem -> problem.contains("null")),
        () -> "no message may contain a literal null: " + problems);
  }

  @Test
  @DisplayName("An installation without a library for the search type is reported")
  void noMatchingLibrary() throws IOException {

    final Path eiOnly = Files.createDirectory(root.resolve("ei-only-install"));
    Files.createDirectories(eiOnly.resolve("MSPepSearch"));
    Files.createFile(eiOnly.resolve("MSPepSearch/MSPepSearch64.exe"));
    Files.createDirectory(eiOnly.resolve("mainlib"));
    Files.createFile(eiOnly.resolve("mainlib/alphanam.in6"));

    final List<String> problems = config(eiOnly.toFile(), NistSearchMode.MSMS_HIRES).validate();

    assertEquals(1, problems.size());
    assertTrue(problems.getFirst().contains("No MS/MS library was found"),
        () -> "unexpected problem: " + problems);
    assertTrue(config(eiOnly.toFile(), NistSearchMode.GC_EI_IDENTITY).validate().isEmpty());
  }

  @Test
  @DisplayName("The automatic search type accepts an installation that only fits one workflow")
  void automaticModeValidatesAgainstBothWorkflows() throws IOException {

    // the search type is only decided once the feature list is known, so an EI only installation
    // must not be rejected just because it cannot run the MS/MS search
    final Path eiOnly = Files.createDirectory(root.resolve("ei-only-install"));
    Files.createDirectories(eiOnly.resolve("MSPepSearch"));
    Files.createFile(eiOnly.resolve("MSPepSearch/MSPepSearch64.exe"));
    Files.createDirectory(eiOnly.resolve("mainlib"));
    Files.createFile(eiOnly.resolve("mainlib/alphanam.in6"));

    assertTrue(config(root.toFile(), NistSearchMode.AUTO).validate().isEmpty());
    assertTrue(config(eiOnly.toFile(), NistSearchMode.AUTO).validate().isEmpty());

    // an installation without any spectral library is still reported, once per workflow
    final Path empty = Files.createDirectory(root.resolve("no-library-install"));
    Files.createDirectories(empty.resolve("MSPepSearch"));
    Files.createFile(empty.resolve("MSPepSearch/MSPepSearch64.exe"));

    final List<String> problems = config(empty.toFile(), NistSearchMode.AUTO).validate();

    assertEquals(2, problems.size(), () -> "unexpected problems: " + problems);
    assertTrue(problems.stream().anyMatch(problem -> problem.contains("No EI library was found")),
        () -> "unexpected problems: " + problems);
    assertTrue(
        problems.stream().anyMatch(problem -> problem.contains("No MS/MS library was found")),
        () -> "unexpected problems: " + problems);
  }

  @Test
  @DisplayName("The retention index library is never searched - it holds no spectra")
  void retentionIndexLibraryIsNeverSearched() {

    for (final NistSearchMode mode : NistSearchMode.searchTypes()) {
      assertTrue(!librariesOf(mode).contains("nist_ri"),
          () -> "nist_ri must not be searched by " + mode);
    }
  }
}
