/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 * SPDX-License-Identifier: MIT
 */
package io.github.mzmine.modules.dataprocessing.id_nist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NistMsSearchTest {

  @TempDir
  Path temporaryDirectory;

  @Test
  void findsMsPepSearchInDateStampedNistSubfolder() throws IOException {
    // NIST ships MSPepSearch as a date-stamped archive, so a NIST root must resolve too.
    final Path nistRoot = Files.createDirectories(temporaryDirectory.resolve("NIST23"));
    final Path release = Files.createDirectories(nistRoot.resolve("2024_03_15_MSPepSearch_x64"));
    final Path exe = Files.createFile(release.resolve("MSPepSearch64.exe"));

    assertEquals(exe.toFile(),
        NistMsSearchParameters.findMsPepSearchExecutable(nistRoot.toFile()));
    assertEquals(exe.toFile(),
        NistMsSearchParameters.findMsPepSearchExecutable(release.toFile()));
  }

  @Test
  void prefersNewestMsPepSearchReleaseFolder() throws IOException {
    final Path nistRoot = Files.createDirectories(temporaryDirectory.resolve("NIST24"));
    Files.createFile(Files.createDirectories(
        nistRoot.resolve("2022_01_01_MSPepSearch_x64")).resolve("MSPepSearch64.exe"));
    final Path newer = Files.createFile(Files.createDirectories(
        nistRoot.resolve("2024_03_15_MSPepSearch_x64")).resolve("MSPepSearch64.exe"));

    assertEquals(newer.toFile(),
        NistMsSearchParameters.findMsPepSearchExecutable(nistRoot.toFile()));
  }

  @Test
  void resolvesMsPepSearchFromRealNistInstallWhenPresent() {
    // Opt-in check against a real NIST installation. Skips where none exists.
    final File nistRoot = new File("C:" + File.separator + "NIST23");
    if (!nistRoot.isDirectory()) {
      return;
    }
    final File fromRoot = NistMsSearchParameters.findMsPepSearchExecutable(nistRoot);
    assertNotNull(fromRoot, "MSPepSearch should be discoverable from " + nistRoot);
    assertTrue(fromRoot.getName().toLowerCase().startsWith("mspepsearch"), fromRoot.toString());

    final File msSearch = new File(nistRoot, "MSSEARCH");
    if (msSearch.isDirectory()) {
      assertNotNull(NistMsSearchParameters.findMsPepSearchExecutable(msSearch),
          "MSPepSearch should also resolve from " + msSearch);
    }
  }

  @Test
  void returnsNullWhenNoMsPepSearchIsPresent() throws IOException {
    final Path empty = Files.createDirectories(temporaryDirectory.resolve("empty"));
    Files.createDirectories(empty.resolve("MSSEARCH"));
    assertNull(NistMsSearchParameters.findMsPepSearchExecutable(empty.toFile()));
    assertNull(NistMsSearchParameters.findMsPepSearchExecutable(null));
  }

  @Test
  void prefersCommonNicotineNameByCas() {
    assertEquals("Nicotine", NistCommonNameResolver.preferredDisplayName("54-11-5",
        "Pyridine, 3-(1-methyl-2-pyrrolidinyl)-, (S)-", List.of()));
    assertEquals("Nicotine", NistCommonNameResolver.preferredDisplayName("22083-74-5",
        "Pyridine, 3-(1-methyl-2-pyrrolidinyl)-", List.of()));
  }

  @Test
  void prefersConciseEquivalentNistName() {
    assertEquals("Aspirin", NistCommonNameResolver.preferredDisplayName("50-78-2",
        "2-Acetoxybenzoic acid", List.of("Benzoic acid, 2-(acetyloxy)-", "Aspirin")));
  }

  @Test
  void parsesQuotedTabOutput() {
    assertEquals(List.of("1", "Compound, name", "contains \"quote\"", "900"),
        NistMsSearchTask.parseTabLine("1\t\"Compound, name\"\t\"contains \"\"quote\"\"\"\t900"));
  }

  @Test
  void acceptsNistRootOrMsSearchDirectory() {
    File msSearch = new File("C:\\NIST23\\MSSEARCH");
    File root = new File("C:\\NIST23");
    assertEquals(msSearch, NistMsSearchParameters.normalizeMsSearchDirectory(msSearch));
    if (new File(root, "MSSEARCH").isDirectory()) {
      assertEquals(msSearch, NistMsSearchParameters.normalizeMsSearchDirectory(root));
    }
  }

  @Test
  void chartPeakGroupingPrioritizesRetentionTime() {
    assertTrue(NistMatchUtils.isSameChartPeakRetentionTime(9.378, 9.395));
    assertFalse(NistMatchUtils.isSameChartPeakRetentionTime(9.378, 23.000));
  }

  @Test
  void parsesReverseMatchFactorFromStoredNistComment() {
    assertEquals(873, NistMatchUtils.parseReverseMatchFactor(
        "Library: mainlib; rank: 1; MF: 821; reverse MF: 873; probability: 42.0%"));
    assertEquals(0, NistMatchUtils.parseReverseMatchFactor("No reverse score"));
  }

  @Test
  void rawApexRetryUsesPercentAboveLocalBaseline() {
    assertTrue(NistMsSearchTask.isAboveLocalBaseline(125d, 100d, 25d));
    assertFalse(NistMsSearchTask.isAboveLocalBaseline(124.9d, 100d, 25d));
    assertTrue(NistMsSearchTask.isAboveLocalBaseline(1d, 0d, 25d));
  }
}
