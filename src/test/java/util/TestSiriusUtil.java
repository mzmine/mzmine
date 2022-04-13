package util;

import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.SimpleCompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.annotations.SmilesStructureType;
import io.github.mzmine.modules.dataprocessing.id_sirius_cli.SiriusExecutionUtil;
import io.github.mzmine.modules.dataprocessing.id_sirius_cli.SiriusImportUtil;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class TestSiriusUtil {

  private static final Logger logger = Logger.getLogger(TestSiriusUtil.class.getName());

  @Test
  @Disabled
  void testBestFingerIdImport() {
    final Map<Integer, CompoundDBAnnotation> integerCompoundDBAnnotationMap = SiriusImportUtil.readBestCompoundIdentifications(
        new File("D:\\Programme\\sirius-gui\\files\\results_atenolol"));

    Assertions.assertEquals(9, integerCompoundDBAnnotationMap.size());
  }

  @Test
  @Disabled
  void testAllCompoundCandidates() {
    final Map<Integer, List<CompoundDBAnnotation>> allCandidates = SiriusImportUtil.readAllStructureCandidatesFromProject(
        new File("D:\\Programme\\sirius-gui\\files\\results_atenolol"));
    logger.info(allCandidates.toString());
  }

  @Test
  void testSiriusDatabaseGeneration() {

    CompoundDBAnnotation a1 = new SimpleCompoundDBAnnotation();
    a1.put(new SmilesStructureType(),
        "CN(CC(CO)O)C(=O)C1=C(C(=C(C(=C1I)C(=O)NCC(CO)O)I)NC(=O)COC)I");
    a1.put(new CompoundNameType(), "Iopromide");

    CompoundDBAnnotation a2 = new SimpleCompoundDBAnnotation();
    a2.put(new SmilesStructureType(), "C1CCN(CC1)C(=O)C=CC=CC2=CC3=C(C=C2)OCO3");

    final Map<String, CompoundDBAnnotation> db = new HashMap<>();
    db.put(a1.getSmiles(), a1);
    db.put(a2.getSmiles(), a2);

    /*final File dbFile = new File("F:\\sirius_temp\\test\\db.tsv");
    SiriusExecutionUtil.writeCustomDatabase(db, dbFile);
    final File dbFolder = SiriusExecutionUtil.generateCustomDatabase(dbFile,
        new File("D:\\Programme\\sirius_5\\sirius\\sirius.exe"));*/
    final File dbFolder = new File("F:\\sirius_temp\\test\\db");

    SiriusExecutionUtil.runFingerId(new File("F:\\sirius_temp\\test\\100_ms_aligned_corr.mgf"),
        dbFolder, new File("F:\\sirius_temp\\test\\project5"),
        new File("D:\\Programme\\sirius_5\\sirius\\sirius.exe"));
    logger.info("done");
  }
}
