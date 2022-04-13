import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.SimpleCompoundDBAnnotation;
import io.github.mzmine.datamodel.identities.iontype.IonModification;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.modules.dataprocessing.id_biotransformer.BioTransformerParameters;
import io.github.mzmine.modules.dataprocessing.id_biotransformer.BioTransformerUtil;
import io.github.mzmine.parameters.ParameterSet;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BioTransformerTest {

  private static final Logger logger = Logger.getLogger(BioTransformerTest.class.getName());

  @Test
  void testCmdGeneration() {
    final File outputFile = new File("valsartan-transformation.csv");
    final File path = new File("BioTransformer3.0.jar");
    List<String> expected = new ArrayList<>(
        List.of("java", "-jar", path.getName(), "-k", "pred", "-b", "env", "-s", "1",
            "-ismi", "\"CCCCC(=O)N(CC1=CC=C(C=C1)C2=CC=CC=C2C3=NNN=N3)C(C(C)C)C(=O)O\"",
            "-ocsv", "\"" + outputFile.getAbsolutePath() + "\""));

    ParameterSet params = new BioTransformerParameters().cloneParameterSet();

    params.setParameter(BioTransformerParameters.bioPath, path);
    params.setParameter(BioTransformerParameters.steps, 1);
    params.setParameter(BioTransformerParameters.transformationType, "env");
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
    final List<CompoundDBAnnotation> compoundDBAnnotations = BioTransformerUtil.parseLibrary(
        file, new IonType[]{new IonType(IonModification.H)}, new AtomicBoolean(false),
        new AtomicInteger(0));

    final CompoundDBAnnotation expected = new SimpleCompoundDBAnnotation("C23H29N5O",
        392.244486548d, new IonType(IonModification.H),
        "CCCCC(=O)N(CC1=CC=C(C=C1)C2=CC=CC=C2C3=NNN=N3)CC(C)C", "QMAQKWMYJDPUDV-UHFFFAOYSA-N",
        "EAWAG_RULE_BT0051_PATTERN3", "BTM00001", 2.3947f);
    Assertions.assertEquals(9, compoundDBAnnotations.size());
    Assertions.assertEquals(expected, compoundDBAnnotations.get(0));
  }

  @Test
  void combinedTest() throws IOException {
    final File outputFile = new File("valsartan-transformation2.csv");
    outputFile.deleteOnExit();
    final File biotransformer = new File(
        BioTransformerTest.class.getResource("biotransformer/BioTransformer3.0.jar").getFile());

    List<String> expectedCmd = new ArrayList<>(
        List.of("java", "-jar", biotransformer.getName(), "-k", "pred", "-b", "env", "-s", "1",
            "-ismi", "\"CCCCC(=O)N(CC1=CC=C(C=C1)C2=CC=CC=C2C3=NNN=N3)C(C(C)C)C(=O)O\"",
            "-ocsv", "\"" + outputFile.getAbsolutePath() + "\""));

    ParameterSet params = new BioTransformerParameters().cloneParameterSet();
    params.setParameter(BioTransformerParameters.bioPath, biotransformer);
    params.setParameter(BioTransformerParameters.steps, 1);
    params.setParameter(BioTransformerParameters.transformationType, "env");
//    params.setParameter(BioTransformerParameters.cmdOptions, "");
    final List<String> cmdLine = BioTransformerUtil.buildCommandLineArguments(
        "CCCCC(=O)N(CC1=CC=C(C=C1)C2=CC=CC=C2C3=NNN=N3)C(C(C)C)C(=O)O", params, outputFile);
    Assertions.assertEquals(expectedCmd, cmdLine);

    Assertions.assertTrue(
        BioTransformerUtil.runCommandAndWait(biotransformer.getParentFile(), cmdLine));

    final List<CompoundDBAnnotation> compoundDBAnnotations = BioTransformerUtil.parseLibrary(
        outputFile, new IonType[]{new IonType(IonModification.H)}, new AtomicBoolean(false),
        new AtomicInteger(0));

    final CompoundDBAnnotation expected = new SimpleCompoundDBAnnotation("C23H29N5O",
        392.24448654799994d, new IonType(IonModification.H),
        "CCCCC(=O)N(CC1=CC=C(C=C1)C2=CC=CC=C2C3=NNN=N3)CC(C)C", "QMAQKWMYJDPUDV-UHFFFAOYSA-N",
        "EAWAG_RULE_BT0051_PATTERN3", "BTM00001", 2.3947f);
    Assertions.assertEquals(9, compoundDBAnnotations.size());
//    Assertions.assertEquals(expected, compoundDBAnnotations.get(0));
  }
}
