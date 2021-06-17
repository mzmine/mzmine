package io.github.mzmine.modules.dataprocessing.featdet_imsmsi;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSImagingRawDataFile;
import io.github.mzmine.datamodel.ImagingFrame;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.IonMobilogramTimeSeriesFactory;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilitySeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.FeatureDataType;
import io.github.mzmine.datamodel.features.types.FeatureShapeMobilogramType;
import io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder.RetentionTimeMobilityDataPoint;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.FeatureConvertorIonMobility;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;

public class IonMobilityImageExpanderTask extends AbstractTask {

  private static final Logger logger = Logger
      .getLogger(IonMobilityImageExpanderTask.class.getName());

  private MZmineProject project;
  private final ModularFeatureList flist;
  //  private final MZTolerance mzTolerance;
  private final int featuresPerStep;
  private AtomicInteger processed = new AtomicInteger(0);
  private int total;
  private final ParameterSet parameters;
  private int newRowId = 1;
  private String description;
  private BinningMobilogramDataAccess access;

  public IonMobilityImageExpanderTask(MZmineProject project, ParameterSet parameters,
      ModularFeatureList flist, @Nullable MemoryMapStorage storage) {
    super(storage);
    this.project = project;
    this.flist = flist;
//    mzTolerance = new MZTolerance(0, 20);
    featuresPerStep = 300;
    total = flist.getNumberOfRows();
    this.parameters = parameters;
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  @Override
  public double getFinishedPercentage() {
    return processed.get() / (double)total;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    description = "Sorting features";
    List<ModularFeature> sortedFeatures = flist.modularStreamFeatures()
        .sorted(Comparator.comparingDouble(ModularFeature::getMZ)).collect(
            Collectors.toList());

//    List<List<ModularFeature>> partionedLists = Lists.partition(sortedFeatures, featuresPerStep);

    final IMSImagingRawDataFile file = (IMSImagingRawDataFile) flist.getRawDataFile(0);
    access = new BinningMobilogramDataAccess(file, 1);

    description = "Sorting frames";
    List<ImagingFrame> frames = new ArrayList<>((Collection<ImagingFrame>) file.getFrames(1));
    frames.sort(Comparator.comparingInt(Frame::getFrameId));

    final ModularFeatureList newflist = new ModularFeatureList(flist.getName() + " ims",
        getMemoryMapStorage(), file);
    newflist.setSelectedScans(file, flist.getSeletedScans(file));
    newflist.addRowType(new FeatureShapeMobilogramType());
    AtomicInteger emptyImageCounter = new AtomicInteger(0);
    List<ModularFeature> sublist = sortedFeatures;
    Range<Double> mzRange = getListMZRange(sublist);
    SortedSet<RetentionTimeMobilityDataPoint> dps = extractDataPoints(frames, mzRange);

    description = "Extracting data points";
    if (isCanceled()) {
      return;
    }

      // sort dps and features by their intensity to associate them. I know this is rather
      // primitive, but should work for now.
      sublist.sort((f1, f2) -> Double.compare(f1.getHeight(), f2.getHeight()) * -1);
      processed.set(0);
      total = sublist.size();
      description = "Assigning ims data points for feature " + processed.get() + "/" + total;
    Collection<BuildingImage> images = assignFeaturesToDataPoints(sublist, dps);

      total = images.size();
      processed.set(0);
      images.forEach(image -> {
        description = "Building image for feature " + processed.get() + "/" + total;
        if (!image.getDataPoints().isEmpty()) {
          ModularFeature newFeature = buildingImageToModularFeature(newflist, image);
          newflist.addRow(new ModularFeatureListRow(newflist, newRowId++, newFeature));
        } else {
          emptyImageCounter.getAndIncrement();
        }
        processed.getAndIncrement();
      });
//    }

    newflist.getAppliedMethods().addAll(flist.getAppliedMethods());
    newflist.getAppliedMethods()
        .add(new SimpleFeatureListAppliedMethod(IonMobilityImageExpanderModule.class, parameters));

    logger.info(emptyImageCounter.get() + " removed empty images");
    project.addFeatureList(newflist);
    setStatus(TaskStatus.FINISHED);
  }

  private Collection<BuildingImage> assignFeaturesToDataPoints(Collection<ModularFeature> sublist, Collection<RetentionTimeMobilityDataPoint> dps) {
    List<BuildingImage> images = new ArrayList<>();
    RangeMap<Double, BuildingImage> featuresMap = TreeRangeMap.create();
    sublist.forEach(f -> featuresMap.put(f.getRawDataPointsMZRange(), new BuildingImage(f)));

    processed.set(0);
    total = dps.size();
    for (Iterator<RetentionTimeMobilityDataPoint> iterator = dps.iterator(); iterator.hasNext(); ) {
      RetentionTimeMobilityDataPoint dp = iterator.next();
      BuildingImage img = featuresMap.get(dp.getMZ());
      processed.getAndIncrement();
      if(img == null) {
        continue;
      }
      img.addDataPoint(dp);
//      if(img.addDataPoint(dp)) {
//        iterator.remove();
//      }
    }

    return featuresMap.asMapOfRanges().values();
  }

  private Range<Double> getListMZRange(List<ModularFeature> features) {
    if (features.isEmpty()) {
      return Range.singleton(0d);
    }
    return Range.closed(features.get(0).getRawDataPointsMZRange().lowerEndpoint(),
        features.get(features.size() - 1).getRawDataPointsMZRange().upperEndpoint());
  }

  private int getMaxNumberOfDataPoints(List<? extends Frame> frames) {
    int i = 0;
    for (Frame frame : frames) {
      List<MobilityScan> mobilityScans = frame.getMobilityScans();
      for (MobilityScan scan : mobilityScans) {
        if (scan.getNumberOfDataPoints() > i) {
          i = scan.getNumberOfDataPoints();
        }
      }
    }
    return i;
  }

  private void resetBuffer(double[][] data) {
    Arrays.fill(data[0], 0d);
    Arrays.fill(data[1], 0d);
  }

  private SortedSet<RetentionTimeMobilityDataPoint> extractDataPoints(List<? extends Frame> frames,
      Range<Double> mzRange) {
//    int bufferSize = getMaxNumberOfDataPoints(frames);
    double[][] dataBuffer = new double[2][];
    dataBuffer[0] = new double[0];
    dataBuffer[1] = new double[0];
    resetBuffer(dataBuffer);

    SortedSet<RetentionTimeMobilityDataPoint> dps = new TreeSet<>(
        /*new Comparator<RetentionTimeMobilityDataPoint>() {
          @Override
          public int compare(RetentionTimeMobilityDataPoint o1, RetentionTimeMobilityDataPoint o2) {
            int cmp = Double.compare(o1.getMZ(), o2.getMZ());
            // we don't want to delete equal elements (tree set checks equality by compareTo)
            return cmp == 0 ? -1 : cmp;
          }
        }*/);

    processed.set(0);
    total = frames.size();
    for (Frame frame : frames) {
      description = "Extracting data points for frame " + processed.get() + "/" + total;
      List<MobilityScan> mobilityScans = frame.getMobilityScans();
      for (MobilityScan mobScan : mobilityScans) {
        MassList ml = mobScan.getMassList();
        dataBuffer[0] = ml.getMzValues(dataBuffer[0]);
        dataBuffer[1] = ml.getIntensityValues(dataBuffer[1]);
//        double[][] filtered = DataPointUtils
//            .getDataPointsInMzRange(dataBuffer[0], dataBuffer[1], mzRange);
        for (int i = 0; i < ml.getNumberOfDataPoints(); i++) {
          dps.add(new RetentionTimeMobilityDataPoint(mobScan, dataBuffer[0][i], dataBuffer[1][i]));
        }
        resetBuffer(dataBuffer);
      }
      processed.getAndIncrement();
    }

    return dps;
  }

  public ModularFeature buildingImageToModularFeature(ModularFeatureList newflist,
      BuildingImage image) {
    SortedMap<Frame, SortedSet<RetentionTimeMobilityDataPoint>> sortedMap = FeatureConvertorIonMobility
        .groupDataPointsByFrameId(image.getDataPoints());

    List<IonMobilitySeries> mobilograms = new ArrayList<>();
    for (SortedSet<RetentionTimeMobilityDataPoint> dps : sortedMap.values()) {
      double[][] data = DataPointUtils.getDataPointsAsDoubleArray(dps);
      List<MobilityScan> scans = dps.stream().map(RetentionTimeMobilityDataPoint::getMobilityScan)
          .collect(Collectors.toList());
      if (scans.size() == 0) {
        ;
      }
      mobilograms.add(new SimpleIonMobilitySeries(null, data[0], data[1],
          scans));
    }
    IonMobilogramTimeSeries ionMobilogramTimeSeries = IonMobilogramTimeSeriesFactory.of(
        newflist.getMemoryMapStorage(), mobilograms, access);

    ModularFeature feature = new ModularFeature(newflist, image.getOriginalFeature());
    feature.set(FeatureDataType.class, ionMobilogramTimeSeries);
    FeatureDataUtils.recalculateIonSeriesDependingTypes(feature);

    return feature;
  }
}
