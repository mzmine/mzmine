/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder;

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.google.common.math.Quantiles;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.DataTypeUtils;
import io.github.mzmine.util.FeatureConvertorIonMobility;
import io.github.mzmine.util.FeatureConvertors;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * Worker task to build ion mobility traces
 */
public class IonMobilityTraceBuilderTask extends AbstractTask {

  public static final int DEFAULT_ALLOWED_MISSING_MOBILITY_SCANS = 0;
  public static final int DEFAULT_ALLOWED_MISSING_FRAMES = 0;
  private static final double STEPS = 4d;

  private static Logger logger = Logger.getLogger(IonMobilityTraceBuilderTask.class.getName());
  private final MZmineProject project;
  private final RawDataFile rawDataFile;
  private final String suffix;
  private final List<Frame> frames;
  private final MZTolerance mzTolerance;
  private final int minDataPointsRt;
  private final int minTotalSignals;
  private final int timsBindWidth;
  private final int twimsBindWidth;
  private final int dtimsBindWidth;
  private final ScanSelection scanSelection;
  private final ParameterSet parameters;
  private RangeSet<Double> rangeSet = TreeRangeSet.create();
  private HashMap<Range<Double>, IIonMobilityTrace> rangeToIonTraceMap = new HashMap<>();
  private double progress = 0.0;
  private String taskDescription = "";
  private final String descriptionPrefix;
  private int allowedMissingFrames = DEFAULT_ALLOWED_MISSING_FRAMES;
  private int allowedMissingMobilityScans = DEFAULT_ALLOWED_MISSING_MOBILITY_SCANS;

  @SuppressWarnings("unchecked")
  public IonMobilityTraceBuilderTask(MZmineProject project, RawDataFile rawDataFile,
      List<Frame> frames, ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(MemoryMapStorage.forFeatureList(), moduleCallDate); // Ims files are usually big, so we create our own
    this.project = project;
    this.rawDataFile = rawDataFile;
    this.mzTolerance = parameters.getParameter(IonMobilityTraceBuilderParameters.mzTolerance)
        .getValue();
    this.minDataPointsRt = parameters
        .getParameter(IonMobilityTraceBuilderParameters.minDataPointsRt).getValue();
    this.minTotalSignals = parameters
        .getParameter(IonMobilityTraceBuilderParameters.minTotalSignals).getValue();
    this.scanSelection = parameters.getParameter(IonMobilityTraceBuilderParameters.scanSelection)
        .getValue();
    this.frames = (List<Frame>) scanSelection.getMatchingScans((frames));
    this.suffix = parameters.getParameter(IonMobilityTraceBuilderParameters.suffix).getValue();

    final ParameterSet advancedParam = parameters
        .getParameter(IonMobilityTraceBuilderParameters.advancedParameters).getValue();
    timsBindWidth =
        advancedParam.getParameter(AdvancedImsTraceBuilderParameters.timsBinningWidth).getValue()
            ? advancedParam.getParameter(AdvancedImsTraceBuilderParameters.timsBinningWidth)
            .getEmbeddedParameter().getValue()
            : BinningMobilogramDataAccess.getRecommendedBinWidth((IMSRawDataFile) rawDataFile);
    dtimsBindWidth =
        advancedParam.getParameter(AdvancedImsTraceBuilderParameters.dtimsBinningWidth).getValue()
            ? advancedParam.getParameter(AdvancedImsTraceBuilderParameters.dtimsBinningWidth)
            .getEmbeddedParameter().getValue()
            : BinningMobilogramDataAccess.getRecommendedBinWidth((IMSRawDataFile) rawDataFile);
    twimsBindWidth =
        advancedParam.getParameter(AdvancedImsTraceBuilderParameters.twimsBinningWidth).getValue()
            ? advancedParam.getParameter(AdvancedImsTraceBuilderParameters.twimsBinningWidth)
            .getEmbeddedParameter().getValue()
            : BinningMobilogramDataAccess.getRecommendedBinWidth((IMSRawDataFile) rawDataFile);

    this.parameters = parameters;
    descriptionPrefix = "Ion mobility trace builder on " + rawDataFile.getName() + ": ";
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
    rtMobilityDataPoints = null;
    SortedSet<IIonMobilityTrace> ionMobilityTraces = finishIonMobilityTraces();
    buildModularFeatureList(ionMobilityTraces);
    progress = 1.0;
    setStatus(TaskStatus.FINISHED);
  }

  // Extract all retention time and mobility resolved data point sorted by intensity
  private Set<RetentionTimeMobilityDataPoint> extractAllDataPointsFromFrames() {
    logger.info("Start data point extraction");
    taskDescription = descriptionPrefix + " Getting data points from frames";
    int processedFrame = 1;
    SortedSet<RetentionTimeMobilityDataPoint> allDataPoints = new TreeSet<>(
        new Comparator<RetentionTimeMobilityDataPoint>() {
          @Override
          public int compare(RetentionTimeMobilityDataPoint o1, RetentionTimeMobilityDataPoint o2) {
            if (o1.getIntensity() > o2.getIntensity()) {
              return -1;
            } else {
              return 1;
            }
          }
        });
    for (Frame frame : frames) {
      if (!scanSelection.matches(frame)) {
        continue;
      }

      double[] mzBuffer = new double[0];
      double[] intensityBuffer = new double[0];
      int numDp = 0;

      for (MobilityScan scan : frame.getMobilityScans()) {
        if (scan.getMassList() == null) {
          setStatus(TaskStatus.ERROR);
          setErrorMessage("Scan #" + scan.getMobilityScanNumber()
              + " does not have a mass list. Run mass detection ");
        } else {
          MassList ml = scan.getMassList();
          mzBuffer = ml.getMzValues(mzBuffer);
          intensityBuffer = ml.getIntensityValues(intensityBuffer);
          numDp = scan.getMassList().getNumberOfDataPoints();
          for (int i = 0; i < numDp; i++) {
            allDataPoints
                .add(new RetentionTimeMobilityDataPoint(scan, mzBuffer[i], intensityBuffer[i]));
          }
        }
      }
      progress = (processedFrame / (double) frames.size()) / STEPS;
      processedFrame++;
    }
    logger.info("Extracted " + allDataPoints.size() + " ims data points");
    return allDataPoints;
  }

  private void createIonMobilityTraceTargetSet(
      Set<RetentionTimeMobilityDataPoint> rtMobilityDataPoints) {
    logger.info("Start m/z ranges calculation");
    taskDescription = descriptionPrefix + "Calculating m/z ranges.";

    final double progressStep = (1 / (double) rtMobilityDataPoints.size()) / STEPS;
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
          throw new IllegalStateException(String
              .format("Incorrect range [%f, %f] for m/z %f", toBeLowerBound, toBeUpperBound,
                  rtMobilityDataPoint.getMZ()));
        }

      } else {
        // In this case we do not need to update the rangeSet

        IIonMobilityTrace currentIonMobilityIonTrace = rangeToIonTraceMap
            .get(containsDataPointRange);
        currentIonMobilityIonTrace.getDataPoints().add(rtMobilityDataPoint);

        // update the entry in the map
        rangeToIonTraceMap.put(containsDataPointRange, currentIonMobilityIonTrace);
      }

      progress += progressStep;
    }
  }

  private SortedSet<IIonMobilityTrace> finishIonMobilityTraces() {
    Set<Range<Double>> ranges = rangeSet.asRanges();
    Iterator<Range<Double>> rangeIterator = ranges.iterator();
    SortedSet<IIonMobilityTrace> ionMobilityTraces = new TreeSet<>(
        new Comparator<IIonMobilityTrace>() {
          @Override
          public int compare(IIonMobilityTrace o1, IIonMobilityTrace o2) {
            if (o1.getMz() > o2.getMz()) {
              return 1;
            } else {
              return -1;
            }
          }
        });

    final double progressStep = (!ranges.isEmpty()) ? 1.0d / ranges.size() / STEPS : 0.0;
    while (rangeIterator.hasNext()) {
      if (isCanceled()) {
        break;
      }
      progress += progressStep;
      Range<Double> currentRangeKey = rangeIterator.next();
      IIonMobilityTrace ionTrace = rangeToIonTraceMap.get(currentRangeKey);
      if (ionTrace.getDataPoints().size() >= minTotalSignals) {
        ionTrace = finishIonTrace(ionTrace);
        if (ionTrace != null) {
          ionMobilityTraces.add(ionTrace);
        }
      }
    }
    return ionMobilityTraces;
  }

  private IIonMobilityTrace finishIonTrace(IIonMobilityTrace ionTrace) {
    SortedMap<Frame, SortedSet<RetentionTimeMobilityDataPoint>> groupedDps = FeatureConvertorIonMobility
        .groupDataPointsByFrameId(ionTrace.getDataPoints());

    if (!checkConsecutiveFrames(groupedDps, frames)) {
      return null;
    }

    // fill borders of peaks with 0s
    Set<RetentionTimeMobilityDataPoint> frameFillers = addZerosForFrames(frames,
        (SortedSet<Frame>) groupedDps.keySet(), ionTrace.getMz(), ionTrace.getDataPoints(),
        allowedMissingFrames, 1);
    Set<RetentionTimeMobilityDataPoint> mobilityScanFillers = new HashSet<>();

    findMedianMobility(ionTrace.getDataPoints());

    for (var sortedRetentionTimeMobilityDataPoints : groupedDps.entrySet()) {
      mobilityScanFillers.addAll(
          addZerosForMobilityScans(sortedRetentionTimeMobilityDataPoints.getKey(),
              sortedRetentionTimeMobilityDataPoints.getValue(), ionTrace.getMz(),
              allowedMissingMobilityScans, 1));
    }

    ionTrace.getDataPoints().addAll(frameFillers);
    ionTrace.getDataPoints().addAll(mobilityScanFillers);

    return ionTrace;
  }

  private boolean checkConsecutiveFrames(SortedMap<Frame, ?> groupedDps, List<Frame> frames) {
    // check for consecutive frames
    int consecutive = 0;

    Iterator<Frame> frameIterator = groupedDps.keySet().iterator();
    Frame frame = frameIterator.next();
    int index = frames.indexOf(frame);
    boolean found = false;
    for (int i = index + 1; i < frames.size() && frameIterator.hasNext(); i++) {
      // now go forward
      frame = frameIterator.next();
      if (frame == frames.get(i)) {
        consecutive++;
        if (consecutive >= minDataPointsRt) {
          found = true;
          break;
        }
      } else {
        i = frames.indexOf(frame);
        consecutive = 0;
      }
    }
    return found;
  }

  private Set<RetentionTimeMobilityDataPoint> addZerosForMobilityScans(Frame frame,
      SortedSet<RetentionTimeMobilityDataPoint> dataPoints, double mz, int allowedGap, int zeros) {

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
      if (allScansIndex - lastScanIndex > (allowedGap + 1)) {
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
      dataPointsToAdd
          .add(new RetentionTimeMobilityDataPoint(mobilityScans.get(lastScanIndex + 1), mz, 0d));
    }
    return dataPointsToAdd;
  }

  public Set<RetentionTimeMobilityDataPoint> addZerosForFrames(List<Frame> allFrames,
      SortedSet<Frame> currentFrames, double mz, Set<RetentionTimeMobilityDataPoint> currentDps,
      int allowedGap, int zeros) {
    final int numFrames = frames.size();
    final int offset = currentFrames.first().getMobilityScans().get(0).getMobilityScanNumber();
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
      if (allFramesIndex - lastFrameIndex > (allowedGap + 1)) {
        for (int i = 1; i <= zeros; i++) {
          if (lastFrameIndex + i < numFrames && lastFrameIndex != 0) {
            Frame firstEmptyFrame = allFrames.get(lastFrameIndex + i); // the next frame
            MobilityScan mostFrequentScan = null;
            // in tims, each subscan number in different frames has the same mobility
            if (mobilityType == MobilityType.TIMS) {
              mostFrequentScan = firstEmptyFrame.getMobilityScan(timsMobilityScanNumber - offset);
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
              mostFrequentScan = lastEmptyFrame.getMobilityScan(timsMobilityScanNumber - offset);
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
        mostFrequentScan = firstEmptyFrame.getMobilityScan(timsMobilityScanNumber - offset);
      } else {
        mostFrequentScan = findMobilityScanWithClosestMobility(medianMobility,
            firstEmptyFrame.getMobilityScans());
      }
      dataPointsToAdd.add(new RetentionTimeMobilityDataPoint(mostFrequentScan, mz, 0d));
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
        .groupingBy(dp -> dp.getMobilityScan().getMobilityScanNumber(), Collectors.counting()));
    Entry<Integer, Long> mostFrequent = count.entrySet().stream()
        .max(Comparator.comparingLong(Entry::getValue)).get();
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
        /*logger.info(String.format("Original mobility: %f\t closest mobility: %f", mobility,
            mobilityScans.get(i - 1).getMobility()));*/
        return mobilityScans.get(i - 1);
      }
    }
    return mobilityScans.get(mobilityScans.size() - 1);
  }

  private void buildModularFeatureList(SortedSet<IIonMobilityTrace> ionMobilityTraces) {
    taskDescription = descriptionPrefix + "Building feature list.";
    ModularFeatureList featureList = new ModularFeatureList(rawDataFile + " " + suffix,
        getMemoryMapStorage(), rawDataFile);
    // ensure that the default columns are available
    DataTypeUtils.addDefaultChromatographicTypeColumns(featureList);
    DataTypeUtils.addDefaultIonMobilityTypeColumns(featureList);
    featureList.setSelectedScans(rawDataFile, frames);

    final int binWidth = switch (((IMSRawDataFile) rawDataFile).getMobilityType()) {
      case DRIFT_TUBE -> dtimsBindWidth;
      case TIMS -> timsBindWidth;
      case TRAVELING_WAVE -> twimsBindWidth;
      default -> 1;
    };

    final BinningMobilogramDataAccess mobilogramBinner = EfficientDataAccess
        .of((IMSRawDataFile) rawDataFile, binWidth);

    final double progressStep = 1.0d / ionMobilityTraces.size() / STEPS;

    int featureId = 1;
    for (IIonMobilityTrace ionTrace : ionMobilityTraces) {
      ionTrace.setFeatureList(featureList);
      ModularFeature modular = FeatureConvertors.IonMobilityIonTraceToModularFeature(ionTrace,
          rawDataFile, mobilogramBinner);
      ModularFeatureListRow newRow = new ModularFeatureListRow(featureList, featureId, modular);
//      newRow.set(MobilityType.class, ionTrace.getMobility());
      featureList.addRow(newRow);
      featureId++;
      progress += progressStep;
    }

    // sort and reset IDs here to have the same sorting for every feature list
    FeatureListUtils.sortByDefaultRT(featureList, true);

    rawDataFile.getAppliedMethods().forEach(m -> featureList.getAppliedMethods().add(m));
    featureList.getAppliedMethods().add(
        new SimpleFeatureListAppliedMethod(IonMobilityTraceBuilderModule.class, parameters,
            getModuleCallDate()));

    project.addFeatureList(featureList);
  }
}
