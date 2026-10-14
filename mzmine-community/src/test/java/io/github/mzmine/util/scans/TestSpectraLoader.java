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

package io.github.mzmine.util.scans;

import io.github.mzmine.datamodel.impl.masslist.SimpleMassList;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * Loads the test spectra in rawdatafiles/testspectra for the SpectraMerging regression test.
 */
public class TestSpectraLoader {

  public static final String TEST_SPECTRA_DIR = "rawdatafiles/testspectra/";
  public static final List<String> TEST_SPECTRA = List.of("spectrum_1.txt", "spectrum_2.txt",
      "spectrum_3.txt");

  /**
   * @return all test spectra as mass lists
   */
  @NotNull
  public static List<SimpleMassList> loadTestSpectra() throws IOException {
    final List<SimpleMassList> spectra = new ArrayList<>();
    for (final String name : TEST_SPECTRA) {
      final double[][] columns = readTsvColumns(TEST_SPECTRA_DIR + name, 2);
      spectra.add(new SimpleMassList(null, columns[0], columns[1]));
    }
    return spectra;
  }

  /**
   * Reads a tab separated resource file with a single header line.
   *
   * @param resource   the resource path
   * @param numColumns the number of columns to read
   * @return the columns
   */
  @NotNull
  public static double[][] readTsvColumns(@NotNull final String resource, final int numColumns)
      throws IOException {
    final List<double[]> rows = new ArrayList<>();
    try (final InputStream in = Objects.requireNonNull(
        TestSpectraLoader.class.getClassLoader().getResourceAsStream(resource),
        "Missing resource " + resource); final BufferedReader reader = new BufferedReader(
        new InputStreamReader(in, StandardCharsets.UTF_8))) {
      // skip header
      reader.readLine();
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.isBlank()) {
          continue;
        }
        final String[] split = line.split("\t");
        final double[] row = new double[numColumns];
        for (int c = 0; c < numColumns; c++) {
          row[c] = Double.parseDouble(split[c]);
        }
        rows.add(row);
      }
    }

    final double[][] columns = new double[numColumns][rows.size()];
    for (int r = 0; r < rows.size(); r++) {
      for (int c = 0; c < numColumns; c++) {
        columns[c][r] = rows.get(r)[c];
      }
    }
    return columns;
  }
}
