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

package io.github.mzmine.datamodel.impl.masslist;

import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.IOException;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FrameMassList extends SimpleMassList {

  private static Logger logger = Logger.getLogger(FrameMassList.class.getName());

  protected DoubleBuffer mobilityScanMzBuffer;
  protected DoubleBuffer mobilityScanIntensityBuffer;

  protected int maxMobilityScanDatapoints = -1;

  public FrameMassList(@Nonnull MemoryMapStorage storage,
      @Nonnull double[] mzValues,
      @Nonnull double[] intensityValues) {
    super(storage, mzValues, intensityValues);
  }

  public static int[] generateOffsets(List<double[][]> mzsIntensities) {
    final int[] offsets = new int[mzsIntensities.size()];
    offsets[0] = 0;
    for (int i = 1; i < offsets.length; i++) {
      offsets[i] = offsets[i - 1] + mzsIntensities.get(i - 1)[0].length;
    }
    return offsets;
  }

  /**
   * @param mobilityScanPeaks List of values. it will be iterated over mobilityScanPeans[arrayIndex][i]
   * @param arrayIndex        the index of the first dimension of the input array.
   * @param dst               the destination array of an appropriate size.
   * @return An array of base peak indices if arrayIndex == 1.
   */
  public static int[] putAllValuesIntoOneArray(final List<double[][]> mobilityScanPeaks,
      final int arrayIndex, double[] dst) {
    int[] basePeakIndices = null;
    if (arrayIndex == 1) {
      basePeakIndices = new int[mobilityScanPeaks.size()];
      Arrays.fill(basePeakIndices, -1);
    }

    int dpCounter = 0;
    for (int scanNum = 0, numScans = mobilityScanPeaks.size(); scanNum < numScans; scanNum++) {
      double[][] mzIntensity = mobilityScanPeaks.get(scanNum);
      double maxIntensity = -1d;

      double[] doubles = mzIntensity[arrayIndex];
      for (int peakNum = 0; peakNum < doubles.length; peakNum++) {
        double thisMzOrIntensity = doubles[peakNum];
        dst[dpCounter] = thisMzOrIntensity;
        dpCounter++;
        if (arrayIndex == 1 && thisMzOrIntensity > maxIntensity) {
          maxIntensity = thisMzOrIntensity;
          basePeakIndices[scanNum] = peakNum;
        }
      }
    }

    return basePeakIndices;
  }

  /**
   * Genereates mass lists for the given mobility scans and adds them to the respective mobility
   * scans.
   *
   * @param mobilityScans
   * @param storage
   * @param massDetector
   * @param massDetectorParameters
   */
  public void generateAndAddMobilityScanMassLists(
      @Nonnull List<MobilityScan> mobilityScans,
      @Nullable MemoryMapStorage storage,
      @Nonnull MassDetector massDetector,
      @Nonnull ParameterSet massDetectorParameters) {

    // mobility scan -> [0][] = mzs, [1][] = intensities
    final List<double[][]> mobilityScanPeaks = new ArrayList<>();

    for (MobilityScan mobilityScan : mobilityScans) {
      double[][] mzIntensity = massDetector.getMassValues(mobilityScan, massDetectorParameters);
      mobilityScanPeaks.add(mzIntensity);
    }

    final int[] offsets = generateOffsets(mobilityScanPeaks);
    final int numDp =
        offsets[offsets.length - 1] + mobilityScanPeaks.get(mobilityScanPeaks.size() - 1)[0].length;

    // put all values into one array
    double[] allMzOrIntensity = new double[numDp];
    putAllValuesIntoOneArray(mobilityScanPeaks, 0, allMzOrIntensity);

    if (storage != null) {
      try {
        mobilityScanMzBuffer = storage.storeData(allMzOrIntensity);
      } catch (IOException e) {
        logger.log(Level.SEVERE, e.getMessage(), e);
        mobilityScanMzBuffer = DoubleBuffer.wrap(allMzOrIntensity);
        allMzOrIntensity = new double[numDp];
      }
    } else {
      mobilityScanMzBuffer = DoubleBuffer.wrap(allMzOrIntensity);
      allMzOrIntensity = new double[numDp];
    }

    int[] basePeakIndices = putAllValuesIntoOneArray(mobilityScanPeaks, 1, allMzOrIntensity);
    if (storage != null) {
      try {
        mobilityScanIntensityBuffer = storage.storeData(allMzOrIntensity);
      } catch (IOException e) {
        logger.log(Level.SEVERE, e.getMessage(), e);
        mobilityScanIntensityBuffer = DoubleBuffer.wrap(allMzOrIntensity);
      }
    } else {
      mobilityScanIntensityBuffer = DoubleBuffer.wrap(allMzOrIntensity);
    }

    for (int i = 0, mobilityScansSize = mobilityScans.size(); i < mobilityScansSize; i++) {
      MobilityScan mobilityScan = mobilityScans.get(i);
      mobilityScan.addMassList(new MobilityScanMassList(offsets[i],
          mobilityScanPeaks.get(i)[0].length, basePeakIndices[i], this));

      if (mobilityScanPeaks.get(i)[0].length > maxMobilityScanDatapoints) {
        maxMobilityScanDatapoints = mobilityScanPeaks.get(i)[0].length;
      }
    }
  }

  void getMobilityScanMzValues(MobilityScanMassList massList, double[] dst) {
    assert massList.getNumberOfDataPoints() <= dst.length;
    mobilityScanMzBuffer.get(massList.getStorageOffset(), dst, 0, massList.getNumberOfDataPoints());
  }

  void getMobilityScanIntensityValues(MobilityScanMassList massList, double[] dst) {
    assert massList.getNumberOfDataPoints() <= dst.length;
    mobilityScanIntensityBuffer
        .get(massList.getStorageOffset(), dst, 0, massList.getNumberOfDataPoints());
  }

  double getMobilityScanMzValue(MobilityScanMassList massList, int index) {
    assert index < massList.getNumberOfDataPoints();
    return mobilityScanMzBuffer.get(massList.getStorageOffset() + index);
  }

  double getMobilityScanIntensityValue(MobilityScanMassList massList, int index) {
    assert index < massList.getNumberOfDataPoints();
    return mobilityScanIntensityBuffer.get(massList.getStorageOffset() + index);
  }

  /**
   *
   * @return The maximum number of data points in a mobility scan mass list
   */
  public int getMaxMobilityScanDatapoints() {
    return maxMobilityScanDatapoints;
  }

}
