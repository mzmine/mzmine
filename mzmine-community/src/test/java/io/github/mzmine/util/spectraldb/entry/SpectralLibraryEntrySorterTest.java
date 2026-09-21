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

package io.github.mzmine.util.spectraldb.entry;

import io.github.mzmine.datamodel.PolarityType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The order of library entries must not depend on the order the import tasks happened to finish.
 */
class SpectralLibraryEntrySorterTest {

  private static SpectralLibraryEntry entry(final double precursorMz, final String polarity,
      final String name, final String smiles, final int signals) {
    final Map<DBEntryField, Object> fields = new EnumMap<>(DBEntryField.class);
    fields.put(DBEntryField.PRECURSOR_MZ, precursorMz);
    fields.put(DBEntryField.POLARITY, polarity);
    fields.put(DBEntryField.NAME, name);
    fields.put(DBEntryField.SMILES, smiles);

    final double[] mzs = new double[signals];
    final double[] intensities = new double[signals];
    for (int i = 0; i < signals; i++) {
      mzs[i] = 100d + i;
      intensities[i] = 1d;
    }
    return SpectralLibraryEntryFactory.create(null, fields, mzs, intensities);
  }

  /**
   * The pair that made the result flip in the workshop batch: the same compound at the same
   * precursor, written once as the keto and once as the enol tautomer.
   */
  private static List<SpectralLibraryEntry> tautomerPair() {
    return new ArrayList<>(List.of( //
        entry(310.24, "+", "Pseudane", "CCCCCCCCCC=CC1=CC(C=2C=CC=CC2N1)=O", 8),
        entry(310.24, "+", "Pseudane", "CCCCCCCCCC=CC1=CC(=C2C=CC=CC2=N1)O", 8)));
  }

  @Test
  void testTiedEntriesGetAStableOrder() {
    final List<SpectralLibraryEntry> expected = new ArrayList<>(tautomerPair());
    expected.sort(SpectralLibraryEntrySorter.DETERMINISTIC);

    // whatever order the import produced, sorting has to land on the same sequence
    final List<SpectralLibraryEntry> shuffled = tautomerPair();
    Collections.reverse(shuffled);
    shuffled.sort(SpectralLibraryEntrySorter.DETERMINISTIC);

    Assertions.assertEquals(smiles(expected), smiles(shuffled),
        "entries that only differ in the structure were not ordered deterministically");
    // and the two are actually distinguished, not treated as equal
    Assertions.assertNotEquals(0,
        SpectralLibraryEntrySorter.DETERMINISTIC.compare(expected.get(0), expected.get(1)));
  }

  @Test
  void testOrderIsIndependentOfInputOrder() {
    final List<SpectralLibraryEntry> entries = new ArrayList<>(List.of( //
        entry(200.1, "-", "B", "CCO", 5), //
        entry(100.5, "+", "A", "CCN", 3), //
        entry(200.1, "+", "B", "CCCl", 5), //
        entry(200.1, "+", "B", "CCBr", 5), //
        entry(100.5, "+", "A", "CCN", 7)));

    final List<SpectralLibraryEntry> reference = new ArrayList<>(entries);
    reference.sort(SpectralLibraryEntrySorter.DETERMINISTIC);

    final Random random = new Random(42);
    for (int i = 0; i < 20; i++) {
      final List<SpectralLibraryEntry> shuffled = new ArrayList<>(entries);
      Collections.shuffle(shuffled, random);
      shuffled.sort(SpectralLibraryEntrySorter.DETERMINISTIC);
      Assertions.assertEquals(smiles(reference), smiles(shuffled), "shuffle " + i);
      Assertions.assertEquals(names(reference), names(shuffled), "shuffle " + i);
    }

    // sorted by precursor m/z first, which the matching relies on for its binary search
    Assertions.assertEquals(List.of(100.5, 100.5, 200.1, 200.1, 200.1),
        reference.stream().map(SpectralLibraryEntry::getPrecursorMZ).toList());
  }

  /**
   * GC-EI entries have no precursor, they must not throw and must stay at one end.
   */
  @Test
  void testMissingPrecursorSortsLast() {
    final Map<DBEntryField, Object> noPrecursor = new EnumMap<>(DBEntryField.class);
    noPrecursor.put(DBEntryField.NAME, "GC entry");
    final SpectralLibraryEntry without = SpectralLibraryEntryFactory.create(null, noPrecursor,
        new double[]{100d}, new double[]{1d});

    final List<SpectralLibraryEntry> entries = new ArrayList<>(
        List.of(without, entry(100.5, "+", "A", "CCN", 3)));
    entries.sort(SpectralLibraryEntrySorter.DETERMINISTIC);
    Assertions.assertEquals("GC entry", entries.getLast().getOrElse(DBEntryField.NAME, ""));

    Collections.reverse(entries);
    entries.sort(SpectralLibraryEntrySorter.DETERMINISTIC);
    Assertions.assertEquals("GC entry", entries.getLast().getOrElse(DBEntryField.NAME, ""));
  }

  private static List<String> smiles(final List<SpectralLibraryEntry> entries) {
    return entries.stream().map(e -> e.getAsString(DBEntryField.SMILES).orElse("")).toList();
  }

  private static List<String> names(final List<SpectralLibraryEntry> entries) {
    return entries.stream().map(e -> e.getAsString(DBEntryField.NAME).orElse("")).toList();
  }

  @Test
  void testPolarityIsCompared() {
    final SpectralLibraryEntry positive = entry(150d, "+", "X", "CCN", 4);
    final SpectralLibraryEntry negative = entry(150d, "-", "X", "CCN", 4);
    Assertions.assertNotEquals(0,
        SpectralLibraryEntrySorter.DETERMINISTIC.compare(positive, negative));
    Assertions.assertEquals(PolarityType.POSITIVE, positive.getPolarity());
    Assertions.assertEquals(PolarityType.NEGATIVE, negative.getPolarity());
  }
}
