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
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.PseudoSpectrum;
import io.github.mzmine.datamodel.PseudoSpectrumType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.data_access.ScanDataAccess;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.featuredata.IntensityTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.correlation.CorrelationData;
import io.github.mzmine.datamodel.features.types.numbers.MobilityType;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.datamodel.impl.SimplePseudoSpectrum;
import io.github.mzmine.datamodel.msms.IonMobilityMsMsInfo;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ADAPChromatogramBuilderParameters;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ModularADAPChromatogramBuilderModule;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ModularADAPChromatogramBuilderTask;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.correlation.FeatureCorrelationUtil.DIA;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.scans.SpectraMerging;
import io.github.mzmine.util.scans.SpectraMerging.IntensityMergingType;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
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
import java.util.Set;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DiaMs2CorrTaskNew extends AbstractTask {

  private static final Logger logger = Logger.getLogger(DiaMs2CorrTaskNew.class.getName());

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

  protected DiaMs2CorrTaskNew(@Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
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

  @Override
  public String getTaskDescription() {
    return "DIA MS2 for feature list: " + flist.getName() + " " + (
        adapTask != null && !adapTask.isFinished() ? adapTask.getTaskDescription() : description);
  }

  @Override
  public double getFinishedPercentage() {
    return (adapTask != null ? adapTask.getFinishedPercentage() * 0.5 : 0)
        + (currentRow / (double) numRows) * 0.5d;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    if (flist.getNumberOfRawDataFiles() != 1) {
      setErrorMessage("Cannot build DIA MS2 for feature lists with more than one raw data file.");
      setStatus(TaskStatus.ERROR);
    }

    final RawDataFile file = flist.getRawDataFile(0);
    final Map<IsolationWindow, List<Scan>> isolationWindowScanMap = extractIsolationWindows(file);
    final Map<IsolationWindow, RawDataFile> isolationWindowFileMap = buildIsolationWindowFiles(
        isolationWindowScanMap);
    final Map<IsolationWindow, FeatureList> ms2Flists = buildChromatograms(isolationWindowFileMap);
    final Map<IsolationWindow, RangeMap<Double, IonTimeSeries<?>>> isoWindowEicsMap = mapIsoWindowToEics(
        ms2Flists, file);
    final Set<IsolationWindow> isolationWindows = isoWindowEicsMap.keySet();

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
      processIsolationWindows(feature, matchingWindows, isoWindowEicsMap, isolationWindowScanMap);


    }

    if (ms2Flists.isEmpty()) {
      flist.getAppliedMethods().add(
          new SimpleFeatureListAppliedMethod(DiaMs2CorrModule.class, parameters,
              getModuleCallDate()));
      setStatus(TaskStatus.FINISHED);
      return;
    }

    final ScanDataAccess access = EfficientDataAccess.of(file, ScanDataType.MASS_LIST,
        ms2ScanSelection);

    flist.getAppliedMethods().add(
        new SimpleFeatureListAppliedMethod(DiaMs2CorrModule.class, parameters,
            getModuleCallDate()));
    setStatus(TaskStatus.FINISHED);
  }

  private void processIsolationWindows(Feature feature, List<IsolationWindow> matchingWindows,
      Map<IsolationWindow, RangeMap<Double, IonTimeSeries<?>>> isoWindowEicsMap,
      Map<IsolationWindow, List<Scan>> isoWindowScansMap) {
    final IonTimeSeries<? extends Scan> ms1Eic = feature.getFeatureData();
    final double[][] shape = extractPointsAroundMaximum(feature.getHeight() * correlationThreshold,
        ms1Eic, feature.getRepresentativeScan());
    if (shape == null || shape[0].length < minCorrPoints) {
      return;
    }

    final double[] ms1Rts = shape[0];
    final double[] ms1Intensities = shape[1];
    final Range<Float> correlationRange = Range.closed((float) ms1Rts[0],
        (float) ms1Rts[ms1Rts.length - 1]);

    for (IsolationWindow window : matchingWindows) {
      var ms2Scans = isoWindowScansMap.get(window);
      if (!checkMs2ScanRequirements(feature.getRT(), ms2Scans, correlationRange)) {
        continue;
      }

      final Scan closestMs2 = getClosestMs2(feature.getRT(), ms2Scans);
      final RangeMap<Double, IonTimeSeries<?>> eics = isoWindowEicsMap.get(window);

      final List<IonTimeSeries<?>> eligibleEics = getEligibleEics(closestMs2, eics);
      if (eligibleEics.isEmpty()) {
        continue;
      }

      final PseudoSpectrum ms2 = extractCorrelatedMs2(feature, correlationRange, eligibleEics,
          ms1Rts, ms1Intensities);

      if (ms2 == null) {
        continue;
      }

      ms2.getRetentionTime()

    }

  }

  private @Nullable PseudoSpectrum extractCorrelatedMs2(Feature feature,
      Range<Float> correlationRange, List<IonTimeSeries<?>> eligibleEics, double[] ms1Rts,
      double[] ms1Intensities) {
    DoubleArrayList ms2Mzs = new DoubleArrayList();
    DoubleArrayList ms2Intensities = new DoubleArrayList();

    for (IonTimeSeries<?> ms2Eic : eligibleEics) {
      final IntensityTimeSeries subSeries = ms2Eic.subSeries(getMemoryMapStorage(),
          correlationRange.lowerEndpoint(), correlationRange.upperEndpoint());
      final int num = subSeries.getNumberOfValues();
      final double[] intensities = new double[num];
      final double[] rts = new double[num];
      for (int i = 0; i < num; i++) {
        intensities[i] = subSeries.getIntensity(i);
        rts[i] = subSeries.getRetentionTime(i);
      }

      final CorrelationData correlationData = DIA.corrFeatureShape(ms1Rts, ms1Intensities, rts,
          intensities, minCorrPoints, 2, minMs2Intensity / 3);
      if (correlationData != null && correlationData.isValid() && correlationData.getPearsonR() > 0
          && correlationData.getPearsonR() > minPearson) {
        int startIndex = -1;
        int endIndex = -1;
        double maxIntensity = Double.NEGATIVE_INFINITY;

        final List<Scan> spectra = (List<Scan>) ms2Eic.getSpectra();
        for (int j = 0; j < spectra.size(); j++) {
          Scan spectrum = spectra.get(j);
          if (startIndex == -1 && correlationRange.contains(spectrum.getRetentionTime())) {
            startIndex = j;
          }
          if (startIndex != -1 && ms2Eic.getIntensity(j) > maxIntensity) {
            maxIntensity = ms2Eic.getIntensity(j);
          }
          if (startIndex != -1 && !correlationRange.contains(spectrum.getRetentionTime())) {
            endIndex = j - 1;
            break;
          }
        }
        // no value in ms1 feature rt range
        if (startIndex == -1) {
          continue;
        }
        // all values in ms1 feature rt range
        if (endIndex == -1) {
          endIndex = ms2Eic.getNumberOfValues() - 1;
        }

        final double mz = FeatureDataUtils.calculateCenterMz(ms2Eic,
            FeatureDataUtils.DEFAULT_CENTER_FUNCTION, startIndex, endIndex);

        // for IMS measurements, the ion must be present in the MS2 mobility scans in the during
        // the feature's rt window and within the mobility scans of the feature's mobility window.
        // we could also look at mobility shape and correlate that, but it would probably take a
        // lot of optimisation and/or too long to compute
        if (mergedMobilityScan != null && mergedMobilityScan.getNumberOfDataPoints() > 1) {
          boolean mzFound = false;
          final double upper = mzTolerance.getToleranceRange(mz).upperEndpoint();
          for (int i = 0; i < mergedMobilityScan.getNumberOfDataPoints(); i++) {
            if (mzTolerance.checkWithinTolerance(mz, mergedMobilityScan.getMzValue(i))) {
              mzFound = true;
              break;
            } else if (mergedMobilityScan.getMzValue(i) > upper) {
              break;
            }
          }
          if (!mzFound) {
            continue; // dont add this mz
          }
        }
        ms2Mzs.add(mz);
        ms2Intensities.add(maxIntensity);
      }
    }

    if (ms2Mzs.isEmpty()) {
      return null;
    }

    PseudoSpectrum ms2 = new SimplePseudoSpectrum(originalDataFile, 2, feature.getRT(), null,
        ms2Mzs.toDoubleArray(), ms2Intensities.toDoubleArray(),
        feature.getRepresentativeScan().getPolarity(),
        String.format("Pseudo MS2 (R >= %.2f)", minPearson), PseudoSpectrumType.LC_DIA);
    return ms2;
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

    return result;
  }

  private boolean checkMs2ScanRequirements(float featureRt, List<Scan> ms2Scans,
      Range<Float> correlationRange) {
    final List<Scan> ms2sInRtRange = ms2Scans.stream()
        .filter(scan -> correlationRange.contains(scan.getRetentionTime())).toList();
    Scan closestMs2 = getClosestMs2(featureRt, ms2sInRtRange);
    if (closestMs2 == null || ms2sInRtRange.isEmpty() || ms2sInRtRange.size() < minCorrPoints) {
      logger.fine(() -> "Could not find enough ms2s in rtRange %s".formatted(correlationRange));
      return false;
    }
    return true;
  }

  private static @NotNull Map<IsolationWindow, RangeMap<Double, IonTimeSeries<?>>> mapIsoWindowToEics(
      Map<IsolationWindow, FeatureList> ms2Flists, RawDataFile file) {
    final Map<IsolationWindow, RangeMap<Double, IonTimeSeries<?>>> isoWindowEicsMap = new HashMap<>();

    for (Entry<IsolationWindow, FeatureList> entry : ms2Flists.entrySet()) {
      // store feature data in TreeRangeMap, to query by m/z in ms2 spectra
      var ms2Flist = entry.getValue();
      final RangeMap<Double, IonTimeSeries<?>> ms2Eics = TreeRangeMap.create();
      ms2Flist.getRows().stream().map(row -> row.getFeature(file)).filter(Objects::nonNull)
          .sorted(Comparator.comparingDouble(Feature::getHeight).reversed()).forEach(
              feature -> ms2Eics.put(SpectraMerging.createNewNonOverlappingRange(ms2Eics,
                  feature.getRawDataPointsMZRange()), feature.getFeatureData()));
      isoWindowEicsMap.put(entry.getKey(), ms2Eics);
    }
    return isoWindowEicsMap;
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

      for (Entry<IsolationWindow, List<Scan>> entry : isolationWindowScanMap.entrySet()) {
        final IsolationWindow isolationWindow = entry.getKey();
        final IMSRawDataFileImpl windowFile = new IMSRawDataFileImpl(
            file.getName() + " %s".formatted(isolationWindow.toString()), null,
            getMemoryMapStorage());

        for (Scan scan : entry.getValue()) {
          if (!(scan instanceof Frame frame)) {
            logger.warning(
                () -> "Data file %s is an ims file but also contains scans without ims dimension %s.".formatted(
                    file.getName(), ScanUtils.scanToString(scan)));
            continue;
          }

          // merge scans from isolation window only
          final List<MobilityScan> mobilityScansInWindow = frame.getMobilityScans().stream()
              .filter(isolationWindow::contains).toList();
          final double[][] mzIntensities = SpectraMerging.calculatedMergedMzsAndIntensities(
              mobilityScansInWindow, mzTolerance, IntensityMergingType.SUMMED,
              SpectraMerging.DEFAULT_CENTER_FUNCTION, null, null, 2);

          final SimpleFrame newFrame = new SimpleFrame(windowFile, scan.getScanNumber(),
              scan.getMSLevel(), scan.getRetentionTime(), mzIntensities[0], mzIntensities[1],
              scan.getSpectrumType(), scan.getPolarity(), scan.getScanDefinition(),
              scan.getScanningMZRange(), ((Frame) scan).getMobilityType(), null,
              scan.getInjectionTime());
          windowFile.addScan(newFrame);
        }

        isolationWindowMergingProgress = (++finishedIsolationWindows) / numIsolationWindows;
        logger.finest(
            "File: %s - Finished merging isolation window %s (%.0f/%.0f)".formatted(file.getName(),
                isolationWindow.toString(), finishedIsolationWindows, numIsolationWindows));

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

  private void processFeatureList(List<FeatureListRow> rows, RawDataFile originalDataFile,
      final RangeMap<Double, IonTimeSeries<?>> ms2Eics, final List<Scan> ms2Scans,
      ScanDataAccess ms2ScanAccess) {
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
    if (adapTask != null) {
      adapTask.cancel();
    }
  }

  private Map<IsolationWindow, List<Scan>> extractIsolationWindows(
      @NotNull final RawDataFile file) {
    final ScanSelection scanSelection = new ScanSelection(MsLevelFilter.of(2));
    Map<IsolationWindow, List<Scan>> windowScanMap = new HashMap<>();
    for (Scan scan : scanSelection.getMatchingScans(file)) {

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
