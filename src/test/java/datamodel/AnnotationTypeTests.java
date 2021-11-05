package datamodel;

import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.ModularTypeProperty;
import io.github.mzmine.datamodel.features.types.abstr.UrlShortName;
import io.github.mzmine.datamodel.features.types.annotations.CommentType;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.annotations.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.GNPSClusterUrlType;
import io.github.mzmine.datamodel.features.types.annotations.GNPSLibraryUrlType;
import io.github.mzmine.datamodel.features.types.annotations.GNPSNetworkUrlType;
import io.github.mzmine.datamodel.features.types.annotations.IdentityType;
import io.github.mzmine.datamodel.features.types.annotations.InChIStructureType;
import io.github.mzmine.datamodel.features.types.annotations.LipidAnnotationMsMsScoreType;
import io.github.mzmine.datamodel.features.types.annotations.ManualAnnotationType;
import io.github.mzmine.datamodel.features.types.annotations.PossibleIsomerType;
import io.github.mzmine.datamodel.features.types.annotations.RdbeType;
import io.github.mzmine.datamodel.features.types.annotations.SmilesStructureType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonAdductType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonNetworkIDType;
import io.github.mzmine.datamodel.features.types.annotations.iin.MsMsMultimerVerifiedType;
import io.github.mzmine.datamodel.features.types.annotations.iin.PartnerIdsType;
import io.github.mzmine.datamodel.impl.SimpleFeatureIdentity;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import javafx.beans.property.ListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AnnotationTypeTests {

  @Test
  void manualAnnotationTypeTest() {

    RawDataFile file = null;
    try {
      file = new RawDataFileImpl("testfile", null, null, Color.BLACK);
    } catch (IOException e) {
      e.printStackTrace();
      Assertions.fail("Cannot initialise data file.");
    }
    Assertions.assertNotNull(file);

    // test load/save for row
    final ModularFeatureList flist = new ModularFeatureList("flist", null, file);
    final ModularFeatureListRow row = new ModularFeatureListRow(flist, 1);

    FeatureIdentity id1 = new SimpleFeatureIdentity("name1", "form1", "method1", "id1", "url1");
    FeatureIdentity id2 = new SimpleFeatureIdentity("name2", "form2", "method2", "id2", "url2");

    // test row load
    ManualAnnotationType type = new ManualAnnotationType();
    ObservableList<FeatureIdentity> list = FXCollections.observableList(List.of(id1, id2));
    ModularTypeProperty value = type.createProperty();
    value.set(IdentityType.class, list);
    final ModularTypeProperty loaded = (ModularTypeProperty) DataTypeTestUtils.saveAndLoad(type,
        value, flist, row, null, null);

    ListProperty<FeatureIdentity> featureIdentities = loaded.get(new IdentityType());
    Assertions.assertEquals(list.size(), featureIdentities.size());

    FeatureIdentity loaded1 = featureIdentities.get().get(0);
    FeatureIdentity loaded2 = featureIdentities.get().get(1);

    Assertions.assertEquals(id1.getAllProperties().size(), loaded1.getAllProperties().size());
    for (Entry<String, String> entry : id1.getAllProperties().entrySet()) {
      Assertions.assertEquals(entry.getValue(), loaded1.getPropertyValue(entry.getKey()));
    }

    Assertions.assertEquals(id2.getAllProperties().size(), loaded2.getAllProperties().size());
    for (Entry<String, String> entry : id2.getAllProperties().entrySet()) {
      Assertions.assertEquals(entry.getValue(), loaded2.getPropertyValue(entry.getKey()));
    }

    // test null value
    DataTypeTestUtils.testSaveLoad(type, null, flist, row, null, null);
  }

  @Test
  void commentTypeTest() {
    CommentType type = new CommentType();
    Object value = "comment";
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }

  @Test
  void compoundNameTypeTest() {
    CompoundNameType type = new CompoundNameType();
    Object value = "name";
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }

  // todo FormulaType
  // todo FormulaConsensusSummaryType
  // todo FormulaSummaryType

  @Test
  void formulaTypeTest() {
    FormulaType type = new FormulaType();
    Object value = "C5H6O4F3";
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }

  @Test
  void GNPSClusterUrlTypeTest() {
    GNPSClusterUrlType type = new GNPSClusterUrlType();
    UrlShortName value = new UrlShortName("https://github.com/mzmine/mzmine3/pull/370", "short");
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }

  @Test
  void GNPSLibraryUrlTypeTest() {
    GNPSLibraryUrlType type = new GNPSLibraryUrlType();
    UrlShortName value = new UrlShortName("https://github.com/mzmine/mzmine3/pull/370", "short");
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }

  @Test
  void GNPSNetworkUrlTypeTest() {
    GNPSNetworkUrlType type = new GNPSNetworkUrlType();
    UrlShortName value = new UrlShortName("https://github.com/mzmine/mzmine3/pull/370", "short");
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }

  // todo GNPSSpectralLibMatchSummaryType
  // todo GNPSSpectralLibraryMatchType

  @Test
  void identityTypeTest() {
    RawDataFile file = null;
    try {
      file = new RawDataFileImpl("testfile", null, null, Color.BLACK);
    } catch (IOException e) {
      e.printStackTrace();
      Assertions.fail("Cannot initialise data file.");
    }
    Assertions.assertNotNull(file);

    // test load/save for row
    final ModularFeatureList flist = new ModularFeatureList("flist", null, file);
    final ModularFeatureListRow row = new ModularFeatureListRow(flist, 1);

    FeatureIdentity id1 = new SimpleFeatureIdentity("name1", "form1", "method1", "id1", "url1");
    FeatureIdentity id2 = new SimpleFeatureIdentity("name2", "form2", "method2", "id2", "url2");

    IdentityType type = new IdentityType();
    ObservableList<FeatureIdentity> list = FXCollections.observableList(List.of(id1, id2));
    final List<?> loaded = (List<?>) DataTypeTestUtils.saveAndLoad(type, list, flist, row, null,
        null);

    Assertions.assertEquals(list.size(), loaded.size());

    FeatureIdentity loaded1 = (FeatureIdentity) loaded.get(0);
    FeatureIdentity loaded2 = (FeatureIdentity) loaded.get(1);

    Assertions.assertEquals(id1.getAllProperties().size(), loaded1.getAllProperties().size());
    for (Entry<String, String> entry : id1.getAllProperties().entrySet()) {
      Assertions.assertEquals(entry.getValue(), loaded1.getPropertyValue(entry.getKey()));
    }

    Assertions.assertEquals(id2.getAllProperties().size(), loaded2.getAllProperties().size());
    for (Entry<String, String> entry : id2.getAllProperties().entrySet()) {
      Assertions.assertEquals(entry.getValue(), loaded2.getPropertyValue(entry.getKey()));
    }

    DataTypeTestUtils.testSaveLoad(type, null, flist, row, null, null);
  }

  @Test
  void inchiStructureTypeTest() {
    InChIStructureType type = new InChIStructureType();
    String value = "1S/C18H24I3N3O8/c1-24(4-9(28)6-26)18(31)12-13(19)11(17(30)22-3-8(27)5-25)14"
        + "(20)16(15(12)21)23-10(29)7-32-2/h8-9,25-28H,3-7H2,1-2H3,(H,22,30)(H,23,29)";
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }

  @Test
  void lipidAnotationMsMsScoreTypeTest() {
    LipidAnnotationMsMsScoreType type = new LipidAnnotationMsMsScoreType();
    Float value = 0.978f;
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }

  // todo LipidAnnotationSummaryType -> see RegularScanTypesTest
  // todo LipidAnnotationType

  // todo LipidSpectrumType

  @Test
  void possibleIsomerTypeTest() {
    PossibleIsomerType type = new PossibleIsomerType();
    List<Integer> value = List.of(5, 3, 4, 9, 7);
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }

  @Test
  void rdbeTypeTest() {
    RdbeType type = new RdbeType();
    Float value = 3f;
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }

  @Test
  void smilesStructureTypeTest() {
    SmilesStructureType type = new SmilesStructureType();
    String value = "CN(CC(CO)O)C(=O)C1=C(C(=C(C(=C1I)C(=O)NCC(CO)O)I)NC(=O)COC)I";
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }

  /**
   * {@link io.github.mzmine.datamodel.features.types.annotations.SpectralLibMatchSummaryType} in
   * {@link RegularScanTypesTest#spectralLibMatchSummaryTypeTest()} and {@link
   * IMSScanTypesTest#spectralLibMatchSummaryTypeTest()}
   */

  // todo SpectralLibraryMatchType
  @Test
  void ionAdductTestType() {
    IonAdductType type = new IonAdductType();
    String value = "[M-2H2O+H+Na]2+";
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }

  // todo IonIdentityListType
  // todo IonIdentityModularType

  @Test
  void ionNetworkIdTypeTest() {
    IonNetworkIDType type = new IonNetworkIDType();
    int value = 3;
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }

  @Test
  void msmsMultimerVerifiedTypeTest() {
    MsMsMultimerVerifiedType type = new MsMsMultimerVerifiedType();
    Boolean value = true;
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }

  @Test
  void partnerIdsTypeTest() {
    PartnerIdsType type = new PartnerIdsType();
    String value = "15;32;21;56";
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }
}
