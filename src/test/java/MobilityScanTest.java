/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.MobilityScanDataType;
import io.github.mzmine.datamodel.data_access.MobilityScanDataAccess;
import io.github.mzmine.datamodel.featuredata.impl.StorageUtils;
import io.github.mzmine.datamodel.impl.BuildingMobilityScan;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.exceptions.MissingMassListException;
import java.io.IOException;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import javafx.scene.paint.Color;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MobilityScanTest {

  private static Logger logger = Logger.getLogger(MobilityScanTest.class.getName());

  @Test
  public void doubleBufferTest() {
    Random rnd = new Random(System.currentTimeMillis());
    double[] numbers = rnd.doubles().limit(10).toArray();

    MemoryMapStorage storage = null; // MemoryMapStorage.create();

    DoubleBuffer stored = null;
    stored = StorageUtils.storeValuesToDoubleBuffer(storage, numbers);

    for (int i = 0; i < numbers.length; i++) {
      Assert.assertEquals(numbers[i], stored.get(i), 0E-8);
    }

    for (int i = 0; i < numbers.length; i++) {
      double[] d = new double[1];
      stored.get(i, d, 0, 1);
      Assert.assertEquals(numbers[i], d[0], 1E-8);
    }
  }

  public List<BuildingMobilityScan> makeSomeScans(int numScans) {
    Random rnd = new Random(System.currentTimeMillis());
    List<BuildingMobilityScan> scans = new ArrayList<>();

    for (int i = 0; i < numScans; i++) {
      int numDataPoints = (int) (rnd.nextFloat() * 10);
      double mzs[] = new double[numDataPoints];
      double intensities[] = new double[numDataPoints];
      for (int j = 0; j < numDataPoints; j++) {
        mzs[j] = rnd.nextDouble() * 2000d;
        intensities[j] = rnd.nextDouble() * 1E6;
      }
      scans.add(new BuildingMobilityScan(i, intensities, mzs));
    }

    return scans;
  }

  public List<Frame> makeSomeFrames(IMSRawDataFile file, int numFrames) {

    List<Frame> frames = new ArrayList<>();

    double[] mobilities = new double[50];
    for (int j = 0; j < mobilities.length; j++) {
      mobilities[j] = 0;
    }

    for (int i = 0; i < numFrames; i++) {
      SimpleFrame frame = new SimpleFrame(file, 1, 1, 0f, new double[]{0d, 1},
          new double[]{15d, 1E5}, MassSpectrumType.CENTROIDED, PolarityType.POSITIVE, "test",
          Range.closed(0d, 1d), MobilityType.TIMS, null);

      List<BuildingMobilityScan> scans = makeSomeScans(mobilities.length);
      frame.setMobilityScans(scans, true);
      frame.setMobilities(mobilities);
      frames.add(frame);
    }
    return frames;
  }

  @Test
  public void testStorage() {

    logger.info("Creating raw data file.");
    RawDataFile rawDataFile = null;
    try {
      rawDataFile = new IMSRawDataFileImpl("mobility scan test file", null, null, Color.WHITE);
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    }

    logger.info("Creating frame.");
    SimpleFrame frame = new SimpleFrame(rawDataFile, 1, 1, 0f, new double[]{0d, 1},
        new double[]{15d, 1E5}, MassSpectrumType.CENTROIDED, PolarityType.POSITIVE, "test",
        Range.closed(0d, 1d), MobilityType.TIMS, null);

    List<BuildingMobilityScan> scans = makeSomeScans(100);
    frame.setMobilityScans(scans, true);

    logger.info("Checking mobility scan values.");
    for (int i = 0; i < scans.size(); i++) {
      double[] originalMzs = scans.get(i).getMzValues();
      double[] originalIntensities = scans.get(i).getIntensityValues();

      MobilityScan mobilityScan = frame.getMobilityScan(i);
      int numValues = mobilityScan.getNumberOfDataPoints();

      if (numValues != scans.get(i).getNumberOfDataPoints()) {
        Assert.fail("Number of stored values does not match number of original values");
      }
      double[] actualMzs = new double[numValues];
      double[] actualIntensities = new double[numValues];
      actualMzs = mobilityScan.getMzValues(actualMzs);
      actualIntensities = mobilityScan.getIntensityValues(actualIntensities);

      Assert.assertArrayEquals(originalMzs, actualMzs, 1E-8);
      Assert.assertArrayEquals(originalIntensities, actualIntensities, 1E-8);
    }
    logger.info("Mobility scan storing and loading ok.");
  }

  @Test
  void testMobilityScanDataAccess() throws IOException, MissingMassListException {

    logger.info("Creating raw data file.");
    IMSRawDataFile file = null;
    try {
      file = new IMSRawDataFileImpl("mobility scan test file", null, null, Color.WHITE);
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    }

    final List<Frame> frames = makeSomeFrames(file, 10);
    for (Frame frame : frames) {
      file.addScan(frame);
    }

    final MobilityScanDataAccess access = new MobilityScanDataAccess(file, MobilityScanDataType.RAW,
        frames);

    logger.info("Checking mobility scan values.");

    int frameIndex = -1;
    while (access.hasNextFrame()) {
      access.nextFrame();
      frameIndex++;
      int mobilityScanIndex = -1;
      while (access.hasNextMobilityScan()) {
        final MobilityScan mobScan = access.nextMobilityScan();
        mobilityScanIndex++;

        for (int i = 0; i < access.getNumberOfDataPoints(); i++) {
          final double expectedMz = file.getFrame(frameIndex).getMobilityScan(mobilityScanIndex)
              .getMzValue(i);
          final double expectedIntensity = file.getFrame(frameIndex)
              .getMobilityScan(mobilityScanIndex).getIntensityValue(i);
          Assertions.assertEquals(
              file.getFrame(frameIndex).getMobilityScan(mobilityScanIndex).getNumberOfDataPoints(),
              access.getNumberOfDataPoints());

          final double actualMz = access.getMzValue(i);
          final double actualIntensity = access.getIntensityValue(i);

          Assertions.assertEquals(expectedMz, actualMz);
          Assertions.assertEquals(expectedIntensity, actualIntensity);
        }
      }
    }
  }
}
