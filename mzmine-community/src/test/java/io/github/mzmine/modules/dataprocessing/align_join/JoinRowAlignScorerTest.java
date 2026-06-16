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

package io.github.mzmine.modules.dataprocessing.align_join;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import org.junit.jupiter.api.Test;

class JoinRowAlignScorerTest {

  private static final ScanSelection POS = new ScanSelection(1, PolarityType.POSITIVE);
  private static final ScanSelection NEG = new ScanSelection(1, PolarityType.NEGATIVE);

  private static ModularFeatureListRow row(final ModularFeatureList flist, final int id,
      final ScanSelection selection) {
    final ModularFeatureListRow row = new ModularFeatureListRow(flist, id);
    if (selection != null) {
      row.setScanSelection(selection);
    }
    return row;
  }

  @Test
  void onlySameScanSelectionAligns() {
    final RawDataFile file = RawDataFile.createDummyFile();
    final ModularFeatureList flist = new ModularFeatureList("test", null, file);

    final ModularFeatureListRow pos1 = row(flist, 1, POS);
    final ModularFeatureListRow pos2 = row(flist, 2, POS);
    final ModularFeatureListRow neg = row(flist, 3, NEG);
    final ModularFeatureListRow untagged = row(flist, 4, null);

    // same selection -> align
    assertTrue(JoinRowAlignScorer.compatibleScanSelection(pos1, pos2));
    // different explicit selection (e.g. positive vs negative) -> never align
    assertFalse(JoinRowAlignScorer.compatibleScanSelection(pos1, neg));
    assertFalse(JoinRowAlignScorer.compatibleScanSelection(neg, pos1));
    // null = unknown (legacy/untagged) -> permissive, do not block
    assertTrue(JoinRowAlignScorer.compatibleScanSelection(pos1, untagged));
    assertTrue(JoinRowAlignScorer.compatibleScanSelection(untagged, neg));
    assertTrue(JoinRowAlignScorer.compatibleScanSelection(untagged, row(flist, 5, null)));
  }
}
