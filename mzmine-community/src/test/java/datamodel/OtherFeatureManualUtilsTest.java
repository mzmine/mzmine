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
 * peaks, replacing a correlated peak and removing features.
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
        msRow1, processed, null);

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
    Assertions.assertTrue(OtherFeatureManualUtils.isCorrelated(ofl, 1, processed));
    Assertions.assertFalse(OtherFeatureManualUtils.isCorrelated(ofl, 2, processed));
  }

  @Test
  void testReplaceCorrelatedPeakKeepsDuplicate() {
    final OtherFeatureList ofl = OtherFeatureManualUtils.getOrCreateAlignedList(flist);
    final OtherFeatureImpl a1 = createFeature(fileA, 13.3f);
    final OtherFeatureImpl b1 = createFeature(fileB, 13.4f);
    final OtherFeatureImpl a2 = createFeature(fileA, 13.5f);
    final OtherFeatureListRow row1 = ofl.createRow(TraceKey.of(a1));
    row1.setFeature(a1);
    row1.setFeature(b1);
    ofl.addRow(row1);
    final OtherFeatureListRow row2 = ofl.createRow(TraceKey.of(a2));
    row2.setFeature(a2);
    ofl.addRow(row2);
    final MsOtherCorrelationMaps maps = ofl.getMsOtherCorrelationMaps();
    maps.setCorrelations(1,
        List.of(new OtherCorrelationLink(row1.getID(), calculated(fileA, fileB))));

    // the user picks the aligned peak at 13.5 instead of the correlated one at 13.3
    final ManualOtherCorrelationResult result = OtherFeatureManualUtils.correlateManually(flist,
        msRow1, a2, row2);

    Assertions.assertSame(row1, result.aligned().row());
    Assertions.assertSame(a1, result.replaced());
    Assertions.assertFalse(result.createdRow());
    Assertions.assertTrue(OtherFeatureManualUtils.isSamePeak(a2, row1.getFeature(fileA)));
    Assertions.assertEquals((13.5f + 13.4f) / 2, row1.getRT(), 1e-5f);
    // the peak stays in its own row as well
    Assertions.assertSame(a2, row2.getFeature(fileA));
    Assertions.assertEquals(2, ofl.getNumberOfRows());
    Assertions.assertEquals(List.of(row1, row2), OtherFeatureManualUtils.findRowsWithPeak(ofl, a2));

    final OtherCorrelationLink link = maps.getCorrelations(1).getFirst();
    Assertions.assertEquals(MsOtherCorrelationType.MANUAL, link.perFile().get(fileA).origin());
    Assertions.assertEquals(MsOtherCorrelationType.CALCULATED, link.perFile().get(fileB).origin());
  }

  @Test
  void testAlignedPeakUsesItsRow() {
    final OtherFeatureList ofl = OtherFeatureManualUtils.getOrCreateAlignedList(flist);
    final OtherFeatureImpl a1 = createFeature(fileA, 2f);
    final ManualOtherCorrelationResult first = OtherFeatureManualUtils.correlateManually(flist,
        msRow1, a1, null);

    // another MS row without a correlation to this trace links to the row that holds the peak
    final ManualOtherCorrelationResult second = OtherFeatureManualUtils.correlateManually(flist,
        msRow2, createFeature(fileA, 2f), null);
    Assertions.assertSame(first.aligned().row(), second.aligned().row());
    Assertions.assertFalse(second.createdRow());
    Assertions.assertNull(second.replaced());
    Assertions.assertEquals(1, ofl.getNumberOfRows());
  }

  @Test
  void testRemoveFeatureDropsCorrelationAndEmptyRow() {
    final OtherFeatureList ofl = OtherFeatureManualUtils.getOrCreateAlignedList(flist);
    final OtherFeatureImpl a1 = createFeature(fileA, 2f);
    final OtherFeatureImpl b1 = createFeature(fileB, 2.1f);
    final OtherFeatureListRow row = OtherFeatureManualUtils.addAsNewRow(ofl, a1).row();
    OtherFeatureManualUtils.setFeature(row, b1);
    final MsOtherCorrelationMaps maps = ofl.getMsOtherCorrelationMaps();
    maps.setCorrelations(1,
        List.of(new OtherCorrelationLink(row.getID(), calculated(fileA, fileB))));

    Assertions.assertNotNull(OtherFeatureManualUtils.removeFeature(row, fileA));
    Assertions.assertEquals(2.1f, row.getRT(), 1e-5f);
    Assertions.assertFalse(maps.getCorrelations(1).getFirst().perFile().containsKey(fileA));
    Assertions.assertNull(OtherFeatureManualUtils.removeFeature(row, fileA));

    // the last feature removes the row and its correlations
    Assertions.assertNotNull(OtherFeatureManualUtils.removeFeature(row, fileB));
    Assertions.assertTrue(ofl.isEmpty());
    Assertions.assertTrue(maps.isEmpty());
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
