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

package io.github.mzmine.modules.dataprocessing.featdet_mobilogram_interpolation;

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data_access.SummedMobilogramDataAccess;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.SummedIntensityMobilitySeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.FeatureDataType;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleFunction;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MobilogramInterpolationTask extends AbstractTask {

  private static Logger logger = Logger.getLogger(MobilogramInterpolationTask.class.getName());

  private final ModularFeatureList originalFeatureList;
  private final ParameterSet parameters;
  private final String suffix;
  private final boolean createNewFlist;
  private final MZmineProject project;
  private final int windowWidth;
  private final int minNumIntensities;
  private final int filterWidth;
  private long totalFeatures = 1;
  private AtomicLong processedFeatures = new AtomicLong(0);


  public MobilogramInterpolationTask(@Nullable MemoryMapStorage storage,
      @Nonnull final ModularFeatureList originalFeatureList,
      @Nonnull final ParameterSet parameters,
      @Nonnull final MZmineProject project) {
    super(storage);
    this.parameters = parameters;
    this.originalFeatureList = originalFeatureList;
    suffix = parameters.getParameter(MobilogramInterpolationParameters.suffix).getValue();
    createNewFlist = parameters.getParameter(MobilogramInterpolationParameters.createNewFeatureList)
        .getValue();
    windowWidth = parameters.getParameter(MobilogramInterpolationParameters.windowWidth).getValue();
    minNumIntensities = parameters.getParameter(MobilogramInterpolationParameters.numIntensities)
        .getValue();
    filterWidth = parameters.getParameter(MobilogramInterpolationParameters.filterWidth).getValue();
    this.project = project;
  }

  public static boolean isEligible(@Nonnull final SummedMobilogramDataAccess summedAccess,
      final int searchStart, final int searchEnd, final int windowWidth,
      final int minNumIntensityInWindow) {

    assert searchStart + searchEnd >= windowWidth;
    assert windowWidth < summedAccess.getNumberOfValues();
    assert minNumIntensityInWindow <= windowWidth;

    final CountingBooleanStack hasIntensityStack = new CountingBooleanStack(windowWidth);

    for (int i = searchStart; i < searchEnd; i++) {
      // check if we meet the requirements
      hasIntensityStack.push(Double.compare(summedAccess.getIntensity(i), 0d) == 1);
      if (hasIntensityStack.size() < windowWidth) {
        continue;
      }

      if (hasIntensityStack.getNumTrue() >= minNumIntensityInWindow) {
        return true;
      }
    }
    return false;
  }

  public static boolean isEligible(@Nonnull final SummedMobilogramDataAccess summedAccess,
      final int windowWidth, final int minNumIntensityInWindow) {
    return isEligible(summedAccess, 0, summedAccess.getNumberOfValues(), windowWidth,
        minNumIntensityInWindow);
  }

  public static List<Range<Integer>> getEligibleRanges(
      @Nonnull final SummedMobilogramDataAccess summedAccess,
      final int windowWidth, final int minNumIntensityInWindow) {
    assert windowWidth < summedAccess.getNumberOfValues();
    assert minNumIntensityInWindow <= windowWidth;

    final CountingBooleanStack hasIntensityStack = new CountingBooleanStack(windowWidth);

    final RangeSet<Integer> ranges = TreeRangeSet.create();
    Integer currentRangeStart = null;

    for (int i = 0; i < summedAccess.getNumberOfValues(); i++) {
      hasIntensityStack.push(Double.compare(summedAccess.getIntensity(i), 0d) == 1);
      if (hasIntensityStack.size() < windowWidth) {
        continue;
      }

      // check if we meet the requirements
      if (hasIntensityStack.getNumTrue() >= minNumIntensityInWindow) {
        // if the requirements are met, and no range is currently processed, we start a range
        if (currentRangeStart == null) {
          currentRangeStart = Math.max(i - windowWidth, 0);
        }
      } else {
        // if we were processing a range, end now, since we don't meet the criteria anymore.
        if (currentRangeStart != null) {
          ranges.add(Range.closed(currentRangeStart, i));
          currentRangeStart = null;
        }
      }
    }

    // if we were working on a range, add it now
    if (currentRangeStart != null) {
      ranges.add(Range.closed(currentRangeStart, summedAccess.getNumberOfValues() - 1));
    }

    return new ArrayList<>(ranges.asRanges());
  }

  public static double[][] process(
      @Nonnull final SummedMobilogramDataAccess dataAccess,
      @Nonnull final List<Range<Integer>> ranges, final int filterWidth) {

    final double[] newMobilities = new double[dataAccess.getNumberOfValues()];
    final double[] newIntensities = new double[dataAccess.getNumberOfValues()];

    int currentRangeIndex = 0;
    int newValuesIndex = 0;

    int lastNonZeroIndex = 0;
    int nextNonZeroIndex = -1;
    DoubleFunction<Double> interpolationFuncton = null; // interpolate between two points

    Range<Integer> currentRange = ranges.get(currentRangeIndex);
    for (int i = 0; i < dataAccess.getNumberOfValues(); i++) {
      final double currentIntensity = dataAccess.getIntensity(i);

      // if intensity is > 0, we won't edit it.
      if (currentIntensity > 0d) {
        newMobilities[newValuesIndex] = dataAccess.getMobility(i);
        newIntensities[newValuesIndex] = dataAccess.getIntensity(i);
        newValuesIndex++;
        lastNonZeroIndex = i;
        continue;
      } else if (Double.compare(dataAccess.getIntensity(Math.max(0, i - 1)), 0d) == 0 && Double
          .compare(dataAccess.getIntensity(Math.min(i + 1, dataAccess.getNumberOfValues() - 1)), 0d)
          == 0) {
        // if the previuos and the next intensity are zero, we don't have to store that.
        continue;
      }

      // check if we need to pick a new range
      if (currentRange.upperEndpoint() < i && currentRangeIndex + 1 < ranges.size()) {
        currentRangeIndex++;
        currentRange = ranges.get(currentRangeIndex);
      }

      // if we are out out bounds of the interpolation, reset
      if (i >= nextNonZeroIndex) {
        nextNonZeroIndex = -1;
        interpolationFuncton = null;
      }

      // if the current intensity is 0 and if we are in a eligible range, we try to linearly interpolate
      if (currentRange.contains(i) && Double.compare(0d, currentIntensity) == 0
          && interpolationFuncton == null) {
        nextNonZeroIndex = getNextNonZeroIndex(dataAccess, i + 1, filterWidth);

        // if there is a non-zero value within the filter width, we make a function
        if (nextNonZeroIndex != -1) {
          interpolationFuncton = lineBetweenPoints(dataAccess.getMobility(lastNonZeroIndex),
              dataAccess.getIntensity(lastNonZeroIndex), dataAccess.getMobility(nextNonZeroIndex),
              dataAccess.getIntensity(nextNonZeroIndex));
        }
      }

      // no matter what, we add a value here.
      newMobilities[newValuesIndex] = dataAccess.getMobility(i);
      if (interpolationFuncton != null) {
        newIntensities[newValuesIndex] = interpolationFuncton.apply(dataAccess.getMobility(i));
      } else {
        newIntensities[newValuesIndex] = 0d;
      }
      newValuesIndex++;
    }

    return new double[][]{
        Arrays.copyOfRange(newMobilities, 0, newValuesIndex - 1),
        Arrays.copyOfRange(newIntensities, 0, newValuesIndex - 1)
    };
  }

  private static int getNextNonZeroIndex(SummedMobilogramDataAccess dataAccess, int startIndex,
      int filterWidth) {
    for (int i = startIndex; i <= startIndex + filterWidth && i < dataAccess.getNumberOfValues();
        i++) {
      if (dataAccess.getIntensity(i) > 0d) {
        return i;
      }
    }
    return -1;
  }

  private static DoubleFunction<Double> lineBetweenPoints(final double x1, final double y1,
      final double x2, final double y2) {
    // y = mx + b
    // => m = dy/dx
    // => b = y1 - mx1 = y1 - dy/dx*x1
    // <=> y = dy/dx * x + y1 - dy/dx * x1
    // <=> y = dy/dx (x - x1) + y1
    return value -> (y2 - y1) / (x2 - x1) * (value - x1) + y1;
  }

  @Override
  public String getTaskDescription() {
    return originalFeatureList.getName() + ": Processing mobilogram " + processedFeatures.get()
        + "/" + totalFeatures;
  }

  @Override
  public double getFinishedPercentage() {
    return processedFeatures.get() / (double) totalFeatures;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    final ModularFeatureList flist = createNewFlist ? originalFeatureList
        .createCopy(originalFeatureList.getName() + " " + suffix, getMemoryMapStorage())
        : originalFeatureList;

    totalFeatures = flist.streamFeatures().count();

    for (RawDataFile file : flist.getRawDataFiles()) {
      if (!(file instanceof IMSRawDataFile)) {
        return;
      }

      final List<ModularFeature> features = (List<ModularFeature>) (List<? extends Feature>) flist
          .getFeatures(file);
      final SummedMobilogramDataAccess summedAccess = new SummedMobilogramDataAccess(
          (IMSRawDataFile) file, 0d);

      for (ModularFeature feature : features) {
        processedFeatures.getAndIncrement();
        if (!(feature.getFeatureData() instanceof IonMobilogramTimeSeries series)) {
          continue;
        }

        summedAccess.setMobilogram(series.getSummedMobilogram());

        if (!isEligible(summedAccess, windowWidth, minNumIntensities)) {
          continue;
        }
        List<Range<Integer>> ranges = getEligibleRanges(summedAccess, windowWidth,
            minNumIntensities);

        if (ranges.isEmpty()) {
          logger.info("Mobilogram was eligible but no range was found.");
          continue;
        }

        final double[][] processed = process(summedAccess, ranges, filterWidth);
        final SummedIntensityMobilitySeries mobilogram = new SummedIntensityMobilitySeries(
            flist.getMemoryMapStorage(), processed[0],
            processed[1], feature.getMZ());
        feature.set(
            FeatureDataType.class, series.copyAndReplace(flist.getMemoryMapStorage(), mobilogram));
      }
    }

    flist.getAppliedMethods()
        .add(new SimpleFeatureListAppliedMethod(MobilogramInterpolationModule.class, parameters));
    if (createNewFlist) {
      project.addFeatureList(flist);
    }
    setStatus(TaskStatus.FINISHED);
  }
}
