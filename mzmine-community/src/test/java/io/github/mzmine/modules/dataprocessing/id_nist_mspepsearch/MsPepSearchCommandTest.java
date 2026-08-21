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

import io.github.mzmine.modules.dataprocessing.id_nist_mspepsearch.NistEiSearchParameters.NistRetentionIndexParameters;
import io.github.mzmine.modules.dataprocessing.id_nist_mspepsearch.NistEiSearchParameters.RIPenaltyRate;
import io.github.mzmine.modules.dataprocessing.id_nist_mspepsearch.NistMsMsSearchParameters.HiResThreshold;
import io.github.mzmine.modules.dataprocessing.id_nist_mspepsearch.NistPepSearchAdvancedParameters.Presearch;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.RIColumn;
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

  @TempDir
  Path root;

  private NistPepSearchParameters parameters;
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

    // the static parameter objects are shared between instances, so every value the tests rely on
    // is set explicitly here
    parameters = new NistPepSearchParameters();
    parameters.setParameter(NistPepSearchParameters.nistDirectory, root.toFile());
    parameters.setParameter(NistPepSearchParameters.minMatchFactor, 400);
    parameters.setParameter(NistPepSearchParameters.maxHits, 10);

    final ParameterSet advanced = parameters.getParameter(NistPepSearchParameters.advanced)
        .getEmbeddedParameters();
    advanced.setParameter(NistPepSearchAdvancedParameters.presearch, Presearch.DEFAULT);
    advanced.setParameter(NistPepSearchAdvancedParameters.reverseSearch, false);
    advanced.setParameter(NistPepSearchAdvancedParameters.librariesInMemory, false);
    advanced.setParameter(NistPepSearchAdvancedParameters.elevatedPriority, false);
    advanced.setParameter(NistPepSearchAdvancedParameters.extraArguments, "");

    final ParameterSet ei = parameters.getParameter(NistPepSearchParameters.eiParameters)
        .getEmbeddedParameters();
    ei.getParameter(NistEiSearchParameters.retentionIndex).setValue(false);

    final ParameterSet msms = parameters.getParameter(NistPepSearchParameters.msmsParameters)
        .getEmbeddedParameters();
    msms.setParameter(NistMsMsSearchParameters.matchPrecursor, true);
    msms.setParameter(NistMsMsSearchParameters.alternativePeakMatching, true);
    msms.setParameter(NistMsMsSearchParameters.ignorePrecursorRegion, true);
    msms.setParameter(NistMsMsSearchParameters.threshold, HiResThreshold.HIGH);
    msms.setParameter(NistMsMsSearchParameters.precursorTolerance, new MZTolerance(0.005, 20));
    msms.setParameter(NistMsMsSearchParameters.fragmentTolerance, new MZTolerance(0.01, 40));
  }

  private List<String> build(final Integer mwForLoss) {
    return MsPepSearchCommand.build(parameters, executable, queryFile, outputFile, root.toFile(),
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

    parameters.setParameter(NistPepSearchParameters.searchMode, NistSearchMode.GC_EI_IDENTITY);
    parameters.setParameter(NistPepSearchParameters.libraries, List.of("mainlib", "replib"));

    final List<String> command = build(null);

    assertEquals(executable.getAbsolutePath(), command.getFirst());
    assertEquals("dvI", command.get(1));

    // /PATH does not work in MSPepSearch, absolute library paths are mandatory
    assertFalse(command.contains("/PATH"), () -> "/PATH must never be emitted: " + command);
    assertEquals(root.resolve("mainlib").toFile().getAbsolutePath(), valueAfter(command, "/MAIN"));
    assertEquals(root.resolve("replib").toFile().getAbsolutePath(), valueAfter(command, "/REPL"));
    assertFalse(command.contains("/LIB"));

    assertEquals(queryFile.getAbsolutePath(), valueAfter(command, "/INP"));
    assertEquals(outputFile.getAbsolutePath(), valueAfter(command, "/OUTTAB"));
    assertEquals("10", valueAfter(command, "/HITS"));
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
  @DisplayName("GC-EI identity with /RI builds the sut10rAV token")
  void eiIdentityWithRetentionIndex() {

    parameters.setParameter(NistPepSearchParameters.searchMode, NistSearchMode.GC_EI_IDENTITY);
    parameters.setParameter(NistPepSearchParameters.libraries, List.of("mainlib", "nist_ri"));

    final ParameterSet ei = parameters.getParameter(NistPepSearchParameters.eiParameters)
        .getEmbeddedParameters();
    ei.getParameter(NistEiSearchParameters.retentionIndex).setValue(true);

    final ParameterSet ri = ei.getParameter(NistEiSearchParameters.retentionIndex)
        .getEmbeddedParameters();
    ri.setParameter(NistRetentionIndexParameters.column, RIColumn.SEMIPOLAR);
    ri.setParameter(NistRetentionIndexParameters.tolerance, 10);
    ri.setParameter(NistRetentionIndexParameters.overrideFromSpectrum, false);
    ri.setParameter(NistRetentionIndexParameters.useOtherNonPolar, false);
    ri.setParameter(NistRetentionIndexParameters.assumeUnspecified, true);
    ri.getParameter(NistRetentionIndexParameters.penalty).setValue(true);
    ri.getParameter(NistRetentionIndexParameters.penalty).getEmbeddedParameter()
        .setValue(RIPenaltyRate.AVERAGE);

    final List<String> command = build(null);

    assertEquals("sut10rAV", valueAfter(command, "/RI"));
    assertEquals(root.resolve("nist_ri").toFile().getAbsolutePath(), valueAfter(command, "/LIB"));
  }

  @Test
  @DisplayName("Without an RI penalty the token ends in x so the score is not changed")
  void retentionIndexWithoutPenalty() {

    parameters.setParameter(NistPepSearchParameters.searchMode, NistSearchMode.GC_EI_IDENTITY);
    parameters.setParameter(NistPepSearchParameters.libraries, List.of("mainlib"));

    final ParameterSet ei = parameters.getParameter(NistPepSearchParameters.eiParameters)
        .getEmbeddedParameters();
    ei.getParameter(NistEiSearchParameters.retentionIndex).setValue(true);

    final ParameterSet ri = ei.getParameter(NistEiSearchParameters.retentionIndex)
        .getEmbeddedParameters();
    ri.setParameter(NistRetentionIndexParameters.column, RIColumn.NONPOLAR);
    ri.setParameter(NistRetentionIndexParameters.overrideFromSpectrum, false);
    ri.setParameter(NistRetentionIndexParameters.useOtherNonPolar, false);
    ri.setParameter(NistRetentionIndexParameters.assumeUnspecified, false);
    ri.getParameter(NistRetentionIndexParameters.penalty).setValue(false);

    assertEquals("nx", valueAfter(build(null), "/RI"));
  }

  @Test
  @DisplayName("High resolution MS/MS: option token maivzhG with ppm tolerances")
  void msmsHiRes() {

    parameters.setParameter(NistPepSearchParameters.searchMode, NistSearchMode.MSMS_HIRES);
    parameters.setParameter(NistPepSearchParameters.libraries, List.of("hr_msms_nist"));

    final List<String> command = build(null);

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

    parameters.setParameter(NistPepSearchParameters.searchMode, NistSearchMode.MSMS_HIRES);
    parameters.setParameter(NistPepSearchParameters.libraries, List.of("hr_msms_nist"));

    final ParameterSet msms = parameters.getParameter(NistPepSearchParameters.msmsParameters)
        .getEmbeddedParameters();
    msms.setParameter(NistMsMsSearchParameters.precursorTolerance, new MZTolerance(0.5, 0));
    msms.setParameter(NistMsMsSearchParameters.fragmentTolerance, new MZTolerance(0.02, 0));

    final List<String> command = build(null);

    assertEquals("0.5", valueAfter(command, "/Z"));
    assertEquals("0.02", valueAfter(command, "/M"));
    assertFalse(command.contains("/ZPPM"));
  }

  @Test
  @DisplayName("Not matching the precursor uses u, and the presearch falls back from m to d")
  void withoutPrecursorMatching() {

    parameters.setParameter(NistPepSearchParameters.searchMode, NistSearchMode.MSMS_HIRES);
    parameters.setParameter(NistPepSearchParameters.libraries, List.of("hr_msms_nist"));

    final ParameterSet msms = parameters.getParameter(NistPepSearchParameters.msmsParameters)
        .getEmbeddedParameters();
    msms.setParameter(NistMsMsSearchParameters.matchPrecursor, false);
    msms.setParameter(NistMsMsSearchParameters.ignorePrecursorRegion, false);
    msms.setParameter(NistMsMsSearchParameters.threshold, HiResThreshold.LOW);

    // MSPepSearch fails with "Presearch type 'm' is not compatible with High Resolution search
    // option 'u'", so the default presearch has to become d here rather than m
    assertEquals("davulG", build(null).get(1));
  }

  @Test
  @DisplayName("The hybrid search passes /MwForLoss and uses the H search type")
  void hybrid() {

    parameters.setParameter(NistPepSearchParameters.searchMode, NistSearchMode.GC_EI_HYBRID);
    parameters.setParameter(NistPepSearchParameters.libraries, List.of("mainlib"));

    final List<String> command = build(278);

    assertEquals("dvH", command.get(1));
    assertEquals("278", valueAfter(command, "/MwForLoss"));
  }

  @Test
  @DisplayName("Similarity uses the S search type and no /MwForLoss")
  void similarity() {

    parameters.setParameter(NistPepSearchParameters.searchMode, NistSearchMode.GC_EI_SIMILARITY);
    parameters.setParameter(NistPepSearchParameters.libraries, List.of("mainlib"));

    final List<String> command = build(278);

    assertEquals("dvS", command.get(1));
    assertFalse(command.contains("/MwForLoss"));
  }

  @Test
  @DisplayName("The p flag is never emitted - it crashes MSPepSearch on NIST 26 libraries")
  void penalizeRareCompoundsIsNeverEmitted() {

    parameters.setParameter(NistPepSearchParameters.libraries, List.of("mainlib"));

    for (final NistSearchMode mode : NistSearchMode.values()) {
      parameters.setParameter(NistPepSearchParameters.searchMode, mode);
      final String token = build(278).get(1);
      assertFalse(token.contains("p"),
          () -> "the option token must not contain p, was " + token + " for " + mode);
    }
  }

  @Test
  @DisplayName("Presearch off maps to s for both resolutions")
  void presearchOff() {

    parameters.setParameter(NistPepSearchParameters.libraries, List.of("mainlib"));
    parameters.getParameter(NistPepSearchParameters.advanced).getEmbeddedParameters()
        .setParameter(NistPepSearchAdvancedParameters.presearch, Presearch.OFF);

    parameters.setParameter(NistPepSearchParameters.searchMode, NistSearchMode.GC_EI_IDENTITY);
    assertEquals("svI", build(null).get(1));

    parameters.setParameter(NistPepSearchParameters.libraries, List.of("hr_msms_nist"));
    parameters.setParameter(NistPepSearchParameters.searchMode, NistSearchMode.MSMS_HIRES);
    assertEquals("saivzhG", build(null).get(1));
  }

  @Test
  @DisplayName("Extra arguments are appended and flags are emitted")
  void advancedFlags() {

    parameters.setParameter(NistPepSearchParameters.searchMode, NistSearchMode.GC_EI_IDENTITY);
    parameters.setParameter(NistPepSearchParameters.libraries, List.of("mainlib"));

    final ParameterSet advanced = parameters.getParameter(NistPepSearchParameters.advanced)
        .getEmbeddedParameters();
    advanced.setParameter(NistPepSearchAdvancedParameters.librariesInMemory, true);
    advanced.setParameter(NistPepSearchAdvancedParameters.elevatedPriority, true);
    advanced.setParameter(NistPepSearchAdvancedParameters.reverseSearch, true);
    advanced.setParameter(NistPepSearchAdvancedParameters.extraArguments, "/MinInt 5 /OnlyFound");

    final List<String> command = build(null);

    assertEquals("drvI", command.get(1));
    assertTrue(command.contains("/LibInMem"));
    assertTrue(command.contains("/HiPri"));
    assertEquals("5", valueAfter(command, "/MinInt"));
    assertTrue(command.contains("/OnlyFound"));
  }

  @Test
  @DisplayName("A path with spaces stays one command line element")
  void pathsWithSpacesAreSeparateElements() throws IOException {

    parameters.setParameter(NistPepSearchParameters.searchMode, NistSearchMode.GC_EI_IDENTITY);
    parameters.setParameter(NistPepSearchParameters.libraries, List.of("mainlib"));

    final File spaced = Files.createDirectory(root.resolve("with space")).toFile();
    final File spacedQuery = new File(spaced, "query file.msp");

    final List<String> command = MsPepSearchCommand.build(parameters, executable, spacedQuery,
        outputFile, spaced, null);

    assertTrue(command.contains(spacedQuery.getAbsolutePath()));
    assertEquals(spacedQuery.getAbsolutePath(), valueAfter(command, "/INP"));
  }
}
