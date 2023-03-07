/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.SimpleCompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.ListWithSubsType;
import io.github.mzmine.datamodel.features.types.abstr.UrlShortName;
import io.github.mzmine.datamodel.features.types.annotations.CommentType;
import io.github.mzmine.datamodel.features.types.annotations.CompoundDatabaseMatchesType;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.annotations.GNPSClusterUrlType;
import io.github.mzmine.datamodel.features.types.annotations.GNPSLibraryUrlType;
import io.github.mzmine.datamodel.features.types.annotations.GNPSNetworkUrlType;
import io.github.mzmine.datamodel.features.types.annotations.IdentityType;
import io.github.mzmine.datamodel.features.types.annotations.InChIStructureType;
import io.github.mzmine.datamodel.features.types.annotations.ManualAnnotation;
import io.github.mzmine.datamodel.features.types.annotations.ManualAnnotationType;
import io.github.mzmine.datamodel.features.types.annotations.PossibleIsomerType;
import io.github.mzmine.datamodel.features.types.annotations.RdbeType;
import io.github.mzmine.datamodel.features.types.annotations.SmilesStructureType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.DatabaseNameType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaListType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonAdductType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonNetworkIDType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonTypeType;
import io.github.mzmine.datamodel.features.types.annotations.iin.MsMsMultimerVerifiedType;
import io.github.mzmine.datamodel.features.types.annotations.iin.PartnerIdsType;
import io.github.mzmine.datamodel.features.types.numbers.CCSType;
import io.github.mzmine.datamodel.features.types.numbers.MobilityType;
import io.github.mzmine.datamodel.features.types.numbers.scores.LipidAnnotationMsMsScoreType;
import io.github.mzmine.datamodel.identities.iontype.IonModification;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleFeatureIdentity;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import io.github.mzmine.modules.dataprocessing.id_localcsvsearch.LocalCSVDatabaseSearchModule;
import io.github.mzmine.modules.tools.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.util.FeatureUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class AnnotationTypeTests {

  @Test
  void manualAnnotationTypeTest() {

    RawDataFile file = null;
    file = new RawDataFileImpl("testfile", null, null, Color.BLACK);
    Assertions.assertNotNull(file);

    // test load/save for row
    final ModularFeatureList flist = new ModularFeatureList("flist", null, file);
    final ModularFeatureListRow row = new ModularFeatureListRow(flist, 1);

    final MZmineProject project = new MZmineProjectImpl();
    project.addFile(file);
    project.addFeatureList(flist);

    FeatureIdentity id1 = new SimpleFeatureIdentity("name1", "form1", "method1", "id1", "url1");
    FeatureIdentity id2 = new SimpleFeatureIdentity("name2", "form2", "method2", "id2", "url2");

    // test row load
    ManualAnnotationType type = new ManualAnnotationType();
    ObservableList<FeatureIdentity> list = FXCollections.observableList(List.of(id1, id2));
    ManualAnnotation value = new ManualAnnotation();
    value.setIdentities(list);
    final ManualAnnotation loaded = (ManualAnnotation) DataTypeTestUtils.saveAndLoad(type, value,
        project, flist, row, null, null);

    List<FeatureIdentity> featureIdentities = loaded.getIdentities();
    Assertions.assertEquals(list.size(), featureIdentities.size());

    FeatureIdentity loaded1 = featureIdentities.get(0);
    FeatureIdentity loaded2 = featureIdentities.get(1);

    Assertions.assertEquals(id1.getAllProperties().size(), loaded1.getAllProperties().size());
    for (Entry<String, String> entry : id1.getAllProperties().entrySet()) {
      Assertions.assertEquals(entry.getValue(), loaded1.getPropertyValue(entry.getKey()));
    }

    Assertions.assertEquals(id2.getAllProperties().size(), loaded2.getAllProperties().size());
    for (Entry<String, String> entry : id2.getAllProperties().entrySet()) {
      Assertions.assertEquals(entry.getValue(), loaded2.getPropertyValue(entry.getKey()));
    }

    // test null value
    DataTypeTestUtils.testSaveLoad(type, null, project, flist, row, null, null);
  }

  @Test
  void commentTypeTest() {
    CommentType type = new CommentType();
    String value = "comment";
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }

  @Test
  void compoundNameTypeTest() {
    CompoundNameType type = new CompoundNameType();
    String value = "name";
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }

  // todo FormulaType
  // todo FormulaConsensusSummaryType
  // todo FormulaSummaryType

  @Test
  void formulaTypeTest() {
    FormulaType type = new FormulaType();
    String value = "C5H6O4F3";
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
    file = new RawDataFileImpl("testfile", null, null, Color.BLACK);
    Assertions.assertNotNull(file);

    // test load/save for row
    final ModularFeatureList flist = new ModularFeatureList("flist", null, file);
    final ModularFeatureListRow row = new ModularFeatureListRow(flist, 1);

    final MZmineProject project = new MZmineProjectImpl();
    project.addFile(file);
    project.addFeatureList(flist);

    FeatureIdentity id1 = new SimpleFeatureIdentity("name1", "form1", "method1", "id1", "url1");
    FeatureIdentity id2 = new SimpleFeatureIdentity("name2", "form2", "method2", "id2", "url2");

    IdentityType type = new IdentityType();
    ObservableList<FeatureIdentity> list = FXCollections.observableList(List.of(id1, id2));
    final List<?> loaded = (List<?>) DataTypeTestUtils.saveAndLoad(type, list, project, flist, row,
        null, null);

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

    DataTypeTestUtils.testSaveLoad(type, null, project, flist, row, null, null);
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

  @Test
  void CompoundDatabaseMatchesTypeTest() {

    var type = new CompoundDatabaseMatchesType();

    final IonType ionType = new IonType(IonModification.NH4);
    final CompoundDBAnnotation newIdentity = new SimpleCompoundDBAnnotation();
    newIdentity.put(new CompoundNameType(), "glucose");
    newIdentity.put(new FormulaType(), "C6H6O6");
    newIdentity.put(new SmilesStructureType(), "C(C1C(C(C(C(O1)O)O)O)O)O");
    newIdentity.put(new DatabaseNameType(), LocalCSVDatabaseSearchModule.MODULE_NAME);
    newIdentity.put(new IonAdductType(), "[M+H]+");
    newIdentity.put(new CCSType(), null);
    newIdentity.put(new MobilityType(), 0.56f);
    newIdentity.put(new IonTypeType(), ionType);

    String name = newIdentity.getCompoundName();

    final CompoundDBAnnotation newIdentity2 = new SimpleCompoundDBAnnotation();
    newIdentity2.put(new CompoundNameType(), "mannose");
    newIdentity2.put(new FormulaType(), "C6H6O6");
    newIdentity2.put(new SmilesStructureType(), "C(C1C(C(C(C(O1)O)O)O)O)O");
    newIdentity2.put(new DatabaseNameType(), LocalCSVDatabaseSearchModule.MODULE_NAME);
    newIdentity2.put(new IonAdductType(), "[M+H]+");
    newIdentity2.put(new CCSType(), null);
    newIdentity2.put(new MobilityType(), 0.56f);
    newIdentity2.put(new IonTypeType(), ionType);

    var value = new ArrayList<>(List.of(newIdentity, newIdentity2));

    RawDataFile file = null;
    file = new RawDataFileImpl("testfile", null, null, Color.BLACK);
    Assertions.assertNotNull(file);

    // test load/save for row
    final ModularFeatureList flist = new ModularFeatureList("flist", null, file);
    final ModularFeatureListRow row = new ModularFeatureListRow(flist, 1);
    flist.addRow(row);
    row.set(type, value);

    final MZmineProject project = new MZmineProjectImpl();
    project.addFile(file);
    project.addFeatureList(flist);

    DataTypeTestUtils.testSaveLoad(type, value, project, flist, row, null, file);
    DataTypeTestUtils.testSaveLoad(type, List.of(), project, flist, row, null, file);

    Assertions.assertNotEquals(newIdentity, newIdentity2);
    Assertions.assertNotEquals(newIdentity, null);

    // test FeatureUtils.extractSubValueFromAllAnnotations
    final Map<? extends ListWithSubsType<?>, String> formulas = FeatureUtils.extractSubValueFromAllAnnotations(
        row, FormulaType.class);
    Assertions.assertEquals(formulas.get(new CompoundDatabaseMatchesType()), "C6H6O6");
  }

  @Test
  void formulaListTypeTest() {
    final IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
    final IMolecularFormula form1 = MolecularFormulaManipulator.getMajorIsotopeMolecularFormula(
        "C15H28F2O2", builder);
    final IMolecularFormula form2 = MolecularFormulaManipulator.getMajorIsotopeMolecularFormula(
        "C18H34O2", builder);
    final IMolecularFormula form3 = MolecularFormulaManipulator.getMajorIsotopeMolecularFormula(
        "GdC50H80O10N4", builder);

    ResultFormula formula1 = new ResultFormula(form1,
        IsotopePatternCalculator.calculateIsotopePattern(form1, 0.01, 1, PolarityType.POSITIVE),
        0.5f, 0.1f,
        Map.of(new SimpleDataPoint(513.25, 1d), "C132", new SimpleDataPoint(200.26, 1d), "COF"),
        MolecularFormulaManipulator.getMass(form1, 3));
    ResultFormula formula2 = new ResultFormula(form2,
        IsotopePatternCalculator.calculateIsotopePattern(form2, 0.01, 1, PolarityType.POSITIVE),
        0.5f, 0.1f, null, MolecularFormulaManipulator.getMass(form1, 3));
    ResultFormula formula3 = new ResultFormula(form3, null, 0.5f, null, null,
        MolecularFormulaManipulator.getMass(form1, 3));

    final List<ResultFormula> value = List.of(formula1, formula2, formula3);
    final FormulaListType type = new FormulaListType();

    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }
}
