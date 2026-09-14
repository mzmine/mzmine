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
import java.util.Comparator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Orders library entries so that a search over several libraries always sees them in the same
 * order.
 * <p>
 * Spectral libraries are imported by one task per file, so the order in which they end up in the
 * project is the order in which those tasks happen to finish. Matching then sorts entries by
 * precursor m/z only, and sorts the matches of a row by score only. Both are stable sorts, so every
 * tie falls back to that arbitrary import order and the same data set can end up with a different
 * annotation from one run to the next. Comparing further fields removes that.
 */
public final class SpectralLibraryEntrySorter {

  /**
   * Total order over the fields that distinguish two entries. Entries that compare equal here carry
   * the same metadata and the same number of signals, so it does not matter which of them a tie
   * picks.
   */
  public static final Comparator<SpectralLibraryEntry> DETERMINISTIC = Comparator //
      .comparing(SpectralLibraryEntry::getPrecursorMZ, // first to allow binary search
          Comparator.nullsLast(Comparator.naturalOrder())) // GC-EI has precursor mz null
      .thenComparing(SpectralLibraryEntrySorter::polarityOf) //
      .thenComparingInt(entry -> entry.getMsLevel().orElse(Integer.MAX_VALUE)) //
      .thenComparingInt(SpectralLibraryEntry::getNumberOfDataPoints) //
      .thenComparing(field(DBEntryField.ENTRY_ID)) //
      .thenComparing(field(DBEntryField.NAME)) //
      .thenComparing(field(DBEntryField.ION_TYPE)) //
      .thenComparing(field(DBEntryField.FORMULA)) //
      .thenComparing(field(DBEntryField.INCHIKEY)) //
      .thenComparing(field(DBEntryField.INCHI)) //
      .thenComparing(field(DBEntryField.SMILES)) //
      .thenComparing(field(DBEntryField.DATASET_ID));

  private SpectralLibraryEntrySorter() {
  }

  /**
   * Compares one field by its string form, which keeps this independent of the type a parser
   * happened to store.
   */
  private static @NotNull Comparator<SpectralLibraryEntry> field(@NotNull final DBEntryField f) {
    return Comparator.comparing(entry -> entry.getAsString(f).orElse(""));
  }

  private static @NotNull String polarityOf(@Nullable final SpectralLibraryEntry entry) {
    final PolarityType polarity = entry == null ? null : entry.getPolarity();
    return polarity == null ? "" : polarity.name();
  }
}
