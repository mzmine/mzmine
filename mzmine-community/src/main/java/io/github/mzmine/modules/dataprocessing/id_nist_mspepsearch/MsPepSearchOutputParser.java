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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Parses the tab delimited hit table MSPepSearch writes with {@code /OUTTAB}.
 * <p>
 * The parser is header driven because the columns differ between search modes - a low resolution
 * search reports {@code MF} / {@code R.Match} / {@code DotProd} where a high resolution one reports
 * {@code Score} / {@code Rev-Dot} / {@code Dot Product} - and because the optional
 * {@code /Out...} switches insert columns anywhere in the row. Unknown columns are ignored and
 * missing ones simply yield null.
 * <p>
 * It never throws on content. A malformed row is recorded as a warning and skipped, and a file
 * without a header row means "no hits" - MSPepSearch writes only its {@code >} prefixed banner and
 * footer when nothing was found.
 */
final class MsPepSearchOutputParser {

  /**
   * Lines MSPepSearch uses for its banner, the echoed command line and the closing summary. They
   * appear both before and after the table.
   */
  private static final String COMMENT_PREFIX = ">";

  private static final String SEPARATOR = "\t";

  /**
   * Canonical column name to the labels MSPepSearch is known to use for it. Lower case, because the
   * lookup is case insensitive. Several labels map to the same field so that a NIST version which
   * renames a column does not silently drop it.
   */
  private static final Map<Column, List<String>> COLUMN_LABELS = Map.ofEntries(
      Map.entry(Column.SPEC_NUM, List.of("num")),
      Map.entry(Column.UNKNOWN, List.of("unknown")), Map.entry(Column.RANK, List.of("rank")),
      Map.entry(Column.LIBRARY, List.of("library", "lib")), Map.entry(Column.ID, List.of("id")),
      Map.entry(Column.NAME, List.of("name", "peptide")),
      Map.entry(Column.FORMULA, List.of("formula")),
      Map.entry(Column.CAS, List.of("cas", "cas r.n.", "cas rn")),
      Map.entry(Column.NIST_RN, List.of("nist r.n.", "nist rn", "nist no")),
      Map.entry(Column.INCHIKEY, List.of("inchikey")),
      Map.entry(Column.EXACT_MASS, List.of("mass")),
      Map.entry(Column.NOMINAL_MW, List.of("lib mw", "mw")),
      // primary score: MF for low resolution searches, Score for high resolution ones
      Map.entry(Column.MATCH_FACTOR, List.of("mf", "score")),
      Map.entry(Column.REV_MATCH_FACTOR, List.of("r.match", "rev-dot", "rmf", "rev dot")),
      Map.entry(Column.DOT_PRODUCT, List.of("dotprod", "dot product")),
      Map.entry(Column.PROBABILITY, List.of("prob(%)", "prob", "probability")),
      Map.entry(Column.NUM_MATCHED_PEAKS, List.of("nummp")),
      Map.entry(Column.NUM_PEAKS, List.of("num.peaks", "num peaks")),
      Map.entry(Column.PRECURSOR_MZ, List.of("lib precursor m/z")),
      Map.entry(Column.PRECURSOR_TYPE, List.of("prec.type", "precursor type")),
      Map.entry(Column.CHARGE, List.of("charge")),
      Map.entry(Column.COLLISION_ENERGY, List.of("ce", "collision energy")),
      Map.entry(Column.INSTRUMENT_TYPE, List.of("instr.type", "instrument type")));

  private MsPepSearchOutputParser() {
  }

  /**
   * Parses an MSPepSearch output file.
   *
   * @param file the {@code /OUTTAB} file. A missing file is reported as a warning, not an error,
   *             because MSPepSearch does not create it when it finds nothing at all.
   */
  static @NotNull NistPepSearchResult parse(@NotNull final File file) throws IOException {

    if (!file.isFile()) {
      return new NistPepSearchResult(List.of(),
          List.of("MSPepSearch wrote no results file " + file));
    }

    try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
      return parse(reader);
    }
  }

  /**
   * Parses MSPepSearch output from a reader.
   */
  static @NotNull NistPepSearchResult parse(@NotNull final Reader reader) throws IOException {

    final BufferedReader buffered =
        reader instanceof BufferedReader b ? b : new BufferedReader(reader);

    final List<NistHit> hits = new ArrayList<>();
    final List<String> warnings = new ArrayList<>();
    Map<Column, Integer> indices = null;

    int lineNumber = 0;
    String line;
    while ((line = buffered.readLine()) != null) {

      lineNumber++;

      if (line.isBlank() || line.startsWith(COMMENT_PREFIX)) {
        continue;
      }

      final String[] cells = line.split(SEPARATOR, -1);

      if (indices == null) {
        // the first non comment, non blank line is the header
        indices = mapColumns(cells, warnings);
        continue;
      }

      final NistHit hit = parseRow(cells, indices, lineNumber, warnings);
      if (hit != null) {
        hits.add(hit);
      }
    }

    if (indices == null) {
      // MSPepSearch omits the header entirely when no spectrum produced a hit
      return new NistPepSearchResult(List.of(), warnings);
    }

    return new NistPepSearchResult(hits, warnings);
  }

  /**
   * Maps the known columns to their position in the header line.
   */
  private static Map<Column, Integer> mapColumns(final String[] header,
      final List<String> warnings) {

    final Map<String, Column> labelToColumn = new HashMap<>();
    COLUMN_LABELS.forEach((column, labels) -> labels.forEach(label -> {
      // first declared label wins, so "mf" is not overwritten by a later alias
      labelToColumn.putIfAbsent(label, column);
    }));

    final Map<Column, Integer> indices = new HashMap<>();
    for (int i = 0; i < header.length; i++) {

      final String label = header[i].trim().toLowerCase(Locale.ROOT);
      final Column column = labelToColumn.get(label);

      // Keep the first occurrence: the high resolution table has both a search spectrum
      // "Precursor m/z" and a "Lib Precursor m/z", and "o.NumMP" must not shadow "NumMP".
      if (column != null) {
        indices.putIfAbsent(column, i);
      }
    }

    if (!indices.containsKey(Column.NAME)) {
      warnings.add("MSPepSearch output has no recognised compound name column, header was: "
          + String.join(", ", header));
    }
    if (!indices.containsKey(Column.MATCH_FACTOR)) {
      warnings.add("MSPepSearch output has no recognised match factor column, header was: "
          + String.join(", ", header));
    }

    return indices;
  }

  /**
   * Builds a hit from one table row.
   *
   * @return the hit, or null if the row could not be used.
   */
  private static @Nullable NistHit parseRow(final String[] cells,
      final Map<Column, Integer> indices, final int lineNumber, final List<String> warnings) {

    final String name = string(cells, indices, Column.NAME);
    if (name == null) {
      warnings.add("Ignoring row " + lineNumber + " of the MSPepSearch output: no compound name");
      return null;
    }

    final Integer specNum = integer(cells, indices, Column.SPEC_NUM);
    final String unknown = string(cells, indices, Column.UNKNOWN);
    if (specNum == null && unknown == null) {
      warnings.add("Ignoring row " + lineNumber
          + " of the MSPepSearch output: neither a spectrum number nor an unknown name to map it to");
      return null;
    }

    final Integer rank = integer(cells, indices, Column.RANK);

    return new NistHit(specNum == null ? -1 : specNum, unknown, rank == null ? -1 : rank, name,
        string(cells, indices, Column.LIBRARY), string(cells, indices, Column.ID),
        string(cells, indices, Column.NIST_RN), cas(cells, indices),
        string(cells, indices, Column.INCHIKEY), string(cells, indices, Column.FORMULA),
        decimal(cells, indices, Column.EXACT_MASS), integer(cells, indices, Column.NOMINAL_MW),
        integer(cells, indices, Column.MATCH_FACTOR),
        integer(cells, indices, Column.REV_MATCH_FACTOR),
        integer(cells, indices, Column.DOT_PRODUCT), decimal(cells, indices, Column.PROBABILITY),
        integer(cells, indices, Column.NUM_MATCHED_PEAKS), integer(cells, indices, Column.NUM_PEAKS),
        decimal(cells, indices, Column.PRECURSOR_MZ),
        string(cells, indices, Column.PRECURSOR_TYPE), integer(cells, indices, Column.CHARGE),
        string(cells, indices, Column.COLLISION_ENERGY),
        string(cells, indices, Column.INSTRUMENT_TYPE));
  }

  /**
   * MSPepSearch writes an unformatted CAS number and uses {@code 0} when it has none.
   */
  private static @Nullable String cas(final String[] cells, final Map<Column, Integer> indices) {

    final String cas = string(cells, indices, Column.CAS);
    return cas == null || "0".equals(cas) ? null : cas;
  }

  private static @Nullable String string(final String[] cells, final Map<Column, Integer> indices,
      final Column column) {

    final Integer index = indices.get(column);
    if (index == null || index >= cells.length) {
      return null;
    }

    final String value = cells[index].trim();
    // "-" is MSPepSearch's placeholder for an absent value, e.g. in the uRI column
    return value.isEmpty() || "-".equals(value) ? null : value;
  }

  private static @Nullable Integer integer(final String[] cells, final Map<Column, Integer> indices,
      final Column column) {

    final String value = string(cells, indices, column);
    if (value == null) {
      return null;
    }

    try {
      return Integer.valueOf(value);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static @Nullable Double decimal(final String[] cells, final Map<Column, Integer> indices,
      final Column column) {

    final String value = string(cells, indices, column);
    if (value == null) {
      return null;
    }

    try {
      return Double.valueOf(value);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /**
   * The columns this parser understands.
   */
  private enum Column {
    SPEC_NUM, UNKNOWN, RANK, LIBRARY, ID, NAME, FORMULA, CAS, NIST_RN, INCHIKEY, EXACT_MASS,
    NOMINAL_MW, MATCH_FACTOR, REV_MATCH_FACTOR, DOT_PRODUCT, PROBABILITY, NUM_MATCHED_PEAKS,
    NUM_PEAKS, PRECURSOR_MZ, PRECURSOR_TYPE, CHARGE, COLLISION_ENERGY, INSTRUMENT_TYPE
  }
}
