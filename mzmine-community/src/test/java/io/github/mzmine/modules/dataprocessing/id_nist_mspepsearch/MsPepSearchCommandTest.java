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
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests the MSPepSearch command line built by {@link MsPepSearchCommand}.
 * <p>
 * The expectations mirror command lines that were verified to actually run against a NIST 26
 * installation.
 */
class MsPepSearchCommandTest {

  private static final MZTolerance PRECURSOR_PPM = new MZTolerance(0.005, 20);
  private static final MZTolerance FRAGMENT_PPM = new MZTolerance(0.01, 40);

  @TempDir
  Path root;

  private File executable;
  private File queryFile;
  private File outputFile;

  @BeforeEach
  void setUp() throws IOException {

    Files.createDirectories(root.resolve("MSPepSearch"));
    executable = Files.createFile(root.resolve("MSPepSearch/MSPepSearch64.exe")).toFile();

    for (final String[] library : new String[][]{{"mainlib", "alphanam.in6"},
        {"replib", "contrib.inr"}, {"nist_ri", "ALPHANAM.INU"},
        {"hr_msms_nist", "ALPHANAM.INU"}}) {
      Files.createDirectory(root.resolve(library[0]));
      Files.createFile(root.resolve(library[0]).resolve(library[1]));
    }

    queryFile = root.resolve("query.msp").toFile();
    outputFile = root.resolve("out.tsv").toFile();
  }

  private NistSearchConfig config(final NistSearchMode mode, final String... libraries) {
    return new NistSearchConfig(root.toFile(), List.of(libraries), mode, 400, PRECURSOR_PPM,
        FRAGMENT_PPM, null);
  }

  private List<String> build(final NistSearchConfig config, final Integer mwForLoss) {
    return MsPepSearchCommand.build(config, executable, queryFile, outputFile, root.toFile(),
        mwForLoss);
  }

  /**
   * The value that follows an argument in the command line.
   */
  private static String valueAfter(final List<String> command, final String argument) {
    final int index = command.indexOf(argument);
    return index < 0 || index + 1 >= command.size() ? null : command.get(index + 1);
  }

  @Test
  @DisplayName("GC-EI identity: option token dvI, libraries as absolute paths, no /PATH")
  void eiIdentity() {

    final List<String> command = build(
        config(NistSearchMode.GC_EI_IDENTITY, "mainlib", "replib"), null);

    assertEquals(executable.getAbsolutePath(), command.getFirst());
    assertEquals("dvI", command.get(1));

    // /PATH does not work in MSPepSearch, absolute library paths are mandatory
    assertFalse(command.contains("/PATH"), () -> "/PATH must never be emitted: " + command);
    assertEquals(root.resolve("mainlib").toFile().getAbsolutePath(), valueAfter(command, "/MAIN"));
    assertEquals(root.resolve("replib").toFile().getAbsolutePath(), valueAfter(command, "/REPL"));
    assertFalse(command.contains("/LIB"));

    assertEquals(queryFile.getAbsolutePath(), valueAfter(command, "/INP"));
    assertEquals(outputFile.getAbsolutePath(), valueAfter(command, "/OUTTAB"));
    assertEquals("20", valueAfter(command, "/HITS"));
    assertEquals("400", valueAfter(command, "/MinMF"));
    assertEquals("1", valueAfter(command, "/OutSpecNum"));

    // columns the result parser needs
    assertTrue(command.containsAll(
        List.of("/OutChemForm", "/OutCASrn", "/OutNISTrn", "/OutIK", "/OutNumMP", "/OutNumPk",
            "/OutMW")));
    // MS/MS only columns must not appear
    assertFalse(command.contains("/OutPrecursorType"));
    assertFalse(command.contains("/ZPPM"));
  }

  @Test
  @DisplayName("/RI is never emitted - retention indices are not used for matching")
  void retentionIndexIsNeverRequested() {

    for (final NistSearchMode mode : NistSearchMode.values()) {

      final List<String> command = build(config(mode, "mainlib", "nist_ri"), 278);
      assertFalse(command.contains("/RI"), () -> "unexpected /RI for " + mode + ": " + command);
    }
  }

  @Test
  @DisplayName("MS/MS: option token maivzhG with ppm tolerances")
  void msmsHiRes() {

    final List<String> command = build(config(NistSearchMode.MSMS_HIRES, "hr_msms_nist"), null);

    // verified against MSPepSearch 0.9.7.5: presearch m, alternative peak matching a, ignore
    // precursor region i, reverse match column v, match precursor z, high threshold h, generic G
    assertEquals("maivzhG", command.get(1));
    assertEquals("20", valueAfter(command, "/ZPPM"));
    assertEquals("40", valueAfter(command, "/MPPM"));
    assertFalse(command.contains("/Z"));
    assertFalse(command.contains("/M"));

    assertEquals(root.resolve("hr_msms_nist").toFile().getAbsolutePath(),
        valueAfter(command, "/LIB"));
    assertTrue(command.containsAll(
        List.of("/OutPrecursorMZ", "/OutDeltaPrecursorMZ", "/OutPrecursorType", "/OutInstrType",
            "/OutCE")));
    assertFalse(command.contains("/OutMW"));
  }

  @Test
  @DisplayName("An absolute tolerance uses /Z and /M instead of the ppm form")
  void absoluteTolerances() {

    final NistSearchConfig config = new NistSearchConfig(root.toFile(), List.of("hr_msms_nist"),
        NistSearchMode.MSMS_HIRES, 400, new MZTolerance(0.5, 0), new MZTolerance(0.02, 0), null);

    final List<String> command = build(config, null);

    assertEquals("0.5", valueAfter(command, "/Z"));
    assertEquals("0.02", valueAfter(command, "/M"));
    assertFalse(command.contains("/ZPPM"));
  }

  @Test
  @DisplayName("The hybrid search passes /MwForLoss and uses the H search type")
  void hybrid() {

    final List<String> command = build(config(NistSearchMode.GC_EI_HYBRID, "mainlib"), 278);

    assertEquals("dvH", command.get(1));
    assertEquals("278", valueAfter(command, "/MwForLoss"));
  }

  @Test
  @DisplayName("Similarity uses the S search type and no /MwForLoss")
  void similarity() {

    final List<String> command = build(config(NistSearchMode.GC_EI_SIMILARITY, "mainlib"), 278);

    assertEquals("dvS", command.get(1));
    assertFalse(command.contains("/MwForLoss"));
  }

  @Test
  @DisplayName("The p flag is never emitted - it crashes MSPepSearch on NIST 26 libraries")
  void penalizeRareCompoundsIsNeverEmitted() {

    for (final NistSearchMode mode : NistSearchMode.values()) {
      final String token = build(config(mode, "mainlib"), 278).get(1);
      assertFalse(token.contains("p"),
          () -> "the option token must not contain p, was " + token + " for " + mode);
    }
  }

  @Test
  @DisplayName("At most one main and one replicate library, and at most 16 in total")
  void librariesAreReducedToWhatMsPepSearchAccepts() throws IOException {

    // a second main library and 20 user libraries on top of the ones created in setUp
    Files.createDirectory(root.resolve("mainlib2"));
    Files.createFile(root.resolve("mainlib2").resolve("alphanam.in6"));

    final List<String> names = new java.util.ArrayList<>(
        List.of("mainlib", "mainlib2", "replib", "nist_ri", "hr_msms_nist"));
    for (int i = 0; i < 20; i++) {
      final String name = "user" + i;
      Files.createDirectory(root.resolve(name));
      Files.createFile(root.resolve(name).resolve("ALPHANAM.INU"));
      names.add(name);
    }

    final NistSearchConfig config = new NistSearchConfig(root.toFile(), names,
        NistSearchMode.GC_EI_IDENTITY, 400, PRECURSOR_PPM, FRAGMENT_PPM, null);

    final List<String> command = build(config, null);

    assertEquals(1, command.stream().filter("/MAIN"::equals).count());
    assertEquals(1, command.stream().filter("/REPL"::equals).count());
    assertEquals(NistSearchConfig.MAX_LIBRARIES,
        command.stream().filter(arg -> arg.startsWith("/MAIN") || arg.startsWith("/REPL")
            || arg.equals("/LIB")).count());
  }

  @Test
  @DisplayName("A path with spaces stays one command line element")
  void pathsWithSpacesAreSeparateElements() throws IOException {

    final File spaced = Files.createDirectory(root.resolve("with space")).toFile();
    final File spacedQuery = new File(spaced, "query file.msp");

    final List<String> command = MsPepSearchCommand.build(
        config(NistSearchMode.GC_EI_IDENTITY, "mainlib"), executable, spacedQuery, outputFile,
        spaced, null);

    assertTrue(command.contains(spacedQuery.getAbsolutePath()));
    assertEquals(spacedQuery.getAbsolutePath(), valueAfter(command, "/INP"));
  }
}
