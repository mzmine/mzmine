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

package io.github.mzmine.datamodel.data_access;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.featuredata.IntensitySeries;
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.MobilitySeries;
import io.github.mzmine.datamodel.featuredata.impl.SummedIntensityMobilitySeries;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder.IonMobilityTraceBuilderModule;
import io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder.IonMobilityTraceBuilderParameters;
import io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder.OptionalImsTraceBuilderParameters;
import io.github.mzmine.modules.dataprocessing.featdet_mobilogram_summing.MobilogramBinningModule;
import io.github.mzmine.modules.dataprocessing.featdet_mobilogram_summing.MobilogramBinningParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.IonMobilityUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Used to efficiently access mobilogram data of a raw data file. The data can be binned by mobility
 * to generate less noisy mobilograms.
 */
public class BinningMobilogramDataAccess implements IntensitySeries, MobilitySeries {

  private static Logger logger = Logger.getLogger(BinningMobilogramDataAccess.class.getName());

  private final IMSRawDataFile dataFile;

  private final double[] intensities;
  private final double[] mobilities;
  private final double[] tempMobilities;
  private final double[] tempIntensities;
  private final double binWidth;

  public BinningMobilogramDataAccess(@Nonnull final IMSRawDataFile rawDataFile,
      final double binningWidth) {
    this.dataFile = rawDataFile;
    final Double maxTic = rawDataFile.getDataMaxTotalIonCurrent(1);
    assert maxTic != null && !maxTic.isNaN();

    // get the then most intense frames, even if empty scans have been removed, we should at least
    // be able to find the smallest mobility data.
    final List<Frame> frames = rawDataFile.getFrames(1).stream()
        .filter(frame -> frame.getTIC() >= maxTic * 0.8).limit(10).collect(
            Collectors.toList());

    double delta = frames.stream()
        .mapToDouble(IonMobilityUtils::getSmallestMobilityDelta).min().orElse(-1d);
    assert Double.compare(-1, delta) != 0;
    final double smallestDelta = delta - delta * 1E-10;

    if (smallestDelta > binningWidth) {
      logger.info(() ->
          "The requested binning width is smaller than the resolution of the supplied data in "
              + rawDataFile.getName() + ". Bin width will be adjusted to " + smallestDelta + ".");
      this.binWidth = smallestDelta;
    } else {
      this.binWidth = binningWidth;
    }

    final RangeMap<Double, Double> mobilityToIntensity = TreeRangeMap.create();
    final Map<Range<Double>, Double> mapOfRanges;

    // make a range map that contains all mobility values we could find
    // we need to know how many different mobility values there are to be able to store all
    // mobilograms later on.
    for (final Frame frame : frames) {
      double[] mobilities = DataPointUtils.getDoubleBufferAsArray(frame.getMobilities());
      for (int i = 0; i < mobilities.length; i++) {
        Entry<Range<Double>, Double> entry = mobilityToIntensity.getEntry(mobilities[i]);
        if (entry == null) {
          mobilityToIntensity.put(Range
                  .open(mobilities[i] - smallestDelta / 2, mobilities[i] + smallestDelta / 2),
              mobilities[i]);
        }
      }
    }

    mapOfRanges = mobilityToIntensity.asMapOfRanges();
    final int numEntries = mapOfRanges.size();
    // out temp values need to be able to fit the data in any case.
    tempMobilities = new double[numEntries];
    tempIntensities = new double[numEntries];

    // now bin the mobility values together in the requested width
    final List<Double> binnedValues = new ArrayList<>();
    double currentBinLimit = -1E10;
    for (Entry<Range<Double>, Double> entry : mapOfRanges.entrySet()) {
      final Double mobility = entry.getValue();
      if (Double.compare(mobility - this.binWidth / 2, currentBinLimit) == 1) {
        binnedValues.add(mobility + this.binWidth / 2); // add the center of the new bin
        currentBinLimit = mobility + this.binWidth / 2;
      }
    }

    mobilities = binnedValues.stream().mapToDouble(Double::doubleValue).toArray();
    intensities = new double[mobilities.length];
  }

  private void clearIntensities() {
    Arrays.fill(intensities, 0d);
  }

  /**
   * Note that re-binning an already binned mobilogram with a lower binning width than before will
   * lead to 0-intensity values. Consider using {@link #setMobilogram(List)} instead.
   *
   * @param mobilityValues   the mobility values to be binned.
   * @param intensitiyValues the intensity values to be binned.
   */
  public void setMobilogram(@Nonnull final double[] mobilityValues,
      @Nonnull final double[] intensitiyValues) {

    final int numValues = intensitiyValues.length;
    assert numValues <= tempIntensities.length;
    assert mobilityValues.length == intensitiyValues.length;

    int order = 1;
    if (mobilityValues.length > 1) {
      if (mobilityValues[0] > mobilityValues[1]) {
        order = -1;
      }
    }

    // mobilities may be sorted in decreasing order
    final int start = order == 1 ? 0 : numValues - 1;
    int rawIndex = start;

    for (int binnedIndex = 0;
        binnedIndex < intensities.length && rawIndex < numValues && rawIndex >= 0;
        binnedIndex++) {
      // ensure we are above the current lower-binning-limit
      while (rawIndex < numValues && rawIndex >= 0
          && Double.compare(tempMobilities[rawIndex], mobilities[binnedIndex] - binWidth / 2)
          == -1) {
        rawIndex += order;
      }

      // ensure we are below the current upper-binning-limit
      while (rawIndex < numValues && rawIndex >= 0
          && Double.compare(tempMobilities[rawIndex], mobilities[binnedIndex] + binWidth / 2)
          == -1) {
        intensities[binnedIndex] += tempIntensities[rawIndex];
        rawIndex += order;
      }
    }
  }

  /**
   * Re-bins an already summed mobilogram. Note that re-binning an already binned mobilogram with a
   * lower binnign width than before will lead to 0-intensity values. Consider using {@link
   * #setMobilogram(List)} instead.
   *
   * @param summedMobilogram
   */
  public void setMobilogram(@Nonnull final SummedIntensityMobilitySeries summedMobilogram) {
    clearIntensities();

    final int numValues = summedMobilogram.getNumberOfValues();
    assert numValues <= tempIntensities.length;

    summedMobilogram.getIntensityValues(tempIntensities);
    summedMobilogram.getMobilityValues(tempMobilities);

    int rawIndex = 0;
    for (int binnedIndex = 0; binnedIndex < intensities.length && rawIndex < numValues;
        binnedIndex++) {
      // ensure we are above the current lower-binning-limit
      while (rawIndex < numValues
          && Double.compare(tempMobilities[rawIndex], mobilities[binnedIndex] - binWidth / 2)
          == -1) {
        rawIndex++;
      }

      // ensure we are below the current upper-binning-limit
      while (rawIndex < numValues &&
          Double.compare(tempMobilities[rawIndex], mobilities[binnedIndex] + binWidth / 2) == -1) {
        intensities[binnedIndex] += tempIntensities[rawIndex];
        rawIndex++;
      }
    }
  }

  /**
   * Constructs a binned summed mobilogram from the supplied list of individual mobilograms.
   *
   * @param mobilograms The list of {@link IonMobilitySeries}.
   */
  public void setMobilogram(@Nonnull final List<IonMobilitySeries> mobilograms) {
    clearIntensities();

    int order = 1;
    if (!mobilograms.isEmpty()) {
      order = mobilograms.get(0).getSpectrum(0).getFrame().getMobilityType()
          == MobilityType.TIMS ? -1 : +1;
    }

    for (IonMobilitySeries ims : mobilograms) {
      final int numValues = ims.getNumberOfValues();
      ims.getIntensityValues(tempIntensities);

      for (int i = 0; i < numValues; i++) {
        tempMobilities[i] = ims.getMobility(i);
      }

      // in tims, the mobilograms are sorted by decreasing order
      final int start = order == 1 ? 0 : numValues - 1;
      int rawIndex = start;

      for (int binnedIndex = 0;
          binnedIndex < intensities.length && rawIndex < numValues && rawIndex >= 0;
          binnedIndex++) {
        // ensure we are above the current lower-binning-limit
        while (rawIndex < numValues && rawIndex >= 0
            && Double.compare(tempMobilities[rawIndex], mobilities[binnedIndex] - binWidth / 2)
            == -1) {
          rawIndex += order;
        }

        // ensure we are below the current upper-binning-limit
        while (rawIndex < numValues && rawIndex >= 0
            && Double.compare(tempMobilities[rawIndex], mobilities[binnedIndex] + binWidth / 2)
            == -1) {
          intensities[binnedIndex] += tempIntensities[rawIndex];
          rawIndex += order;
        }
      }
    }
  }

  public SummedIntensityMobilitySeries toSummedMobilogram(@Nullable MemoryMapStorage storage) {
    // todo: maybe remove all but flanking zeros here?
    return new SummedIntensityMobilitySeries(storage,
        Arrays.copyOf(mobilities, mobilities.length),
        Arrays.copyOf(intensities, intensities.length));
  }

  @Override
  public DoubleBuffer getIntensityValues() {
    throw new UnsupportedOperationException(
        "This data access is designed to loop over intensities/mobilities.");
  }

  @Override
  public double[] getIntensityValues(double[] dst) {
    throw new UnsupportedOperationException(
        "This data access is designed to loop over intensities/mobilities.");
  }

  @Override
  public double getIntensity(int index) {
    return intensities[index];
  }

  @Override
  public int getNumberOfValues() {
    return intensities.length;
  }

  @Override
  public double getMobility(int index) {
    return mobilities[index];
  }

  public IMSRawDataFile getDataFile() {
    return dataFile;
  }

  public double getBinWidth() {
    return binWidth;
  }

  public static Double getPreviousBinningWith(@Nonnull final ModularFeatureList flist,
      MobilityType mt) {
    List<FeatureListAppliedMethod> methods = flist.getAppliedMethods();

    Double binWidth = null;
    for (int i = methods.size() - 1; i >= 0; i--) {
      FeatureListAppliedMethod method = methods.get(i);
      if (method.getModule()
          .equals(MZmineCore.getModuleInstance(IonMobilityTraceBuilderModule.class))) {
        final ParameterSet parameterSet = method.getParameters();
        final var advancedParam = parameterSet
            .getParameter(IonMobilityTraceBuilderParameters.advancedParameters).getValue();
        binWidth = switch (mt) {
          case TIMS ->
              advancedParam.getParameter(OptionalImsTraceBuilderParameters.timsBinningWidth)
                  .getValue() ? advancedParam
                  .getParameter(OptionalImsTraceBuilderParameters.timsBinningWidth)
                  .getEmbeddedParameter().getValue()
                  : OptionalImsTraceBuilderParameters.DEFAULT_TIMS_BIN_WIDTH;
          case DRIFT_TUBE ->
              advancedParam.getParameter(OptionalImsTraceBuilderParameters.dtimsBinningWidth)
                  .getValue() ? advancedParam
                  .getParameter(OptionalImsTraceBuilderParameters.dtimsBinningWidth)
                  .getEmbeddedParameter().getValue()
                  : OptionalImsTraceBuilderParameters.DEFAULT_DTIMS_BIN_WIDTH;
          case TRAVELING_WAVE ->
              advancedParam.getParameter(OptionalImsTraceBuilderParameters.twimsBinningWidth)
                  .getValue() ? advancedParam
                  .getParameter(OptionalImsTraceBuilderParameters.twimsBinningWidth)
                  .getEmbeddedParameter().getValue()
                  : OptionalImsTraceBuilderParameters.DEFAULT_TWIMS_BIN_WIDTH;
          default -> null;
        };
        break;
      }

      if (method.getModule().equals(MZmineCore.getModuleInstance(MobilogramBinningModule.class))) {
        final ParameterSet parameterSet = method.getParameters();
        binWidth = switch (mt) {
          case TIMS -> parameterSet.getParameter(MobilogramBinningParameters.timsBinningWidth)
              .getValue();
          case DRIFT_TUBE -> parameterSet
              .getParameter(MobilogramBinningParameters.dtimsBinningWidth).getValue();
          case TRAVELING_WAVE -> parameterSet
              .getParameter(MobilogramBinningParameters.twimsBinningWidth).getValue();
          default -> null;
        };
        break;
      }
    }
    if (binWidth == null) {
      logger.info(
          () -> "Previous binning width not recognised. Has the mobility type been implemented?");
    }
    return binWidth;
  }
}
