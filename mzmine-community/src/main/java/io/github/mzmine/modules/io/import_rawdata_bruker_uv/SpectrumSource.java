/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.io.import_rawdata_bruker_uv;

import static io.github.mzmine.modules.io.import_rawdata_bruker_uv.BrukerUvReader.DOUBLE_ARRAY_LAYOUT;

import java.lang.foreign.MemorySegment;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public record SpectrumSource(Long id, String description, String instrument, String InstrumentId,
                             double[] xAxis, Integer xAxisUnit, Integer yAxisUnit,
                             Double timeOffset, Long numSpectra) {

  public static List<SpectrumSource> loadFromSqlite(Connection connection) throws SQLException {
    final String query = "SELECT Id,Description,Instrument,InstrumentId,XAxis,XAxisUnit,YAxisUnit,TimeOffset FROM SpectrumSources";
    List<SpectrumSource> spectrumSources = new ArrayList<>();

    try (final Statement statement = connection.createStatement()) {
      final ResultSet sourcesResult = statement.executeQuery(query);

      while (sourcesResult.next()) {
        final long numSpectra = getNumberOfSpectra(connection, sourcesResult);
        final byte[] domainAxisBytes = sourcesResult.getBytes(5);
        final MemorySegment domainAxisSegment = MemorySegment.ofArray(domainAxisBytes);
        final double[] domainAxisValues = domainAxisSegment.toArray(
            DOUBLE_ARRAY_LAYOUT); // todo how to ensure this is read little endian?

        final SpectrumSource spectrumSource = new SpectrumSource(sourcesResult.getLong(1), //
            sourcesResult.getString(2), //
            sourcesResult.getString(3), //
            sourcesResult.getString(4), //
            domainAxisValues, //
            sourcesResult.getInt(6), //
            sourcesResult.getInt(7), //
            sourcesResult.getDouble(8), //
            numSpectra);
        spectrumSources.add(spectrumSource);
      }
      sourcesResult.close();
    }
    return spectrumSources;
  }

  private static long getNumberOfSpectra(Connection connection, ResultSet sourcesResult)
      throws SQLException {
    final String countSpectraQuery = "SELECT COUNT(*) FROM Spectra WHERE Source=%d".formatted(
        sourcesResult.getInt(1));

    final long numSpectra;
    try (var countStatement = connection.createStatement()) {
      final ResultSet countSpectraResult = countStatement.executeQuery(countSpectraQuery);
      assert countSpectraResult.next();
      numSpectra = countSpectraResult.getLong(1);
      countSpectraResult.close();
    }
    return numSpectra;
  }
}
