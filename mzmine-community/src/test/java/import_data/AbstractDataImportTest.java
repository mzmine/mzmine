/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package import_data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectors;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.factor_of_lowest.FactorOfLowestMassDetectorParameters;
import io.github.mzmine.modules.io.import_rawdata_all.AdvancedSpectraImportParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.project.ProjectService;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import testutils.MZmineTestUtil;

@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
@DisabledOnOs(OS.MAC)
public abstract class AbstractDataImportTest {

  private static final Logger logger = Logger.getLogger(AbstractDataImportTest.class.getName());
  public static double lowestMz = 350d;

  public AbstractDataImportTest() {
  }

  public abstract List<String> getFileNames();

  @BeforeAll
  public void initialize() {
    MZmineTestUtil.startMzmineCore();
  }

  @AfterAll
  public void tearDown() {
    //clean the project after this integration test
    MZmineTestUtil.cleanProject();
  }

  /**
   * @return null if no advanced import should be checked
   */
  @Nullable
  public static AdvancedSpectraImportParameters createAdvancedImportSettings() {
    ParameterSet massDetectorParam = MassDetectors.FACTOR_OF_LOWEST.getModuleParameters()
        .cloneParameterSet();
    massDetectorParam.setParameter(FactorOfLowestMassDetectorParameters.noiseFactor, 3d);

    AdvancedSpectraImportParameters advanced = (AdvancedSpectraImportParameters) new AdvancedSpectraImportParameters().cloneParameterSet();

    // set value first and then parameters
    advanced.setParameter(AdvancedSpectraImportParameters.msMassDetection, true);
    advanced.setParameter(AdvancedSpectraImportParameters.ms2MassDetection, true);

    advanced.getParameter(AdvancedSpectraImportParameters.msMassDetection).getEmbeddedParameter()
        .setValue(MassDetectors.FACTOR_OF_LOWEST, massDetectorParam);
    advanced.getParameter(AdvancedSpectraImportParameters.ms2MassDetection).getEmbeddedParameter()
        .setValue(MassDetectors.FACTOR_OF_LOWEST, massDetectorParam.cloneParameterSet());

    advanced.setParameter(AdvancedSpectraImportParameters.mzRange, true,
        Range.closed(lowestMz, 5000d));
    advanced.setParameter(AdvancedSpectraImportParameters.denormalizeMSnScans, true);
    advanced.setParameter(AdvancedSpectraImportParameters.scanFilter, ScanSelection.ALL_SCANS);
    return advanced;
  }

  @Test
  @Order(1)
//  @Disabled
  @DisplayName("Test data import of mzML and mzXML without advanced parameters")
  public void dataImportTest() throws InterruptedException {
    MZmineTestUtil.cleanProject();
    MZmineTestUtil.importFiles(getFileNames(), 360);
    Map<String, DataFileStats> stats = DataFileStatsIO.readJson(getClass());
    DataImportTestUtils.testDataStatistics(getFileNames(), stats, false);
  }

  @Test
  @Order(2)
//  @Disabled
  @DisplayName("Test data advanced import of mzML and mzXML with advanced parameters like mass detection")
  public void advancedDataImportTest() throws InterruptedException {
    var advanced = createAdvancedImportSettings();

    MZmineTestUtil.cleanProject();
    MZmineTestUtil.importFiles(getFileNames(), 360, advanced);
    Map<String, DataFileStats> stats = DataFileStatsIO.readJson(getClass());
    DataImportTestUtils.testDataStatistics(getFileNames(), stats, true);

    //
    for (final RawDataFile raw : ProjectService.getProject().getDataFiles()) {
      if (raw instanceof IMSRawDataFile && raw.getFileName().toLowerCase().endsWith(".mzml")) {
        // we do not have frames in mzml import yet so the mass list is null
        continue; // skip
      }
      String msg = " Error in " + raw.getName();
      for (final Scan scan : raw.getScans()) {
        // advanced sets mass list
        assertNotNull(scan.getMassList(), msg);
        assertEquals(scan.getNumberOfDataPoints(), scan.getMassList().getNumberOfDataPoints(), msg);
        if (scan.getNumberOfDataPoints() > 0 && scan.getMSLevel() == 1) {
          // MS1 scans were cropped
          assertTrue(scan.getMzValue(0) >= lowestMz,
              scan.getMzValue(0) + " was higher than " + lowestMz + msg);
        }
      }
    }
  }
}
