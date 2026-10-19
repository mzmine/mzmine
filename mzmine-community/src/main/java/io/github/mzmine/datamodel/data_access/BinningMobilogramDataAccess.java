/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.datamodel.data_access;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.featuredata.IntensitySeries;
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.MobilitySeries;
import io.github.mzmine.datamodel.featuredata.impl.SummedIntensityMobilitySeries;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_imsexpander.ImsExpanderModule;
import io.github.mzmine.modules.dataprocessing.featdet_imsexpander.ImsExpanderParameters;
import io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder.AdvancedImsTraceBuilderParameters;
import io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder.IonMobilityTraceBuilderModule;
import io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder.IonMobilityTraceBuilderParameters;
import io.github.mzmine.modules.dataprocessing.featdet_mobilogram_summing.MobilogramBinningModule;
import io.github.mzmine.modules.dataprocessing.featdet_mobilogram_summing.MobilogramBinningParameters;
import io.github.mzmine.modules.dataprocessing.featdet_recursiveimsbuilder.RecursiveIMSBuilderAdvancedParameters;
import io.github.mzmine.modules.dataprocessing.featdet_recursiveimsbuilder.RecursiveIMSBuilderModule;
import io.github.mzmine.modules.dataprocessing.featdet_recursiveimsbuilder.RecursiveIMSBuilderParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.IonMobilityUtils;
import io.github.mzmine.util.MemoryMapStorage;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleImmutableList;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Used to efficiently access mobilogram data of a raw data file. The data can be binned by mobility
 * to generate less noisy mobilograms.
 *
 * @author https://github.com/SteffenHeu
 */
public class BinningMobilogramDataAccess implements IntensitySeries, MobilitySeries {

  private static final double MOBILITY_EPSILON = 0.00001;

  private static final Logger logger = Logger.getLogger(
      BinningMobilogramDataAccess.class.getName());

  private final IMSRawDataFile dataFile;

  private final double[] intensities;
  private final double[] mobilities;
  private final double[] upperBinLimits;
  private final int binWidth;

  // not final: grown on demand if a summed mobilogram with more values than expected is set
  private double[] tempMobilities;
  private double[] tempIntensities;

  private final double approximateBinSize;

  public BinningMobilogramDataAccess(@NotNull final IMSRawDataFile rawDataFile,
      final int binWidth) {
    if (binWidth < 1) {
      throw new IllegalArgumentException("Illegal bin width (" + binWidth + ")");
    }
    dataFile = rawDataFile;
    this.binWidth = binWidth;

    // multiple mobility ranges are possible in tims. in MALDI acquisitions every single frame may
    // have its own mobility segment, so anything sized by the number of segments has to be avoided.
    final Map<Frame, Range<Double>> ranges = IonMobilityUtils.getUniqueMobilityRanges(rawDataFile);
    final int maxMobilityScans = rawDataFile.getFrames().stream()
        .mapToInt(Frame::getNumberOfMobilityScans).max().orElseThrow();

    final MobilityType mt = rawDataFile.getMobilityType();

    // decision: process the segments starting at the outer end of the mobility axis. The filter
    // below only ever extends the collected values in one direction, so starting anywhere else
    // would silently discard everything a later segment adds beyond the first segment's outer limit.
    // tims mobilities descend with mobility scan number, all other types ascend.
    final List<Frame> orderedFrames = new ArrayList<>(ranges.keySet());
    orderedFrames.sort(mt == MobilityType.TIMS //
        ? Comparator.comparingDouble((Frame f) -> f.getMobilityRange().upperEndpoint()).reversed()
        : Comparator.comparingDouble((Frame f) -> f.getMobilityRange().lowerEndpoint()));

    final DoubleArrayList distinctMobilities = new DoubleArrayList(
        orderedFrames.getFirst().getMobilities().size());
    for (final Frame frame : orderedFrames) {
      final DoubleImmutableList frameMobilities = frame.getMobilities();
      if (frameMobilities == null) {
        continue;
      }
      for (int i = 0; i < frameMobilities.size(); i++) {
        final double mobility = frameMobilities.getDouble(i);
        if (!distinctMobilities.isEmpty()
            // either not tims and current mobility > highest mobility
            && ((distinctMobilities.getDouble(distinctMobilities.size() - 1) > mobility
            && mt != MobilityType.TIMS)
            // or tims and current mobility < lowest mobility
            || (distinctMobilities.getDouble(distinctMobilities.size() - 1) < mobility
            && mt == MobilityType.TIMS))) {
          continue;
        }
        distinctMobilities.add(mobility);
      }
    }

    // the segment order above guarantees that these span every mobility range of the file: the first
    // segment contributes all of its values, so the outer limit is the outermost of all segments, and
    // every later segment extends the opposite end. No value of any frame can fall outside all bins.
    final double[] sortedMobilities = distinctMobilities.toDoubleArray();
    Arrays.sort(sortedMobilities);

    final int numBins = (sortedMobilities.length + binWidth - 1) / binWidth;
    mobilities = new double[numBins];
    upperBinLimits = new double[numBins];
    for (int bin = 0; bin < numBins; bin++) {
      final int i = bin * binWidth;

      int currentBins = 0;
      double summedMobility = 0d;
      for (int j = 0; i + j < sortedMobilities.length && j < binWidth; j++) {
        summedMobility += sortedMobilities[i + j];
        currentBins++;
      }
      mobilities[bin] = summedMobility / Math.max(currentBins, 1);
      upperBinLimits[bin] =
          sortedMobilities[Math.min(i + binWidth - 1, sortedMobilities.length - 1)]
              + MOBILITY_EPSILON;
    }

    intensities = new double[mobilities.length];

    // the temp arrays only ever hold a single mobilogram (one value per mobility scan of one frame)
    // or a single summed mobilogram (one value per bin at bin width 1). They are grown on demand in
    // setMobilogram, so an underestimate here is safe.
    final int tempSize = Math.max(maxMobilityScans, sortedMobilities.length);
    tempIntensities = new double[tempSize];
    tempMobilities = new double[tempSize];

    double previous = mobilities[0];
    double deltas = 0;
    for (int i = 1; i < mobilities.length; i++) {
      deltas += mobilities[i] - previous;
      previous = mobilities[i];
    }

    approximateBinSize = deltas / (mobilities.length - 2); // - 2 to slightly overestimate
    logger.finest(
        () -> "Bin width set to " + binWidth + " scans. (approximately " + approximateBinSize + " "
            + rawDataFile.getMobilityType().getUnit() + ")");
  }

  /**
   * Creates a new data access with the last parameters for binning etc
   */
  public static BinningMobilogramDataAccess createWithPreviousParameters(
      @NotNull IMSRawDataFile imsFile, @NotNull FeatureList flist) {
    if (!flist.getRawDataFiles().contains(imsFile)) {
      throw new IllegalArgumentException("FeatureList flist does not contain data from imsFile");
    }
    // is never null at this point as we are having at least one imsFile
    final Integer previousBinningWidth = getPreviousBinningWidth(flist, imsFile.getMobilityType());
    return new BinningMobilogramDataAccess(imsFile, previousBinningWidth);
  }

  @NotNull
  public static int getRecommendedBinWidth(IMSRawDataFile file) {
    // timsTOF data can be empty in the early scans, so we use one from the middle
    final Frame frame = file.getFrame(file.getNumberOfFrames() / 2);
    return switch (frame.getMobilityType()) {
      case NONE, DRIFT_TUBE, TRAVELING_WAVE, FAIMS, MIXED, OTHER -> 1;
      case SLIM -> 10;
      case TIMS -> {
        final int index = frame.getNumberOfMobilityScans() / 2;
        final double mob1 = frame.getMobilityScan(index).getMobility();
        final double mob2 = frame.getMobilityScan(index + 1).getMobility();
        final double delta = Math.abs(mob1 - mob2);
        yield (int) Math.max(1d, 0.0008 / delta) * 2;
      }
    };
  }

  @Nullable
  public static Integer getPreviousBinningWidth(@NotNull final FeatureList flist, MobilityType mt) {
    List<FeatureListAppliedMethod> methods = flist.getAppliedMethods();
    final IMSRawDataFile imsFile = flist.getRawDataFiles().stream()
        .filter(IMSRawDataFile.class::isInstance).map(IMSRawDataFile.class::cast).findFirst()
        .orElse(null);

    if (imsFile == null) {
      logger.warning(
          "Did not find IMS raw data file in feature list. This should not be the case in getPreviousBinningWith");
      return null;
    }

    Integer binWidth = null;
    for (int i = methods.size() - 1; i >= 0; i--) {
      FeatureListAppliedMethod method = methods.get(i);
      if (method.getModule()
          .equals(MZmineCore.getModuleInstance(IonMobilityTraceBuilderModule.class))) {
        final ParameterSet parameterSet = method.getParameters();
        final var advancedParam = parameterSet.getParameter(
            IonMobilityTraceBuilderParameters.advancedParameters).getValue();
        binWidth = switch (mt) {
          case NONE, MIXED, OTHER, FAIMS -> null;
          case TIMS ->
              advancedParam.getParameter(AdvancedImsTraceBuilderParameters.timsBinningWidth)
                  .getValue() ? advancedParam.getParameter(
                  AdvancedImsTraceBuilderParameters.timsBinningWidth).getEmbeddedParameter()
                                              .getValue() : getRecommendedBinWidth(imsFile);
          case DRIFT_TUBE ->
              advancedParam.getParameter(AdvancedImsTraceBuilderParameters.dtimsBinningWidth)
                  .getValue() ? advancedParam.getParameter(
                  AdvancedImsTraceBuilderParameters.dtimsBinningWidth).getEmbeddedParameter()
                                              .getValue() : getRecommendedBinWidth(imsFile);
          case TRAVELING_WAVE, SLIM ->
              advancedParam.getParameter(AdvancedImsTraceBuilderParameters.twimsBinningWidth)
                  .getValue() ? advancedParam.getParameter(
                  AdvancedImsTraceBuilderParameters.twimsBinningWidth).getEmbeddedParameter()
                                              .getValue() : getRecommendedBinWidth(imsFile);
        };
        break;
      }

      if (method.getModule()
          .equals(MZmineCore.getModuleInstance(RecursiveIMSBuilderModule.class))) {
        final ParameterSet parameterSet = method.getParameters();
        final var advancedParam = parameterSet.getParameter(
            RecursiveIMSBuilderParameters.advancedParameters).getValue();
        binWidth = switch (mt) {
          case NONE, MIXED, OTHER, FAIMS -> null;
          case TIMS ->
              advancedParam.getParameter(RecursiveIMSBuilderAdvancedParameters.timsBinningWidth)
                  .getValue() ? advancedParam.getParameter(
                  RecursiveIMSBuilderAdvancedParameters.timsBinningWidth).getEmbeddedParameter()
                                              .getValue() : getRecommendedBinWidth(imsFile);
          case DRIFT_TUBE ->
              advancedParam.getParameter(RecursiveIMSBuilderAdvancedParameters.dtimsBinningWidth)
                  .getValue() ? advancedParam.getParameter(
                  RecursiveIMSBuilderAdvancedParameters.dtimsBinningWidth).getEmbeddedParameter()
                                              .getValue() : getRecommendedBinWidth(imsFile);
          case TRAVELING_WAVE, SLIM ->
              advancedParam.getParameter(RecursiveIMSBuilderAdvancedParameters.twimsBinningWidth)
                  .getValue() ? advancedParam.getParameter(
                  RecursiveIMSBuilderAdvancedParameters.twimsBinningWidth).getEmbeddedParameter()
                                              .getValue() : getRecommendedBinWidth(imsFile);
        };
        break;
      }

      if (method.getModule().equals(MZmineCore.getModuleInstance(MobilogramBinningModule.class))) {
        final ParameterSet parameterSet = method.getParameters();
        binWidth = switch (mt) {
          case NONE, FAIMS, OTHER, MIXED -> null;
          case TIMS ->
              parameterSet.getParameter(MobilogramBinningParameters.timsBinningWidth).getValue();
          case DRIFT_TUBE ->
              parameterSet.getParameter(MobilogramBinningParameters.dtimsBinningWidth).getValue();
          case TRAVELING_WAVE, SLIM ->
              parameterSet.getParameter(MobilogramBinningParameters.twimsBinningWidth).getValue();
        };
        break;
      }

      if (method.getModule().equals(MZmineCore.getModuleInstance(ImsExpanderModule.class))) {
        final ParameterSet parameterSet = method.getParameters();
        binWidth = parameterSet.getParameter(ImsExpanderParameters.mobilogramBinWidth).getValue()
            ? parameterSet.getParameter(ImsExpanderParameters.mobilogramBinWidth)
              .getEmbeddedParameter().getValue() : getRecommendedBinWidth(imsFile);
        break;
      }
    }
    if (binWidth == null) {
      logger.info(
          () -> "Previous binning width not recognised. Has the mobility type been implemented?");
      binWidth = getRecommendedBinWidth(imsFile);
    }
    return binWidth;
  }

  private void clearIntensities() {
    Arrays.fill(intensities, 0d);
  }

  /**
   * Ensures the temp buffers can hold {@code numValues}. The copy methods of
   * {@link IntensitySeries} and {@link MobilitySeries} silently allocate a new array if the
   * destination is too small and return it, so without this the buffers would keep stale data.
   */
  private void ensureTempCapacity(final int numValues) {
    if (tempIntensities.length < numValues) {
      tempIntensities = new double[numValues];
      tempMobilities = new double[numValues];
    }
  }

  /**
   * Re-bins an already summed mobilogram. Note that re-binning an already binned mobilogram with a
   * lower binnign width than before will lead to 0-intensity values. Consider using
   * {@link #setMobilogram(List)} instead.
   *
   * @param summedMobilogram
   */
  public void setMobilogram(@NotNull final SummedIntensityMobilitySeries summedMobilogram) {
    clearIntensities();

    final int numValues = summedMobilogram.getNumberOfValues();
    ensureTempCapacity(numValues);

    summedMobilogram.getIntensityValues(tempIntensities);
    summedMobilogram.getMobilityValues(tempMobilities);

    int rawIndex = 0;
    for (int binnedIndex = 0; binnedIndex < intensities.length && rawIndex < numValues;
        binnedIndex++) {
      // ensure we are above the current lower-binning-limit
      while (rawIndex < numValues && Double.compare(tempMobilities[rawIndex],
          binnedIndex == 0 ? 0d : upperBinLimits[binnedIndex - 1]) == -1) {
        rawIndex++;
      }

      // ensure we are below the current upper-binning-limit
      while (rawIndex < numValues
          && Double.compare(tempMobilities[rawIndex], upperBinLimits[binnedIndex]) == -1) {
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
  public void setMobilogram(@NotNull final List<IonMobilitySeries> mobilograms) {
    clearIntensities();

    int order = 1;
    if (!mobilograms.isEmpty()) {
      order =
          mobilograms.get(0).getSpectrum(0).getFrame().getMobilityType() == MobilityType.TIMS ? -1
              : +1;
    }

    for (IonMobilitySeries ims : mobilograms) {
      final int numValues = ims.getNumberOfValues();
      ensureTempCapacity(numValues);
      ims.getIntensityValues(tempIntensities);

      for (int i = 0; i < numValues; i++) {
        tempMobilities[i] = ims.getMobility(i);
      }

      // in tims, the mobilograms are sorted by decreasing order
      final int start = order == 1 ? 0 : numValues - 1;
      int rawIndex = start;

      // rawIndex only ever moves forward, so counting the additions counts the assigned values
      int numAssigned = 0;

      for (int i = 0; i < upperBinLimits.length && rawIndex >= 0; i++) {

        // waters records DT = 0, so it cannot be 0
        final double binStart = i == 0 ? -MOBILITY_EPSILON : upperBinLimits[i - 1];
        final double binEnd = upperBinLimits[i];
        // if we are in the correct bin, add all values that fit
        while (rawIndex >= 0 && rawIndex < numValues && tempMobilities[rawIndex] < binEnd
            && tempMobilities[rawIndex] > binStart) {

          intensities[i] += tempIntensities[rawIndex];
          numAssigned++;
          rawIndex += order;
        }
      }

      if (numAssigned != numValues) {
        // happens if a mobilogram uses a mobility segment that extends beyond the binned range
        final int assigned = numAssigned;
        logger.finest(() -> "Assigned " + assigned + "/" + numValues + " mobility values to bins.");
      }
    }
  }

  public SummedIntensityMobilitySeries toSummedMobilogram(@Nullable MemoryMapStorage storage) {
    int firstNonZero = -1;
    int lastNonZero = -1;

    for (int i = 0; i < mobilities.length; i++) {
      if (firstNonZero == -1 && intensities[i] > 0d) {
        firstNonZero = i;
      }
      if (intensities[i] > 0d) {
        lastNonZero = i;
      }
    }

    if (firstNonZero == -1) {
      return new SummedIntensityMobilitySeries(storage, new double[0], new double[0]);
    }
    // first non zero - 1, include one zero.
    firstNonZero = Math.max(firstNonZero - 1, 0);
    // last non zero + 2 because Arrays.copyOfRange is exclusive, include one zero.
    lastNonZero = Math.min(lastNonZero + 2, mobilities.length);

    return new SummedIntensityMobilitySeries(storage,
        Arrays.copyOfRange(mobilities, firstNonZero, lastNonZero),
        Arrays.copyOfRange(intensities, firstNonZero, lastNonZero));
  }

  @Override
  public MemorySegment getIntensityValueBuffer() {
    throw new UnsupportedOperationException(
        "This data access is designed to loop over intensities/mobilities.");
  }

  /**
   * @param dst a buffer to copy the intensities to. must be of appropriate size. If null is passed,
   *            the intensity array is returned directly. Do not modify.
   * @return The intensity values.
   */
  @Override
  public double[] getIntensityValues(double[] dst) {
    if (dst != null) {
      assert dst.length >= getNumberOfValues();
      System.arraycopy(intensities, 0, dst, 0, getNumberOfValues());
      return dst;
    } else {
      return intensities;
    }
  }

  public double[] getIntensityValues() {
    return intensities;
  }

  public double[] getMobilityValues() {
    return mobilities;
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

  /**
   * @param dst a buffer to copy the mobilities to. must be of appropriate size. If null is passed,
   *            the intensity array is returned directly. Do not modify.
   * @return The intensity values.
   */
  public double[] getMobilityValues(@Nullable double[] dst) {
    if (dst != null) {
      assert dst.length >= mobilities.length;
      System.arraycopy(mobilities, 0, dst, 0, mobilities.length);
      return dst;
    } else {
      return mobilities;
    }
  }

  public IMSRawDataFile getDataFile() {
    return dataFile;
  }

  public double getBinWidth() {
    return binWidth;
  }

  /**
   * @return The approximate bin size in mobility units with respect to the raw data file.
   */
  public double getApproximateBinSize() {
    return approximateBinSize;
  }

}
