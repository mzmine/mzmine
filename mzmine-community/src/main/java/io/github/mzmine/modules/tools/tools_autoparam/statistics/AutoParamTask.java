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

package io.github.mzmine.modules.tools.tools_autoparam.statistics;

import com.google.common.collect.Range;
import com.google.common.collect.TreeRangeMap;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.SimpleRange.SimpleFloatRange;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.BuildingIonSeries;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.datamodel.impl.masslist.ScanPointerMassList;
import io.github.mzmine.datamodel.impl.masslist.SimpleMassList;
import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.gui.mainwindow.SimpleTab;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.featdet_extract_mz_ranges.ExtractMzRangesIonSeriesFunction;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.auto.AutoMassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_mobilityscanmerger.MobilityScanMergerParameters;
import io.github.mzmine.modules.dataprocessing.featdet_mobilityscanmerger.MobilityScanMergerTask;
import io.github.mzmine.modules.tools.batchwizard.builders.BaseWizardBatchBuilder;
import io.github.mzmine.modules.tools.tools_autoparam.runphases.RunPhaseDetection;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractRawDataFileTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.RawDataFileTypeDetector;
import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.maths.CenterFunction;
import io.github.mzmine.util.scans.SpectraMerging;
import it.unimi.dsi.fastutil.doubles.Double2ObjectArrayMap;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AutoParamTask extends AbstractRawDataFileTask {

  private static final Logger logger = Logger.getLogger(AutoParamTask.class.getName());

  private static final List<MZTolerance> tolerances = MzToleranceSearchOptions.ALL_TOLERANCE_OPTIONS;
  /**
   * Number of additional seed peaks per spectrum besides the base peak.
   */
  private static final int NUM_SECONDARY_SEEDS = 2;
  /**
   * Secondary seed peaks must be further than this from the base peak and previously picked peaks
   * of the same spectrum.
   */
  private static final double SECONDARY_SEED_EXCLUSION_MZ = 10d;
  /*new MZTolerance[]{new MZTolerance(0.0005, 2), //
      new MZTolerance(0.001, 5), //
      new MZTolerance(0.005, 15), //
      new MZTolerance(0.008, 15), //
      new MZTolerance(0.01, 20), //
      new MZTolerance(0.015, 25), //
      new MZTolerance(0.02, 25), //
      new MZTolerance(0.05, 25) //
  };*/

  /**
   * for matching {@link #additionalFeatures} to the base peak mzs.
   */
  private final MZTolerance referenceMatchingTol = new MZTolerance(0.01, 20);

  private final RawDataFile file;

  /**
   * externally provided list of ions that shall be searched for. (also searches for isotopes)
   */
  @Nullable
  private final List<FeatureRecord> additionalFeatures;
  private final boolean showTab;
  private @Nullable DataFileStatistics dataFileStats;

  /**
   * @param storage        The {@link MemoryMapStorage} used to store results of this task (e.g.
   *                       RawDataFiles, MassLists, FeatureLists). May be null if results shall be
   *                       stored in ram. For now, one storage should be created per module call in
   * @param moduleCallDate the call date of module to order execution order
   * @param parameters
   * @param moduleClass
   * @param raw
   */
  public AutoParamTask(@Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      @NotNull ParameterSet parameters, @NotNull Class<? extends MZmineModule> moduleClass,
      @NotNull RawDataFile raw, final @Nullable List<FeatureRecord> additionalFeatures) {
    this(storage, moduleCallDate, parameters, moduleClass, raw, additionalFeatures, true);
  }

  /**
   * @param showTab if false, suppresses the individual per-file {@link AutoParametersPane} tab
   *                (used when the caller opens a combined dashboard instead)
   */
  public AutoParamTask(@Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      @NotNull ParameterSet parameters, @NotNull Class<? extends MZmineModule> moduleClass,
      @NotNull RawDataFile raw, final @Nullable List<FeatureRecord> additionalFeatures,
      boolean showTab) {
    super(storage, moduleCallDate, parameters, moduleClass);
    file = raw;
    this.showTab = showTab;
    this.additionalFeatures = additionalFeatures != null ? additionalFeatures.stream().sorted(
        Comparator.comparingDouble(FeatureRecord::mz)).toList() : null;
  }

  private static double[] getMainPeakMzs(List<Scan> scans,
      final @Nullable List<FeatureRecord> additionalFeatures) {
    final MZTolerance oneMzTolerance = new MZTolerance(1, 0);
    final TreeRangeMap<Double, Double> mzScanMap = TreeRangeMap.create();

    if (additionalFeatures != null) {
      for (double mz : additionalFeatures.stream().mapToDouble(FeatureRecord::mz).toArray()) {
        final Double entry = mzScanMap.get(mz);
        if (entry == null) {
          final Range<Double> range = SpectraMerging.createNewNonOverlappingRange(mzScanMap,
              oneMzTolerance.getToleranceRange(mz));
          mzScanMap.put(range, mz);
        }
      }
    }

    final List<MassList> mzSortedScans = scans.stream().map(Scan::getMassList)
        .filter(ml -> ml.getBasePeakMz() != null)
        .sorted(Comparator.comparingDouble(MassList::getBasePeakMz).reversed()).toList();

    for (MassList scan : mzSortedScans) {
      addSeedMz(mzScanMap, oneMzTolerance, scan.getBasePeakMz());
    }

    // decision: secondary peaks are added after all base peaks, so base peaks keep priority for
    // the 1 Da bins
    for (MassList scan : mzSortedScans) {
      for (double mz : findSecondaryPeakMzs(scan, SECONDARY_SEED_EXCLUSION_MZ,
          NUM_SECONDARY_SEEDS)) {
        addSeedMz(mzScanMap, oneMzTolerance, mz);
      }
    }

    final double[] basePeakMzs = mzScanMap.asMapOfRanges().values().stream().mapToDouble(v -> v)
        .toArray();
    return basePeakMzs;
  }

  private static void addSeedMz(final @NotNull TreeRangeMap<Double, Double> mzScanMap,
      final @NotNull MZTolerance oneMzTolerance, final double mz) {
    if (mzScanMap.get(mz) != null) {
      return;
    }
    final Range<Double> range = SpectraMerging.createNewNonOverlappingRange(mzScanMap,
        oneMzTolerance.getToleranceRange(mz));
    mzScanMap.put(range, mz);
  }

  /**
   * Finds the next most intense peaks after the base peak. Each peak must be more than
   * {@code exclusionMz} away from the base peak and from the peaks picked before it.
   *
   * @return m/z values in descending intensity order, may be shorter than {@code count}
   */
  static double @NotNull [] findSecondaryPeakMzs(final @NotNull MassList massList,
      final double exclusionMz, final int count) {
    final Integer basePeakIndex = massList.getBasePeakIndex();
    if (basePeakIndex == null) {
      return new double[0];
    }
    final int numDp = massList.getNumberOfDataPoints();
    final double[] picked = new double[count + 1];
    picked[0] = massList.getMzValue(basePeakIndex);
    int numPicked = 1;

    // one linear pass per peak, cheaper than sorting all signals of the zero-intensity mass list
    while (numPicked <= count) {
      int bestIndex = -1;
      double bestIntensity = 0d;
      for (int i = 0; i < numDp; i++) {
        final double intensity = massList.getIntensityValue(i);
        if (intensity <= bestIntensity) {
          continue;
        }
        final double mz = massList.getMzValue(i);
        boolean excluded = false;
        for (int p = 0; p < numPicked; p++) {
          if (Math.abs(mz - picked[p]) <= exclusionMz) {
            excluded = true;
            break;
          }
        }
        if (!excluded) {
          bestIndex = i;
          bestIntensity = intensity;
        }
      }
      if (bestIndex < 0) {
        break;
      }
      picked[numPicked++] = massList.getMzValue(bestIndex);
    }
    return Arrays.copyOfRange(picked, 1, numPicked);
  }

  @Override
  protected @NotNull List<RawDataFile> getProcessedDataFiles() {
    return List.of();
  }

  /**
   * Replaces the frame mass lists by a merge of the mobility scans with the tested tolerance, as
   * the wizard batch merges with the scan-to-scan tolerance. Otherwise, the best tolerance would
   * depend on how well the reference merge fits the raw data. Requires the mobility scan mass lists
   * of {@link #mergeMs1MobilityScansIntoFrames(IMSRawDataFile)}.
   */
  private static void mergeFrameMassLists(@NotNull List<Scan> frames,
      @NotNull MZTolerance tolerance) {
    final CenterFunction centerFunction = new CenterFunction(SpectraMerging.DEFAULT_CENTER_MEASURE,
        MobilityScanMergerParameters.DEFAULT_WEIGHTING);
    for (final Scan frame : frames) {
      final double[][] merged = MobilityScanMergerTask.mergeMobilityScans((Frame) frame, tolerance,
          MobilityScanMergerParameters.DEFAULT_MERGING_TYPE, centerFunction,
          MobilityScanMergerParameters.DEFAULT_NOISE_LEVEL,
          MobilityScanMergerParameters.DEFAULT_MIN_DETECTIONS);
      // decision: in RAM, as each merge is replaced by the next one and the memory map storage
      // would keep every replaced merge
      frame.addMassList(new SimpleMassList(null, merged[0], merged[1]));
    }
  }

  @Override
  protected void process() {
    // polarity switching data alternate polarities scan by scan, which breaks every trace
    final PolarityType polarity = getParameters().getValue(AutoParamParameters.POLARITY);
    final ScanSelection scanSelection = new ScanSelection(1, polarity);
    final List<Scan> scans = scanSelection.getMatchingScans(file.getScans());
    final boolean mergeMobilityScans =
        file instanceof IMSRawDataFile && !BaseWizardBatchBuilder.hasImsFrameSpectra(
            RawDataFileTypeDetector.detectDataFileType(file.getAbsoluteFilePath()));
    if (mergeMobilityScans) {
      // reference merge with the default tolerance for the seeds and the run phases
      mergeMs1MobilityScansIntoFrames((IMSRawDataFile) file);
    } else {
      applyZeroIntensityMassDetection(scans);
    }
    // decision: the mass spectrometer presets allowed with ion mobility are never estimated with
    // a wider tolerance, and wide merges distort the frame spectra
    final List<MZTolerance> testedTolerances = file instanceof IMSRawDataFile ? tolerances
            .subList(0, MzToleranceSearchOptions.MAX_HIGH_RESOLUTION_INDEX + 1) : tolerances;
    // needs only the MS1 mass lists, cheap compared to the isotope trace extraction below
    final SimpleFloatRange effectiveRtRange = RunPhaseDetection.detect(file, scans);
    logger.finest("Effective RT range of %s: %s".formatted(file.getName(), effectiveRtRange));

    final double[] basePeakMzs = Arrays.stream(getMainPeakMzs(scans, additionalFeatures)).sorted()
        .toArray();

    final Double2ObjectArrayMap<List<FeatureWithIsotopeTraces>> mzsToIsotopeTraces = new Double2ObjectArrayMap<>();

    List<FeatureWithIsotopeTraces> featureWithIsotopeTraces = new ArrayList<>();
    for (MZTolerance tolerance : testedTolerances) {
      if (mergeMobilityScans) {
        mergeFrameMassLists(scans, tolerance);
      }
      final List<Range<Double>> mzRangesSorted = Arrays.stream(basePeakMzs)
          .mapToObj(tolerance::getToleranceRange).toList();
      final List<ModularFeature> mainFeatures = getMainSignalFeatures(scans, mzRangesSorted,
          additionalFeatures);

      for (ModularFeature feature : mainFeatures) {
        final int initialMzIndex = BinarySearch.binarySearch(basePeakMzs, feature.getMZ(),
            DefaultTo.CLOSEST_VALUE);
        final double initialMz = basePeakMzs[initialMzIndex];
        final FeatureWithIsotopeRanges withIsotopeRanges = FeatureWithIsotopeRanges.of(initialMz,
            feature, tolerance);
        if (withIsotopeRanges == null) {
          continue;
        }

        final FeatureWithIsotopeTraces envelope = FeatureWithIsotopeTraces.of(initialMz, file,
            tolerance, withIsotopeRanges, getMemoryMapStorage(), this);
        if (envelope == null) {
          logger.finest(
              "No correlated isotopes found in file %s for m/z %.4f at a tolerance of %s".formatted(
                  file.getName(), initialMz, tolerance.toString()));
          continue;
        }
        featureWithIsotopeTraces.add(envelope);
        final List<FeatureWithIsotopeTraces> mzResults = mzsToIsotopeTraces.computeIfAbsent(
            initialMz, k -> new ArrayList<>());
        mzResults.add(envelope);
      }
    }
    if (mergeMobilityScans) {
      // restore the reference merge, the frame spectra of the file
      for (final Scan frame : scans) {
        frame.addMassList(new ScanPointerMassList(frame));
      }
    }

    final List<FeatureStatistics> featureStats = mzsToIsotopeTraces.double2ObjectEntrySet().stream()
        .map(e -> new FeatureStatistics(e.getValue()))
        .sorted(Comparator.comparingDouble(FeatureStatistics::getMz)).toList();
    final Double referenceInjectionTime = RawDataParameterEstimation.estimateReferenceInjectionTime(
        file, scans, effectiveRtRange);
    dataFileStats = new DataFileStatistics(file, featureStats, effectiveRtRange,
        referenceInjectionTime);

    final String tolStr = "mz\tabs\trel\n" + Arrays.stream(dataFileStats.getBestTolerances()).map(
        pair -> "%.4f\t%.4f\t%.1f".formatted(pair.mz(), pair.tolerance().getMzTolerance(),
            pair.tolerance().getPpmTolerance())).collect(Collectors.joining("\n"));
    final String intensitiesStr = Arrays.stream(dataFileStats.getEdgeIntensities())
        .mapToObj("%f"::formatted).collect(Collectors.joining(","));
    final String fwhmStr = Arrays.stream(dataFileStats.getIsotopePeakFwhms())
        .mapToObj("%f"::formatted).collect(Collectors.joining(","));
    final String numIsoDpStr = Arrays.stream(dataFileStats.getNumberOfLowestIsotopeDataPoints())
        .mapToObj(Integer::toString).collect(Collectors.joining(", "));
    logger.finest("Tolerances for data file %s:".formatted(file.getName()));
    logger.finest(tolStr);
    logger.finest("Isotope edge intensities:");
    logger.finest(intensitiesStr);
    logger.finest("Isotope fwhms:");
    logger.finest(fwhmStr);
    logger.finest("Combined tolerances: " + dataFileStats.getMzToleranceForIsotopes());
    logger.finest("Number of isotope dp: " + numIsoDpStr);

    if (showTab && DesktopService.isGUI()) {
      MZmineCore.getDesktop()
          .addTab(new SimpleTab("Auto param", new AutoParametersPane(dataFileStats)));
    }
  }

  /**
   * @param scans              The scans to search in
   * @param mzRangesSorted     the mz ranges to search in
   * @param additionalFeatures
   * @return A list of features of the given mz ranges capped at 5% of the maximum intensity
   */
  private @NotNull List<ModularFeature> getMainSignalFeatures(List<Scan> scans,
      List<Range<Double>> mzRangesSorted, @Nullable List<FeatureRecord> additionalFeatures) {
    final List<ModularFeature> mainPeaks = new ArrayList<>();

    final ExtractMzRangesIonSeriesFunction eicExtraction = new ExtractMzRangesIonSeriesFunction(
        file, scans, mzRangesSorted, ScanDataType.MASS_LIST, this);
    final @NotNull BuildingIonSeries[] ionSeries = eicExtraction.get();
    final ModularFeatureList dummyFlist = FeatureList.createDummy();

    for (int j = 0; j < ionSeries.length; j++) {
      final BuildingIonSeries buildingSeries = ionSeries[j];
      final IonTimeSeries<? extends Scan> fullChromatogram = buildingSeries.toIonTimeSeriesWithLeadingAndTrailingZero(
          getMemoryMapStorage(), scans);

      if(fullChromatogram.getNumberOfValues() < 1) {
        continue;
      }

      final ModularFeature fullFeature = new ModularFeature(dummyFlist, file, fullChromatogram,
          FeatureStatus.DETECTED);

      // check if it was an externally provided feature and if yes, use that as main RT and search from there
      final float rt;
      if (additionalFeatures != null) {
        final int index = BinarySearch.binarySearch(fullFeature.getMZ(), DefaultTo.CLOSEST_VALUE, 0,
            additionalFeatures.size(), i -> additionalFeatures.get(i).mz());
        if (index >= 0 && referenceMatchingTol.checkWithinTolerance(additionalFeatures.get(index).mz(),
            fullFeature.getMZ())) {
          rt = additionalFeatures.get(index).rt();
        } else {
          rt = fullFeature.getRT();
        }
      } else {
        rt = fullFeature.getRT();
      }

      final int mostIntenseIndex = BinarySearch.binarySearch(rt, DefaultTo.CLOSEST_VALUE, 0,
          fullChromatogram.getNumberOfValues(),
          i -> fullChromatogram.getSpectrum(i).getRetentionTime());
      final float highest = (float) fullFeature.getFeatureData().getIntensity(mostIntenseIndex);
      final float edgeIntensity = highest * 0.05f;

      int i = mostIntenseIndex;
      while (i < fullChromatogram.getNumberOfValues()
          && fullChromatogram.getIntensity(i) > edgeIntensity) {
        i++;
      }
      final int rightEdge = i;
      i = mostIntenseIndex;
      while (i > 0 && fullChromatogram.getIntensity(i) > edgeIntensity) {
        i--;
      }
      final int leftEdge = i;
      final int maxEnlargement = Math.min(leftEdge,
          fullChromatogram.getNumberOfValues() - rightEdge);
      final int enlargement = Math.min(maxEnlargement, (int) ((rightEdge - leftEdge) * 0.3));
      if (Math.abs(rightEdge - leftEdge) < 5) {
        // feature was not detected in this case
        continue;
      }

      final IonTimeSeries<? extends Scan> peak = fullChromatogram.subSeries(getMemoryMapStorage(),
          leftEdge - enlargement, rightEdge + enlargement);

      final ModularFeature mainFeature = new ModularFeature(dummyFlist, file, peak,
          FeatureStatus.DETECTED);

      // don't use features with long peaks that may just be chromatographic noise
      if (RangeUtils.rangeLength(mainFeature.getRawDataPointsRTRange())
          > RangeUtils.rangeLength(file.getDataRTRange()) * 0.15) {
        continue;
      }
      mainPeaks.add(mainFeature);
    }
    return mainPeaks;
  }

  /**
   * The frames of IMS files without frame spectra, e.g., from mzML, are empty. Merges the MS1
   * mobility scans into the frames with the default tolerance, as the wizard batch does, so the
   * frames can be analyzed like scans. This reference merge stays in the frames of the file, the
   * tolerance search re-merges into temporary mass lists, see
   * {@link #mergeFrameMassLists(List, MZTolerance)}.
   */
  private void mergeMs1MobilityScansIntoFrames(@NotNull IMSRawDataFile imsFile) {
    final ScanSelection ms1 = ScanSelection.MS1;
    // decision: all MS1 frames, not only those of the polarity, as the merger requires mass lists
    // for all frames of its scan selection
    final AutoMassDetector detector = new AutoMassDetector(0d);
    for (final Frame frame : ms1.getMatchingScans(imsFile.getFrames())) {
      ((SimpleFrame) frame).getMobilityScanStorage()
          .generateAndAddMobilityScanMassLists(getMemoryMapStorage(), detector, false);
    }

    // the defaults, but only MS1
    final MobilityScanMergerParameters mergerParameters = MobilityScanMergerParameters.create(
        new RawDataFilesSelection(new RawDataFile[]{imsFile}),
        MobilityScanMergerParameters.DEFAULT_NOISE_LEVEL,
        MobilityScanMergerParameters.DEFAULT_MERGING_TYPE,
        MobilityScanMergerParameters.DEFAULT_WEIGHTING, ms1,
        MobilityScanMergerParameters.DEFAULT_MZ_TOLERANCE,
        MobilityScanMergerParameters.DEFAULT_MIN_DETECTIONS);
    final MobilityScanMergerTask mergerTask = new MobilityScanMergerTask(imsFile, mergerParameters,
        getModuleCallDate());
    mergerTask.run();
    if (mergerTask.getStatus() != TaskStatus.FINISHED) {
      throw new IllegalStateException(
          "Cannot merge the mobility scans of %s: %s".formatted(imsFile.getName(),
              mergerTask.getErrorMessage()));
    }
  }

  private void applyZeroIntensityMassDetection(List<Scan> scans) {
    final AutoMassDetector detector = new AutoMassDetector(0d);
    for (Scan scan : scans) {
      scan.addMassList(new SimpleMassList(getMemoryMapStorage(), detector.getMassValues(scan)));
    }
  }

  @Override
  public String getTaskDescription() {
    return "";
  }

  public @NotNull RawDataFile getDataFile() {
    return file;
  }

  /**
   * @return the statistics, or null if the task did not run yet, failed, or was canceled
   */
  public @Nullable DataFileStatistics get() {
    return dataFileStats;
  }

  /**
   * Runs the task on this thread.
   *
   * @return the statistics, or null if the task failed or was canceled. The reason is in
   * {@link #getErrorMessage()}.
   */
  public @Nullable DataFileStatistics runAndGet() {
    run();
    return get();
  }
}
