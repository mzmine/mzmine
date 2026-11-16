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

package datamodel;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.numbers.HeightType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.features.types.otherdectectors.ChromatogramTypeType;
import io.github.mzmine.datamodel.otherdetectors.MsOtherCorrelationMaps;
import io.github.mzmine.datamodel.otherdetectors.MsOtherCorrelationType;
import io.github.mzmine.datamodel.otherdetectors.OtherCorrelationLink;
import io.github.mzmine.datamodel.otherdetectors.OtherDataFileImpl;
import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
import io.github.mzmine.datamodel.otherdetectors.OtherFeatureImpl;
import io.github.mzmine.datamodel.otherdetectors.OtherFeatureList;
import io.github.mzmine.datamodel.otherdetectors.OtherFeatureListRow;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeriesDataImpl;
import io.github.mzmine.datamodel.otherdetectors.PerFileCorrelation;
import io.github.mzmine.datamodel.otherdetectors.SimpleOtherTimeSeries;
import io.github.mzmine.datamodel.otherdetectors.TraceKey;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.ChromatogramType;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.util.ManualOtherCorrelationResult;
import io.github.mzmine.util.OtherFeatureManualUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests manual edits of the aligned other detector features: creating the list, adding unaligned
 * peaks, replacing a correlated peak and moving aligned peaks with their correlations.
 */
class OtherFeatureManualUtilsTest {

  final RawDataFileImpl fileA = new RawDataFileImpl("fileA", null, null);
  final RawDataFileImpl fileB = new RawDataFileImpl("fileB", null, null);
  final ModularFeatureList flist = new ModularFeatureList("flist", null, fileA, fileB);
  final ModularFeatureListRow msRow1 = new ModularFeatureListRow(flist, 1);
  final ModularFeatureListRow msRow2 = new ModularFeatureListRow(flist, 2);

  @BeforeEach
  void init() {
    flist.addRow(msRow1);
    flist.addRow(msRow2);
  }

  /**
   * Creates a processed other feature of the given file at the given RT. All features share one
   * trace key.
   */
  private static @NotNull OtherFeatureImpl createFeature(@NotNull final RawDataFileImpl file,
      final float rt) {
    final OtherDataFileImpl otherFile = new OtherDataFileImpl(file);
    otherFile.setDescription("DAD");
    final OtherTimeSeriesDataImpl data = new OtherTimeSeriesDataImpl(otherFile);
    otherFile.setOtherTimeSeriesData(data);
    final SimpleOtherTimeSeries series = new SimpleOtherTimeSeries(null,
        new float[]{rt - 0.1f, rt, rt + 0.1f}, new double[]{1d, 5d, 1d}, "254 nm", data);
    final OtherFeatureImpl feature = new OtherFeatureImpl(series);
    feature.set(ChromatogramTypeType.class, ChromatogramType.ABSORPTION);
    feature.set(RTType.class, rt);
    return feature;
  }

  private static @NotNull Map<RawDataFile, PerFileCorrelation> calculated(
      @NotNull final RawDataFile... files) {
    final Map<RawDataFile, PerFileCorrelation> perFile = new HashMap<>();
    for (final RawDataFile file : files) {
      perFile.put(file, new PerFileCorrelation(MsOtherCorrelationType.CALCULATED, 0.9f));
    }
    return perFile;
  }

  /**
   * Adds a row with the given features (stored as is) to the list. All features need the same trace
   * key.
   */
  private static @NotNull OtherFeatureListRow addRow(@NotNull final OtherFeatureList ofl,
      @NotNull final OtherFeature... features) {
    final OtherFeatureListRow row = ofl.createRow(TraceKey.of(features[0]));
    for (final OtherFeature feature : features) {
      row.setFeature(feature);
    }
    ofl.addRow(row);
    return row;
  }

  @Test
  void testSamePeak() {
    final OtherFeatureImpl a = createFeature(fileA, 2f);
    Assertions.assertTrue(OtherFeatureManualUtils.isSamePeak(a, a.copy()));
    Assertions.assertTrue(OtherFeatureManualUtils.isSamePeak(a, createFeature(fileA, 2f)));
    Assertions.assertFalse(OtherFeatureManualUtils.isSamePeak(a, createFeature(fileA, 2.5f)));
    Assertions.assertFalse(OtherFeatureManualUtils.isSamePeak(a, createFeature(fileB, 2f)));
  }

  @Test
  void testFullyManualCreatesListAndRow() {
    Assertions.assertNull(flist.getAlignedOtherFeatures());
    final OtherFeatureImpl processed = createFeature(fileA, 2f);

    final ManualOtherCorrelationResult result = OtherFeatureManualUtils.correlateManually(flist,
        msRow1, processed);

    final OtherFeatureList ofl = flist.getAlignedOtherFeatures();
    Assertions.assertNotNull(ofl);
    Assertions.assertTrue(result.createdRow());
    Assertions.assertNull(result.replaced());
    Assertions.assertEquals(1, ofl.getNumberOfRows());
    // the aligned list owns a copy
    final OtherFeature stored = result.aligned().row().getFeature(fileA);
    Assertions.assertNotSame(processed, stored);
    Assertions.assertTrue(OtherFeatureManualUtils.isSamePeak(processed, stored));
    Assertions.assertEquals(2f, result.aligned().row().getRT());
    Assertions.assertTrue(ofl.hasFeatureType(HeightType.class));

    final OtherCorrelationLink link = ofl.getMsOtherCorrelationMaps().getCorrelations(1).getFirst();
    Assertions.assertEquals(result.aligned().row().getID(), link.otherRowId());
    Assertions.assertEquals(MsOtherCorrelationType.MANUAL, link.perFile().get(fileA).origin());
    Assertions.assertSame(result.aligned().row(),
        OtherFeatureManualUtils.findCorrelatedRow(ofl, 1, processed));
    Assertions.assertNull(OtherFeatureManualUtils.findCorrelatedRow(ofl, 2, processed));
  }

  /**
   * The user picks an unaligned peak at 13.5 instead of the correlated one at 13.3: the peak
   * replaces the cell, the replaced peak leaves the list.
   */
  @Test
  void testReplaceCorrelatedPeakWithUnalignedPeak() {
    final OtherFeatureList ofl = OtherFeatureManualUtils.getOrCreateAlignedList(flist);
    final OtherFeatureImpl a1 = createFeature(fileA, 13.3f);
    final OtherFeatureImpl b1 = createFeature(fileB, 13.4f);
    final OtherFeatureImpl a2 = createFeature(fileA, 13.5f);
    final OtherFeatureListRow row1 = addRow(ofl, a1, b1);
    final MsOtherCorrelationMaps maps = ofl.getMsOtherCorrelationMaps();
    maps.setCorrelations(1,
        List.of(new OtherCorrelationLink(row1.getID(), calculated(fileA, fileB))));
    // another MS row (e.g., an adduct) is linked to the same other-row
    maps.setCorrelations(2,
        List.of(new OtherCorrelationLink(row1.getID(), calculated(fileA, fileB))));

    final ManualOtherCorrelationResult result = OtherFeatureManualUtils.correlateManually(flist,
        msRow1, a2);

    Assertions.assertSame(row1, result.aligned().row());
    Assertions.assertSame(a1, result.replaced());
    Assertions.assertFalse(result.createdRow());
    Assertions.assertNull(result.movedFrom());
    Assertions.assertNotSame(a2, row1.getFeature(fileA));
    Assertions.assertTrue(OtherFeatureManualUtils.isSamePeak(a2, row1.getFeature(fileA)));
    Assertions.assertEquals((13.5f + 13.4f) / 2, row1.getRT(), 1e-5f);
    Assertions.assertEquals(1, ofl.getNumberOfRows());

    final OtherCorrelationLink link = maps.getCorrelations(1).getFirst();
    Assertions.assertEquals(MsOtherCorrelationType.MANUAL, link.perFile().get(fileA).origin());
    Assertions.assertEquals(MsOtherCorrelationType.CALCULATED, link.perFile().get(fileB).origin());
    Assertions.assertSame(row1, OtherFeatureManualUtils.findCorrelatedRow(ofl, 1, a2));
    Assertions.assertNull(OtherFeatureManualUtils.findCorrelatedRow(ofl, 1, a1));

    // the shared row now points to 13.5 for MS row 2 as well: manual without a score in file A
    final PerFileCorrelation otherMsRowInA = maps.getCorrelations(2).getFirst().perFile()
        .get(fileA);
    Assertions.assertEquals(MsOtherCorrelationType.MANUAL, otherMsRowInA.origin());
    Assertions.assertNull(otherMsRowInA.pearson());
    Assertions.assertEquals(MsOtherCorrelationType.CALCULATED,
        maps.getCorrelations(2).getFirst().perFile().get(fileB).origin());
  }

  /**
   * The picked peak is aligned in a row without correlations: it moves to the correlated row, no
   * duplicate stays. The source row keeps its other peaks, as it was never correlated.
   */
  @Test
  void testAlignedOnlyPeakMovesWithoutDuplicate() {
    final OtherFeatureList ofl = OtherFeatureManualUtils.getOrCreateAlignedList(flist);
    final OtherFeatureImpl a13 = createFeature(fileA, 13.3f);
    final OtherFeatureImpl a15 = createFeature(fileA, 13.5f);
    final OtherFeatureImpl b15 = createFeature(fileB, 13.5f);
    final OtherFeatureListRow correlated = addRow(ofl, a13);
    final OtherFeatureListRow alignedOnly = addRow(ofl, a15, b15);
    final MsOtherCorrelationMaps maps = ofl.getMsOtherCorrelationMaps();
    maps.setCorrelations(1,
        List.of(new OtherCorrelationLink(correlated.getID(), calculated(fileA))));

    final ManualOtherCorrelationResult result = OtherFeatureManualUtils.correlateManually(flist,
        msRow1, createFeature(fileA, 13.5f));

    Assertions.assertSame(correlated, result.aligned().row());
    Assertions.assertSame(alignedOnly, result.movedFrom());
    Assertions.assertSame(a13, result.replaced());
    // the stored copy moved, no new copy
    Assertions.assertSame(a15, correlated.getFeature(fileA));
    Assertions.assertNull(alignedOnly.getFeature(fileA));
    Assertions.assertSame(alignedOnly, ofl.findRowByID(alignedOnly.getID()));
    Assertions.assertSame(b15, alignedOnly.getFeature(fileB));
    Assertions.assertEquals(MsOtherCorrelationType.MANUAL,
        maps.getPerFileCorrelation(1, correlated.getID(), fileA).origin());

    // the last peak of a row moves: the empty row is removed
    final ManualOtherCorrelationResult second = OtherFeatureManualUtils.correlateManually(flist,
        msRow1, createFeature(fileB, 13.5f));
    Assertions.assertSame(alignedOnly, second.movedFrom());
    Assertions.assertNull(second.replaced());
    Assertions.assertNull(ofl.findRowByID(alignedOnly.getID()));
    Assertions.assertEquals(1, ofl.getNumberOfRows());
  }

  @Test
  void testAlignedPeakUsesItsRow() {
    final OtherFeatureList ofl = OtherFeatureManualUtils.getOrCreateAlignedList(flist);
    final OtherFeatureImpl a1 = createFeature(fileA, 2f);
    final ManualOtherCorrelationResult first = OtherFeatureManualUtils.correlateManually(flist,
        msRow1, a1);

    // another MS row without a correlation to this trace links to the row that holds the peak
    final ManualOtherCorrelationResult second = OtherFeatureManualUtils.correlateManually(flist,
        msRow2, createFeature(fileA, 2f));
    Assertions.assertSame(first.aligned().row(), second.aligned().row());
    Assertions.assertFalse(second.createdRow());
    Assertions.assertNull(second.replaced());
    Assertions.assertEquals(1, ofl.getNumberOfRows());
  }

  @Test
  void testAlreadyCorrelatedKeepsCalculated() {
    final OtherFeatureList ofl = OtherFeatureManualUtils.getOrCreateAlignedList(flist);
    final OtherFeatureImpl a1 = createFeature(fileA, 2f);
    final OtherFeatureListRow row = addRow(ofl, a1);
    ofl.getMsOtherCorrelationMaps()
        .setCorrelations(1, List.of(new OtherCorrelationLink(row.getID(), calculated(fileA))));

    final ManualOtherCorrelationResult result = OtherFeatureManualUtils.correlateManually(flist,
        msRow1, createFeature(fileA, 2f));
    Assertions.assertFalse(result.changed());
    Assertions.assertEquals(MsOtherCorrelationType.CALCULATED,
        ofl.getMsOtherCorrelationMaps().getPerFileCorrelation(1, row.getID(), fileA).origin());
  }

  /**
   * Misaligned in file B: the correct peak (1.3) is in row N, row M holds 1.4. Picking 1.3 for the
   * MS row moves it to M with its calculated correlation. 1.4 fits row N, so the peaks swap and the
   * correlation of the other MS row follows the 1.4 peak.
   */
  @Test
  void testMisalignedPeakMovesWithCorrelations() {
    final OtherFeatureList ofl = OtherFeatureManualUtils.getOrCreateAlignedList(flist);
    final OtherFeatureImpl a13 = createFeature(fileA, 1.3f);
    final OtherFeatureImpl b14 = createFeature(fileB, 1.4f);
    final OtherFeatureImpl b13 = createFeature(fileB, 1.3f);
    final OtherFeatureImpl a14 = createFeature(fileA, 1.4f);
    final OtherFeatureListRow rowM = addRow(ofl, a13, b14);
    final OtherFeatureListRow rowN = addRow(ofl, b13, a14);

    final MsOtherCorrelationMaps maps = ofl.getMsOtherCorrelationMaps();
    final PerFileCorrelation scoreFor13 = new PerFileCorrelation(MsOtherCorrelationType.CALCULATED,
        0.95f);
    maps.setCorrelations(1,
        List.of(new OtherCorrelationLink(rowM.getID(), calculated(fileA, fileB)),
            new OtherCorrelationLink(rowN.getID(), Map.of(fileB, scoreFor13))));
    // another MS row correlated to the misaligned 1.4 peak in row M
    maps.setCorrelations(2, List.of(new OtherCorrelationLink(rowM.getID(), calculated(fileB))));

    final ManualOtherCorrelationResult result = OtherFeatureManualUtils.correlateManually(flist,
        msRow1, createFeature(fileB, 1.3f));

    Assertions.assertTrue(result.changed());
    Assertions.assertSame(rowM, result.aligned().row());
    Assertions.assertSame(rowN, result.movedFrom());
    Assertions.assertTrue(OtherFeatureManualUtils.isSamePeak(b13, rowM.getFeature(fileB)));
    Assertions.assertTrue(OtherFeatureManualUtils.isSamePeak(b14, result.replaced()));
    // the moved correlation keeps its score, the MS row's correlation to 1.4 is gone
    Assertions.assertEquals(scoreFor13, maps.getPerFileCorrelation(1, rowM.getID(), fileB));
    Assertions.assertEquals(1, maps.getCorrelations(1).size());
    // the peaks swapped: N holds both 1.4 peaks
    Assertions.assertSame(rowN, ofl.findRowByID(rowN.getID()));
    Assertions.assertTrue(OtherFeatureManualUtils.isSamePeak(b14, rowN.getFeature(fileB)));
    Assertions.assertSame(a14, rowN.getFeature(fileA));
    Assertions.assertEquals(1.4f, rowN.getRT(), 1e-5f);
    // the other MS row follows the 1.4 peak with its score
    Assertions.assertNull(maps.getPerFileCorrelation(2, rowM.getID(), fileB));
    Assertions.assertEquals(MsOtherCorrelationType.CALCULATED,
        maps.getPerFileCorrelation(2, rowN.getID(), fileB).origin());
  }

  /**
   * A moved correlation keeps the preference position of the MS row, even if the link to the source
   * row is removed.
   */
  @Test
  void testMovedCorrelationKeepsPreference() {
    final OtherFeatureList ofl = OtherFeatureManualUtils.getOrCreateAlignedList(flist);
    final OtherFeatureListRow target = addRow(ofl, createFeature(fileA, 13.3f));
    final OtherFeatureListRow source = addRow(ofl, createFeature(fileA, 13.5f));
    final OtherFeatureImpl otherTrace = createFeature(fileA, 13.5f);
    otherTrace.set(ChromatogramTypeType.class, ChromatogramType.UNKNOWN);
    final OtherFeatureListRow otherTraceRow = addRow(ofl, otherTrace);
    final MsOtherCorrelationMaps maps = ofl.getMsOtherCorrelationMaps();
    maps.setCorrelations(1, List.of(new OtherCorrelationLink(target.getID(), calculated(fileA))));
    // MS row 2 prefers the trace of the moved peak over another trace
    maps.setCorrelations(2, List.of(new OtherCorrelationLink(source.getID(), calculated(fileA)),
        new OtherCorrelationLink(otherTraceRow.getID(), calculated(fileA))));

    final ManualOtherCorrelationResult result = OtherFeatureManualUtils.correlateManually(flist,
        msRow1, createFeature(fileA, 13.5f));

    Assertions.assertSame(source, result.movedFrom());
    // the source row is empty, the replaced 13.3 peak did not fit and left the list
    Assertions.assertNull(ofl.findRowByID(source.getID()));
    Assertions.assertEquals(2, ofl.getNumberOfRows());
    final List<OtherCorrelationLink> links = maps.getCorrelations(2);
    Assertions.assertEquals(List.of(target.getID(), otherTraceRow.getID()),
        links.stream().map(OtherCorrelationLink::otherRowId).toList());
    Assertions.assertEquals(MsOtherCorrelationType.CALCULATED,
        links.getFirst().perFile().get(fileA).origin());
  }

  /**
   * The replaced peak fits the source row: both rows keep their peaks, the source row stays even
   * though the MS row no longer correlates to it.
   */
  @Test
  void testSwapKeepsSourceRow() {
    final OtherFeatureList ofl = OtherFeatureManualUtils.getOrCreateAlignedList(flist);
    final OtherFeatureImpl b14 = createFeature(fileB, 1.4f);
    final OtherFeatureListRow rowM = addRow(ofl, b14);
    final OtherFeatureListRow rowN = addRow(ofl, createFeature(fileB, 1.3f),
        createFeature(fileA, 1.4f));
    final MsOtherCorrelationMaps maps = ofl.getMsOtherCorrelationMaps();
    maps.setCorrelations(1, List.of(new OtherCorrelationLink(rowM.getID(), calculated(fileB)),
        new OtherCorrelationLink(rowN.getID(), calculated(fileB))));
    maps.setCorrelations(2, List.of(new OtherCorrelationLink(rowN.getID(), calculated(fileA))));

    final ManualOtherCorrelationResult result = OtherFeatureManualUtils.correlateManually(flist,
        msRow1, createFeature(fileB, 1.3f));

    Assertions.assertSame(rowN, result.movedFrom());
    Assertions.assertSame(rowN, ofl.findRowByID(rowN.getID()));
    Assertions.assertSame(b14, rowN.getFeature(fileB));
    Assertions.assertEquals(1.4f, rowN.getRT(), 1e-5f);
    Assertions.assertEquals(List.of(rowM.getID()),
        maps.getCorrelations(1).stream().map(OtherCorrelationLink::otherRowId).toList());
    Assertions.assertEquals(MsOtherCorrelationType.CALCULATED,
        maps.getPerFileCorrelation(2, rowN.getID(), fileA).origin());
  }

  @Test
  void testRowRejectsOtherTraceAndForeignList() {
    final OtherFeatureList ofl = OtherFeatureManualUtils.getOrCreateAlignedList(flist);
    final OtherFeatureImpl a1 = createFeature(fileA, 2f);
    final OtherFeatureListRow row = ofl.createRow(TraceKey.of(a1));
    final OtherFeatureImpl otherTrace = createFeature(fileA, 2f);
    otherTrace.set(ChromatogramTypeType.class, ChromatogramType.UNKNOWN);
    Assertions.assertThrows(IllegalArgumentException.class, () -> row.setFeature(otherTrace));

    final OtherFeatureList foreign = new OtherFeatureList("foreign", null, List.of(fileA));
    Assertions.assertThrows(IllegalArgumentException.class, () -> foreign.addRow(row));
  }
}
