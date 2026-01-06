/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
import io.github.mzmine.datamodel.features.FeatureList;
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
import io.github.mzmine.datamodel.features.types.annotations.SpectralLibraryMatchesType;
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
import io.github.mzmine.datamodel.features.types.numbers.stats.AnovaResultsType;
import io.github.mzmine.datamodel.identities.iontype.IonModification;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleFeatureIdentity;
import io.github.mzmine.modules.dataanalysis.significance.anova.AnovaResult;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import io.github.mzmine.modules.dataprocessing.id_localcsvsearch.LocalCSVDatabaseSearchModule;
import io.github.mzmine.modules.tools.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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

  @Test
  public void testAnovaType() {
    final MZmineProject project = Mockito.mock(MZmineProject.class);
    final ModularFeatureList flist = Mockito.mock(ModularFeatureList.class);
    final ModularFeatureListRow row = Mockito.mock(ModularFeatureListRow.class);

    AnovaResult result = new AnovaResult(row, "test_column", 0.03, .98);
    DataTypeTestUtils.testSaveLoad(new AnovaResultsType(), result, project, flist, row, null, null);
    DataTypeTestUtils.testSaveLoad(new AnovaResultsType(), null, project, flist, row, null, null);
  }

  @Test
  public void testLegacySpeclib() {
    String xml = """
        <?xml version='1.0' encoding='UTF-8'?>
        <atestelelement>
        <datatype type="spectral_db_matches">
              <feature_annotation annotation_type="spectral_library_annotation">
                <spectraldatabaseentry library_file="MoNA-export-LC-MS-MS_Spectra.json (2 spectra)">
                  <mzs>55.0541;57.0697;67.0542;69.0697;71.0855;81.0699;83.0854;85.1013;95.0855;109.1011;123.1167;268.2637</mzs>
                  <intensities>1.379018;3.010173;3.222499;2.371836;3.785486;5.42292;1.36387;2.605006;7.595636;3.686055;2.111024;100.0</intensities>
                  <databasefieldslist>
                    <entry name="COLLISION_ENERGY">50 % (nominal)</entry>
                    <entry name="PRINCIPAL_INVESTIGATOR">Liza-Marie Beckers, Werner Brack, Janek-Paul Dann, Martin Krauss, Erik Mueller, Tobias Schulze, Helmholtz Centre for Environmental Research GmbH - UFZ, Leipzig, Germany</entry>
                    <entry name="RESOLUTION">15000</entry>
                    <entry name="ION_TYPE">[M]+</entry>
                    <entry name="MOLWEIGHT">371.3274</entry>
                    <entry name="INCHIKEY">QGCUAFIULMNFPJ-UHFFFAOYSA-O</entry>
                    <entry name="MS_LEVEL">MS2</entry>
                    <entry name="INSTRUMENT">LTQ Orbitrap XL Thermo Scientific</entry>
                    <entry name="CHEMSPIDER">91247</entry>
                    <entry name="PUBCHEM">100998</entry>
                    <entry name="SMILES">CCCCCCCCCCCCCC(=O)NCCC[N+](C)(C)CC(O)=O</entry>
                    <entry name="POLARITY">positive</entry>
                    <entry name="NAME">Myristamidopropyl betaine, carboxymethyl-dimethyl-[3-(tetradecanoylamino)propyl]azanium</entry>
                    <entry name="INCHI">InChI=1S/C21H42N2O3/c1-4-5-6-7-8-9-10-11-12-13-14-16-20(24)22-17-15-18-23(2,3)19-21(25)26/h4-19H2,1-3H3,(H-,22,24,25,26)/p+1</entry>
                    <entry name="FORMULA">[C21H43N2O3]+</entry>
                    <entry name="ION_SOURCE">ESI</entry>
                    <entry name="PRECURSOR_MZ">371.3268</entry>
                    <entry name="INSTRUMENT_TYPE">LC-ESI-ITFT</entry>
                    <entry name="EXACT_MASS">371.3274</entry>
                    <entry name="MONA_ID">UP000250</entry>
                    <entry name="DATA_COLLECTOR">Liza-Marie Beckers, Werner Brack, Janek-Paul Dann, Martin Krauss, Erik Mueller, Tobias Schulze, Helmholtz Centre for Environmental Research GmbH - UFZ, Leipzig, Germany</entry>
                  </databasefieldslist>
                </spectraldatabaseentry>
                <spectralsimilarity>
                  <similairtyfunction>Weighted cosine similarity</similairtyfunction>
                  <overlappingpeaks>8</overlappingpeaks>
                  <score>0.9415251115351752</score>
                  <explainedLibraryIntensity>0.9408553450503069</explainedLibraryIntensity>
                  <libraryspectrum>
                    <mzs>55.0541;57.0697;67.0542;69.0697;71.0855;81.0699;83.0854;85.1013;95.0855;109.1011;123.1167;268.2637</mzs>
                    <intensities>1.379018;3.010173;3.222499;2.371836;3.785486;5.42292;1.36387;2.605006;7.595636;3.686055;2.111024;100.0</intensities>
                  </libraryspectrum>
                  <queryspectrum>
                    <mzs>57.070773133356674;58.0658940193675;69.07076263427734;71.0863039721832;81.07044219970703;85.10161530041424;95.0859858314324;109.10144537105035;211.2055137479161;268.2632444835646;382.9527587890625</mzs>
                    <intensities>1633940.68359375;682458.046875;355739.7216796875;1687846.9921875;646939.1162109375;1050999.08203125;1530973.125;761776.962890625;827457.099609375;5.0200965E7;489632.607421875</intensities>
                  </queryspectrum>
                  <alignedspectrumlist numvalues="2">
                    <alignedspectrum>
                      <mzs>57.0697;69.0697;71.0855;81.0699;85.1013;95.0855;109.1011;268.2637</mzs>
                      <intensities>3.010173;2.371836;3.785486;5.42292;2.605006;7.595636;3.686055;100.0</intensities>
                    </alignedspectrum>
                    <alignedspectrum>
                      <mzs>57.070773133356674;69.07076263427734;71.0863039721832;81.07044219970703;85.10161530041424;95.0859858314324;109.10144537105035;268.2632444835646</mzs>
                      <intensities>1633940.68359375;355739.7216796875;1687846.9921875;646939.1162109375;1050999.08203125;1530973.125;761776.962890625;5.0200965E7</intensities>
                    </alignedspectrum>
                  </alignedspectrumlist>
                </spectralsimilarity>
                <ccserror>NULL_VALUE</ccserror>
                <testedmz>371.3269326159603</testedmz>
                <testedrt>4.0783625</testedrt>
              </feature_annotation>
            </datatype>
            </atestelelement>
        """;

    final ByteArrayOutputStream os = new ByteArrayOutputStream(xml.getBytes().length);
    os.writeBytes(xml.getBytes());
    final ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
    final ModularFeatureList flist = FeatureList.createDummy();
    final RawDataFile file = RawDataFile.createDummyFile();
    final MZmineProject proj = new MZmineProjectImpl();
    proj.addFile(file);
    proj.addFeatureList(flist);
    final ModularFeatureListRow row = new ModularFeatureListRow(flist, 1);

    Object match = DataTypeTestUtils.loadDataTypeFromInputStream(new SpectralLibraryMatchesType(),
        null, proj, flist, row, null, null, is, os);
    Assertions.assertTrue(match instanceof List);

    SpectralDBAnnotation first = ((List<SpectralDBAnnotation>) match).getFirst();

    Assertions.assertEquals(first.getSimilarity().getScore(), 0.9415251115351752);
    Assertions.assertNull(first.getCCSError());
    Assertions.assertEquals(371.3269326159603, first.getTestedPrecursorMz());
    Assertions.assertEquals(4.0783625f, first.getTestedRt());
  }
}
