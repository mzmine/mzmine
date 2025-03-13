/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.filter_diams2;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MergedMassSpectrum;
import io.github.mzmine.datamodel.MergedMassSpectrum.MergingType;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.PseudoSpectrum;
import io.github.mzmine.datamodel.PseudoSpectrumType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.correlation.CorrelationData;
import io.github.mzmine.datamodel.features.types.numbers.MobilityType;
import io.github.mzmine.datamodel.impl.DDAMsMsInfoImpl;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.datamodel.impl.SimplePseudoSpectrum;
import io.github.mzmine.datamodel.impl.masslist.ScanPointerMassList;
import io.github.mzmine.datamodel.msms.ActivationMethod;
import io.github.mzmine.datamodel.msms.DDAMsMsInfo;
import io.github.mzmine.datamodel.msms.IonMobilityMsMsInfo;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ADAPChromatogramBuilderParameters;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ModularADAPChromatogramBuilderModule;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ModularADAPChromatogramBuilderTask;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.correlation.FeatureCorrelationUtil.DIA;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.IonMobilityUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import io.github.mzmine.util.collections.EmptyIndexRange;
import io.github.mzmine.util.collections.IndexRange;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.scans.SpectraMerging;
import io.github.mzmine.util.scans.SpectraMerging.IntensityMergingType;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.lang.foreign.ValueLayout;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DiaMs2CorrTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(DiaMs2CorrTask.class.getName());

  private final ModularFeatureList flist;
  private final ScanSelection ms2ScanSelection;
  private final double minMs1Intensity;
  private final double minMs2Intensity;
  private final int minCorrPoints;
  private final MZTolerance mzTolerance;
  private final double minPearson;
  private final double correlationThreshold = 0.1d;

  private final ParameterSet parameters;
  private final ParameterSet adapParameters;
  //  private final ParameterSet smoothingParameters;
//  private final ParameterSet resolverParameters;
  private final int numRows;
  private int currentRow = 0;

  private String description = "";

  private double isolationWindowMergingProgress = 0d;
  private double adapTaskProgess = 0d;

  protected DiaMs2CorrTask(@Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      ModularFeatureList flist, ParameterSet parameters) {
    super(storage, moduleCallDate);

    this.flist = flist;
    this.parameters = parameters;
    ms2ScanSelection = parameters.getValue(DiaMs2CorrParameters.ms2ScanSelection);
    minMs1Intensity = parameters.getValue(DiaMs2CorrParameters.minMs1Intensity);
    minMs2Intensity = parameters.getValue(DiaMs2CorrParameters.minMs2Intensity);
    minCorrPoints = parameters.getValue(DiaMs2CorrParameters.numCorrPoints);
    mzTolerance = parameters.getValue(DiaMs2CorrParameters.ms2ScanToScanAccuracy);
    minPearson = parameters.getValue(DiaMs2CorrParameters.minPearson);
    numRows = flist.getNumberOfRows();

    adapParameters = MZmineCore.getConfiguration()
        .getModuleParameters(ModularADAPChromatogramBuilderModule.class).cloneParameterSet();
    final RawDataFilesSelection adapFiles = new RawDataFilesSelection(
        RawDataFilesSelectionType.SPECIFIC_FILES);
    adapFiles.setSpecificFiles(flist.getRawDataFiles().toArray(new RawDataFile[0]));
    adapParameters.setParameter(ADAPChromatogramBuilderParameters.dataFiles, adapFiles);
    adapParameters.setParameter(ADAPChromatogramBuilderParameters.scanSelection, ms2ScanSelection);
    adapParameters.setParameter(ADAPChromatogramBuilderParameters.minimumConsecutiveScans,
        minCorrPoints);
    adapParameters.setParameter(ADAPChromatogramBuilderParameters.mzTolerance, mzTolerance);
    adapParameters.setParameter(ADAPChromatogramBuilderParameters.suffix, "chroms");
    adapParameters.setParameter(ADAPChromatogramBuilderParameters.minGroupIntensity,
        minMs2Intensity / 5);
    adapParameters.setParameter(ADAPChromatogramBuilderParameters.minHighestPoint, minMs2Intensity);
  }

  /**
   * @param mzTolerance If the feauture's raw data point mz range is smaller than this range, the
   *                    range specified by the mz tolerance will be used.
   */
  private static @NotNull Map<IsolationWindow, RangeMap<Double, IonTimeSeries<?>>> mapIsoWindowToEics(
      Map<IsolationWindow, FeatureList> ms2Flists, @NotNull MZTolerance mzTolerance) {
    final Map<IsolationWindow, RangeMap<Double, IonTimeSeries<?>>> isoWindowEicsMap = new HashMap<>();

    for (Entry<IsolationWindow, FeatureList> entry : ms2Flists.entrySet()) {
      final RawDataFile file = entry.getValue().getRawDataFile(0);
      // store feature data in TreeRangeMap, to query by m/z in ms2 spectra
      var ms2Flist = entry.getValue();
      if (ms2Flist.isEmpty()) {
        continue;
      }
      final RangeMap<Double, IonTimeSeries<?>> ms2Eics = TreeRangeMap.create();
      ms2Flist.getRows().stream().map(row -> row.getFeature(file)).filter(Objects::nonNull)
          .sorted(Comparator.comparingDouble(Feature::getHeight).reversed()).forEach(
              feature -> ms2Eics.put(SpectraMerging.createNewNonOverlappingRange(ms2Eics,
                  feature.getRawDataPointsMZRange()), feature.getFeatureData()));
      isoWindowEicsMap.put(entry.getKey(), ms2Eics);
    }
    return isoWindowEicsMap;
  }

  @Override
  public String getTaskDescription() {
    return "DIA MS2 for feature list: " + flist.getName() + " - " + description;
  }

  @Override
  public double getFinishedPercentage() {
    return isolationWindowMergingProgress * 0.25 + adapTaskProgess * 0.25
           + (currentRow / (double) numRows) * 0.5d;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    if (flist.getNumberOfRawDataFiles() != 1) {
      error(
          "Cannot build DIA MS2 for feature lists with more than one raw data file. Run DIA pseudo MS2 builder before alignment.");
      return;
    }

    final RawDataFile file = flist.getRawDataFile(0);
    description = "Extracting isolation windows.";
    final Map<IsolationWindow, List<Scan>> isolationWindowScanMap = extractIsolationWindows(file);
    description = "Building isolation window files.";
    final Map<IsolationWindow, RawDataFile> isolationWindowFileMap = buildIsolationWindowFiles(
        isolationWindowScanMap);
    description = "Building isolation window chromatograms.";
    final Map<IsolationWindow, FeatureList> ms2Flists = buildChromatograms(isolationWindowFileMap);
    final Map<IsolationWindow, RangeMap<Double, IonTimeSeries<?>>> isoWindowEicsMap = mapIsoWindowToEics(
        ms2Flists, mzTolerance);
    final Set<IsolationWindow> isolationWindows = isoWindowEicsMap.keySet();

    description = "Finding correlated MS2 chromatograms.";
    for (FeatureListRow row : flist.getRows()) {
      currentRow++;
      if (isCanceled()) {
        return;
      }

      final Feature feature = row.getFeature(file);
      if (feature == null || feature.getFeatureStatus() != FeatureStatus.DETECTED
          || feature.getHeight() < minMs1Intensity) {
        continue;
      }

      final List<IsolationWindow> matchingWindows = getIsolationWindows(feature, isolationWindows);
      final List<@NotNull PseudoSpectrum> correlatedMs2s = processIsolationWindows(feature,
          matchingWindows, isoWindowEicsMap, isolationWindowScanMap);

      PseudoSpectrum reoccurringIons = refineMs2s(correlatedMs2s);
      feature.setAllMS2FragmentScans(
          reoccurringIons != null ? List.of(reoccurringIons) : List.of());
    }

    flist.getAppliedMethods().add(
        new SimpleFeatureListAppliedMethod(DiaMs2CorrModule.class, parameters,
            getModuleCallDate()));
    setStatus(TaskStatus.FINISHED);

  }

  /**
   * @param correlatedMs2s A list of all correlated ms2 spectra.
   * @return A new PseudoSpectrum, which only containsMobility ions that appear in every individual
   * pseudo spectrum. Null if no ions were found or the list was empty.
   */
  private @Nullable PseudoSpectrum refineMs2s(
      @NotNull List<@NotNull PseudoSpectrum> correlatedMs2s) {
    if (correlatedMs2s.isEmpty()) {
      return null;
    }
    if (correlatedMs2s.size() == 1) {
      return correlatedMs2s.getFirst();
    }

    PseudoSpectrum mostIntense = correlatedMs2s.stream()
        .max(Comparator.comparing(ps -> Objects.requireNonNullElse(ps.getTIC(), 0d))).orElse(null);
    if (mostIntense == null) {
      return null;
    }

    // only compare ions with other spectra that have at least 25% of the tic of the most intense
    final double minTic = Objects.requireNonNullElse(mostIntense.getTIC(), 0d) * 0.25;
    final List<@NotNull PseudoSpectrum> minTicSpectra = correlatedMs2s.stream()
        .filter(ms2 -> Objects.requireNonNullElse(ms2.getTIC(), 0d) >= minTic).toList();

    DoubleArrayList mzs = new DoubleArrayList();
    DoubleArrayList intensities = new DoubleArrayList();

    for (int i = 0; i < mostIntense.getNumberOfDataPoints(); i++) {
      final double mzInMostIntense = mostIntense.getMzValue(i);
      boolean foundInAll = true;

      for (@NotNull PseudoSpectrum ms2 : minTicSpectra) {
        if (ms2 == mostIntense) {
          continue;
        }
        final int closestIndex = ms2.binarySearch(mzInMostIntense, DefaultTo.CLOSEST_VALUE);
        if (!mzTolerance.checkWithinTolerance(mzInMostIntense, ms2.getMzValue(closestIndex))) {
          foundInAll = false;
          break;
        }
      }
      if (foundInAll) {
        mzs.add(mzInMostIntense);
        intensities.add(mostIntense.getIntensityValue(i));
      }
    }

    if (mzs.isEmpty()) {
//      logger.finest("No reoccurring ions found in %d spectra.".formatted(correlatedMs2s.size()));
      return null;
    }

    final MsMsInfo msMsInfo = correlatedMs2s.stream().map(PseudoSpectrum::getMsMsInfo)
        .filter(Objects::nonNull).map(MsMsInfo::createCopy).findFirst().orElse(null);

    return new SimplePseudoSpectrum(mostIntense.getDataFile(), mostIntense.getMSLevel(),
        mostIntense.getRetentionTime(), msMsInfo, mzs.toDoubleArray(), intensities.toDoubleArray(),
        mostIntense.getPolarity(), mostIntense.getScanDefinition(),
        mostIntense.getPseudoSpectrumType());
  }

  private @NotNull List<@NotNull PseudoSpectrum> processIsolationWindows(Feature feature,
      List<IsolationWindow> matchingWindows,
      Map<IsolationWindow, RangeMap<Double, IonTimeSeries<?>>> isoWindowEicsMap,
      Map<IsolationWindow, List<Scan>> isoWindowScansMap) {
    final IonTimeSeries<? extends Scan> ms1Eic = feature.getFeatureData();
    final double[][] shape = extractPointsAroundMaximum(feature.getHeight() * correlationThreshold,
        ms1Eic, feature.getRepresentativeScan());
    if (shape == null || shape[0].length < minCorrPoints) {
      return List.of();
    }

    final double[] ms1Rts = shape[0];
    final double[] ms1Intensities = shape[1];
    final Range<Float> correlationRange = Range.closed((float) ms1Rts[0],
        (float) ms1Rts[ms1Rts.length - 1]);

    final List<PseudoSpectrum> correlatedMs2s = new ArrayList<>();

    for (IsolationWindow window : matchingWindows) {
      var ms2Scans = isoWindowScansMap.get(window);
      if (!checkMs2ScanRequirements(feature.getRT(), ms2Scans, correlationRange)) {
        continue;
      }

      final Scan closestMs2 = getClosestMs2(feature.getRT(), ms2Scans);
      if (closestMs2 == null) {
        continue;
      }
      final RangeMap<Double, IonTimeSeries<?>> eics = isoWindowEicsMap.get(window);

      final List<IonTimeSeries<?>> eligibleEics = getEligibleEics(closestMs2, eics);
      if (eligibleEics.isEmpty()) {
        continue;
      }

      final PseudoSpectrum ms2 = extractCorrelatedMs2(feature,
          () -> extractMergedMobilityScan(feature, closestMs2,
              ms2Scans.stream().filter(s -> correlationRange.contains(s.getRetentionTime()))
                  .toList()), correlationRange, eligibleEics, ms1Rts, ms1Intensities);

      if (ms2 != null) {
        correlatedMs2s.add(ms2);
      }
    }

    return correlatedMs2s;
  }

  /**
   * @param feature                   The feature to extract the ms2 for
   * @param extractMergedMobilityScan A supplier to get a merged mobility scan for this feature.
   *                                  Only called if necessary
   * @param correlationRange          The rt correlation range between the ms1 and ms2 traces.
   * @param eligibleEics              The MS2 EICs that appear during the elution of the MS1
   *                                  feature.
   * @param ms1Rts                    The rt values of the ms1 feature during the correlation range
   * @param ms1Intensities            The intensity values of the ms1 feature during the correlation
   *                                  range
   * @return A {@link PseudoSpectrum} or null.
   */
  private @Nullable PseudoSpectrum extractCorrelatedMs2(Feature feature,
      Supplier<MergedMassSpectrum> extractMergedMobilityScan, Range<Float> correlationRange,
      List<IonTimeSeries<?>> eligibleEics, double[] ms1Rts, double[] ms1Intensities) {
    DoubleArrayList ms2Mzs = new DoubleArrayList();
    DoubleArrayList ms2Intensities = new DoubleArrayList();
    DoubleArrayList collisionEnergies = new DoubleArrayList();
    MergedMassSpectrum mergedMobilityScan = null; // lazy initialization
    ActivationMethod activationMethod = ActivationMethod.UNKNOWN;

    for (IonTimeSeries<?> ms2Eic : eligibleEics) {
      final IonTimeSeries<?> subSeries = ms2Eic.subSeries(getMemoryMapStorage(),
          correlationRange.lowerEndpoint(), correlationRange.upperEndpoint());
      if (subSeries.getNumberOfValues() < minCorrPoints) {
        continue;
      }
      if (activationMethod == ActivationMethod.UNKNOWN) {
        activationMethod = ScanUtils.streamMsMsInfos(subSeries.getSpectra(), feature.getMZ())
            .map(MsMsInfo::getActivationMethod).findFirst().orElse(ActivationMethod.UNKNOWN);
      }
      final double[] rts = new double[subSeries.getNumberOfValues()];
      for (int i = 0; i < subSeries.getNumberOfValues(); i++) {
        rts[i] = subSeries.getRetentionTime(i);
      }

      final CorrelationData correlationData = DIA.corrFeatureShape(ms1Rts, ms1Intensities, rts,
          subSeries.getIntensityValueBuffer().toArray(ValueLayout.JAVA_DOUBLE), minCorrPoints, 2,
          minMs2Intensity / 5);
      if (correlationData == null || !correlationData.isValid()
          || correlationData.getPearsonR() < minPearson) {
        continue;
      }

      final IndexRange ms2CorrelatedIndexRange = BinarySearch.indexRange(
          correlationRange.lowerEndpoint(), correlationRange.upperEndpoint(),
          ms2Eic.getNumberOfValues(), ms2Eic::getRetentionTime);
      if (ms2CorrelatedIndexRange.equals(EmptyIndexRange.INSTANCE)
          || ms2CorrelatedIndexRange.min() == -1) {
        continue;
      }

      double maxIntensity = Double.NEGATIVE_INFINITY;
      for (int j = ms2CorrelatedIndexRange.min(); j < ms2CorrelatedIndexRange.maxExclusive(); j++) {
        if (ms2Eic.getIntensity(j) > maxIntensity) {
          maxIntensity = ms2Eic.getIntensity(j);
        }
      }

      final double mz = FeatureDataUtils.calculateCenterMz(ms2Eic,
          FeatureDataUtils.DEFAULT_CENTER_FUNCTION, ms2CorrelatedIndexRange.min(),
          ms2CorrelatedIndexRange.maxInclusive());

      // lazy initialization, in case we never get here in the first place.
      mergedMobilityScan =
          mergedMobilityScan == null ? extractMergedMobilityScan.get() : mergedMobilityScan;

      // for IMS measurements, the ion must be present in the MS2 mobility scans in the during
      // the feature's rt window and within the mobility scans of the feature's mobility window.
      // we could also look at mobility shape and correlate that, but it would probably take a
      // lot of optimisation and/or too long to compute
      if (!checkIfMzAppearsInMergedMobilityScan(mergedMobilityScan, mz, mzTolerance)) {
        continue; // dont add this mz
      }

      ms2Mzs.add(mz);
      ms2Intensities.add(maxIntensity);
      ScanUtils.streamMsMsInfos(subSeries.getSpectra(), feature.getMZ())
          .map(MsMsInfo::getActivationEnergy).filter(Objects::nonNull)
          .mapToDouble(Float::doubleValue).average().ifPresent(collisionEnergies::add);
    }

    if (ms2Mzs.isEmpty()) {
      return null;
    }

    final DDAMsMsInfo info = new DDAMsMsInfoImpl(feature.getMZ(), feature.getCharge(),
        collisionEnergies.isEmpty() ? null
            : (float) collisionEnergies.doubleStream().average().getAsDouble(), null, null, 2,
        activationMethod, null);

    return new SimplePseudoSpectrum(feature.getRawDataFile(),
        Objects.requireNonNullElse(ms2ScanSelection.msLevel().getSingleMsLevelOrNull(), 2),
        feature.getRT(), info, ms2Mzs.toDoubleArray(), ms2Intensities.toDoubleArray(),
        feature.getRepresentativeScan().getPolarity(),
        String.format("Pseudo MS2 (R >= %.2f)", minPearson), PseudoSpectrumType.LC_DIA);
  }

  /**
   * @param mergedMobilityScan The merged mobility scan
   * @param mz                 The mz to search for
   * @param mzTolerance        The allowed tolerance.
   * @return true if the mz appears in the merged mobility scan within the given mzTolerance or if
   * the merged mobility scan is null (no ims data).
   */
  private boolean checkIfMzAppearsInMergedMobilityScan(
      @Nullable MergedMassSpectrum mergedMobilityScan, double mz, MZTolerance mzTolerance) {
    // no ims-ms data
    if (mergedMobilityScan == null) {
      return true;
    }

    final int closestIndex = mergedMobilityScan.binarySearch(mz, DefaultTo.CLOSEST_VALUE);
    if (closestIndex == -1 || !mzTolerance.checkWithinTolerance(mz,
        mergedMobilityScan.getMzValue(closestIndex))) {
      return false;
    }

    return true;
  }

  /**
   * @param feature       the feature
   * @param closestMs2    the closest ms2 scan
   * @param ms2sInRtRange A list of spectra to extract the {@link MobilityScan}s from. Ideally only
   *                      in the rt-correlation range of the feature.
   * @return A summed mobility scan across all {@code ms2sInRtRange} in the mobility fwhm of the
   * feature or null if the feature is not an IMS feature.
   */
  private MergedMassSpectrum extractMergedMobilityScan(Feature feature, Scan closestMs2,
      List<Scan> ms2sInRtRange) {
    if (!(closestMs2 instanceof Frame)
        || !(feature.getFeatureData() instanceof IonMobilogramTimeSeries imts)) {
      return null;
    }

    final MobilityScan bestMobilityScan = IonMobilityUtils.getBestMobilityScan(feature);
    if (bestMobilityScan == null) {
      return null;
    }

    // for ims data, later check if we can find the mz in the closest ms2 frame with the same mobility
    final Range<Float> mobilityRange = IonMobilityUtils.getMobilityFWHM(imts.getSummedMobilogram());
    if (mobilityRange == null) {
      return null;
    }

    final List<MobilityScan> mobilityScans = ms2sInRtRange.stream()
        .flatMap(s -> ((Frame) s).getMobilityScans().stream())
        .filter(m -> mobilityRange.contains((float) m.getMobility())).toList();
    if (mobilityScans.isEmpty()) {
      return null; // if we have ims data, and there are no mobility scans to be merged, something is fishy.
    }
    return SpectraMerging.mergeSpectra(mobilityScans, mzTolerance, MergingType.ALL_ENERGIES, null);
  }

  private List<IonTimeSeries<?>> getEligibleEics(Scan ms2,
      RangeMap<Double, IonTimeSeries<?>> eics) {
    final List<IonTimeSeries<?>> result = new ArrayList<>();

    for (int i = 0; i < ms2.getNumberOfDataPoints(); i++) {
      if (ms2.getIntensityValue(i) < minMs2Intensity) {
        continue;
      }

      final double mz = ms2.getMzValue(i);
      final IonTimeSeries<?> eic = eics.get(mz);
      if (eic != null) {
        result.add(eic);
      }
    }

    return result.stream().distinct().toList();
  }

  private boolean checkMs2ScanRequirements(float featureRt, List<Scan> ms2Scans,
      Range<Float> correlationRange) {
    final List<Scan> ms2sInRtRange = ms2Scans.stream()
        .filter(scan -> correlationRange.contains(scan.getRetentionTime())).toList();
    if (ms2sInRtRange.isEmpty()) {
      return false;
    }
    Scan closestMs2 = getClosestMs2(featureRt, ms2sInRtRange);
    if (closestMs2 == null || ms2sInRtRange.size() < minCorrPoints) {
//      logger.fine(() -> "Could not find enough ms2s in rtRange %s".formatted(correlationRange));
      return false;
    }
    return true;
  }

  /**
   * Builds one dummy file per isolation window. For {@link IMSRawDataFile}s, the mobility scans
   * must be merged first, accounting for considerable processing time.
   */
  private Map<IsolationWindow, RawDataFile> buildIsolationWindowFiles(
      Map<IsolationWindow, List<Scan>> isolationWindowScanMap) {

    final Map<IsolationWindow, RawDataFile> result = new HashMap<>();
    final RawDataFile file = flist.getRawDataFile(0);

    if (file instanceof IMSRawDataFile && flist.hasFeatureType(MobilityType.class)) {
      // merge to new frames
      logger.finest(() -> "Merging isolation windows of frames to new frame");
      final double numIsolationWindows = isolationWindowScanMap.size();
      double finishedIsolationWindows = 0;

      final long framesToMerge = isolationWindowScanMap.entrySet().stream()
          .mapToLong(e -> e.getValue().size()).sum();
      long mergedFrames = 0;

      for (Entry<IsolationWindow, List<Scan>> entry : isolationWindowScanMap.entrySet()) {
        final IsolationWindow isolationWindow = entry.getKey();
        if (entry.getValue().size() < minCorrPoints) {
          continue;
        }
        final IMSRawDataFileImpl windowFile = new IMSRawDataFileImpl(
            file.getName() + " %s".formatted(isolationWindow.toString()), null,
            getMemoryMapStorage());

        for (Scan scan : entry.getValue()) {
          if (!(scan instanceof Frame frame)) {
            logger.warning(
                () -> "Data file %s is an ims file but also containsMobility scans without ims dimension %s.".formatted(
                    file.getName(), ScanUtils.scanToString(scan)));
            continue;
          }

          // merge scans from isolation window only
          final List<MobilityScan> mobilityScansInWindow = frame.getMobilityScans().stream()
              .filter(isolationWindow::containsMobility).toList();
          final double[][] mzIntensities = SpectraMerging.calculatedMergedMzsAndIntensities(
              mobilityScansInWindow.stream().map(MobilityScan::getMassList).toList(), mzTolerance,
              IntensityMergingType.SUMMED, SpectraMerging.DEFAULT_CENTER_FUNCTION, null, null, 2);

          // all scans have the same msmsInfo - therefore ok to use just one of them
          final Optional<IonMobilityMsMsInfo> msMsInfo = mobilityScansInWindow.stream()
              .map(MobilityScan::getMsMsInfo).filter(IonMobilityMsMsInfo.class::isInstance)
              .map(IonMobilityMsMsInfo.class::cast).findFirst().map(
                  info -> (IonMobilityMsMsInfo) info.createCopy()); // copy to secure original from changes

          // also set the msmsinfo as the precursorInfos - will be just one representative
          final Set<IonMobilityMsMsInfo> precursorInfos = msMsInfo.map(Set::of).orElse(null);

          final SimpleFrame newFrame = new SimpleFrame(windowFile, scan.getScanNumber(),
              scan.getMSLevel(), scan.getRetentionTime(), mzIntensities[0], mzIntensities[1],
              scan.getSpectrumType(), scan.getPolarity(), scan.getScanDefinition(),
              scan.getScanningMZRange(), ((Frame) scan).getMobilityType(), precursorInfos,
              scan.getInjectionTime());
          //set to regular msmsinfo so we can extract for later CE setting
          newFrame.setMsMsInfo(msMsInfo.orElse(null));
          newFrame.addMassList(new ScanPointerMassList(newFrame));
          windowFile.addScan(newFrame);

          isolationWindowMergingProgress = (double) (++mergedFrames) / framesToMerge;
        }

        /*logger.finest(
            "File: %s - Finished merging isolation window %s (%.0f/%.0f)".formatted(file.getName(),
                isolationWindow.toString(), finishedIsolationWindows, numIsolationWindows));*/

        result.put(isolationWindow, windowFile);
      }

    } else {
      // just append to new file
      for (Entry<IsolationWindow, List<Scan>> entry : isolationWindowScanMap.entrySet()) {
        final RawDataFileImpl windowFile = new RawDataFileImpl(
            file.getName() + " %s".formatted(entry.getKey().toString()), null,
            getMemoryMapStorage());
        entry.getValue().forEach(windowFile::addScan);
        result.put(entry.getKey(), windowFile);
      }
      isolationWindowMergingProgress = 1d; // nothing to calculate
    }

    return result;
  }

  private Scan getClosestMs2(float rt, List<Scan> ms2sInRtRange) {
    if (ms2sInRtRange.getFirst().getRetentionTime() > rt
        || ms2sInRtRange.getLast().getRetentionTime() < rt) {
      return null;
    }

    final int index = BinarySearch.binarySearch(rt, DefaultTo.CLOSEST_VALUE, ms2sInRtRange.size(),
        i -> ms2sInRtRange.get(i).getRetentionTime());
    return ms2sInRtRange.get(index);
  }

  private Map<IsolationWindow, FeatureList> buildChromatograms(
      Map<IsolationWindow, RawDataFile> isolationWindowFilesMap) {

    final double totalAdapTasks = isolationWindowFilesMap.size();
    double finishedAdapTasks = 0;

    final Map<IsolationWindow, FeatureList> result = new HashMap<>();
    for (Entry<IsolationWindow, RawDataFile> entry : isolationWindowFilesMap.entrySet()) {
      final MZmineProjectImpl dummyProject = new MZmineProjectImpl();
      dummyProject.addFile(entry.getValue());
      // currently the consecutive scans are used
      var adapTask = ModularADAPChromatogramBuilderTask.forChromatography(dummyProject,
          entry.getValue(), adapParameters, getMemoryMapStorage(), getModuleCallDate(),
          DiaMs2CorrModule.class);
      adapTask.run();

      if (dummyProject.getCurrentFeatureLists().isEmpty()) {
        // file name includes isolation window for merged.
        logger.warning("Cannot find ms2 feature list for file %s".formatted(entry.getValue()));
        continue;
      }
      var ms2Flist = dummyProject.getCurrentFeatureLists().get(0);

      adapTaskProgess = (++finishedAdapTasks) / totalAdapTasks;
      logger.finest(
          "Finished chromatogram building for file %s (%.0f/%.0f)".formatted(entry.getValue(),
              finishedAdapTasks, totalAdapTasks));
      result.put(entry.getKey(), ms2Flist);
    }
    return result;
  }

  /**
   * Extracts a given number of data points around a maximum. The number of detected points is
   * automatically limited to the bounds of the chromatogram.
   *
   * @param minCorrelationIntensity minimum intensity to extract
   * @param chromatogram            the chromatogram to extract the points from.
   * @param maximumScan             The maximum scan in the chromatogram.
   * @return a 2d array [0][] = rts, [1][] = intensities.
   */
  @Nullable
  private double[][] extractPointsAroundMaximum(final double minCorrelationIntensity,
      final IonTimeSeries<? extends Scan> chromatogram, @Nullable final Scan maximumScan) {
    if (maximumScan == null) {
      return null;
    }

    final List<? extends Scan> spectra = chromatogram.getSpectra();
    final int index = Math.abs(Collections.binarySearch(spectra, maximumScan));
    // final int index2 = spectra.indexOf(maximumScan);

    // take one point more each, because MS1 and MS2 are acquired in alternating fashion, so we
    // need one more ms1 point on each side for the rt range, so we can fit the determined number
    // of ms2 points
    // final int lower = Math.max(index - numPoints / 2 - 1, 0);
    // final int upper = Math.min(index + numPoints / 2 + 1, chromatogram.getNumberOfValues() - 1);

    int lower = 0;
    for (int i = index; i >= 0; i--) {
      if (chromatogram.getIntensity(i) < minCorrelationIntensity) {
        lower = i;
        break;
      }
    }
    int upper = chromatogram.getNumberOfValues() - 1;
    for (int i = index; i < chromatogram.getNumberOfValues(); i++) {
      if (chromatogram.getIntensity(i) < minCorrelationIntensity) {
        upper = i;
        break;
      }
    }

    final double[] rts = new double[upper - lower];
    final double[] intensities = new double[upper - lower];
    for (int i = lower; i < upper; i++) {
      rts[i - lower] = chromatogram.getRetentionTime(i);
      intensities[i - lower] = chromatogram.getIntensity(i);
    }

    return new double[][]{rts, intensities};
  }

  @Override
  public void cancel() {
    super.cancel();
  }

  private Map<IsolationWindow, List<Scan>> extractIsolationWindows(
      @NotNull final RawDataFile file) {
    Map<IsolationWindow, List<Scan>> windowScanMap = new HashMap<>();
    for (Scan scan : ms2ScanSelection.getMatchingScans(file)) {

      if (scan instanceof Frame frame) {
        final Set<IonMobilityMsMsInfo> imsMsMsInfos = frame.getImsMsMsInfos();
        for (IonMobilityMsMsInfo info : imsMsMsInfos) {
          IsolationWindow window = new IsolationWindow(info.getIsolationWindow(),
              info.getMobilityRange());
          final List<Scan> scans = windowScanMap.computeIfAbsent(window, w -> new ArrayList<>());
          // have to extract some mocked frames later
          scans.add(scan);
        }
      } else {
        final MsMsInfo msMsInfo = scan.getMsMsInfo();
        if (msMsInfo == null) {
          continue;
        }

        final Range<Double> mzRange = msMsInfo.getIsolationWindow();
        IsolationWindow window = new IsolationWindow(mzRange, null);
        final List<Scan> scans = windowScanMap.computeIfAbsent(window, w -> new ArrayList<>());
        scans.add(scan);
      }
    }

    logger.finest(() -> "%s: Extracted %d raw isolation windows.".formatted(file.getName(),
        windowScanMap.size()));
    // now merge some isolation windows, if they are largely overlapping (e.g. MSConvert converted Agilent AllIons files.)
    final List<Entry<IsolationWindow, List<Scan>>> sortedWindowEntries = new ArrayList<>(
        windowScanMap.entrySet().stream().sorted(Comparator.comparingDouble(
            iw -> Objects.requireNonNullElse(iw.getKey().mzIsolation(), Range.singleton(0d))
                .lowerEndpoint())).toList());
    final List<MergingIsolationWindow> mergingWindows = new ArrayList<>();
    for (Entry<IsolationWindow, List<Scan>> entry : sortedWindowEntries) {
      final IsolationWindow window = entry.getKey();
      final Optional<MergingIsolationWindow> bestWindow = mergingWindows.stream()
          .max(Comparator.comparingDouble(mw -> mw.window().overlap(window)));

      if (bestWindow.isEmpty()) {
        mergingWindows.add(new MergingIsolationWindow(window, entry.getValue()));
        continue;
      }
      final MergingIsolationWindow best = bestWindow.get();
      final double overlap = best.window().overlap(window);
      if (overlap > 0.95) {
        best.setWindow(best.window().merge(window));
        best.scans().addAll(entry.getValue());
      } else {
        mergingWindows.add(new MergingIsolationWindow(window, entry.getValue()));
      }
    }

    logger.finest(
        () -> "%s: %d isolation windows remained after merging. (%s)".formatted(file.getName(),
            mergingWindows.size(), mergingWindows.toString()));
    windowScanMap.clear();
    mergingWindows.forEach(mw -> windowScanMap.put(mw.window(),
        mw.scans().stream().sorted(Comparator.comparingDouble(Scan::getRetentionTime)).toList()));

    return windowScanMap;
  }

  private @Nullable List<IsolationWindow> getIsolationWindows(Feature feature,
      Collection<IsolationWindow> windows) {
    List<IsolationWindow> result = new ArrayList<>();
    for (IsolationWindow window : windows) {
      if (window.contains(feature)) {
        result.add(window);
      }
    }
    return result;
  }
}
