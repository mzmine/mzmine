/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_recursiveimsbuilder;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.MobilityScanDataType;
import io.github.mzmine.datamodel.data_access.MobilityScanDataAccess;
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.FeatureShapeMobilogramType;
import io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder.RetentionTimeMobilityDataPoint;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.DataTypeUtils;
import io.github.mzmine.util.FeatureConvertors;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.scans.SpectraMerging;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sun.misc.Unsafe;

public class RecursiveIMSBuilderTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(RecursiveIMSBuilderTask.class.getName());
  private static final int RECURSIVE_THRESHOLD = 50;
  private static final int STEPS = 5;

  private final IMSRawDataFile file;
  private final ParameterSet parameters;
  private final ScanSelection scanSelection;
  private final MZmineProject project;
  private final MZTolerance tolerance;
  private final MemoryMapStorage tempStorage = MemoryMapStorage.forFeatureList();
  private final boolean enableRecursive = true;
  private final int numConsecutiveFrames;
  private final int numDataPoints;
  private final int binWidth;
  private AtomicInteger stepProcessed = new AtomicInteger(0);
  private int stepTotal = 0;
  private int currentStep = 0;

  public RecursiveIMSBuilderTask(@Nullable MemoryMapStorage storage,
      @NotNull final IMSRawDataFile file,
      @NotNull final ParameterSet parameters, MZmineProject project, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    this.file = file;
    this.parameters = parameters;
    scanSelection = parameters.getParameter(RecursiveIMSBuilderParameters.scanSelection).getValue();
    tolerance = parameters.getParameter(RecursiveIMSBuilderParameters.mzTolerance).getValue();
    numConsecutiveFrames = parameters.getParameter(RecursiveIMSBuilderParameters.minNumConsecutive)
        .getValue();
    numDataPoints = parameters.getParameter(RecursiveIMSBuilderParameters.minNumDatapoints)
        .getValue();

    final var advancedParam = parameters
        .getParameter(RecursiveIMSBuilderParameters.advancedParameters).getValue();
    binWidth = switch (file.getMobilityType()) {
      case TIMS ->
          advancedParam.getParameter(RecursiveIMSBuilderAdvancedParameters.timsBinningWidth)
              .getValue() ? advancedParam
              .getParameter(RecursiveIMSBuilderAdvancedParameters.timsBinningWidth)
              .getEmbeddedParameter().getValue()
              : BinningMobilogramDataAccess.getRecommendedBinWidth(file);
      case DRIFT_TUBE ->
          advancedParam.getParameter(RecursiveIMSBuilderAdvancedParameters.dtimsBinningWidth)
              .getValue() ? advancedParam
              .getParameter(RecursiveIMSBuilderAdvancedParameters.dtimsBinningWidth)
              .getEmbeddedParameter().getValue()
              : BinningMobilogramDataAccess.getRecommendedBinWidth(file);
      case TRAVELING_WAVE ->
          advancedParam.getParameter(RecursiveIMSBuilderAdvancedParameters.twimsBinningWidth)
              .getValue() ? advancedParam
              .getParameter(RecursiveIMSBuilderAdvancedParameters.twimsBinningWidth)
              .getEmbeddedParameter().getValue()
              : BinningMobilogramDataAccess.getRecommendedBinWidth(file);
      default -> 1;
    };

    this.project = project;
  }

  private static int findMostFrequentMobilityScanNumber(List<IonMobilitySeries> mobilograms) {
    List<MobilityScan> scans = mobilograms.stream().flatMap(mob -> mob.getSpectra().stream())
        .collect(Collectors.toList());
    Map<Integer, Long> count = scans.stream().collect(Collectors
        .groupingBy(MobilityScan::getMobilityScanNumber, Collectors.counting()));
    Entry<Integer, Long> mostFrequent = count.entrySet().stream().max(
        Comparator.comparingLong(Entry::getValue)).get();
    return mostFrequent.getKey();
  }

  @Override
  public String getTaskDescription() {
    return "Running feature detection on " + file.getName();
  }

  @Override
  public double getFinishedPercentage() {
    return ((stepProcessed.get() / (double) stepTotal) / STEPS + (currentStep / (double) STEPS));
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    final MobilityScanDataAccess access = EfficientDataAccess
        .of(file, MobilityScanDataType.MASS_LIST, scanSelection);

    logger.finest(() -> "Extracting data points from mobility scans and building mobilograms...");
    stepProcessed.set(0);
    stepTotal = access.getNumberOfScans();

    // build mobilograms for all frames
    final TreeSet<BuildingIonMobilitySeries> sortedMobilograms = buildFrameMobilograms(access);
    if (isCanceled()) {
      return;
    }

    // create ion traces from mobilograms
    currentStep++;
    stepProcessed.set(0);
    stepTotal = sortedMobilograms.size();
    final List<TempIMTrace> ionMobilityTraces = createTempIMTraces(
        sortedMobilograms, tolerance);
    if (isCanceled()) {
      return;
    }

    // remove noise and check traces for validity
    stepProcessed.set(0);
    currentStep++;
    stepTotal = ionMobilityTraces.size();
    logger.finest(() -> "Removing noise from traces...");
    final List<TempIMTrace> validTraces = ionMobilityTraces.parallelStream().filter(trace -> {
      stepProcessed.getAndIncrement();
      return checkConsecutiveRemoveNoise(trace, access.getEligibleFrames(), numConsecutiveFrames,
          numDataPoints);
    }).collect(Collectors.toList());

    logger.finest(() -> "Noise removed.");
    if (isCanceled()) {
      return;
    }

    // add leading & trailing zeros
    stepProcessed.set(0);
    currentStep++;
    stepTotal = validTraces.size();
    logger.finest(() -> "Adding leading and trailing zeros...");
    addZerosForFrames(validTraces, access.getEligibleFrames());
    logger.finest(() -> "Leading and trailing zeros added.");

    // build feature list
    stepTotal = validTraces.size();
    stepProcessed.set(0);
    currentStep++;
    final ModularFeatureList flist = new ModularFeatureList(file.getName(), getMemoryMapStorage(),
        file);
    flist.setSelectedScans(file, access.getEligibleFrames());
    logger.finest(() -> "Creation BinningMobilogramDataAccess for raw data file " + file.getName());
    final BinningMobilogramDataAccess binningMobilogramDataAccess = EfficientDataAccess
        .of(file, binWidth);
    int id = 0;
    final List<TempIMTrace> sortedTraces = validTraces.stream()
        .sorted(Comparator.comparingDouble(TempIMTrace::getCenterMz)).collect(Collectors.toList());
    for (TempIMTrace trace : sortedTraces) {
      if (isCanceled()) {
        return;
      }

      final ModularFeature f = FeatureConvertors.tempIMTraceToModularFeature(trace, file,
          binningMobilogramDataAccess, flist);
      final ModularFeatureListRow row = new ModularFeatureListRow(flist, id, f);
      row.set(FeatureShapeMobilogramType.class, true);
      flist.addRow(row);
      id++;
      stepProcessed.getAndIncrement();
    }

    // sort and reset IDs here to have the same sorting for every feature list
    FeatureListUtils.sortByDefaultRT(flist, true);

    flist.getAppliedMethods().addAll(file.getAppliedMethods());
    flist.getAppliedMethods().add(
        new SimpleFeatureListAppliedMethod(RecursiveIMSBuilderModule.class, parameters,
            getModuleCallDate()));
    DataTypeUtils.addDefaultIonMobilityTypeColumns(flist);
    project.addFeatureList(flist);

    /*final Unsafe theUnsafe = initUnsafe();
    if (theUnsafe != null && tempStorage != null) {
      logger.finest(() -> "Clearing temporary files...");
      try {
        tempStorage.discard(theUnsafe);
      } catch (IOException e) {
        e.printStackTrace();
        logger.log(Level.WARNING, e, e::getMessage);
      }
    }*/

    setStatus(TaskStatus.FINISHED);
  }

  private boolean checkConsecutiveRemoveNoise(TempIMTrace trace, List<Frame> eligibleFrames,
      int reqConsecutive, int numDataPoints) {

    final int numDp = trace.getNumberOfDataPoints();
    if (numDp < numDataPoints
        || trace.getMobilograms().size() < reqConsecutive) {
      return false;
    }

    boolean isConsecutive = false;
    int numConsecutive = 0;
    int allFramesIndex = 0;
    int prevIndex = 0;

    if (trace.getMobilograms().size() == 1 && reqConsecutive <= 1) {
      return numDp > numDataPoints;
    }

    Set<IonMobilitySeries> noise = new LinkedHashSet<>();

    List<IonMobilitySeries> mobilograms = trace.getMobilograms();
    final int mobilogramsSize = mobilograms.size();
    final int numFrames = eligibleFrames.size();

    for (int i = 0; i < mobilogramsSize && allFramesIndex < numFrames; i++) {
      IonMobilitySeries mobilogram = mobilograms.get(i);
      final Frame frame = mobilogram.getSpectrum(0).getFrame();

      while (allFramesIndex < numFrames && frame != eligibleFrames.get(allFramesIndex)) {
        allFramesIndex++;
      }

      // remove noise signals
      if (i == 0) {
        if (mobilograms.get(i + 1).getSpectrum(0).getFrame() != eligibleFrames
            .get(allFramesIndex + 1)) {
          // first signal is noise
          noise.add(mobilogram);
        }
      } else if (i == mobilogramsSize - 1) {
        if (mobilograms.get(i - 1).getSpectrum(0).getFrame() != eligibleFrames
            .get(allFramesIndex - 1)) {
          // last mobilogram is noise
          noise.add(mobilogram);
        }
      } else if (mobilograms.get(i - 1).getSpectrum(0).getFrame() != eligibleFrames
          .get(allFramesIndex - 1)
          && mobilograms.get(i + 1).getSpectrum(0).getFrame() != eligibleFrames
          .get(allFramesIndex + 1)) {
        // some mobilogram is noise
        noise.add(mobilogram);
      }

      if (allFramesIndex - prevIndex <= 1) {
        numConsecutive++;
        if (numConsecutive >= reqConsecutive) {
          isConsecutive = true;
        }
      } else {
        numConsecutive = 0;
      }
      prevIndex = allFramesIndex;
    }

//    logger.finest(() -> "Removing " + noise.size() + "/" + mobilogramsSize + " noise signals");
    trace.removeMobilograms(noise);

    return isConsecutive && trace.getNumberOfDataPoints() >= numDataPoints;
  }

  private void addZerosForFrames(Collection<TempIMTrace> traces, List<Frame> eligibleFrames) {
    traces.parallelStream().forEach(trace -> {
          int mostFrequentIndex = // most frequent mobility scan index, corrected by the first scans index.
              findMostFrequentMobilityScanNumber(trace.getMobilograms()) - eligibleFrames.get(0)
                  .getMobilityScans().get(0).getMobilityScanNumber();

          // add a 0 for the first scan
          if (trace.getMobilograms().get(0).getSpectra().get(0).getFrame() != eligibleFrames.get(0)) {
            trace.tryToAddMobilogram(
                new BuildingIonMobilitySeries(null, new double[]{0d}, new double[]{0d},
                    List.of(eligibleFrames.get(0).getMobilityScan(mostFrequentIndex))));
          }

          // it's a tree map, so it's sorted
          Frame[] detected = new Frame[trace.getMobilograms().size()];
          for (int i = 0; i < detected.length; i++) {
            detected[i] = trace.getMobilograms().get(i).getSpectrum(0).getFrame();
          }
          int allFramesIndex = 0;
          int lastDetectedIndex = eligibleFrames.indexOf(detected[0]);
          for (final Frame frame : detected) {
            // find the next frame we have a datapoint for
            while (allFramesIndex < eligibleFrames.size() && frame != eligibleFrames
                .get(allFramesIndex)) {
              allFramesIndex++;
            }

            if (allFramesIndex - lastDetectedIndex > 1) {
              final Frame firstZeroFrame = eligibleFrames.get(lastDetectedIndex + 1);
              trace.tryToAddMobilogram(
                  new BuildingIonMobilitySeries(null, new double[]{0d}, new double[]{0d},
                      List.of(firstZeroFrame.getMobilityScan(
                          Math.min(mostFrequentIndex, firstZeroFrame.getNumberOfMobilityScans())))));

              final Frame lastZeroFrame = eligibleFrames.get(allFramesIndex - 1);
              if (firstZeroFrame != lastZeroFrame) {
                trace.tryToAddMobilogram(
                    new BuildingIonMobilitySeries(null, new double[]{0d}, new double[]{0d},
                        List.of(lastZeroFrame.getMobilityScan(
                            Math.min(mostFrequentIndex, lastZeroFrame.getNumberOfMobilityScans())))));
              }
            }
            lastDetectedIndex = allFramesIndex;
          }

          if (lastDetectedIndex < eligibleFrames.size() - 1) {
            final Frame firstZeroFrame = eligibleFrames.get(lastDetectedIndex + 1);
            trace.tryToAddMobilogram(
                new BuildingIonMobilitySeries(null, new double[]{0d}, new double[]{0d},
                    List.of(firstZeroFrame.getMobilityScan(
                        Math.min(mostFrequentIndex, firstZeroFrame.getNumberOfMobilityScans())))));
          }
          stepProcessed.getAndIncrement();
        }
    );
  }

  private TreeSet<BuildingIonMobilitySeries> buildFrameMobilograms(MobilityScanDataAccess access) {
    Set<BuildingIonMobilitySeries> buildingTraces = new HashSet<>();
    try {

      while (access.hasNextFrame()) {
        if (isCanceled()) {
          return null;
        }

        final Frame frame = access.nextFrame();

        final TreeSet<RetentionTimeMobilityDataPoint> dps = new TreeSet<>(
            (o1, o2) -> {
              if (o1.getIntensity() > o2.getIntensity()) {
                return -1;
              }
              return 1;
            });
        // get all datapoints
        while (access.hasNextMobilityScan()) {
          final MobilityScan currentMobilityScan = access.nextMobilityScan();
          for (int i = 0; i < access.getNumberOfDataPoints(); i++) {
            dps.add(new RetentionTimeMobilityDataPoint(currentMobilityScan, access.getMzValue(i),
                access.getIntensityValue(i)));
          }
        }

        final Set<TempMobilogram> mobilogramMap = calcMobilograms(dps, tolerance);
        final List<BuildingIonMobilitySeries> storedTraces = storeBuldingMobilograms(mobilogramMap);
        buildingTraces.addAll(storedTraces);

        stepProcessed.getAndIncrement();
      }
    } catch (MissingMassListException e) {
      e.printStackTrace();
    }

    // now sort chromatograms like the adap builder
    logger.finest(() -> "Sorting mobilograms");
    final TreeSet<BuildingIonMobilitySeries> sortedMobilograms = new TreeSet<>(
        (o1, o2) -> {
          if (o1.getSummedIntensity() > o2.getSummedIntensity()) {
            return -1;
          }
          return 1;
        });
    sortedMobilograms.addAll(buildingTraces);

    logger.finest(() -> "Mobilograms sorted");

    return sortedMobilograms;
  }

  @NotNull
  private Set<TempMobilogram> calcMobilograms(Collection<RetentionTimeMobilityDataPoint> dps,
      final MZTolerance tolerance) {
    final RangeMap<Double, TempMobilogram> map = TreeRangeMap.create();
    Set<RetentionTimeMobilityDataPoint> leftoverDataPoints = new TreeSet<>((o1, o2) -> {
      if (o1.getIntensity() > o2.getIntensity()) {
        return -1;
      }
      return 1;
    });

    for (final var dp : dps) {
      TempMobilogram mobilogram = map.get(dp.getMZ());
      if (mobilogram == null) {
        final Range<Double> proposed = tolerance.getToleranceRange(dp.getMZ());
        final Range<Double> actual = SpectraMerging.createNewNonOverlappingRange(map, proposed);
        if (proposed.equals(actual)) {
          mobilogram = new TempMobilogram();
          map.put(actual, mobilogram);
        } else {
          leftoverDataPoints.add(dp);
          continue;
        }
      }
      final RetentionTimeMobilityDataPoint previousDp = mobilogram.keepBetterFittingDataPoint(dp);
      if (previousDp != null) {
        leftoverDataPoints.add(previousDp);
      }
    }

    Set<TempMobilogram> mobilograms = new HashSet<>(map.asMapOfRanges().values());

    if (!leftoverDataPoints.isEmpty()) {
      if (enableRecursive && leftoverDataPoints.size() > RECURSIVE_THRESHOLD) {
        Set<TempMobilogram> recursiveMobilograms = calcMobilograms(leftoverDataPoints, tolerance);
//        logger.finest(() -> "Created additional " + recursiveMobilograms.size()
//            + " mobilograms recursively from " + leftoverDataPoints.size() + " datapoints. "
//            + leftoverDataPoints.stream().findFirst().get().getMobilityScan().getFrame()
//            .getFrameId());
        mobilograms.addAll(recursiveMobilograms);
      }
    }
    return mobilograms;
  }

  private List<BuildingIonMobilitySeries> storeBuldingMobilograms(
      final Set<TempMobilogram> traces) {
    List<BuildingIonMobilitySeries> storedTraces = new ArrayList<>(traces.size());
    for (TempMobilogram trace : traces) {
      final BuildingIonMobilitySeries building = trace.toBuildingSeries(tempStorage);
      storedTraces.add(building);
    }
    return storedTraces;
  }

  @Nullable
  private List<TempIMTrace> createTempIMTraces(
      Collection<BuildingIonMobilitySeries> ionMobilitySeries, MZTolerance tolerance) {
    final RangeMap<Double, TempIMTrace> map = TreeRangeMap.create();
    Set<BuildingIonMobilitySeries> leftoverMobilograms = new HashSet<>();
    for (final var mobilogram : ionMobilitySeries) {
      if (isCanceled()) {
        return null;
      }

      TempIMTrace trace = map.get(mobilogram.getAvgMZ());
      if (trace == null) {
        final Range<Double> mzRange = SpectraMerging
            .createNewNonOverlappingRange(map, tolerance.getToleranceRange(mobilogram.getAvgMZ()));
        trace = new TempIMTrace();
        map.put(mzRange, trace);
      }
      final BuildingIonMobilitySeries previousDp = trace.keepBetterFittingDataPoint(mobilogram);
      if (previousDp != null) {
        leftoverMobilograms.add(previousDp);
      }

      stepProcessed.getAndIncrement();
    }

    List<TempIMTrace> traces = new ArrayList<>(map.asMapOfRanges().values());

    if (!leftoverMobilograms.isEmpty()) {
      logger.finest(() -> leftoverMobilograms.size() + "/" + ionMobilitySeries.size()
          + " leftover mobilograms");
      if (enableRecursive && leftoverMobilograms.size() > RECURSIVE_THRESHOLD) {
        List<TempIMTrace> recursiveTraces = createTempIMTraces(leftoverMobilograms, tolerance);
        if (recursiveTraces != null) {
          logger.finest(() -> "Created additional " + recursiveTraces.size()
              + " traces recursively from " + leftoverMobilograms.size() + " mobilograms.");
          traces.addAll(recursiveTraces);
        }
      }
    }

    return traces;
  }


  /**
   * Taken from https://stackoverflow.com/a/48821002
   *
   * @return Instance {@link Unsafe} or null.
   * @author https://github.com/SteffenHeu
   */
  @Nullable
  private Unsafe initUnsafe() {
    try {
      Class unsafeClass = Class.forName("sun.misc.Unsafe");
//      unsafeClass = Class.forName("jdk.internal.misc.Unsafe");
      Method clean = unsafeClass.getMethod("invokeCleaner", ByteBuffer.class);
      clean.setAccessible(true);
      Field theUnsafeField = unsafeClass.getDeclaredField("theUnsafe");
      theUnsafeField.setAccessible(true);
      Object theUnsafe = theUnsafeField.get(null);

      return (Unsafe) theUnsafe;

    } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
             NoSuchFieldException | ClassCastException e) {
      // jdk.internal.misc.Unsafe doesn't yet have an invokeCleaner() method,
      // but that method should be added if sun.misc.Unsafe is removed.
      e.printStackTrace();
    }
    return null;
  }
}
