package io.github.mzmine.modules.dataprocessing.featdet_imsbuilder;

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
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.scans.SpectraMerging;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class IMSBuilderTask extends AbstractTask {


  private static final Logger logger = Logger.getLogger(IMSBuilderTask.class.getName());
  private static final int RECURSIVE_THRESHOLD = 50;

  private final IMSRawDataFile file;
  private final ParameterSet parameters;
  private final ScanSelection scanSelection;
  private final MZmineProject project;
  private final int steps = 3;
  private final MZTolerance tolerance;
  private final MemoryMapStorage tempStorage = MemoryMapStorage.forFeatureList();
  private int stepProcessed = 0;
  private int stepTotal = 0;
  private int currentStep = 1;
  private final boolean enableRecursive = false;

  public IMSBuilderTask(@Nullable MemoryMapStorage storage, @Nonnull final IMSRawDataFile file,
      @Nonnull final ParameterSet parameters, MZmineProject project) {
    super(storage);

    this.file = file;
    this.parameters = parameters;
    scanSelection = parameters.getParameter(IMSBuilderParameters.scanSelection).getValue();
    tolerance = parameters.getParameter(IMSBuilderParameters.mzTolerance).getValue();
    this.project = project;
  }

  @Override
  public String getTaskDescription() {
    return "Running feature detection on " + file.getName();
  }

  @Override
  public double getFinishedPercentage() {
    return stepProcessed / (double) stepTotal * (currentStep / (double) steps);
  }

  @Override
  public void run() {

    final MobilityScanDataAccess access = EfficientDataAccess
        .of(file, MobilityScanDataType.CENTROID, scanSelection);

    final Set<BuildingIonMobilitySeries> buildingTraces = new HashSet<>();

    logger.finest(() -> "Extracting data points from mobility scans and building mobilograms...");
    stepTotal = access.getNumberOfScans();
    // build mobilograms for all frames
    try {

      while (access.hasNextFrame()) {
        if (isCanceled()) {
          return;
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

        stepProcessed++;
      }
    } catch (MissingMassListException e) {
      e.printStackTrace();
    }

    currentStep++;
    stepProcessed = 0;
    stepTotal = buildingTraces.size();
    logger.finest(() -> "Sorting mobilograms");

    // now build chromatograms like the adap builder
    final TreeSet<BuildingIonMobilitySeries> sortedMobilograms = new TreeSet<>(
        (o1, o2) -> {
          if (o1.getSummedIntensity() > o2.getSummedIntensity()) {
            return -1;
          }
          return 1;
        });
    sortedMobilograms.addAll(buildingTraces);

    logger.finest(() -> "Mobilograms sorted");

    final Set<TempIMTrace> ionMobilityTraces = createTempIMTraces(
        sortedMobilograms, tolerance);
    if (isCanceled()) {
      return;
    }

    final ModularFeatureList flist = new ModularFeatureList(file.getName(), getMemoryMapStorage(),
        file);
    flist.setSelectedScans(file, scanSelection.getMachtingScans(file.getFrames()));

    logger.finest(() -> "Creation BinningMobilogramDataAccess for raw data file " + file.getName());
    final BinningMobilogramDataAccess binningMobilogramDataAccess = EfficientDataAccess
        .of(file, 0.008);

    stepProcessed = 0;
    stepTotal = ionMobilityTraces.size();
    currentStep++;

    int id = 0;
    for (TempIMTrace trace : ionMobilityTraces) {
      if (isCanceled()) {
        return;
      }

      if (trace.getMobilograms().size() < 5) {
        stepProcessed++;
        continue;
      }
      ModularFeature f = FeatureConvertors
          .tempIMTraceToModularFeature(trace, file, binningMobilogramDataAccess, flist);
      ModularFeatureListRow row = new ModularFeatureListRow(flist, id, f);
      row.set(FeatureShapeMobilogramType.class, false);
      flist.addRow(row);
      id++;
      stepProcessed++;
    }

    flist.getAppliedMethods()
        .add(new SimpleFeatureListAppliedMethod(IMSBuilderModule.class, parameters));
    DataTypeUtils.addDefaultIonMobilityTypeColumns(flist);
    project.addFeatureList(flist);
    setStatus(TaskStatus.FINISHED);
  }

  private Set<TempMobilogram> calcMobilograms(
      Collection<RetentionTimeMobilityDataPoint> dps,
      final MZTolerance tolerance) {
    final RangeMap<Double, TempMobilogram> map = TreeRangeMap.create();
    Set<RetentionTimeMobilityDataPoint> leftoverDataPoints = new TreeSet<>((o1, o2) -> {
      if (o1.getIntensity() > o2.getIntensity()) {
        return -1;
      }
      return 1;
    });

    for (final var dp : dps) {
      TempMobilogram trace = map.get(dp.getMZ());
      if (trace == null) {
        final Range<Double> mzRange = SpectraMerging
            .createNewNonOverlappingRange(map, tolerance.getToleranceRange(dp.getMZ()));
        trace = new TempMobilogram();
        map.put(mzRange, trace);
      }
      final RetentionTimeMobilityDataPoint previousDp = trace.keepBetterFittingDataPoint(dp);
      if (previousDp != null) {
        leftoverDataPoints.add(previousDp);
      }
    }

    Set<TempMobilogram> mobilograms = new HashSet<>(map.asMapOfRanges().values());

    if (!leftoverDataPoints.isEmpty()) {
      logger.finest(() -> leftoverDataPoints.size() + "/" + dps.size() + " leftover data points");
      if (enableRecursive && leftoverDataPoints.size() > RECURSIVE_THRESHOLD) {
        Set<TempMobilogram> recursiveMobilograms = calcMobilograms(leftoverDataPoints, tolerance);
        logger.finest(() -> "Created additional " + recursiveMobilograms.size()
            + " mobilograms recursively from " + leftoverDataPoints.size() + " datapoints. "
            + leftoverDataPoints.stream().findFirst().get().getMobilityScan().getFrame()
            .getFrameId());
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

  private Set<TempIMTrace> createTempIMTraces(
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

      stepProcessed++;
    }

    Set<TempIMTrace> traces = new HashSet<>(map.asMapOfRanges().values());

    if (!leftoverMobilograms.isEmpty()) {
      logger.finest(() -> leftoverMobilograms.size() + "/" + ionMobilitySeries.size()
          + " leftover mobilograms");
      if (enableRecursive && leftoverMobilograms.size() > RECURSIVE_THRESHOLD) {
        Set<TempIMTrace> recursiveTraces = createTempIMTraces(leftoverMobilograms, tolerance);
        logger.finest(() -> "Created additional " + recursiveTraces.size()
            + " traces recursively from " + leftoverMobilograms.size() + " mobilograms.");
        if (traces != null) {
          traces.addAll(recursiveTraces);
        }
      }
    }

    return traces;
  }


}
