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

package io.github.mzmine.modules.dataprocessing.id_spectral_match_sort;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.util.scans.similarity.SpectralSimilarity;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import io.github.mzmine.util.spectraldb.entry.SpectralDBEntry;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.util.List;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that {@link SortSpectralMatchesTask#sortIdentities} ranks the complete list of a row by
 * similarity score, also for matches that were already on the row before the last search.
 */
class SortSpectralMatchesTaskTest {

  private static final double ROW_MZ = 300.1234;

  private ModularFeatureListRow row;

  private static SpectralDBAnnotation match(final double cosine) {
    final SpectralLibraryEntry entry = new SpectralDBEntry(null, new double[]{100d, 200d},
        new double[]{1d, 1d});
    entry.putIfNotNull(DBEntryField.NAME, "cosine " + cosine);
    entry.putIfNotNull(DBEntryField.PRECURSOR_MZ, ROW_MZ);

    return new SpectralDBAnnotation(entry, new SpectralSimilarity("test", cosine, 2, 1d), null,
        null, ROW_MZ, null, null);
  }

  private static List<String> names(final List<SpectralDBAnnotation> matches) {
    return matches.stream().map(SpectralDBAnnotation::getCompoundName).toList();
  }

  @BeforeEach
  void setUp() {
    final ModularFeatureList flist = new ModularFeatureList("flist", null,
        new RawDataFileImpl("file", null, null, Color.BLACK));
    row = new ModularFeatureListRow(flist, 1);
    row.set(MZType.class, ROW_MZ);
    flist.addRow(row);
  }

  @Test
  void sortsCompleteListAfterNewMatchesWereAppended() {
    final SpectralDBAnnotation strong = match(0.90);
    final SpectralDBAnnotation medium = match(0.50);
    final SpectralDBAnnotation weak = match(0.30);

    // a previous search already annotated the row with the best match of all
    row.addSpectralLibraryMatches(List.of(strong));
    // a second search appends weaker matches at the end of the list
    row.addSpectralLibraryMatches(List.of(weak, medium));

    SortSpectralMatchesTask.sortIdentities(row);

    // the new matches are ranked against the previous one, not only among themselves
    assertEquals(names(List.of(strong, medium, weak)), names(row.getSpectralLibraryMatches()));
  }

  @Test
  void filtersByMinSimilarityAndKeepsListSorted() {
    row.addSpectralLibraryMatches(List.of(match(0.30), match(0.90), match(0.50)));

    SortSpectralMatchesTask.sortIdentities(row, true, 0.4);

    assertEquals(names(List.of(match(0.90), match(0.50))), names(row.getSpectralLibraryMatches()));
  }

  @Test
  void sortedListStaysMutableForFollowingSearches() {
    row.addSpectralLibraryMatches(List.of(match(0.30)));
    SortSpectralMatchesTask.sortIdentities(row);

    // appending to a row modifies the stored list in place, so sorting must not store an
    // immutable list
    row.addSpectralLibraryMatches(List.of(match(0.90)));
    assertEquals(2, row.getSpectralLibraryMatches().size());
  }
}
