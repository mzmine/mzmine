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

import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.SimpleCompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.annotations.InChIKeyStructureType;
import io.github.mzmine.datamodel.features.types.annotations.InChIStructureType;
import io.github.mzmine.datamodel.features.types.annotations.SmilesStructureType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.ALogPType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.EnzymeType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.ReactionType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonTypeType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.datamodel.identities.iontype.IonModification;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.modules.dataprocessing.id_biotransformer.BioTransformerParameters;
import io.github.mzmine.modules.dataprocessing.id_biotransformer.BioTransformerParameters.TransformationTypes;
import io.github.mzmine.modules.dataprocessing.id_biotransformer.BioTransformerUtil;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking.IonNetworkLibrary;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class BioTransformerTest {

  private static final Logger logger = Logger.getLogger(BioTransformerTest.class.getName());

  @Test
  void testCmdGeneration() {
    final File outputFile = new File("valsartan-transformation.csv");
    final File path = new File("BioTransformer3.0.jar");
    List<String> expected = new ArrayList<>(
        List.of("java", "-jar", path.getName(), "-k", "pred", "-b", "env", "-s", "1", "-ismi",
            "\"CCCCC(=O)N(CC1=CC=C(C=C1)C2=CC=CC=C2C3=NNN=N3)C(C(C)C)C(=O)O\"", "-ocsv",
            "\"" + outputFile.getAbsolutePath() + "\""));

    ParameterSet params = new BioTransformerParameters().cloneParameterSet();

    params.setParameter(BioTransformerParameters.bioPath, path);
    params.setParameter(BioTransformerParameters.steps, 1);
    params.setParameter(BioTransformerParameters.transformationType, TransformationTypes.env);
//    params.setParameter(BioTransformerParameters.cmdOptions, "");

    final List<String> cmdLine = BioTransformerUtil.buildCommandLineArguments(
        "CCCCC(=O)N(CC1=CC=C(C=C1)C2=CC=CC=C2C3=NNN=N3)C(C(C)C)C(=O)O", params, outputFile);

    Assertions.assertEquals(expected, cmdLine);
  }

  @Test
  void parseLibraryTest() throws IOException {
    final URL resource = BioTransformerTest.class.getClassLoader()
        .getResource("biotransformer/transformation.csv");
    final File file = new File(resource.getFile());
    final IonNetworkLibrary library = new IonNetworkLibrary(new MZTolerance(0.005, 10), 1, true, 1,
        new IonModification[]{IonModification.H}, new IonModification[]{});
    final List<CompoundDBAnnotation> compoundDBAnnotations = BioTransformerUtil.parseLibrary(file,
        library);

    final CompoundDBAnnotation expected = new SimpleCompoundDBAnnotation();
    expected.put(FormulaType.class, "C23H29N5O");
    expected.put(IonTypeType.class, new IonType(IonModification.H));
    expected.put(SmilesStructureType.class, "CCCCC(=O)N(CC1=CC=C(C=C1)C2=CC=CC=C2C3=NNN=N3)CC(C)C");
    expected.put(InChIKeyStructureType.class, "QMAQKWMYJDPUDV-UHFFFAOYSA-N");
    expected.put(InChIStructureType.class,
        "InChI=1S/C23H29N5O/c1-4-5-10-22(29)28(15-17(2)3)16-18-11-13-19(14-12-18)20-8-6-7-9-21(20)23-24-26-27-25-23/h6-9,11-14,17H,4-5,10,15-16H2,1-3H3,(H,24,25,26,27)");
    expected.put(ReactionType.class, "EAWAG_RULE_BT0051_PATTERN3");
    expected.put(CompoundNameType.class, "BTM00001");
    expected.put(ALogPType.class, 2.3947f);
    expected.put(EnzymeType.class, "Unspecified environmental bacterial enzyme");
    expected.put(NeutralMassType.class, 391.23721054799995);
    expected.put(PrecursorMZType.class, 392.24448654799994);

    Assertions.assertEquals(9, compoundDBAnnotations.size());
    var actual = compoundDBAnnotations.get(0);
    Assertions.assertEquals(expected.toFullString(), actual.toFullString());
  }

  @Test
  @Disabled("Cannot be run on github without uploading biotransformer jar.")
  void combinedTest() throws IOException {
    final File outputFile = new File("valsartan-transformation2.csv");
    outputFile.deleteOnExit();
    final File biotransformer = new File(
        BioTransformerTest.class.getResource("biotransformer/BioTransformer3.0.jar").getFile());

    List<String> expectedCmd = new ArrayList<>(
        List.of("java", "-jar", biotransformer.getName(), "-k", "pred", "-b", "env", "-s", "1",
            "-ismi", "\"CCCCC(=O)N(CC1=CC=C(C=C1)C2=CC=CC=C2C3=NNN=N3)C(C(C)C)C(=O)O\"", "-ocsv",
            "\"" + outputFile.getAbsolutePath() + "\""));

    ParameterSet params = new BioTransformerParameters().cloneParameterSet();
    params.setParameter(BioTransformerParameters.bioPath, biotransformer);
    params.setParameter(BioTransformerParameters.steps, 1);
    params.setParameter(BioTransformerParameters.transformationType, TransformationTypes.env);
//    params.setParameter(BioTransformerParameters.cmdOptions, "");
    final List<String> cmdLine = BioTransformerUtil.buildCommandLineArguments(
        "CCCCC(=O)N(CC1=CC=C(C=C1)C2=CC=CC=C2C3=NNN=N3)C(C(C)C)C(=O)O", params, outputFile);
    Assertions.assertEquals(expectedCmd, cmdLine);

    Assertions.assertTrue(
        BioTransformerUtil.runCommandAndWait(biotransformer.getParentFile(), cmdLine));

    final IonNetworkLibrary library = new IonNetworkLibrary(new MZTolerance(0.005, 10), 1, true, 1,
        new IonModification[]{IonModification.H}, new IonModification[]{});
    final List<CompoundDBAnnotation> compoundDBAnnotations = BioTransformerUtil.parseLibrary(
        outputFile, library);

    final CompoundDBAnnotation expected = new SimpleCompoundDBAnnotation();
    expected.put(FormulaType.class, "C23H29N5O");
    expected.put(PrecursorMZType.class, 392.244486548d);
    expected.put(IonTypeType.class, new IonType(IonModification.H));
    expected.put(SmilesStructureType.class, "CCCCC(=O)N(CC1=CC=C(C=C1)C2=CC=CC=C2C3=NNN=N3)CC(C)C");
    expected.put(InChIKeyStructureType.class, "QMAQKWMYJDPUDV-UHFFFAOYSA-N");
    expected.put(InChIStructureType.class,
        "InChI=1S/C23H29N5O/c1-4-5-10-22(29)28(15-17(2)3)16-18-11-13-19(14-12-18)20-8-6-7-9-21(20)23-24-26-27-25-23/h6-9,11-14,17H,4-5,10,15-16H2,1-3H3,(H,24,25,26,27)");
    expected.put(ReactionType.class, "EAWAG_RULE_BT0051_PATTERN3");
    expected.put(CompoundNameType.class, "BTM00001");
    expected.put(ALogPType.class, 2.3947f);
    expected.put(EnzymeType.class, "Unspecified environmental bacterial enzyme");
    expected.put(NeutralMassType.class, 391.237210548d);

    Assertions.assertEquals(9, compoundDBAnnotations.size());
//    Assertions.assertEquals(expected, compoundDBAnnotations.get(0));
  }
}
