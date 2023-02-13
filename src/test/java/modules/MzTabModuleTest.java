package modules;

import de.isas.mztab2.io.MzTabFileParser;
import de.isas.mztab2.model.MzTab;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.SimpleCompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.annotations.CompoundDatabaseMatchesType;
import io.github.mzmine.datamodel.features.types.annotations.GNPSSpectralLibraryMatchesType;
import io.github.mzmine.datamodel.features.types.annotations.SmilesStructureType;
import io.github.mzmine.datamodel.features.types.annotations.SpectralLibraryMatchesType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonTypeType;
import io.github.mzmine.datamodel.features.types.numbers.IDType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.identities.iontype.IonModification;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.export_features_mztabm.MZTabmExportModule;
import io.github.mzmine.modules.io.export_features_mztabm.MZTabmExportParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import uk.ac.ebi.pride.jmztab.model.CVParam;
import uk.ac.ebi.pride.jmztab.model.MZTabDescription;
import uk.ac.ebi.pride.jmztab.model.MZTabFile;
import uk.ac.ebi.pride.jmztab.model.Metadata;
import uk.ac.ebi.pride.jmztab2.utils.errors.MZTabErrorList;
import uk.ac.ebi.pride.jmztab2.utils.errors.MZTabErrorType;

public class MzTabModuleTest {

  @Mock
  MZmineProject project;

  @Mock
  MemoryMapStorage storage;

  @Mock
  ParameterSet parameters;

  @Mock
  RawDataFile[] rawDataFiles;


  @Test
  void exportSingleTestFeature() {


//    RawDataFile rawDataFile = MZmineCore.createNewFile("empty raw file", null, null);
//    project.addFile(rawDataFile);
//    ModularFeatureList testList = new ModularFeatureList("test list", null, rawDataFile);
    FeatureList testList = new ModularFeatureList("test list", storage, rawDataFiles);

//    ModularFeatureList testFeatureList = new ModularFeatureList("test feature list",
//            null, rawDataFile);

    ModularFeature feature = new ModularFeature((ModularFeatureList) testList, rawDataFiles[0], null, FeatureStatus.MANUAL);

    testList.addRowType(new IDType());
    testList.addRowType(new RTType());
    testList.addRowType(new MZType());
    testList.addRowType(new CompoundDatabaseMatchesType());
    testList.addRowType(new SpectralLibraryMatchesType());
    testList.addRowType(new GNPSSpectralLibraryMatchesType());


    //using values from 20220613_100AGC_60000Res_pluskal_mce_1D1_A3.mzML
    //feature id 0
    int charge = 0;
    float rt = (float) 1.184;
    float area = (float) 3.3E7;
    double mz = 134.0964;


    feature.setCharge(charge);
    feature.setRT(rt);
    feature.setArea(area);
    feature.setMZ(mz);

    FeatureListRow featureListRow = testList.getRow(0);

    //create compoundDB annotation
    IonType ionType = new IonType(IonModification.H);

    CompoundDBAnnotation compoundDBAnnotation = new SimpleCompoundDBAnnotation();

    final MZTolerance tol = new MZTolerance(0.000001, .01);

    compoundDBAnnotation.put(SmilesStructureType.class, "C1CCN(C1)C(=O)C=CC=CC2=CC3=C(C=C2)OCO3");
    double mzFromSmiles = CompoundDBAnnotation.calcMzForAdduct(compoundDBAnnotation, ionType);

    compoundDBAnnotation.put(FormulaType.class, "C16H17NO3");
    double mzFromFormula = CompoundDBAnnotation.calcMzForAdduct(compoundDBAnnotation, ionType);

    compoundDBAnnotation.put(PrecursorMZType.class,272.1281199);
    compoundDBAnnotation.put(IonTypeType.class, ionType);
    double mzFromMz = CompoundDBAnnotation.calcMzForAdduct(compoundDBAnnotation, ionType);

    compoundDBAnnotation.put(NeutralMassType.class, 271.1208434);
    double mzFromNeutral = CompoundDBAnnotation.calcMzForAdduct(compoundDBAnnotation, ionType);

    featureListRow.addCompoundAnnotation(compoundDBAnnotation);


//    String featureMZ = String.valueOf(feature.getMZ());
//    String featureRT = String.valueOf(String.valueOf(feature.getRT()));
//    String featureHeight = String.valueOf(feature.getHeight());
//    double featureArea = feature.getArea();

    //create small molecule entry
//    MZTabColumnFactory factory = MZTabColumnFactory.getInstance(Section.Small_Molecule);
//    factory.addDefaultStableColumns();
//    SmallMolecule sm = new SmallMolecule(factory, getTestMetadata());
//
//    //get feature identity
//    ModularFeatureList modularFeatureList = new ModularFeatureList("test feature list",
//            null, rawDataFile);
//    modularFeatureList.addRowType(new IDType());
//    modularFeatureList.addRowType(new RTType());
//    modularFeatureList.addRowType(new MZType());

    //How to proceed here? Probs should create parameters as well (at least path to write a file)
    MZTabmExportModule module = MZmineCore.getModuleInstance(
        MZTabmExportModule.class);
    if (module != null) {

      //Mock feature list selection???
      FeatureListsSelection featureListsSelection = new FeatureListsSelection(
          (ModularFeatureList) testList);
      ParameterSet parameterSet = new MZTabmExportParameters();
      //featureLists, filename, exportAll
      parameterSet.setParameter(MZTabmExportParameters.featureLists, featureListsSelection);
      parameterSet.setParameter(MZTabmExportParameters.filename, new File("featuretableio/test_out.mzTab"));
      parameterSet.setParameter(MZTabmExportParameters.exportAll, true);
      List<Task> tasks = new ArrayList<>();
//      MZTabmExportTask task = new MZTabmExportTask(project, parameters, tasks, Instant.now());
//      tasks.add(task);
      module.runModule(MZmineCore.getProjectManager().getCurrentProject(), parameters, tasks,
          Instant.now());
      MZmineCore.getTaskController().addTasks(tasks.toArray(Task[]::new));
    }

    Assertions.assertEquals(2, 2);
  }

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
