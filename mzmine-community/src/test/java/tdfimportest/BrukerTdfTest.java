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

package tdfimportest;

import com.google.common.collect.Range;
import fxinitializer.InitJavaFX;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.MobilityScanDataType;
import io.github.mzmine.datamodel.data_access.MobilityScanDataAccess;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.SimpleSpectralArrays;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.TDFImportModule;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.TDFImportParameters;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.TDFImportTask;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.TDFUtils;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.exceptions.MissingMassListException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

public class BrukerTdfTest {

  private static Logger logger = Logger.getLogger(BrukerTdfTest.class.getName());

  public static IMSRawDataFile importTestFile() throws IOException, InterruptedException {
    InitJavaFX.init();

    MZmineProject project = new MZmineProjectImpl();
    String str = BrukerTdfTest.class.getClassLoader()
        .getResource("rawdatafiles/200ngHeLaPASEF_2min_compressed.d").getFile();
    File file = new File(str);
    AtomicReference<TaskStatus> status = new AtomicReference<>(TaskStatus.WAITING);

    AbstractTask importTask = new TDFImportTask(project, file, null, TDFImportModule.class,
        new TDFImportParameters(), Instant.now());
    importTask.addTaskStatusListener((task, newStatus, oldStatus) -> {
      status.set(newStatus);
    });

    Thread thread = new Thread(importTask);
    thread.start();

    Date start = new Date();
    logger.info("Waiting for file import.");
    while (status.get() != TaskStatus.FINISHED) {
      TimeUnit.SECONDS.sleep(1);
      if (status.get() == TaskStatus.ERROR || status.get() == TaskStatus.CANCELED) {
        Assert.fail();
      }
    }
    Date end = new Date();
    logger.info("TDF import took " + ((end.getTime() - start.getTime()) / 1000) + " seconds");
    logger.info("Compare to 19 seconds on NVME SSD.");

    return (IMSRawDataFile) project.getCurrentRawDataFiles().getFirst();
  }

  @Disabled("Needs test file?")
  @Test
  public void testFile() {
    IMSRawDataFile file = null;
    try {
      file = importTestFile();
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    } catch (InterruptedException e) {
      e.printStackTrace();
      Assert.fail();
    }

//    Assert.assertEquals(1430, file.getNumberOfFrames());
    Assert.assertEquals(MobilityType.TIMS,
        file.getFrame(0).getMobilityScans().get(0).getMobilityType());
    Assert.assertEquals(MobilityType.TIMS,
        file.getFrame(0).getMobilityScans().get(0).getMobilityType());
    Assert.assertEquals(671, file.getFrame(507).getMobilityScans().size());

    Frame frame18 = file.getFrame(17);
    Assert.assertEquals(21616, frame18.getNumberOfDataPoints());
    Assert.assertEquals(599.3259, frame18.getBasePeakMz(), 0.001d);
    Assert.assertEquals(555437, frame18.getBasePeakIntensity(), 1d);
    Assert.assertEquals((double) 3.0823007E7, frame18.getTIC(), 2d);
    Assert.assertEquals(40.044052, frame18.getRetentionTime(), 0.00001f);
    Assert.assertEquals(Range.closed(100d, 1700d), frame18.getScanningMZRange());

    MobilityScan mobilityScan425 = frame18.getMobilityScans().get(425);
    Assert.assertEquals(291, mobilityScan425.getBasePeakIndex().intValue());
    Assert.assertEquals(17238.0, mobilityScan425.getBasePeakIntensity(), 0.0001d);
    Assert.assertEquals(599.3258165182417, mobilityScan425.getBasePeakMz(), 0.00000001d);
    Assert.assertEquals(0.9038559019326673, mobilityScan425.getMobility(), 0.00000001d);
    Assert.assertEquals(18, mobilityScan425.getFrame().getFrameId(), 0.00000001d);
    Assert.assertEquals(833, mobilityScan425.getNumberOfDataPoints(), 0.00000001d);
    Assert.assertEquals(Range.closed(246.15697362418837, 1422.918606530885),
        mobilityScan425.getDataPointMZRange());
//    Assert.assertEquals(107494.0, mobilityScan425.getTIC(), 0.0001d);
  }

  @Disabled("Needs test file?")
  @Test
  public void testMobilogramScanDataAccess()
      throws IOException, InterruptedException, MissingMassListException {
    IMSRawDataFile file = importTestFile();
    ScanSelection selection = new ScanSelection(1);
    List<Frame> frames = (List<Frame>) selection.getMatchingScans(file.getFrames());
    MobilityScanDataAccess access = EfficientDataAccess.of(file, MobilityScanDataType.RAW,
        selection);
    for (int i = 0; i < 5; i++) {
      final Frame realFrame = frames.get(i);
      final Frame accessFrame = access.nextFrame();
      Assertions.assertEquals(realFrame, accessFrame);

      for (int j = 0; j < realFrame.getNumberOfMobilityScans(); j++) {
        Assertions.assertEquals(realFrame.getMobilityScan(j), accessFrame.getMobilityScan(j));

        MobilityScan realMScan = realFrame.getMobilityScan(j);
        MobilityScan accessMScan = access.nextMobilityScan();
        Assertions.assertEquals(realMScan, accessMScan);
        Assertions.assertEquals(realMScan.getNumberOfDataPoints(), access.getNumberOfDataPoints());
        Assertions.assertEquals(realMScan.getMobility(), access.getMobility());

        for (int m = 0; m < realMScan.getNumberOfDataPoints(); m++) {
          Assertions.assertEquals(realMScan.getMzValue(m), access.getMzValue(m));
          Assertions.assertEquals(realMScan.getIntensityValue(m), access.getIntensityValue(m));
        }
      }
    }
  }

  @DisabledOnOs(OS.MAC)
  @Test
  public void testCachedConversion() {
    TDFUtils utils = new TDFUtils();

    final URL resource = this.getClass()
        .getResource("/rawdatafiles/additional/lc-tims-ms-pasef-a.d");
    final long handle = utils.openFile(new File(resource.getFile()));

    final List<SimpleSpectralArrays> f1v1 = utils.loadDataPointsForFrame(1, 660L, 818L);
    final List<SimpleSpectralArrays> f2v1 = utils.loadDataPointsForFrame(2, 660L, 818L);

    final List<SimpleSpectralArrays> f1v2 = utils.loadDataPointsForFrame_v2(1, 660L, 818L);
    final List<SimpleSpectralArrays> f2v2 = utils.loadDataPointsForFrame_v2(2, 660L, 818L);

//    Assertions.assertEquals(f1v1.size(), f1v2.size());
//    Assertions.assertEquals(f2v1.size(), f2v2.size());
//    for (int i = 0; i < f1v1.size(); i++) {
//      final SimpleSpectralArrays v1 = f1v1.get(i);
//      final SimpleSpectralArrays v2 = f1v2.get(i);
//      Assertions.assertEquals(v1, v2);
//
//      final SimpleSpectralArrays d1 = f2v1.get(i);
//      final SimpleSpectralArrays d2 = f2v2.get(i);
//      Assertions.assertEquals(d1, d2);
//    }

    Assertions.assertEquals(f1v1, f1v2);
    Assertions.assertEquals(f2v1, f2v2);
    utils.close();
  }
}
