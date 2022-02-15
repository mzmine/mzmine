import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.SimpleCompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.annotations.SmilesStructureType;
import io.github.mzmine.modules.dataprocessing.id_biotransformer.BioTransformerParameters;
import io.github.mzmine.modules.dataprocessing.id_biotransformer.BioTransformerUtil;
import io.github.mzmine.parameters.ParameterSet;
import java.io.File;
import java.net.URL;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BioTransformerTest {

  @Test
  void testCmdGeneration() {

    String expected = """
        java -jar biotransformer3.0.jar -k pred -b env -s 1 
        -ismi "CCCCC(=O)N(CC1=CC=C(C=C1)C2=CC=CC=C2C3=NNN=N3)C(C(C)C)C(=O)O" 
        -ocsv valsartan-transformation.csv
        """;

    ParameterSet params = new BioTransformerParameters().cloneParameterSet();

    final URL resource = BioTransformerTest.class.getClassLoader()
        .getResource("biotransformer/BioTransformer3.0.jar");
    final File path = new File(resource.getFile());
    params.setParameter(BioTransformerParameters.bioPath, path);
    params.setParameter(BioTransformerParameters.steps, 1);
    params.setParameter(BioTransformerParameters.transformationType, "env");
    params.setParameter(BioTransformerParameters.cmdOptions, "");

    final CompoundDBAnnotation annotation = new SimpleCompoundDBAnnotation();
    annotation.put(SmilesStructureType.class,
        "CCCCC(=O)N(CC1=CC=C(C=C1)C2=CC=CC=C2C3=NNN=N3)C(C(C)C)C(=O)O");
    final String cmdLine = BioTransformerUtil.buildCommandLine(annotation, params,
        new File("valsartan-transformation.csv"));

    Assertions.assertEquals(expected, cmdLine);
  }
}
