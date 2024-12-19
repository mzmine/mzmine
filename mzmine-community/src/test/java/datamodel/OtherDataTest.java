package datamodel;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonTimeSeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.otherdectectors.ChromatogramTypeType;
import io.github.mzmine.datamodel.features.types.otherdectectors.MrmTransitionListType;
import io.github.mzmine.datamodel.features.types.otherdectectors.MsOtherCorrelationResultType;
import io.github.mzmine.datamodel.features.types.otherdectectors.OtherFeatureDataType;
import io.github.mzmine.datamodel.features.types.otherdectectors.OtherFileType;
import io.github.mzmine.datamodel.features.types.otherdectectors.PolarityTypeType;
import io.github.mzmine.datamodel.features.types.otherdectectors.WavelengthType;
import io.github.mzmine.datamodel.otherdetectors.MrmTransition;
import io.github.mzmine.datamodel.otherdetectors.MrmTransitionList;
import io.github.mzmine.datamodel.otherdetectors.MsOtherCorrelationResult;
import io.github.mzmine.datamodel.otherdetectors.MsOtherCorrelationType;
import io.github.mzmine.datamodel.otherdetectors.OtherDataFileImpl;
import io.github.mzmine.datamodel.otherdetectors.OtherFeatureImpl;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeriesDataImpl;
import io.github.mzmine.datamodel.otherdetectors.SimpleOtherTimeSeries;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.ChromatogramType;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OtherDataTest {

  final MZmineProject proj = new MZmineProjectImpl();
  final RawDataFileImpl file = new RawDataFileImpl("testfile", null, null);
  final ModularFeatureList flist = new ModularFeatureList("flist", null, file);
  final ModularFeature feature = new ModularFeature(flist, file, FeatureStatus.DETECTED);
  final ModularFeatureListRow row = new ModularFeatureListRow(flist, 1, feature);

  final OtherDataFileImpl otherFileA = new OtherDataFileImpl(file);
  final OtherTimeSeriesDataImpl dataA = new OtherTimeSeriesDataImpl(otherFileA);
  final SimpleOtherTimeSeries seriesA = new SimpleOtherTimeSeries(null,
      new float[]{0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f},
      new double[]{0d, .1d, .2d, .3d, .4d, .5d, .4d, .3d, .2d, .1d}, "timeSeriesA", dataA);
  final OtherFeatureImpl featureA = new OtherFeatureImpl(seriesA);

  final OtherDataFileImpl otherFileB = new OtherDataFileImpl(file);
  final OtherTimeSeriesDataImpl dataB = new OtherTimeSeriesDataImpl(otherFileB);
  final SimpleOtherTimeSeries seriesB = new SimpleOtherTimeSeries(null,
      new float[]{0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f},
      new double[]{0d, .1d, .2d, .3d, .4d, .5d, .4d, .3d, .2d, .1d}, "timeSeriesB", dataB);
  final OtherFeatureImpl featureB = new OtherFeatureImpl(seriesB);

  @BeforeEach
  void init() {
    proj.addFile(file);
    proj.addFeatureList(flist);
    file.addOtherDataFiles(List.of(otherFileA, otherFileB));
    flist.setSelectedScans(file, List.of());
    flist.addRow(row);

    otherFileA.setOtherTimeSeriesData(dataA);
    otherFileA.setDescription("file a");
    otherFileB.setOtherTimeSeriesData(dataB);
    otherFileB.setDescription("file b");

    dataA.addRawTrace(featureA);
    dataB.addRawTrace(featureB);

    featureA.set(ChromatogramTypeType.class, ChromatogramType.ABSORPTION);
    featureB.set(ChromatogramTypeType.class, ChromatogramType.EMISSION);
  }

  @Test
  void testWavelengthType() {
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(new WavelengthType(), 420d);
  }

  @Test
  void testOtherFeatureDataType() {
    DataTypeTestUtils.testSaveLoad(new OtherFeatureDataType(), seriesA, proj, flist, row, feature,
        file);
    Assertions.assertThrows(RuntimeException.class,
        () -> DataTypeTestUtils.testSaveLoad(new OtherFeatureDataType(), seriesA, proj, flist, row,
            null, null));
    DataTypeTestUtils.testSaveLoad(new OtherFeatureDataType(), null, proj, flist, row, feature,
        file);
  }

  @Test
  void testOtherFileType() {
    DataTypeTestUtils.testSaveLoad(new OtherFileType(), otherFileB, proj, flist, row, feature,
        file);
    DataTypeTestUtils.testSaveLoad(new OtherFileType(), null, proj, flist, row, feature, file);

    // expect null without a feature/file
    Assertions.assertNull(
        DataTypeTestUtils.saveAndLoad(new OtherFileType(), otherFileB, proj, flist, row, null,
            null));
    // expect null without a feature/file
    Assertions.assertNull(
        DataTypeTestUtils.saveAndLoad(new OtherFileType(), null, proj, flist, row, null, null));
  }

  @Test
  void testChromatogramTypeType() {
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(new ChromatogramTypeType(),
        ChromatogramType.ABSORPTION);
  }

  @Test
  void testMsOtherCorrelationResultType() {
    final MsOtherCorrelationResult resultA = new MsOtherCorrelationResult(featureA,
        MsOtherCorrelationType.CALCULATED);
    final MsOtherCorrelationResult resultB = new MsOtherCorrelationResult(featureB,
        MsOtherCorrelationType.MANUAL);
    List<MsOtherCorrelationResult> results = List.of(resultA, resultB);

    DataTypeTestUtils.testSaveLoad(new MsOtherCorrelationResultType(), results, proj, flist, row,
        feature, file);
    DataTypeTestUtils.testSaveLoad(new MsOtherCorrelationResultType(), null, proj, flist, row,
        feature, file);
    Assertions.assertNull(
        DataTypeTestUtils.saveAndLoad(new MsOtherCorrelationResultType(), List.of(), proj, flist,
            row, feature, file));

    Assertions.assertNull(
        DataTypeTestUtils.saveAndLoad(new MsOtherCorrelationResultType(), List.of(), proj, flist,
            row, null, null));
    Assertions.assertNull(
        DataTypeTestUtils.saveAndLoad(new MsOtherCorrelationResultType(), null, proj, flist, row,
            null, null));
  }

  @Test
  void testMsChromatogramPolarity() {
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(new PolarityTypeType(), PolarityType.NEGATIVE);
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(new PolarityTypeType(), PolarityType.POSITIVE);
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(new PolarityTypeType(), PolarityType.UNKNOWN);
  }

  @Test
  void testMrmLoadSave() {

    final List<Scan> scans = IonTimeSeriesTest.makeSomeScans(file, 10);
    scans.forEach(file::addScan);
    final SimpleIonTimeSeries seriesA = new SimpleIonTimeSeries(null,
        new double[]{322, 322, 322, 322, 322}, new double[]{1, 2, 5, 3, 4},
        file.getScans().subList(3, 8));

    final SimpleIonTimeSeries seriesB = new SimpleIonTimeSeries(null,
        new double[]{322, 322, 322, 322, 322}, new double[]{4, 5, 10, 5, 4},
        file.getScans().subList(3, 8));

    // other mz
    final SimpleIonTimeSeries failingSeries = new SimpleIonTimeSeries(null,
        new double[]{100, 322, 322, 322, 322}, new double[]{4, 5, 10, 5, 4},
        file.getScans().subList(3, 8));

    final MrmTransition mrmA = new MrmTransition(322, 300, seriesA);
    final MrmTransition mrmB = new MrmTransition(322, 10, seriesB);
    Assertions.assertThrows(IllegalArgumentException.class,
        () -> new MrmTransition(322, 300, failingSeries));

    final MrmTransitionList mrmTransitionList = new MrmTransitionList(List.of(mrmA, mrmB));

    // no loading of feature or file = null
    Assertions.assertNull(
        DataTypeTestUtils.saveAndLoad(new MrmTransitionListType(), null, proj, flist,
            row, null, null));
    Assertions.assertNull(
        DataTypeTestUtils.saveAndLoad(new MrmTransitionListType(), mrmTransitionList, proj, flist,
            row, null, null));

    Assertions.assertEquals(mrmTransitionList,
        DataTypeTestUtils.saveAndLoad(new MrmTransitionListType(), mrmTransitionList, proj, flist,
            row, feature, file));
    Assertions.assertEquals(null,
        DataTypeTestUtils.saveAndLoad(new MrmTransitionListType(), null, proj, flist,
            row, feature, file));

    // test setting specific quantifier
    mrmTransitionList.setQuantifier(mrmA, null);
    Assertions.assertEquals(mrmTransitionList,
        DataTypeTestUtils.saveAndLoad(new MrmTransitionListType(), mrmTransitionList, proj, flist,
            row, feature, file));
  }
}
