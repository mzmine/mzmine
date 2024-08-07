/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MergedMsMsSpectrum;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.MsMsInfoType;
import io.github.mzmine.datamodel.features.types.annotations.SpectralLibraryMatchesType;
import io.github.mzmine.datamodel.features.types.numbers.BestScanNumberType;
import io.github.mzmine.datamodel.features.types.numbers.FragmentScanNumbersType;
import io.github.mzmine.datamodel.impl.BuildingMobilityScan;
import io.github.mzmine.datamodel.impl.PasefMsMsInfoImpl;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.datamodel.msms.PasefMsMsInfo;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.scans.SpectraMerging;
import io.github.mzmine.util.scans.SpectraMerging.IntensityMergingType;
import io.github.mzmine.util.scans.similarity.HandleUnmatchedSignalOptions;
import io.github.mzmine.util.scans.similarity.SpectralSimilarity;
import io.github.mzmine.util.scans.similarity.Weights;
import io.github.mzmine.util.scans.similarity.impl.composite.CompositeCosineSpectralSimilarity;
import io.github.mzmine.util.scans.similarity.impl.composite.CompositeCosineSpectralSimilarityParameters;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import testutils.MZmineTestUtil;

@TestInstance(Lifecycle.PER_CLASS)
public class IMSScanTypesTest {

  IMSRawDataFile file;
  ModularFeatureList flist;
  ModularFeatureListRow row;
  ModularFeature feature;

  MZmineProject project;

  private static void compareMergedMsMs(MergedMsMsSpectrum value, MergedMsMsSpectrum loaded) {
    Assertions.assertEquals(value.getCollisionEnergy(), loaded.getCollisionEnergy());
    Assertions.assertEquals(value.getBasePeakIndex(), loaded.getBasePeakIndex());
    Assertions.assertEquals(value.getBasePeakMz(), loaded.getBasePeakMz());
    Assertions.assertEquals(value.getCenterFunction(), loaded.getCenterFunction());
    Assertions.assertEquals(value.getBasePeakIntensity(), loaded.getBasePeakIntensity());
    Assertions.assertEquals(value.getDataFile(), loaded.getDataFile());
    Assertions.assertEquals(value.getDataPointMZRange(), loaded.getDataPointMZRange());
    Assertions.assertEquals(value.getScanningMZRange(), loaded.getScanningMZRange());
    Assertions.assertEquals(value.getPolarity(), loaded.getPolarity());
    Assertions.assertEquals(value.getNumberOfDataPoints(), loaded.getNumberOfDataPoints());
    Assertions.assertEquals(value.getScanNumber(), loaded.getScanNumber());
    Assertions.assertEquals(value.getPrecursorCharge(), loaded.getPrecursorCharge());
    Assertions.assertEquals(value.getMsMsInfo(), loaded.getMsMsInfo());
    Assertions.assertEquals(value.getRetentionTime(), loaded.getRetentionTime());
    Assertions.assertEquals(value.getScanDefinition(), loaded.getScanDefinition());
    Assertions.assertEquals(value.getSourceSpectra(), loaded.getSourceSpectra());
    Assertions.assertEquals(value.getTIC(), loaded.getTIC());
    Assertions.assertEquals(value.getMSLevel(), loaded.getMSLevel());
    Assertions.assertEquals(value.getIntensityMergingType(), loaded.getIntensityMergingType());

    for (int i = 0; i < value.getNumberOfDataPoints(); i++) {
      Assertions.assertEquals(value.getIntensityValue(i), loaded.getIntensityValue(i));
      Assertions.assertEquals(value.getMzValue(i), loaded.getMzValue(i));
    }
  }

  @BeforeAll
  public void initialize() {
    MZmineTestUtil.startMzmineCore();
    file = new IMSRawDataFileImpl("testfile", null, null, Color.BLACK);
    Assertions.assertNotNull(file);

    flist = new ModularFeatureList("flist", null, file);
    row = new ModularFeatureListRow(flist, 1);
    feature = new ModularFeature(flist, file, null, null);
    row.addFeature(file, feature);
    flist.addRow(row);

    project = new MZmineProjectImpl();
    project.addFile(file);
    project.addFeatureList(flist);

    // generate ms1 frames
    for (int i = 0; i < 5; i++) {
      List<BuildingMobilityScan> scans = new ArrayList<>();
      for (int j = 0; j < 5; j++) {
        scans.add(new BuildingMobilityScan(j, new double[0], new double[0]));
      }
      SimpleFrame frame = new SimpleFrame(file, i, 1, 0.1f * i, new double[0], new double[0],
          MassSpectrumType.CENTROIDED, PolarityType.POSITIVE, "", Range.closed(0d, 1d),
          MobilityType.TIMS, null, null);

      frame.setMobilities(new double[]{5d, 4d, 3d, 2d, 1d});
      frame.setMobilityScans(scans, true);
      try {
        file.addScan(frame);
      } catch (IOException e) {
        Assertions.fail();
      }
    }

    // generate ms2 frames
    for (int i = 5; i < 10; i++) {
      List<BuildingMobilityScan> scans = new ArrayList<>();
      for (int j = 0; j < 5; j++) {
        scans.add(new BuildingMobilityScan(j, new double[]{500, 600, 700, 800},
            new double[]{500, 600, 700, 800}));
      }
      SimpleFrame frame = new SimpleFrame(file, i, 2, 0.1f * i, new double[0], new double[0],
          MassSpectrumType.CENTROIDED, PolarityType.POSITIVE, "", Range.closed(0d, 1d),
          MobilityType.TIMS, null, null);
      frame.setMobilities(new double[]{5d, 4d, 3d, 2d, 1d});
      frame.setMobilityScans(scans, true);

      try {
        file.addScan(frame);
      } catch (IOException e) {
        Assertions.fail();
      }
    }

    flist.setSelectedScans(file, file.getFrames().subList(0, 4));
  }

  @Test
  void bestScanNumberTypeTest() {
    BestScanNumberType type = new BestScanNumberType();
    Frame value = file.getFrame(3);
    DataTypeTestUtils.testSaveLoad(type, value, project, flist, row, null, null);
    DataTypeTestUtils.testSaveLoad(type, value, project, flist, row, feature, file);

    DataTypeTestUtils.testSaveLoad(type, null, project, flist, row, null, null);
    DataTypeTestUtils.testSaveLoad(type, null, project, flist, row, feature, file);
  }


  @Test
  void fragmentScanNumbersTypeTest() {
    FragmentScanNumbersType type = new FragmentScanNumbersType();

    List<MergedMsMsSpectrum> value = new ArrayList<>();
    for (int i = 5; i < 10; i++) {
      PasefMsMsInfo info = new PasefMsMsInfoImpl(300d, Range.closed(1, 3), 30f, 1,
          file.getFrame(i - 5), file.getFrame(i), Range.closed(299d, 301d));

      MergedMsMsSpectrum scan = SpectraMerging.getMergedMsMsSpectrumForPASEF(info,
          new MZTolerance(0.01, 10), IntensityMergingType.SUMMED, null,
          RangeUtils.toFloatRange(file.getFrame(i).getMobilityRange()), null, null, null);
      value.add(scan);
    }

    List<MergedMsMsSpectrum> loaded = (List<MergedMsMsSpectrum>) DataTypeTestUtils.saveAndLoad(type,
        value, project, flist, row, null, null);
    for (int i = 0; i < value.size(); i++) {
      compareMergedMsMs(value.get(i), loaded.get(i));
    }
    loaded = (List<MergedMsMsSpectrum>) DataTypeTestUtils.saveAndLoad(type, value, project, flist,
        row, feature, file);
    for (int i = 0; i < value.size(); i++) {
      compareMergedMsMs(value.get(i), loaded.get(i));
    }

    DataTypeTestUtils.testSaveLoad(type, null, project, flist, row, null, null);
    DataTypeTestUtils.testSaveLoad(type, null, project, flist, row, feature, file);
  }

  @Test
  void testImsMsMsInfoType() {
    MsMsInfoType type = new MsMsInfoType();
    List<PasefMsMsInfo> list = new ArrayList<>();
    for (int i = 5; i < 10; i++) {
      PasefMsMsInfo info = new PasefMsMsInfoImpl(300d, Range.closed(1, 3), 30f, 1,
          file.getFrame(i - 5), file.getFrame(i), Range.closed(299d, 301d));
      list.add(info);
    }

    DataTypeTestUtils.testSaveLoad(type, list, project, flist, row, feature, file);
    DataTypeTestUtils.testSaveLoad(type, null, project, flist, row, feature, file);

    DataTypeTestUtils.testSaveLoad(type, list, project, flist, row, null, null);
    DataTypeTestUtils.testSaveLoad(type, null, project, flist, row, null, null);

    final IMSRawDataFile file2 = new IMSRawDataFileImpl("file2", null, null, Color.BLACK);
    final MZmineProject newProject = new MZmineProjectImpl();
    newProject.addFile(file);
    newProject.addFile(file2);

    DataTypeTestUtils.testSaveLoad(type, list, newProject, flist, row, feature, file2);
    DataTypeTestUtils.testSaveLoad(type, null, newProject, flist, row, feature, file2);

    DataTypeTestUtils.testSaveLoad(type, list, newProject, flist, row, null, null);
    DataTypeTestUtils.testSaveLoad(type, null, newProject, flist, row, null, null);
  }

  @Test
  void spectralLibMatchSummaryTypeTest() {
    var type = new SpectralLibraryMatchesType();

    var param = new CompositeCosineSpectralSimilarityParameters().cloneParameterSet();
    param.setParameter(CompositeCosineSpectralSimilarityParameters.minCosine, 0.7d);
    param.setParameter(CompositeCosineSpectralSimilarityParameters.handleUnmatched,
        HandleUnmatchedSignalOptions.REMOVE_ALL);
    param.setParameter(CompositeCosineSpectralSimilarityParameters.weight, Weights.MASSBANK);
    CompositeCosineSpectralSimilarity simFunc = new CompositeCosineSpectralSimilarity();

    PasefMsMsInfo info = new PasefMsMsInfoImpl(300d, Range.closed(1, 3), 30f, 1, file.getFrame(2),
        file.getFrame(6), null);
    MergedMsMsSpectrum query = SpectraMerging.getMergedMsMsSpectrumForPASEF(info,
        new MZTolerance(0.01, 10), IntensityMergingType.SUMMED, null,
        RangeUtils.toFloatRange(file.getFrame(5).getMobilityRange()), null, null, null);

    PasefMsMsInfo info2 = new PasefMsMsInfoImpl(300d, Range.closed(1, 3), 30f, 1, file.getFrame(3),
        file.getFrame(7), null);
    MergedMsMsSpectrum library = SpectraMerging.getMergedMsMsSpectrumForPASEF(info2,
        new MZTolerance(0.01, 10), IntensityMergingType.SUMMED, null,
        RangeUtils.toFloatRange(file.getFrame(5).getMobilityRange()), null, null, null);

    Map<DBEntryField, Object> map = Map.of(DBEntryField.ENTRY_ID, "123swd", DBEntryField.CAS,
        "468-531-21", DBEntryField.DATA_COLLECTOR, "Dr. Xy", DBEntryField.CHARGE, 1);

    SpectralLibraryEntry entry = SpectralLibraryEntry.create(null, map,
        ScanUtils.extractDataPoints(library));

    SpectralSimilarity similarity = simFunc.getSimilarity(new MZTolerance(0.005, 15), 0,
        ScanUtils.extractDataPoints(library), ScanUtils.extractDataPoints(query));

    List<SpectralDBAnnotation> value = List.of(
        new SpectralDBAnnotation(entry, similarity, query, null, 215.1234, 2.5f),
        new SpectralDBAnnotation(entry, similarity, query, 0.034f, null, null));

    DataTypeTestUtils.testSaveLoad(type, value, project, flist, row, null, null);
    DataTypeTestUtils.testSaveLoad(type, Collections.emptyList(), project, flist, row, null, null);
    DataTypeTestUtils.testSaveLoad(type, value, project, flist, row, feature, file);
    DataTypeTestUtils.testSaveLoad(type, Collections.emptyList(), project, flist, row, feature,
        file);
  }


}
