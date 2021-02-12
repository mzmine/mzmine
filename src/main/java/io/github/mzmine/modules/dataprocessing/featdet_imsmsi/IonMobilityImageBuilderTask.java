package io.github.mzmine.modules.dataprocessing.featdet_imsmsi;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSImagingRawDataFile;
import io.github.mzmine.datamodel.ImagingFrame;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilogramTimeSeries;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class IonMobilityImageBuilderTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(IonMobilityImageBuilderTask.class.getName());

  private MZmineProject project;
  private final ModularFeatureList flist;
  //  private final MZTolerance mzTolerance;
  private final int featuresPerStep;
  private double processed;
  private int total;
  private final ParameterSet parameters;
  private int newRowId = 1;
  private String description;

  public IonMobilityImageBuilderTask(MZmineProject project, ParameterSet parameters,
      ModularFeatureList flist) {
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
    return processed / total;
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

    description = "Sorting frames";
    List<ImagingFrame> frames = new ArrayList<>((Collection<ImagingFrame>) file.getFrames(1));
    frames.sort(Comparator.comparingInt(Frame::getFrameId));

    final ModularFeatureList newflist = new ModularFeatureList(flist.getName() + " ims", file);
    newflist.addRowType(new FeatureShapeMobilogramType());

    // get a sublist
//    for (List<ModularFeature> sublist : partionedLists) {
    List<ModularFeature> sublist = sortedFeatures;
    Range<Double> mzRange = getListMZRange(sublist);

    description = "Extracting data points";
    SortedSet<RetentionTimeMobilityDataPoint> dps = extractDataPoints(frames, mzRange);
    List<RetentionTimeMobilityDataPoint> dpList = new ArrayList<>(dps);
    List<BuildingImage> images = new ArrayList<>(sublist.size());

    if (isCanceled()) {
      return;
    }

    // sort dps and features by their intensity to associate them. I know this is rather
    // primitive, but should work for now.
    sublist.sort((f1, f2) -> Double.compare(f1.getHeight(), f2.getHeight()) * -1);
    processed = 0;
    total = sublist.size();
    description = "Assigning ims data points for feature " + processed + "/" + total;
    images = assignDataPointsToFeatures(sublist, dpList);

    total = images.size();
    processed = 0;
    AtomicInteger emptyImageCounter = new AtomicInteger(0);
    images.forEach(image -> {
      description = "Building image for feature " + processed + "/" + total;
      if (!image.getDataPoints().isEmpty()) {
        ModularFeature newFeature = buildingImageToModularFeature(newflist, image);
        newflist.addRow(new ModularFeatureListRow(newflist, newRowId++, newFeature));
      } else {
        emptyImageCounter.getAndIncrement();
      }
    });
//    }

    newflist.getAppliedMethods().addAll(flist.getAppliedMethods());
    newflist.getAppliedMethods()
        .add(new SimpleFeatureListAppliedMethod(IonMobilityImageExpanderModule.class, parameters));

    logger.info(emptyImageCounter.get() + " removed empty images");
    project.addFeatureList(newflist);
    setStatus(TaskStatus.FINISHED);
  }

  private List<BuildingImage> assignDataPointsToFeatures(Collection<ModularFeature> sublist, Collection<RetentionTimeMobilityDataPoint> dps) {
    List<BuildingImage> images = new ArrayList<>();
    for (ModularFeature f : sublist) {
      description = "Assigning ims data points for feature " + processed + "/" + total;
      // current assumption: since previous images are build by and ADAP-like algorithm, their m/z
      // ranges do not overlap. So if the mzrange of an image contains a certain data point, the others won't
      BuildingImage img = new BuildingImage(f);
      Range<Double> mzRange = f.getRawDataPointsMZRange();
      for (RetentionTimeMobilityDataPoint dp : dps) {
        if (mzRange.contains(dp.getMZ())) {
          img.addDataPoint(dp);
        }
      }
      description =
          "Removing data points of feature " + processed + "/" + total + " from collection";
      dps.removeAll(img.getDataPoints());
      images.add(img);
      processed++;
    }
    return images;
  }

  /*private List<BuildingImage> assignFeaturesToDataPoints(Collection<ModularFeature> sublist, Collection<RetentionTimeMobilityDataPoint> dps) {
    List<BuildingImage> images = new ArrayList<>();
    for (ModularFeature f : sublist) {
      description = "Assigning ims data points for feature " + processed + "/" + total;
      // current assumption: since previous images are build by and ADAP-like algorithm, their m/z
      // ranges do not overlap. So if the mzrange of an image contains a certain data point, the others won't
      BuildingImage img = new BuildingImage(f);
      for (RetentionTimeMobilityDataPoint dp : dps) {
        if (img.getOriginalFeature().getRawDataPointsMZRange().contains(dp.getMZ())) {
          img.addDataPoint(dp);
        }
      }
      description =
          "Removing data points of feature " + processed + "/" + total + " from collection";
      dps.removeAll(img.getDataPoints());
      images.add(img);
      processed++;
    }
    return images;
  }
*/
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
//    double[][] dataBuffer = new double[2][];
//    dataBuffer[0] = new double[bufferSize];
//    dataBuffer[1] = new double[bufferSize];
//    resetBuffer(dataBuffer);

    SortedSet<RetentionTimeMobilityDataPoint> dps = new TreeSet<>();

    processed = 0;
    total = frames.size();
    for (Frame frame : frames) {
      description = "Extracting data points for frame " + processed + "/" + total;
      List<MobilityScan> mobilityScans = frame.getMobilityScans();
      for (MobilityScan mobScan : mobilityScans) {
        MassList ml = mobScan.getMassLists().stream().findFirst().get();
        DataPoint[] points = ml
            .getDataPoints(); //ScanUtils.getDataPointsByMass(ml.getDataPoints(), mzRange);
        for (DataPoint d : points) {
          dps.add(new RetentionTimeMobilityDataPoint(mobScan, d.getMZ(), d.getIntensity()));
        }

//        mobScan.getMzValues(dataBuffer[0]);
//        mobScan.getIntensityValues(dataBuffer[1]);
//        double[][] filtered = DataPointUtils
//            .getDataPointsInMzRange(dataBuffer[0], dataBuffer[1], mzRange);
//        for (int i = 0; i < filtered[0].length; i++) {
//          dps.add(new RetentionTimeMobilityDataPoint(mobScan, filtered[0][i], filtered[1][i]));
//        }
//        resetBuffer(dataBuffer);
      }
      processed++;
    }

    return dps;
  }

  public ModularFeature buildingImageToModularFeature(ModularFeatureList newflist,
      BuildingImage image) {
    SortedMap<Frame, SortedSet<RetentionTimeMobilityDataPoint>> sortedMap = FeatureConvertorIonMobility
        .groupDataPointsByFrameId(image.getDataPoints());

    List<SimpleIonMobilitySeries> mobilograms = new ArrayList<>();
    for (SortedSet<RetentionTimeMobilityDataPoint> dps : sortedMap.values()) {
      double[][] data = DataPointUtils.getDataPointsAsDoubleArray(dps);
      List<MobilityScan> scans = dps.stream().map(RetentionTimeMobilityDataPoint::getMobilityScan)
          .collect(Collectors.toList());
      if (scans.size() == 0) {
        ;
      }
      mobilograms.add(new SimpleIonMobilitySeries(newflist.getMemoryMapStorage(), data[0], data[1],
          scans));
    }
    IonMobilogramTimeSeries ionMobilogramTimeSeries = new SimpleIonMobilogramTimeSeries(
        newflist.getMemoryMapStorage(), mobilograms);

    ModularFeature feature = new ModularFeature(newflist, image.getOriginalFeature());
    feature.set(FeatureDataType.class, ionMobilogramTimeSeries);
    FeatureDataUtils.recalculateIonSeriesDependingTypes(feature);

    return feature;
  }
}
