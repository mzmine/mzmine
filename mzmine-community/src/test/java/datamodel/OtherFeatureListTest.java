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
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.features.types.otherdectectors.ChromatogramTypeType;
import io.github.mzmine.datamodel.features.types.otherdectectors.RawTraceType;
import io.github.mzmine.datamodel.otherdetectors.OtherFeatureToOtherRowPair;
import io.github.mzmine.datamodel.otherdetectors.CorrelatedOtherFeature;
import io.github.mzmine.datamodel.otherdetectors.MsOtherCorrelationMaps;
import io.github.mzmine.datamodel.otherdetectors.MsOtherCorrelationResolver;
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
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.OtherFeatureListUtils;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the aligned other detector feature list: row ID handling, copies with remapped MS row IDs,
 * pruning of removed MS rows and the per-file removal of correlations.
 */
class OtherFeatureListTest {

  final RawDataFileImpl fileA = new RawDataFileImpl("fileA", null, null);
  final RawDataFileImpl fileB = new RawDataFileImpl("fileB", null, null);
  final ModularFeatureList flist = new ModularFeatureList("flist", null, fileA, fileB);
  final ModularFeatureListRow msRow1 = new ModularFeatureListRow(flist, 1);
  final ModularFeatureListRow msRow2 = new ModularFeatureListRow(flist, 2);

  OtherFeatureList ofl;
  OtherFeatureListRow otherRow1;
  OtherFeatureListRow otherRow2;

  @BeforeEach
  void init() {
    flist.addRow(msRow1);
    flist.addRow(msRow2);

    ofl = new OtherFeatureList("other", null, List.of(fileA, fileB));
    final OtherFeatureImpl a1 = createFeature(fileA, 2f);
    final OtherFeatureImpl b1 = createFeature(fileB, 2.1f);
    final OtherFeatureImpl a2 = createFeature(fileA, 5f);

    final TraceKey key = TraceKey.of(a1);
    otherRow1 = ofl.createRow(key);
    otherRow1.addFeature(a1);
    otherRow1.addFeature(b1);
    otherRow2 = ofl.createRow(key);
    otherRow2.addFeature(a2);
    ofl.applyRowBindings(otherRow1);
    ofl.applyRowBindings(otherRow2);
    ofl.addRow(otherRow1);
    ofl.addRow(otherRow2);
    flist.setAlignedOtherFeatures(ofl);
  }

  /**
   * Creates a processed other feature of the given file at the given RT. Both files use the same
   * trace name and other file description, so their features share one trace key.
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

  @Test
  void testRowIndexAndCounter() {
    Assertions.assertSame(otherRow2, ofl.findRowByID(otherRow2.getID()));
    Assertions.assertNull(ofl.findRowByID(100));

    // rows added with existing IDs (load, copy) raise the counter
    final OtherFeatureList loaded = new OtherFeatureList("loaded", null, List.of(fileA));
    loaded.addRow(new OtherFeatureListRow(7, otherRow1.getTraceKey()));
    Assertions.assertEquals(7, loaded.getRowIdCounter());
    Assertions.assertEquals(8, loaded.createRow(otherRow1.getTraceKey()).getID());

    // a saved counter may be higher than the highest row ID, but never lowers the counter
    loaded.setRowIdCounter(20);
    Assertions.assertEquals(20, loaded.getRowIdCounter());
    loaded.setRowIdCounter(3);
    Assertions.assertEquals(20, loaded.getRowIdCounter());

    Assertions.assertThrows(IllegalArgumentException.class,
        () -> loaded.addRow(new OtherFeatureListRow(7, otherRow1.getTraceKey())));
  }

  @Test
  void testCopyWithRemappedMsRowIds() {
    final MsOtherCorrelationMaps maps = ofl.getMsOtherCorrelationMaps();
    maps.setCorrelations(1,
        List.of(new OtherCorrelationLink(otherRow1.getID(), calculated(fileA))));
    maps.setCorrelations(2,
        List.of(new OtherCorrelationLink(otherRow2.getID(), calculated(fileA))));

    // MS row 1 -> 10, MS row 2 was removed
    final OtherFeatureList copy = OtherFeatureListUtils.createCopy(ofl, null, Map.of(1, 10));

    Assertions.assertEquals(1, copy.getMsOtherCorrelationMaps().getCorrelations(10).size());
    Assertions.assertTrue(copy.getMsOtherCorrelationMaps().getCorrelations(1).isEmpty());
    Assertions.assertTrue(copy.getMsOtherCorrelationMaps().getCorrelations(2).isEmpty());
    Assertions.assertEquals(Set.of(10),
        copy.getMsOtherCorrelationMaps().getMsRowsForOtherRow(otherRow1.getID()));

    // rows and features are copied, not shared. Rows keep their IDs, features their time series
    final OtherFeatureListRow copiedRow = copy.findRowByID(otherRow1.getID());
    Assertions.assertNotNull(copiedRow);
    Assertions.assertNotSame(otherRow1, copiedRow);
    final OtherFeature original = otherRow1.getFeature(fileB);
    final OtherFeature copiedFeature = copiedRow.getFeature(fileB);
    Assertions.assertNotNull(copiedFeature);
    Assertions.assertNotSame(original, copiedFeature);
    Assertions.assertSame(original.getFeatureData(), copiedFeature.getFeatureData());
    Assertions.assertEquals(original.getRT(), copiedFeature.getRT());
    // the test features are raw traces: the copy points to the original as its only raw trace
    Assertions.assertSame(original, copiedFeature.get(RawTraceType.class));
    Assertions.assertEquals(otherRow1.getRT(), copiedRow.getRT());
    Assertions.assertEquals(ofl.getRowIdCounter(), copy.getRowIdCounter());

    // changing the copy does not change the source
    copy.getMsOtherCorrelationMaps().clearAll();
    Assertions.assertEquals(1, maps.getCorrelations(1).size());
  }

  @Test
  void testTransferToNewListWithRenumbering() {
    ofl.getMsOtherCorrelationMaps().setCorrelations(2,
        List.of(new OtherCorrelationLink(otherRow1.getID(), calculated(fileA))));

    final ModularFeatureList target = new ModularFeatureList("target", null, fileA, fileB);
    final ModularFeatureListRow newRow = new ModularFeatureListRow(target, 1);
    target.addRow(newRow);
    // MS row 2 is renumbered to 1 in the new list, MS row 1 is dropped
    final Map<FeatureListRow, ModularFeatureListRow> rowMapping = new IdentityHashMap<>();
    rowMapping.put(msRow2, newRow);

    FeatureListUtils.transferRowRelationsAndIIN(flist, target, rowMapping);

    final OtherFeatureList targetOfl = target.getAlignedOtherFeatures();
    Assertions.assertNotNull(targetOfl);
    Assertions.assertNotSame(ofl, targetOfl);
    Assertions.assertEquals(otherRow1.getID(),
        targetOfl.getMsOtherCorrelationMaps().getCorrelations(1).getFirst().otherRowId());
    Assertions.assertTrue(targetOfl.getMsOtherCorrelationMaps().getCorrelations(2).isEmpty());
    Assertions.assertEquals(ofl.getNumberOfRows(), targetOfl.getNumberOfRows());
  }

  @Test
  void testInPlaceRenumbering() {
    ofl.getMsOtherCorrelationMaps().setCorrelations(2,
        List.of(new OtherCorrelationLink(otherRow1.getID(), calculated(fileA))));

    OtherFeatureListUtils.transferAlignedOtherFeatures(flist, flist, Map.of(1, 2, 2, 1));

    final MsOtherCorrelationMaps maps = flist.getAlignedOtherFeatures().getMsOtherCorrelationMaps();
    Assertions.assertEquals(1, maps.getCorrelations(1).size());
    Assertions.assertTrue(maps.getCorrelations(2).isEmpty());
  }

  @Test
  void testDropCorrelationsOfRemovedRows() {
    final MsOtherCorrelationMaps maps = ofl.getMsOtherCorrelationMaps();
    maps.setCorrelations(1,
        List.of(new OtherCorrelationLink(otherRow1.getID(), calculated(fileA))));
    maps.setCorrelations(2,
        List.of(new OtherCorrelationLink(otherRow1.getID(), calculated(fileA))));

    // rows removed in place, e.g., by the duplicate filter
    flist.setRowsApplySort(msRow1);
    Assertions.assertFalse(maps.getCorrelations(1).isEmpty());
    Assertions.assertTrue(maps.getCorrelations(2).isEmpty());
    Assertions.assertEquals(Set.of(1), maps.getMsRowsForOtherRow(otherRow1.getID()));

    flist.removeRows(List.of(msRow1));
    Assertions.assertTrue(maps.isEmpty());
    Assertions.assertTrue(maps.getMsRowsForOtherRow(otherRow1.getID()).isEmpty());
  }

  @Test
  void testRemoveSingleMsRowDropsOnlyItsCorrelations() {
    final MsOtherCorrelationMaps maps = ofl.getMsOtherCorrelationMaps();
    maps.setCorrelations(1,
        List.of(new OtherCorrelationLink(otherRow1.getID(), calculated(fileA))));
    maps.setCorrelations(2,
        List.of(new OtherCorrelationLink(otherRow1.getID(), calculated(fileA))));

    flist.removeRow(msRow2);
    Assertions.assertTrue(maps.getCorrelations(2).isEmpty());
    Assertions.assertEquals(Set.of(1), maps.getMsRowsForOtherRow(otherRow1.getID()));

    // a row of another list with the same ID is not removed, correlations stay
    final ModularFeatureList other = new ModularFeatureList("other", null, fileA);
    final ModularFeatureListRow foreignRow = new ModularFeatureListRow(other, 1);
    flist.removeRow(foreignRow);
    flist.removeRows(List.of(foreignRow));
    Assertions.assertFalse(maps.getCorrelations(1).isEmpty());

    flist.removeRow(0);
    Assertions.assertTrue(maps.isEmpty());
  }

  @Test
  void testRemoveOtherRowDropsLinks() {
    final MsOtherCorrelationMaps maps = ofl.getMsOtherCorrelationMaps();
    maps.setCorrelations(1, List.of(new OtherCorrelationLink(otherRow1.getID(), calculated(fileA)),
        new OtherCorrelationLink(otherRow2.getID(), calculated(fileA))));
    maps.setCorrelations(2,
        List.of(new OtherCorrelationLink(otherRow1.getID(), calculated(fileA))));
    final int counter = ofl.getRowIdCounter();

    Assertions.assertTrue(ofl.removeRow(otherRow1));
    Assertions.assertFalse(ofl.removeRow(otherRow1));
    Assertions.assertNull(ofl.findRowByID(otherRow1.getID()));
    Assertions.assertEquals(1, ofl.getNumberOfRows());
    // remaining link kept, MS row without remaining links dropped
    Assertions.assertEquals(otherRow2.getID(), maps.getCorrelations(1).getFirst().otherRowId());
    Assertions.assertEquals(1, maps.getCorrelations(1).size());
    Assertions.assertTrue(maps.getCorrelations(2).isEmpty());
    Assertions.assertTrue(maps.getMsRowsForOtherRow(otherRow1.getID()).isEmpty());
    // IDs are never reused
    Assertions.assertEquals(counter, ofl.getRowIdCounter());
  }

  @Test
  void testRemoveCorrelationKeepsPreferredAndMarksAligned() {
    final MsOtherCorrelationMaps maps = ofl.getMsOtherCorrelationMaps();
    maps.setCorrelations(1,
        List.of(new OtherCorrelationLink(otherRow1.getID(), calculated(fileA, fileB)),
            new OtherCorrelationLink(otherRow2.getID(), calculated(fileA))));

    // clear one file of the preferred link: it stays first and the file is only aligned
    maps.removeCorrelation(1, otherRow1.getID(), fileA);
    final List<OtherCorrelationLink> links = maps.getCorrelations(1);
    Assertions.assertEquals(2, links.size());
    Assertions.assertEquals(otherRow1.getID(), links.getFirst().otherRowId());

    final CorrelatedOtherFeature inA = MsOtherCorrelationResolver.resolvePreferredCorrelation(flist,
        msRow1, fileA);
    Assertions.assertNotNull(inA);
    Assertions.assertEquals(MsOtherCorrelationType.ALIGNED, inA.type());
    Assertions.assertSame(otherRow1, inA.otherRow());
    final CorrelatedOtherFeature inB = MsOtherCorrelationResolver.resolvePreferredCorrelation(flist,
        msRow1, fileB);
    Assertions.assertNotNull(inB);
    Assertions.assertEquals(MsOtherCorrelationType.CALCULATED, inB.type());

    // clearing the last correlated file drops the link
    maps.removeCorrelation(1, otherRow1.getID(), fileB);
    Assertions.assertEquals(1, maps.getCorrelations(1).size());
    Assertions.assertEquals(otherRow2.getID(), maps.getCorrelations(1).getFirst().otherRowId());
  }

  @Test
  void testAlignedFeaturesSortedByRt() {
    final List<OtherFeatureToOtherRowPair> aligned = OtherFeatureListUtils.getAlignedFeatures(ofl,
        fileA);
    Assertions.assertEquals(2, aligned.size());
    Assertions.assertSame(otherRow1, aligned.get(0).row());
    Assertions.assertSame(otherRow2, aligned.get(1).row());
    Assertions.assertEquals(1, OtherFeatureListUtils.getAlignedFeatures(ofl, fileB).size());
  }
}
