/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder;

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.google.common.math.Quantiles;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScale;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.DataTypeUtils;
import io.github.mzmine.util.FeatureConvertorIonMobility;
import io.github.mzmine.util.FeatureConvertors;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Worker task to build ion mobility traces
 */
public class IonMobilityTraceBuilderTask extends AbstractTask {

  private static Logger logger = Logger.getLogger(IonMobilityTraceBuilderTask.class.getName());
  private final MZmineProject project;
  private final RawDataFile rawDataFile;
  private final String suffix;
  private final List<Frame> frames;
  private final MZTolerance mzTolerance;
  private final String massList;
  private final int minDataPointsRt;
  private final int minTotalSignals;
  private final ScanSelection scanSelection;
  private RangeSet<Double> rangeSet = TreeRangeSet.create();
  private HashMap<Range<Double>, IIonMobilityTrace> rangeToIonTraceMap = new HashMap<>();
  private double progress = 0.0;
  private String taskDescription = "";
  private List<Frame> sortedFrames;
  private final ParameterSet parameters;

  @SuppressWarnings("unchecked")
  public IonMobilityTraceBuilderTask(MZmineProject project, RawDataFile rawDataFile,
      List<Frame> frames, ParameterSet parameters) {
    this.project = project;
    this.rawDataFile = rawDataFile;
    this.mzTolerance =
        parameters.getParameter(IonMobilityTraceBuilderParameters.mzTolerance).getValue();
    this.massList = parameters.getParameter(IonMobilityTraceBuilderParameters.massList).getValue();
    this.minDataPointsRt =
        parameters.getParameter(IonMobilityTraceBuilderParameters.minDataPointsRt).getValue();
    this.minTotalSignals =
        parameters.getParameter(IonMobilityTraceBuilderParameters.minTotalSignals).getValue();
    this.scanSelection =
        parameters.getParameter(IonMobilityTraceBuilderParameters.scanSelection).getValue();
    this.frames = (List<Frame>) scanSelection.getMachtingScans((frames));
    this.sortedFrames = this.frames.stream().sorted(Comparator.comparingInt(Frame::getFrameId))
        .collect(
            Collectors.toList());
    this.suffix = parameters.getParameter(IonMobilityTraceBuilderParameters.suffix).getValue();
    this.parameters = parameters;
    setStatus(TaskStatus.WAITING);
  }

  @Override
  public String getTaskDescription() {
    return taskDescription;
  }

  @Override
  public double getFinishedPercentage() {
    return progress;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    if (isCanceled()) {
      return;
    }
    progress = 0.0;
    Set<RetentionTimeMobilityDataPoint> rtMobilityDataPoints = extractAllDataPointsFromFrames();
    createIonMobilityTraceTargetSet(rtMobilityDataPoints);
    SortedSet<IIonMobilityTrace> ionMobilityTraces = finishIonMobilityTraces();
    buildModularFeatureList(ionMobilityTraces);
    progress = 1.0;
    setStatus(TaskStatus.FINISHED);
  }

  // Extract all retention time and mobility resolved data point sorted by intensity
  private Set<RetentionTimeMobilityDataPoint> extractAllDataPointsFromFrames() {
    logger.info("Start data point extraction");
    taskDescription = "Get data points from frames";
    int processedFrame = 1;
    SortedSet<RetentionTimeMobilityDataPoint> allDataPoints =
        new TreeSet<>(new Comparator<RetentionTimeMobilityDataPoint>() {
          @Override
          public int compare(RetentionTimeMobilityDataPoint o1, RetentionTimeMobilityDataPoint o2) {
            if (o1.getIntensity() > o2.getIntensity()) {
              return 1;
            } else {
              return -1;
            }
          }
        });
    for (Frame frame : frames) {
      if (!scanSelection.matches(frame)) {
        continue;
      }
      for (MobilityScan scan : frame.getMobilityScans()) {
        if (scan.getMassList(massList) == null) {
          setStatus(TaskStatus.ERROR);
          setErrorMessage(
              "Scan #" + scan.getMobilityScamNumber() + " does not have a mass list " + massList);
        } else {
          Arrays.stream(scan.getMassList(massList).getDataPoints()).forEach(
              dp -> allDataPoints.add(
                  new RetentionTimeMobilityDataPoint(scan, dp.getMZ(), dp.getIntensity())));
        }
      }
      progress = (processedFrame / (double) frames.size()) / 4;
      processedFrame++;
    }
    logger.info("Extracted " + allDataPoints.size() + " ims data points");
    return allDataPoints;
  }

  private void createIonMobilityTraceTargetSet(
      Set<RetentionTimeMobilityDataPoint> rtMobilityDataPoints) {
    logger.info("Start m/z ranges calculation");
    taskDescription = "Calculate m/z ranges";
    int processedDataPoint = 1;
    for (RetentionTimeMobilityDataPoint rtMobilityDataPoint : rtMobilityDataPoints) {
      if (isCanceled()) {
        return;
      }
      Range<Double> containsDataPointRange = rangeSet.rangeContaining(rtMobilityDataPoint.getMZ());
      Range<Double> toleranceRange = mzTolerance.getToleranceRange(rtMobilityDataPoint.getMZ());
      if (containsDataPointRange == null) {
        // look +- mz tolerance to see if ther is a range near by.
        // If there is use the proper boundry of that range for the
        // new range to insure than NON OF THE RANGES OVERLAP.
        Range<Double> plusRange = rangeSet.rangeContaining(toleranceRange.upperEndpoint());
        Range<Double> minusRange = rangeSet.rangeContaining(toleranceRange.lowerEndpoint());
        Double toBeLowerBound;
        Double toBeUpperBound;

        // If both of the above ranges are null then we make the new range spaning the full
        // mz tolerance range.
        // If one or both are not null we need to properly modify the range of the new
        // chromatogram so that none of the points are overlapping.
        if ((plusRange == null) && (minusRange == null)) {
          toBeLowerBound = toleranceRange.lowerEndpoint();
          toBeUpperBound = toleranceRange.upperEndpoint();
        } else if ((plusRange == null) && (minusRange != null)) {
          // the upper end point of the minus range will be the lower
          // range of the new one
          toBeLowerBound = minusRange.upperEndpoint();
          toBeUpperBound = toleranceRange.upperEndpoint();

        } else if ((minusRange == null) && (plusRange != null)) {
          toBeLowerBound = toleranceRange.lowerEndpoint();
          toBeUpperBound = plusRange.lowerEndpoint();
        } else if ((minusRange != null) && (plusRange != null)) {
          toBeLowerBound = minusRange.upperEndpoint();
          toBeUpperBound = plusRange.lowerEndpoint();
        } else {
          toBeLowerBound = 0.0;
          toBeUpperBound = 0.0;
        }

        if (toBeLowerBound < toBeUpperBound) {
          Range<Double> newRange = Range.open(toBeLowerBound, toBeUpperBound);
          IIonMobilityTrace newIonMobilityIonTrace = new IonMobilityTrace(
              rtMobilityDataPoint.getMZ(), rtMobilityDataPoint.getRetentionTime(),
              rtMobilityDataPoint.getMobility(), rtMobilityDataPoint.getIntensity(), newRange);
          Set<RetentionTimeMobilityDataPoint> dataPointsSetForTrace = new HashSet<>();
          dataPointsSetForTrace.add(rtMobilityDataPoint);
          newIonMobilityIonTrace.setDataPoints(dataPointsSetForTrace);
          rangeToIonTraceMap.put(newRange, newIonMobilityIonTrace);
          rangeSet.add(newRange);
        } else if (toBeLowerBound.equals(toBeUpperBound) && plusRange != null) {
          IIonMobilityTrace currentIonMobilityIonTrace = rangeToIonTraceMap.get(plusRange);
          currentIonMobilityIonTrace.getDataPoints().add(rtMobilityDataPoint);
        } else {
          throw new IllegalStateException(String.format("Incorrect range [%f, %f] for m/z %f",
              toBeLowerBound, toBeUpperBound, rtMobilityDataPoint.getMZ()));
        }

      } else {
        // In this case we do not need to update the rangeSet

        IIonMobilityTrace currentIonMobilityIonTrace =
            rangeToIonTraceMap.get(containsDataPointRange);
        currentIonMobilityIonTrace.getDataPoints().add(rtMobilityDataPoint);

        // update the entry in the map
        rangeToIonTraceMap.put(containsDataPointRange, currentIonMobilityIonTrace);
      }
      double progressStep = (processedDataPoint / rtMobilityDataPoints.size()) / 4;
      progress += progressStep;
    }
  }

  private SortedSet<IIonMobilityTrace> finishIonMobilityTraces() {
    Set<Range<Double>> ranges = rangeSet.asRanges();
    Iterator<Range<Double>> rangeIterator = ranges.iterator();
    SortedSet<IIonMobilityTrace> ionMobilityTraces =
        new TreeSet<>(new Comparator<IIonMobilityTrace>() {
          @Override
          public int compare(IIonMobilityTrace o1, IIonMobilityTrace o2) {
            if (o1.getMz() > o2.getMz()) {
              return 1;
            } else {
              return -1;
            }
          }
        });
    double progressStep = (!ranges.isEmpty()) ? 0.75 / ranges.size() : 0.0;
    while (rangeIterator.hasNext()) {
      if (isCanceled()) {
        break;
      }
      progress += progressStep;
      Range<Double> currentRangeKey = rangeIterator.next();
      IIonMobilityTrace ionTrace = rangeToIonTraceMap.get(currentRangeKey);
      if (ionTrace.getDataPoints().size() >= minTotalSignals
          && checkNumberOfRetentionTimeDataPoints(ionTrace.getDataPoints())) {
//        logger.info("Build ion trace for m/z range " + ionTrace.getMzRange());
        finishIonTrace(ionTrace);
        ionMobilityTraces.add(ionTrace);
      }
    }
    return ionMobilityTraces;
  }

  private IIonMobilityTrace finishIonTrace(IIonMobilityTrace ionTrace) {
    Range<Double> rawDataPointsIntensityRange = null;
    Range<Double> rawDataPointsMZRange = null;
    Range<Double> rawDataPointsMobilityRange = null;
    Range<Float> rawDataPointsRtRange = null;
    LinkedHashSet<MobilityScan> scanNumbers = new LinkedHashSet<>();
    /*SortedSet<RetentionTimeMobilityDataPoint> sortedRetentionTimeMobilityDataPoints =
        new TreeSet<>((o1, o2) -> {
          int frameComp = Integer
              .compare(o1.getFrame().getScanNumber(), o2.getFrame().getScanNumber());
          if (frameComp != 0) {
            return frameComp;
          } else {
            return Integer.compare(o1.getMobilityScan().getMobilityScamNumber(),
                o2.getMobilityScan().getMobilityScamNumber());
          }
        });*/

    Float rt = 0.0f;
    double mobility = 0.0f;
    // sortedRetentionTimeMobilityDataPoints.addAll(ionTrace.getDataPoints());
    // Update raw data point ranges, height, rt and representative scan
    SortedMap<Frame, SortedSet<RetentionTimeMobilityDataPoint>> groupedDps = FeatureConvertorIonMobility
        .groupDataPointsByFrameId(ionTrace.getDataPoints());
    double maximumIntensity = Double.MIN_VALUE;

    // fill borders of peaks with 0s
    Set<RetentionTimeMobilityDataPoint> frameFillers = addZerosForFrames(sortedFrames,
        (SortedSet<Frame>) groupedDps.keySet(), ionTrace.getMz(),
        ionTrace.getDataPoints(), 0, null, 4, 1);
    Set<RetentionTimeMobilityDataPoint> mobilityScanFillers = new HashSet<>();

    findMedianMobility(ionTrace.getDataPoints());

    for (var sortedRetentionTimeMobilityDataPoints : groupedDps.entrySet()) {
      mobilityScanFillers.addAll(
          addZerosForMobilityScans(sortedRetentionTimeMobilityDataPoints.getKey(),
              sortedRetentionTimeMobilityDataPoints.getValue(),
              ionTrace.getMz(), 2, 1));
      for (var retentionTimeMobilityDataPoint : sortedRetentionTimeMobilityDataPoints.getValue()) {
        scanNumbers.add(retentionTimeMobilityDataPoint.getMobilityScan());
        // set ranges
        if (rawDataPointsIntensityRange == null && rawDataPointsMZRange == null
            && rawDataPointsMobilityRange == null && rawDataPointsRtRange == null) {
          rawDataPointsIntensityRange =
              Range.singleton(retentionTimeMobilityDataPoint.getIntensity());
          rawDataPointsMZRange = Range.singleton(retentionTimeMobilityDataPoint.getMZ());
          rawDataPointsMobilityRange = Range
              .singleton(retentionTimeMobilityDataPoint.getMobility());
          rawDataPointsRtRange = Range.singleton(retentionTimeMobilityDataPoint.getRetentionTime());
        } else {
          rawDataPointsIntensityRange = rawDataPointsIntensityRange
              .span(Range.singleton(retentionTimeMobilityDataPoint.getIntensity()));
          rawDataPointsMZRange =
              rawDataPointsMZRange.span(Range.singleton(retentionTimeMobilityDataPoint.getMZ()));
          rawDataPointsMobilityRange = rawDataPointsMobilityRange
              .span(Range.singleton(retentionTimeMobilityDataPoint.getMobility()));
          rawDataPointsRtRange = rawDataPointsRtRange
              .span(Range.singleton(retentionTimeMobilityDataPoint.getRetentionTime()));
        }

        // set maxima
        if (maximumIntensity < retentionTimeMobilityDataPoint.getIntensity()) {
          maximumIntensity = retentionTimeMobilityDataPoint.getIntensity();
          rt = retentionTimeMobilityDataPoint.getRetentionTime();
          mobility = retentionTimeMobilityDataPoint.getMobility();
        }
      }
    }

    ionTrace.getDataPoints().addAll(frameFillers);
    ionTrace.getDataPoints().addAll(mobilityScanFillers);

    // TODO think about representative scan
    ionTrace.setScanNumbers(scanNumbers);
    ionTrace.setMobilityRange(rawDataPointsMobilityRange);
    ionTrace.setMzRange(rawDataPointsMZRange);
    ionTrace.setRetentionTimeRange(rawDataPointsRtRange);
    ionTrace.setIntensityRange(rawDataPointsIntensityRange);
    ionTrace.setMaximumIntensity(maximumIntensity);
    ionTrace.setRetentionTime(rt);
    ionTrace.setMobility(mobility);
    // logger.info("Ion Trace results:\n" + "Scan numbers: " + ionTrace.getScanNumbers() + "\n" + //
    // "Mobility range: " + ionTrace.getMobilityRange() + "\n" + //
    // "m/z range: " + ionTrace.getMzRange() + "\n" + //
    // "rt range: " + ionTrace.getRetentionTimeRange() + "\n" + //
    // "intensity range: " + ionTrace.getIntensityRange() + "\n" + //
    // "Max intensity : " + ionTrace.getMaximumIntensity() + "\n" + //
    // "Retention time : " + ionTrace.getRetentionTime() + "\n" + //
    // "Mobility : " + ionTrace.getMobility()//
    // );
    // TODO calc area
    // Update area
    // double area = 0;
    // for (int i = 1; i < allScanNumbers.length; i++) {
    // // For area calculation, we use retention time in seconds
    // double previousRT = dataFile.getScan(allScanNumbers[i - 1]).getRetentionTime() * 60d;
    // double currentRT = dataFile.getScan(allScanNumbers[i]).getRetentionTime() * 60d;
    // double previousHeight = dataPointsMap.get(allScanNumbers[i - 1]).getIntensity();
    // double currentHeight = dataPointsMap.get(allScanNumbers[i]).getIntensity();
    // area += (currentRT - previousRT) * (currentHeight + previousHeight) / 2;
    // }

    // TODO
    // Update fragment scan
    // fragmentScan =
    // ScanUtils.findBestFragmentScan(dataFile, dataFile.getDataRTRange(1), rawDataPointsMZRange);

    // allMS2FragmentScanNumbers = ScanUtils.findAllMS2FragmentScans(dataFile,
    // dataFile.getDataRTRange(1), rawDataPointsMZRange);

    // if (fragmentScan > 0) {
    // Scan fragmentScanObject = dataFile.getScan(fragmentScan);
    // int precursorCharge = fragmentScanObject.getPrecursorCharge();
    // if (precursorCharge > 0)
    // this.charge = precursorCharge;
    // }

    return ionTrace;
  }

  private Set<RetentionTimeMobilityDataPoint> addZerosForMobilityScans(Frame frame,
      SortedSet<RetentionTimeMobilityDataPoint> dataPoints, double mz, int minGap, int zeros) {

    int allScansIndex = 0;
    int lastScanIndex = 0;
    final int numScans = frame.getNumberOfMobilityScans();

    List<MobilityScan> mobilityScans = frame.getMobilityScans();
    Set<RetentionTimeMobilityDataPoint> dataPointsToAdd = new HashSet<>();

    for (RetentionTimeMobilityDataPoint dataPoint : dataPoints) {
      MobilityScan nextScan = dataPoint.getMobilityScan();
      while (allScansIndex < numScans && mobilityScans.get(allScansIndex) != nextScan) {
        allScansIndex++;
      }
      if (allScansIndex - lastScanIndex >= minGap) {
        for (int i = 1; i <= zeros; i++) {
          if (lastScanIndex + i < numScans && lastScanIndex != 0) {
            dataPointsToAdd.add(
                new RetentionTimeMobilityDataPoint(mobilityScans.get(lastScanIndex + i), mz, 0d));
          }
          if (allScansIndex - i >= 0) {
            dataPointsToAdd.add(
                new RetentionTimeMobilityDataPoint(mobilityScans.get(allScansIndex - i), mz, 0d));
          }
        }
      }
      lastScanIndex = allScansIndex;
    }
    if (lastScanIndex + 1 < numScans) {
      dataPointsToAdd.add(
          new RetentionTimeMobilityDataPoint(mobilityScans.get(lastScanIndex + 1), mz, 0d));
    }
    return dataPointsToAdd;
  }

  public Set<RetentionTimeMobilityDataPoint> addZerosForFrames(List<Frame> allFrames,
      SortedSet<Frame> currentFrames, double mz, Set<RetentionTimeMobilityDataPoint> currentDps,
      int mobilityWidth,
      PaintScale ps, int minGap, int zeros) {
    final int numFrames = frames.size();
    final int offset = currentFrames.first().getMobilityScans().get(0).getMobilityScamNumber();
    final MobilityType mobilityType = currentFrames.first().getMobilityType();
    int allFramesIndex = 0;
    int lastFrameIndex = 0;

    int timsMobilityScanNumber = -1;
    double medianMobility = -1d;
    // in tims, each subscan number in different frames has the same mobility
    if (mobilityType == MobilityType.TIMS) {
      timsMobilityScanNumber = findMostFrequentMobilityScanNumber(currentDps);
    } else {
      medianMobility = findMedianMobility(currentDps);
    }

    Set<RetentionTimeMobilityDataPoint> dataPointsToAdd = new HashSet<>();
    for (Frame nextFrame : currentFrames) {
      while (allFramesIndex < numFrames && allFrames.get(allFramesIndex) != nextFrame) {
        allFramesIndex++;
      }
      if (allFramesIndex - lastFrameIndex >= minGap) {
        for (int i = 1; i <= zeros; i++) {
          if (lastFrameIndex + i < numFrames && lastFrameIndex != 0) {
            Frame firstEmptyFrame = allFrames.get(lastFrameIndex + i); // the next frame
            MobilityScan mostFrequentScan = null;
            // in tims, each subscan number in different frames has the same mobility
            if (mobilityType == MobilityType.TIMS) {
              mostFrequentScan = firstEmptyFrame
                  .getMobilityScan(timsMobilityScanNumber - offset);
            } else {
              mostFrequentScan = findMobilityScanWithClosestMobility(medianMobility,
                  firstEmptyFrame.getMobilityScans());
            }
            dataPointsToAdd.add(new RetentionTimeMobilityDataPoint(mostFrequentScan, mz, 0d));
            // mobilityScanNumber - offset <- i know this is dirty, but it should be fine
          }
          if (allFramesIndex - i >= 0) {
            Frame lastEmptyFrame = allFrames.get(allFramesIndex - i);
            MobilityScan mostFrequentScan = null;
            if (mobilityType == MobilityType.TIMS) {
              mostFrequentScan = lastEmptyFrame
                  .getMobilityScan(timsMobilityScanNumber - offset);
            } else {
              mostFrequentScan = findMobilityScanWithClosestMobility(medianMobility,
                  lastEmptyFrame.getMobilityScans());
            }
            dataPointsToAdd.add(new RetentionTimeMobilityDataPoint(mostFrequentScan, mz, 0d));
          }
        }
      }
      lastFrameIndex = allFramesIndex;
    }
    if (lastFrameIndex + 1 < numFrames) {
      Frame firstEmptyFrame = allFrames.get(lastFrameIndex + 1); // the next frame
      MobilityScan mostFrequentScan = null;
      if (mobilityType == MobilityType.TIMS) {
        mostFrequentScan = firstEmptyFrame
            .getMobilityScan(timsMobilityScanNumber - offset);
      } else {
        mostFrequentScan = findMobilityScanWithClosestMobility(medianMobility,
            firstEmptyFrame.getMobilityScans());
      }
      dataPointsToAdd.add(new RetentionTimeMobilityDataPoint(
          mostFrequentScan, mz, 0d));
    }
    return dataPointsToAdd;
  }

  /**
   * In Bruker PASEF, every frame in a segment has the same ion mobility range & association of scan
   * number <-> mobility
   *
   * @param dps
   * @return
   */
  private int findMostFrequentMobilityScanNumber(Collection<RetentionTimeMobilityDataPoint> dps) {
    Map<Integer, Long> count = dps.stream().collect(Collectors
        .groupingBy(dp -> dp.getMobilityScan().getMobilityScamNumber(), Collectors.counting()));
    Entry<Integer, Long> mostFrequent = count.entrySet().stream().max(
        Comparator.comparingLong(Entry::getValue)).get();
    return mostFrequent.getKey();
  }

  /**
   * In DTIMS (at least agilent) the observed mobility window can change, therefore we can't just
   * take the most frequent scan number
   *
   * @param dps
   * @return
   */
  private double findMedianMobility(Collection<RetentionTimeMobilityDataPoint> dps) {
    return Quantiles.median().compute(
        dps.stream().map(RetentionTimeMobilityDataPoint::getMobility).collect(Collectors.toSet()));
  }

  private MobilityScan findMobilityScanWithClosestMobility(double mobility,
      List<MobilityScan> mobilityScans) {
    double delta = Double.MAX_VALUE;
    for (int i = 0; i < mobilityScans.size(); i++) {
      MobilityScan scan = mobilityScans.get(i);
      double currentDelta = Math.abs(scan.getMobility() - mobility);
      if (currentDelta < delta) {
        delta = currentDelta;
      }
      if (currentDelta > delta) {
        logger.info(String.format("Original mobility: %f\t closest mobility: %f", mobility,
            mobilityScans.get(i - 1).getMobility()));
        return mobilityScans.get(i - 1);
      }
    }
    return mobilityScans.get(mobilityScans.size() - 1);
  }

  private boolean checkNumberOfRetentionTimeDataPoints(
      Set<RetentionTimeMobilityDataPoint> dataPoints) {
    Set<Float> retentionTimes = new HashSet<>();
    for (RetentionTimeMobilityDataPoint dataPoint : dataPoints) {
      retentionTimes.add(dataPoint.getRetentionTime());
    }
    return (retentionTimes.size() >= minDataPointsRt);
  }

  private void buildModularFeatureList(SortedSet<IIonMobilityTrace> ionMobilityTraces) {
    taskDescription = "Build feature list";
    ModularFeatureList featureList =
        new ModularFeatureList(rawDataFile + " " + suffix, rawDataFile);
    // ensure that the default columns are available
    DataTypeUtils.addDefaultChromatographicTypeColumns(featureList);
    DataTypeUtils.addDefaultIonMobilityTypeColumns(featureList);
    featureList.setSelectedScans(rawDataFile, frames);

    int featureId = 1;
    for (IIonMobilityTrace ionTrace : ionMobilityTraces) {
      ionTrace.setFeatureList(featureList);
      ModularFeature modular =
          FeatureConvertors.IonMobilityIonTraceToModularFeature(ionTrace, rawDataFile);
      ModularFeatureListRow newRow =
          new ModularFeatureListRow(featureList, featureId, rawDataFile, modular);
//      newRow.set(MobilityType.class, ionTrace.getMobility());
      featureList.addRow(newRow);
      featureId++;
    }

    featureList.getAppliedMethods().add(new SimpleFeatureListAppliedMethod(
        IonMobilityTraceBuilderModule.class, parameters));

    project.addFeatureList(featureList);
  }
}
