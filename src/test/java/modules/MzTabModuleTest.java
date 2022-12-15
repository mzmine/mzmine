package modules;

import de.isas.mztab2.io.MzTabFileParser;
import de.isas.mztab2.model.MzTab;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.annotations.CompoundDatabaseMatchesType;
import io.github.mzmine.datamodel.features.types.annotations.GNPSSpectralLibraryMatchesType;
import io.github.mzmine.datamodel.features.types.annotations.SpectralLibraryMatchesType;
import io.github.mzmine.datamodel.features.types.numbers.IDType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.File;
import java.io.IOException;
import org.junit.Test;
import org.mockito.Mock;
import uk.ac.ebi.pride.jmztab.model.CVParam;
import uk.ac.ebi.pride.jmztab.model.MZTabColumnFactory;
import uk.ac.ebi.pride.jmztab.model.MZTabDescription;
import uk.ac.ebi.pride.jmztab.model.MZTabFile;
import uk.ac.ebi.pride.jmztab.model.Metadata;
import uk.ac.ebi.pride.jmztab.model.Section;
import uk.ac.ebi.pride.jmztab.model.SmallMolecule;
import uk.ac.ebi.pride.jmztab2.utils.errors.MZTabErrorList;
import uk.ac.ebi.pride.jmztab2.utils.errors.MZTabErrorType;

public class MzTabModuleTest {

  @Mock
  MZmineProject project;

  @Mock
  RawDataFile rawDataFile;

  @Test
  void exportSingleTestFeature() throws IOException {


//    RawDataFile rawDataFile = MZmineCore.createNewFile("empty raw file", null, null);
//    project.addFile(rawDataFile);
    ModularFeatureList testList = new ModularFeatureList("test list", null, rawDataFile);

//    ModularFeatureList testFeatureList = new ModularFeatureList("test feature list",
//            null, rawDataFile);

    ModularFeature feature = new ModularFeature(testList, rawDataFile, null, FeatureStatus.MANUAL);

    testList.addRowType(new IDType());
    testList.addRowType(new RTType());
    testList.addRowType(new MZType());
    testList.addRowType(new CompoundDatabaseMatchesType());
    testList.addRowType(new SpectralLibraryMatchesType());
    testList.addRowType(new GNPSSpectralLibraryMatchesType());

    int charge = 0;

    feature.setCharge(charge);


    String featureMZ = String.valueOf(feature.getMZ());
    String featureRT = String.valueOf(String.valueOf(feature.getRT()));
    String featureHeight = String.valueOf(feature.getHeight());
    double featureArea = feature.getArea();

    //create small molecule entry
    MZTabColumnFactory factory = MZTabColumnFactory.getInstance(Section.Small_Molecule);
    factory.addDefaultStableColumns();
    SmallMolecule sm = new SmallMolecule(factory, getTestMetadata());

    //get feature identity
    ModularFeatureList modularFeatureList = new ModularFeatureList("test feature list",
            null, rawDataFile);
    modularFeatureList.addRowType(new IDType());
    modularFeatureList.addRowType(new RTType());
    modularFeatureList.addRowType(new MZType());

//    feature.get
//
//    String identifier = escapeString(featureIdentity.getPropertyValue("ID"));
//    String database = featureIdentity.getPropertyValue("Identification method");
//    String formula = featureIdentity.getPropertyValue("Molecular formula");
//    String description = escapeString(featureIdentity.getPropertyValue("Name"));
//    String url = featureIdentity.getPropertyValue("URL");

  }

//  public static FeatureList importTestFile() throws IOException, InterruptedException {
//    InitJavaFX.init();
//
//    MZmineProject project = new MZmineProjectImpl();
//    String str = BrukerTdfTest.class.getClassLoader()
//        .getResource("featuretableio/lipidomics-example.mzTab").getFile();
//    File file = new File(str);
//
//    MzTabFileParser mzTabmFileParser​ = new MzTabFileParser(file);
//    mzTabmFileParser​.parse(System.err, MZTabErrorType.Level.Info, 500);
//
//
//    AtomicReference<TaskStatus> status = new AtomicReference<>(TaskStatus.WAITING);
//    final MemoryMapStorage storage = MemoryMapStorage.forFeatureList();
//
//    AbstractTask importTask = new MzTabmImportTask(project, new MzTabmImportParameters(), file,
//        storage, null);
//    importTask.addTaskStatusListener((task, newStatus, oldStatus) -> {
//      status.set(newStatus);
//    });
//
//    Thread thread = new Thread(importTask);
//    thread.start();
//
//    Date start = new Date();
//    logger.info("Waiting for file import.");
//    while (status.get() != TaskStatus.FINISHED) {
//      TimeUnit.SECONDS.sleep(1);
//      if (status.get() == TaskStatus.ERROR || status.get() == TaskStatus.CANCELED) {
//        Assert.fail();
//      }
//    }
//    Date end = new Date();
//    logger.info("MzTab import took " + ((end.getTime() - start.getTime()) / 1000) + " seconds");
//
//    return mzTa;
//  }

  private MzTab getTestFile() throws IOException {
    String str = MzTabModuleTest.class.getClassLoader()
        .getResource("featuretableio/lipidomics-example.mzTab").getFile();
    File file = new File(str);

    // Parse test mzTab file
    MzTabFileParser mzTabmFileParser​ = new MzTabFileParser(file);
    mzTabmFileParser​.parse(System.err, MZTabErrorType.Level.Info, 500);

    // inspect the output of the parse and errors
    MZTabErrorList errors = mzTabmFileParser​.getErrorList();

    MzTab mzTabFile = mzTabmFileParser​.getMZTabFile();

    return mzTabFile;
  }

  private ModularFeatureList getTestFeatureList(MZTabFile testFile) throws IOException {
    MZmineProject project = new MZmineProjectImpl();
    String featureListName = "test MzTab feature list";
    String rawFileName = "test MzTab raw file";

    final MemoryMapStorage storage = MemoryMapStorage.forFeatureList();

    MzTab mzTabFile = getTestFile();

    RawDataFile rawDataFile = MZmineCore.createNewFile(rawFileName, null, storage);
    project.addFile(rawDataFile);

    ModularFeatureList testFeatureList = new ModularFeatureList(featureListName,
        storage, rawDataFile);

    return testFeatureList;
  }



  @Test
  void importTestMzTabFile() {

  }

  @Test
  void exportTestMzTabFile() {

  }

  //todo use proper metadata from mzTab-m
  private Metadata getTestMetadata() {
    Metadata mtd = new Metadata();
    mtd.setMZTabMode(MZTabDescription.Mode.Summary);
    mtd.setMZTabType(MZTabDescription.Type.Quantification);
    mtd.setDescription("test feature list");
    mtd.addSoftwareParam(1,
        new CVParam("MS", "MS:1002342", "MZmine", String.valueOf(MZmineCore.getMZmineVersion())));
    mtd.setSmallMoleculeQuantificationUnit(
        new CVParam("PRIDE", "PRIDE:0000330", "Arbitrary quantification unit", null));
    mtd.addSmallMoleculeSearchEngineScoreParam(1,
        new CVParam("MS", "MS:1001153", "search engine specific score", null));
    mtd.addFixedModParam(1,
        new CVParam("MS", "MS:1002453", "No fixed modifications searched", null));
    mtd.addVariableModParam(1,
        new CVParam("MS", "MS:1002454", "No variable modifications searched", null));
    return mtd;
  }

  private String escapeString(final String inputString) {

    if (inputString == null)
      return "";

    // Remove all special characters e.g. \n \t
    return inputString.replaceAll("[\\p{Cntrl}]", " ");
  }

}
