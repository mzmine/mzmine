/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.TDFImportTask;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.exceptions.MissingMassListException;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import javafx.scene.paint.Color;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class BrukerTdfTest {

  private static Logger logger = Logger.getLogger(BrukerTdfTest.class.getName());

  public static IMSRawDataFile importTestFile() throws IOException, InterruptedException {
    InitJavaFX.init();

    MZmineProject project = new MZmineProjectImpl();
    String str = BrukerTdfTest.class.getClassLoader()
        .getResource("rawdatafiles/200ngHeLaPASEF_2min_compressed.d").getFile();
    File file = new File(str);
    IMSRawDataFile rawDataFile = new IMSRawDataFileImpl(file.getName(),
        MemoryMapStorage.forRawDataFile(), Color.BLACK);

    AtomicReference<TaskStatus> status = new AtomicReference<>(TaskStatus.WAITING);

    AbstractTask importTask = new TDFImportTask(project, file, rawDataFile);
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

    return rawDataFile;
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
    MobilityScanDataAccess access = EfficientDataAccess
        .of(file, MobilityScanDataType.RAW, selection);
    for(int i = 0; i < 5; i++) {
      final Frame realFrame = frames.get(i);
      final Frame accessFrame = access.nextFrame();
      Assertions.assertEquals(realFrame, accessFrame);

      for(int j = 0; j < realFrame.getNumberOfMobilityScans(); j++) {
        Assertions.assertEquals(realFrame.getMobilityScan(j), accessFrame.getMobilityScan(j));

        MobilityScan realMScan = realFrame.getMobilityScan(j);
        MobilityScan accessMScan = access.nextMobilityScan();
        Assertions.assertEquals(realMScan, accessMScan);
        Assertions.assertEquals(realMScan.getNumberOfDataPoints(), access.getNumberOfDataPoints());
        Assertions.assertEquals(realMScan.getMobility(), access.getMobility());

        for(int m = 0; m < realMScan.getNumberOfDataPoints(); m++) {
          Assertions.assertEquals(realMScan.getMzValue(m), access.getMzValue(m));
          Assertions.assertEquals(realMScan.getIntensityValue(m), access.getIntensityValue(m));
        }
      }
    }
  }
}
