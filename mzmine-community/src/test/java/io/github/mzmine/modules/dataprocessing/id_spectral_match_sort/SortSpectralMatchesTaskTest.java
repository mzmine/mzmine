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
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.util.scans.similarity.SpectralSimilarity;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import io.github.mzmine.util.spectraldb.entry.SpectralDBEntry;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.util.List;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that {@link SortSpectralMatchesTask#sortIdentities} ranks the complete list of a row by the
 * annotation quality score (AQS) of the feature list's annotation sorter, not by the raw spectral
 * similarity score.
 */
class SortSpectralMatchesTaskTest {

  private static final double ROW_MZ = 300.1234;
  private static final float ROW_RT = 5f;

  private ModularFeatureList flist;
  private ModularFeatureListRow row;

  private static SpectralDBAnnotation match(final double cosine, @Nullable final Float libraryRt) {
    final SpectralLibraryEntry entry = new SpectralDBEntry(null, new double[]{100d, 200d},
        new double[]{1d, 1d});
    entry.putIfNotNull(DBEntryField.NAME, "cosine " + cosine + " rt " + libraryRt);
    entry.putIfNotNull(DBEntryField.PRECURSOR_MZ, ROW_MZ);
    entry.putIfNotNull(DBEntryField.RT, libraryRt);

    return new SpectralDBAnnotation(entry, new SpectralSimilarity("test", cosine, 2, 1d), null,
        null, ROW_MZ, ROW_RT, null);
  }

  private static List<String> names(final List<SpectralDBAnnotation> matches) {
    return matches.stream().map(SpectralDBAnnotation::getCompoundName).toList();
  }

  @BeforeEach
  void setUp() {
    flist = new ModularFeatureList("flist", null,
        new RawDataFileImpl("file", null, null, Color.BLACK));
    // RT needs to be a row type, otherwise the RT score is inactive for the whole feature list
    flist.addRowType(DataTypes.get(MZType.class), DataTypes.get(RTType.class));

    row = new ModularFeatureListRow(flist, 1);
    row.set(MZType.class, ROW_MZ);
    row.set(RTType.class, ROW_RT);
    flist.addRow(row);
  }

  @Test
  void sortsCompleteListAfterNewMatchesWereAppended() {
    final SpectralDBAnnotation weak = match(0.30, null);
    final SpectralDBAnnotation strongNoRt = match(0.90, null);
    final SpectralDBAnnotation rtMatch = match(0.50, ROW_RT);

    // a previous search already annotated the row
    row.addSpectralLibraryMatches(List.of(weak));
    // a second search appends more matches at the end of the list
    row.addSpectralLibraryMatches(List.of(strongNoRt, rtMatch));

    SortSpectralMatchesTask.sortIdentities(row);

    // the RT match wins although its cosine is the lowest of the two new matches, and the old
    // weak match is pushed to the end - so the whole list was ranked, not only the new matches
    assertEquals(names(List.of(rtMatch, strongNoRt, weak)), names(row.getSpectralLibraryMatches()));
  }

  @Test
  void filtersByMinSimilarityAndKeepsListSorted() {
    final SpectralDBAnnotation weak = match(0.30, null);
    final SpectralDBAnnotation strongNoRt = match(0.90, null);
    final SpectralDBAnnotation rtMatch = match(0.50, ROW_RT);
    row.addSpectralLibraryMatches(List.of(weak, strongNoRt, rtMatch));

    SortSpectralMatchesTask.sortIdentities(row, true, 0.4);

    assertEquals(names(List.of(rtMatch, strongNoRt)), names(row.getSpectralLibraryMatches()));
  }

  @Test
  void sortedListStaysMutableForFollowingSearches() {
    row.addSpectralLibraryMatches(List.of(match(0.30, null)));
    SortSpectralMatchesTask.sortIdentities(row);

    // appending to a row modifies the stored list in place, so sorting must not store an
    // immutable list
    row.addSpectralLibraryMatches(List.of(match(0.90, null)));
    assertEquals(2, row.getSpectralLibraryMatches().size());
  }
}
