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

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.modules.dataprocessing.id_nist_mspepsearch.MsPepSearchQueryWriter.MspSpectrum;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests the MSP query file written by {@link MsPepSearchQueryWriter}.
 */
class MsPepSearchQueryWriterTest {

  private static final DataPoint[] PEAKS = {new SimpleDataPoint(57.07, 100),
      new SimpleDataPoint(85.0284, 12.5)};

  @TempDir
  Path root;

  private List<String> write(final MspSpectrum... spectra) throws IOException {

    final File file = root.resolve("query.msp").toFile();
    MsPepSearchQueryWriter.write(file, List.of(spectra));
    return Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
  }

  private static MspSpectrum msms(final String name, final PolarityType polarity) {
    return new MspSpectrum(name, 195.0877, polarity, null, 1.5f, PEAKS);
  }

  @Test
  @DisplayName("An MS/MS query carries its precursor m/z and polarity in the fields MSPepSearch reads")
  void msmsQuery() throws IOException {

    assertEquals(List.of("Name: mzmine_1_0", "PrecursorMZ: 195.0877", "Ion_mode: N", "RT: 1.5",
            "Num Peaks: 2", "57.07\t100.0", "85.0284\t12.5", ""),
        write(msms("mzmine_1_0", PolarityType.NEGATIVE)));
  }

  @Test
  @DisplayName("Positive is written as P - MSPepSearch ignores the spelled out POSITIVE/NEGATIVE")
  void polarityIsASingleLetter() throws IOException {

    final List<String> lines = write(msms("pos", PolarityType.POSITIVE),
        msms("neg", PolarityType.NEGATIVE));

    assertEquals(List.of("Ion_mode: P", "Ion_mode: N"),
        lines.stream().filter(line -> line.startsWith("Ion_mode")).toList());
  }

  @Test
  @DisplayName("A query of unknown polarity has no Ion_mode field")
  void unknownPolarityIsLeftOut() throws IOException {

    final List<String> lines = write(msms("unknown", PolarityType.UNKNOWN),
        msms("neutral", PolarityType.NEUTRAL), msms("any", PolarityType.ANY), msms("null", null));

    assertEquals(4, lines.stream().filter(line -> line.startsWith("Name: ")).count());
    assertEquals(List.of(), lines.stream().filter(line -> line.startsWith("Ion_mode")).toList());
  }

  @Test
  @DisplayName("An EI query has the nominal mass for the hybrid search and no precursor m/z")
  void eiQuery() throws IOException {

    assertEquals(
        List.of("Name: mzmine_7_0", "MW: 278", "Num Peaks: 2", "57.07\t100.0", "85.0284\t12.5", ""),
        write(new MspSpectrum("mzmine_7_0", null, PolarityType.UNKNOWN, 278, null, PEAKS)));
  }
}
