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

import com.sun.xml.txw2.output.IndentingXMLStreamWriter;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.numbers.AreaType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.features.types.otherdectectors.ChromatogramTypeType;
import io.github.mzmine.datamodel.features.types.otherdectectors.WavelengthType;
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
import io.github.mzmine.modules.io.projectload.version_3_0.FeatureListLoadTask;
import io.github.mzmine.modules.io.projectsave.FeatureListSaveTask;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.scene.paint.Color;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Round trip of the aligned other detector features and the MS-to-other correlations through the
 * project XML ({@link FeatureListSaveTask#writeOtherDetectorData} and
 * {@link FeatureListLoadTask#parseOtherDetectorData}).
 */
class OtherDetectorDataSaveLoadTest {

  private static final String TRACE_NAME = "DAD1 A: Sig=254.0,4.0";

  private final RawDataFileImpl fileA = createFile("file_a");
  private final RawDataFileImpl fileB = createFile("file_b");
  private final ModularFeatureList flist = new ModularFeatureList("flist", null, fileA, fileB);
  private final MZmineProject project = new MZmineProjectImpl();

  /**
   * Creates a raw data file with one DAD other data file.
   */
  private static @NotNull RawDataFileImpl createFile(@NotNull final String name) {
    final RawDataFileImpl file = new RawDataFileImpl(name, null, null, Color.BLACK);
    final OtherDataFileImpl otherFile = new OtherDataFileImpl(file);
    otherFile.setDescription("DAD");
    otherFile.setOtherTimeSeriesData(new OtherTimeSeriesDataImpl(otherFile));
    file.addOtherDataFiles(List.of(otherFile));
    return file;
  }

  /**
   * Creates a processed feature of the DAD trace at the given RT.
   */
  private static @NotNull OtherFeature createFeature(@NotNull final RawDataFile file,
      final float rt, final float area) {
    final OtherTimeSeriesDataImpl data = (OtherTimeSeriesDataImpl) file.getOtherDataFiles()
        .getFirst().getOtherTimeSeriesData();
    final SimpleOtherTimeSeries series = new SimpleOtherTimeSeries(null,
        new float[]{rt - 0.1f, rt, rt + 0.1f}, new double[]{1d, 5d, 1d}, TRACE_NAME, data);
    final OtherFeatureImpl feature = new OtherFeatureImpl(series);
    feature.set(ChromatogramTypeType.class, ChromatogramType.ABSORPTION);
    feature.set(WavelengthType.class, 254d);
    feature.set(RTType.class, rt);
    feature.set(AreaType.class, area);
    return feature;
  }

  private static @NotNull Map<RawDataFile, PerFileCorrelation> perFile(
      @NotNull final Object... fileAndCorrelation) {
    final Map<RawDataFile, PerFileCorrelation> map = new LinkedHashMap<>();
    for (int i = 0; i < fileAndCorrelation.length; i += 2) {
      map.put((RawDataFile) fileAndCorrelation[i], (PerFileCorrelation) fileAndCorrelation[i + 1]);
    }
    return map;
  }

  @Test
  void testRoundTrip() throws Exception {
    project.addFile(fileA);
    project.addFile(fileB);
    project.addFeatureList(flist);
    final ModularFeatureListRow msRow1 = new ModularFeatureListRow(flist, 1);
    final ModularFeatureListRow msRow2 = new ModularFeatureListRow(flist, 2);
    flist.addRow(msRow1);
    flist.addRow(msRow2);

    final OtherFeatureList ofl = new OtherFeatureList("aligned", null, flist.getRawDataFiles());
    final OtherFeatureListRow row3 = new OtherFeatureListRow(ofl, 3,
        TraceKey.of(createFeature(fileA, 1f, 1f)));
    row3.setFeature(createFeature(fileA, 2f, 100f));
    row3.setFeature(createFeature(fileB, 2.2f, 200f));
    ofl.addRow(row3);
    final OtherFeatureListRow row7 = new OtherFeatureListRow(ofl, 7, row3.getTraceKey());
    row7.setFeature(createFeature(fileA, 5f, 300f));
    ofl.addRow(row7);
    // rows 8 to 10 were created and removed before saving
    ofl.setRowIdCounter(10);

    final PerFileCorrelation calculatedA = new PerFileCorrelation(MsOtherCorrelationType.CALCULATED,
        0.93f);
    final PerFileCorrelation manual = new PerFileCorrelation(MsOtherCorrelationType.MANUAL, null);
    final PerFileCorrelation calculatedB = new PerFileCorrelation(MsOtherCorrelationType.CALCULATED,
        0.85f);
    final MsOtherCorrelationMaps maps = ofl.getMsOtherCorrelationMaps();
    // row 7 is preferred for MS row 1 although it has the higher row ID
    maps.setCorrelations(1, List.of(new OtherCorrelationLink(7, perFile(fileA, calculatedA)),
        new OtherCorrelationLink(3, perFile(fileA, manual, fileB, calculatedB))));
    maps.setCorrelations(2, List.of(new OtherCorrelationLink(3, perFile(fileB, calculatedB))));
    flist.setAlignedOtherFeatures(ofl);

    final OtherFeatureList loaded = roundTrip(ofl);

    Assertions.assertNotSame(ofl, loaded);
    Assertions.assertEquals(10, loaded.getRowIdCounter());
    Assertions.assertEquals(List.of(3, 7),
        loaded.getRows().stream().map(OtherFeatureListRow::getID).toList());
    Assertions.assertTrue(loaded.hasFeatureType(AreaType.class));

    final OtherFeatureListRow loaded3 = loaded.findRowByID(3);
    Assertions.assertEquals(row3.getTraceKey(), loaded3.getTraceKey());
    Assertions.assertEquals(2, loaded3.getNumberOfFeatures());
    Assertions.assertEquals(2.2f, loaded3.getFeature(fileB).getRT(), 1e-6f);
    Assertions.assertEquals(200f, loaded3.getFeature(fileB).get(AreaType.class), 1e-6f);
    Assertions.assertEquals(254d, loaded3.getFeature(fileA).getWavelength());
    Assertions.assertEquals(TRACE_NAME, loaded3.getFeature(fileA).getFeatureData().getName());
    Assertions.assertEquals(2.1f, loaded3.getRT(), 1e-5f);
    Assertions.assertEquals(1, loaded.findRowByID(7).getNumberOfFeatures());

    // the links keep their order (preference), origin and score, including a null score
    Assertions.assertEquals(maps.getAllCorrelations(),
        loaded.getMsOtherCorrelationMaps().getAllCorrelations());
    // the reverse index is rebuilt
    Assertions.assertEquals(Set.of(1, 2),
        loaded.getMsOtherCorrelationMaps().getMsRowsForOtherRow(3));

    // the feature-level column resolves the preferred trace of the MS row in each file
    final CorrelatedOtherFeature preferredA = MsOtherCorrelationResolver.resolvePreferredCorrelation(
        flist, msRow1, fileA);
    Assertions.assertSame(loaded.findRowByID(7), preferredA.otherRow());
    Assertions.assertEquals(MsOtherCorrelationType.CALCULATED, preferredA.type());
    Assertions.assertEquals(0.93f, preferredA.correlation());
    // the preferred row 7 has no peak in file B
    Assertions.assertNull(
        MsOtherCorrelationResolver.resolvePreferredCorrelation(flist, msRow1, fileB));
    // aligned in file A, but MS row 2 only correlates in file B
    Assertions.assertEquals(MsOtherCorrelationType.ALIGNED,
        MsOtherCorrelationResolver.resolvePreferredCorrelation(flist, msRow2, fileA).type());
  }

  /**
   * Writes the other detector data of {@link #flist}, removes it from the feature list and loads it
   * back.
   *
   * @param ofl the aligned list to save
   * @return the loaded aligned list, attached to {@link #flist}
   */
  private @NotNull OtherFeatureList roundTrip(@NotNull final OtherFeatureList ofl)
      throws Exception {
    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    final XMLStreamWriter writer = new IndentingXMLStreamWriter(
        XMLOutputFactory.newInstance().createXMLStreamWriter(os, "UTF-8"));
    writer.writeStartDocument("UTF-8", "1.0");
    FeatureListSaveTask.writeOtherDetectorData(writer, flist, ofl);
    writer.writeEndDocument();
    writer.close();

    flist.setAlignedOtherFeatures(null);
    final XMLStreamReader reader = XMLInputFactory.newInstance()
        .createXMLStreamReader(new ByteArrayInputStream(os.toByteArray()));
    FeatureListLoadTask.parseOtherDetectorData(reader, project, null, flist);
    reader.close();

    final OtherFeatureList loaded = flist.getAlignedOtherFeatures();
    Assertions.assertNotNull(loaded);
    return loaded;
  }
}
