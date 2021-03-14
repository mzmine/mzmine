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
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.featuredata.impl.StorageUtils;
import io.github.mzmine.datamodel.impl.BuildingMobilityScan;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.IOException;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import javafx.scene.paint.Color;
import org.junit.Assert;
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

    logger.info("Building " + numScans + " random scans");

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

    logger.info("Built " + numScans + " random scans");

    return scans;
  }

  @Test
  public void testStorage() {

    logger.info("Creating raw data file.");
    RawDataFile rawDataFile = null;
    try {
      rawDataFile = new IMSRawDataFileImpl("mobility scan test file", null, Color.WHITE);
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    }

    logger.info("Creating frame.");
    SimpleFrame frame = new SimpleFrame(rawDataFile, 1, 1, 0f, 0d, 0, new double[]{0d, 1},
        new double[]{15d, 1E5}, MassSpectrumType.CENTROIDED, PolarityType.POSITIVE, "test",
        Range.closed(0d, 1d), MobilityType.TIMS, null);

    List<BuildingMobilityScan> scans = makeSomeScans(100);
    frame.setMobilityScans(scans);

    logger.info("Checking mopbility scan values.");
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
}
