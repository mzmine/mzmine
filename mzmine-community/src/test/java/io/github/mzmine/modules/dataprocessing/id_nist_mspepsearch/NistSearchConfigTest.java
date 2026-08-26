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

import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests {@link NistSearchConfig#validate()}, which is the single source of the rules that both the
 * setup dialog and {@link NistPepSearchTask} enforce.
 * <p>
 * Windows only, because MSPepSearch is a Windows executable and validate() says so before it looks
 * at anything else.
 */
@EnabledOnOs(OS.WINDOWS)
class NistSearchConfigTest {

  @TempDir
  Path root;

  @BeforeEach
  void setUp() throws IOException {

    Files.createDirectories(root.resolve("MSPepSearch"));
    Files.createFile(root.resolve("MSPepSearch/MSPepSearch64.exe"));

    for (final String[] library : new String[][]{{"mainlib", "alphanam.in6"},
        {"replib", "contrib.inr"}, {"hr_msms_nist", "ALPHANAM.INU"}}) {
      Files.createDirectory(root.resolve(library[0]));
      Files.createFile(root.resolve(library[0]).resolve(library[1]));
    }
  }

  private NistSearchConfig config(final File nistDirectory, final NistSearchMode mode,
      final List<String> libraries) {
    return new NistSearchConfig(nistDirectory, libraries, mode, 400, new MZTolerance(0.005, 20),
        new MZTolerance(0.01, 40), null);
  }

  @Test
  @DisplayName("A complete configuration has no problems")
  void validConfiguration() {

    assertTrue(config(root.toFile(), NistSearchMode.GC_EI_IDENTITY,
        List.of("mainlib", "replib")).validate().isEmpty());
    assertTrue(config(root.toFile(), NistSearchMode.MSMS_HIRES,
        List.of("hr_msms_nist")).validate().isEmpty());
  }

  @Test
  @DisplayName("A directory without MSPepSearch is reported")
  void missingExecutable() {

    final List<String> problems = config(new File(root.toFile(), "not-an-install"),
        NistSearchMode.MSMS_HIRES, List.of("hr_msms_nist")).validate();

    assertEquals(1, problems.size());
    assertTrue(problems.getFirst().contains("MSPepSearch was not found"),
        () -> "unexpected problem: " + problems);
  }

  @Test
  @DisplayName("A null directory is reported rather than throwing")
  void nullDirectory() {
    assertTrue(config(null, NistSearchMode.MSMS_HIRES, List.of("hr_msms_nist")).validate().stream()
        .anyMatch(problem -> problem.contains("MSPepSearch was not found")));
  }

  @Test
  @DisplayName("Selecting no library is reported, and stops the later checks")
  void noLibraries() {

    final List<String> problems = config(root.toFile(), NistSearchMode.MSMS_HIRES,
        List.of()).validate();

    assertEquals(1, problems.size());
    assertTrue(problems.getFirst().contains("at least one NIST library"),
        () -> "unexpected problem: " + problems);
  }

  @Test
  @DisplayName("More than 16 libraries is reported")
  void tooManyLibraries() throws IOException {

    final List<String> names = new ArrayList<>(List.of("hr_msms_nist"));
    for (int i = 0; i < 20; i++) {
      final String name = "user" + i;
      Files.createDirectory(root.resolve(name));
      Files.createFile(root.resolve(name).resolve("ALPHANAM.INU"));
      names.add(name);
    }

    assertTrue(config(root.toFile(), NistSearchMode.MSMS_HIRES, names).validate().stream()
        .anyMatch(problem -> problem.contains("at most " + NistSearchConfig.MAX_LIBRARIES)));
  }

  @Test
  @DisplayName("MS/MS against only the EI libraries is reported")
  void msmsNeedsATandemLibrary() {

    final List<String> problems = config(root.toFile(), NistSearchMode.MSMS_HIRES,
        List.of("mainlib", "replib")).validate();

    assertEquals(1, problems.size());
    assertTrue(problems.getFirst().contains("needs a tandem library"),
        () -> "unexpected problem: " + problems);
  }

  @Test
  @DisplayName("GC-EI against only a tandem library is reported")
  void eiNeedsAnEiLibrary() {

    final List<String> problems = config(root.toFile(), NistSearchMode.GC_EI_IDENTITY,
        List.of("hr_msms_nist")).validate();

    assertEquals(1, problems.size());
    assertTrue(problems.getFirst().contains("need an EI library"),
        () -> "unexpected problem: " + problems);
  }

  @Test
  @DisplayName("Mixing an EI and a tandem library is allowed - only one sided selections warn")
  void mixedLibrariesAreAllowed() {

    assertTrue(config(root.toFile(), NistSearchMode.MSMS_HIRES,
        List.of("mainlib", "hr_msms_nist")).validate().isEmpty());
    assertTrue(config(root.toFile(), NistSearchMode.GC_EI_IDENTITY,
        List.of("mainlib", "hr_msms_nist")).validate().isEmpty());
  }
}
