package util;

import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.modules.dataprocessing.id_siriusimport.SiriusImportUtil;
import java.io.File;
import java.util.Map;
import java.util.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestSiriusImportUtil {

  private static final Logger logger = Logger.getLogger(TestSiriusImportUtil.class.getName());

  @Test
  void testBestFingerIdImport() {
    final Map<Integer, CompoundDBAnnotation> integerCompoundDBAnnotationMap = SiriusImportUtil.readBestCompoundIdentifications(
        new File("D:\\Programme\\sirius-gui\\files\\results_atenolol"));

    Assertions.assertEquals(9, integerCompoundDBAnnotationMap.size());
  }
}
