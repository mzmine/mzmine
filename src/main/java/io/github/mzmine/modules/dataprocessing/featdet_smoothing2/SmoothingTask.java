package io.github.mzmine.modules.dataprocessing.featdet_smoothing2;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.FeatureDataType;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.MobilogramAccessType;
import io.github.mzmine.datamodel.data_access.FeatureDataAccess;
import io.github.mzmine.datamodel.data_access.MobilogramDataAccess;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.featuredata.IntensitySeries;
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.ModifiableSpectra;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.SummedIntensityMobilitySeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SavitzkyGolayFilter;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing2.SGIntensitySmoothing.ZeroHandlingType;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SmoothingTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(SmoothingTask.class.getName());

  private final ModularFeatureList flist;
  private final ParameterSet parameters;
  private final MZmineProject project;

  private final AtomicInteger processedFeatures = new AtomicInteger(0);
  private final SmoothingDimension dimension = SmoothingDimension.RETENTION_TIME;

  private final int numFeatures;
  private final int rtFilterWidth;
  private final int mobilityFilterWidth;
  private final double[] rtWeights;
  private final double[] mobilityWeights;
  private final ZeroHandlingType zht;
  private final String suffix;
  private final boolean smoothMobility;
  private final boolean smoothRt;

  public SmoothingTask(@Nonnull MZmineProject project, @Nonnull ModularFeatureList flist,
      @Nullable MemoryMapStorage storage, @Nonnull ParameterSet parameters) {
    super(storage);

    this.flist = flist;
    this.parameters = parameters;
    this.project = project;
    numFeatures = flist.getNumberOfRows();
    zht = ZeroHandlingType.KEEP;

    suffix = parameters.getParameter(SmoothingParameters.suffix).getValue();

    smoothRt = parameters.getParameter(SmoothingParameters.rtSmoothing).getValue();
    rtFilterWidth = parameters.getParameter(SmoothingParameters.rtSmoothing).getEmbeddedParameter()
        .getValue();

    smoothMobility = parameters.getParameter(SmoothingParameters.mobilitySmoothing).getValue();
    mobilityFilterWidth = parameters.getParameter(SmoothingParameters.mobilitySmoothing)
        .getEmbeddedParameter().getValue();
    rtWeights = SavitzkyGolayFilter.getNormalizedWeights(rtFilterWidth);
    mobilityWeights = SavitzkyGolayFilter.getNormalizedWeights(mobilityFilterWidth);
  }

  @Override
  public String getTaskDescription() {
    return "Smoothing " + flist.getName() + ": " + processedFeatures.get() + "/" + numFeatures;
  }

  @Override
  public double getFinishedPercentage() {
    return processedFeatures.get() / (double) numFeatures;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    if (flist.getNumberOfRawDataFiles() != 1) {
      setErrorMessage("Cannot smooth feature lists with more than one raw data file.");
      setStatus(TaskStatus.ERROR);
      return;
    }

    if (dimension != SmoothingDimension.RETENTION_TIME) {
      return;
    }

    final ModularFeatureList smoothedList = flist
        .createCopy(flist.getName() + suffix, getMemoryMapStorage());
    final SGIntensitySmoothing smoother = new SGIntensitySmoothing(ZeroHandlingType.KEEP,
        rtWeights);
    // include zeros
    final FeatureDataAccess dataAccess = EfficientDataAccess
        .of(smoothedList, FeatureDataType.INCLUDE_ZEROS);

    while (dataAccess.hasNextFeature()) {
      final ModularFeature feature = (ModularFeature) dataAccess.nextFeature();
      final double[] smoothedIntensities;
      if (smoothRt) {
        smoothedIntensities = smoother.smooth(dataAccess);
      } else {
        smoothedIntensities = new double[feature.getFeatureData().getNumberOfValues()];
        feature.getFeatureData().getIntensityValues(smoothedIntensities);
      }

      final IonTimeSeries<? extends Scan> smoothedSeries = replaceOldIntensities(dataAccess,
          feature, smoothedIntensities);
      feature.set(io.github.mzmine.datamodel.features.types.FeatureDataType.class, smoothedSeries);
      FeatureDataUtils.recalculateIonSeriesDependingTypes(feature);

      processedFeatures.getAndIncrement();
    }

    if (isCanceled()) {
      return;
    }

    smoothedList.getAppliedMethods()
        .add(new SimpleFeatureListAppliedMethod(SmoothingModule.class, parameters));
    project.addFeatureList(smoothedList);

    setStatus(TaskStatus.FINISHED);
  }

  /**
   * Handles {@link ZeroHandlingType#KEEP}
   *
   * @param dataAccess
   * @param feature
   * @param smoothedIntensities
   * @return
   */
  private IonTimeSeries<? extends Scan> replaceOldIntensities(
      @Nonnull final IntensitySeries dataAccess, @Nonnull final ModularFeature feature,
      @Nonnull final double[] smoothedIntensities) {

    final IonTimeSeries<? extends Scan> originalSeries = feature.getFeatureData();
    double[] newIntensities = new double[originalSeries.getNumberOfValues()];
    double[] originalIntensities = new double[originalSeries.getNumberOfValues()];
    originalSeries.getIntensityValues(originalIntensities);

    int newIntensitiesIndex = 0;
    for (int i = 0; i < smoothedIntensities.length; i++) {
      // check if we originally did have an intensity at the current index. I know that the data
      // access contains more zeros and the zeros of different indices will be matched, but the
      // newIntensitiesIndex will "catch" up, once real intensities are reached.
      if (Double.compare(dataAccess.getIntensity(i), originalIntensities[newIntensitiesIndex])
          == 0) {
        newIntensities[newIntensitiesIndex] = smoothedIntensities[i];
        newIntensitiesIndex++;
      }
      if(newIntensitiesIndex == originalIntensities.length) {
        break;
      }
    }

    double[] originalMzs = new double[originalSeries.getNumberOfValues()];
    originalSeries.getMzValues(originalMzs);
    if (smoothMobility && originalSeries instanceof IonMobilogramTimeSeries) {
      SummedIntensityMobilitySeries smoothedMobilogram = smoothSummedMobilogram(
          (IonMobilogramTimeSeries) originalSeries, zht, mobilityWeights, feature.getMZ());
      return new SimpleIonMobilogramTimeSeries(getMemoryMapStorage(), originalMzs, newIntensities,
          ((SimpleIonMobilogramTimeSeries) originalSeries).getMobilogramsModifiable(),
          ((ModifiableSpectra) originalSeries).getSpectraModifiable(), smoothedMobilogram);
    }

    return (IonTimeSeries<? extends Scan>) originalSeries
        .copyAndReplace(getMemoryMapStorage(), originalMzs, newIntensities);
  }

  private SummedIntensityMobilitySeries smoothSummedMobilogram(
      @Nonnull final IonMobilogramTimeSeries originalSeries,
      @Nonnull final ZeroHandlingType zht, @Nonnull final double[] weights, double mz) {

    final double[] mobilities = DataPointUtils.getDoubleBufferAsArray(
        originalSeries.getSummedMobilogram().getMobilityValues());
    final SGIntensitySmoothing smoothedSummed = new SGIntensitySmoothing(ZeroHandlingType.KEEP,
        weights);

    return new SummedIntensityMobilitySeries(
        getMemoryMapStorage(), mobilities,
        smoothedSummed.smooth(originalSeries.getSummedMobilogram()), mz);
  }


  // -----------------------------
  // todo
  private List<IonMobilitySeries> smoothMobilograms(@Nonnull final ModularFeature feature,
      @Nonnull final double[] smoothedRtIntensities,
      @Nonnull final MobilogramAccessType dataAccessType,
      @Nonnull final ZeroHandlingType zht,
      @Nonnull final double[] weights) {
    final IonTimeSeries<? extends Scan> s = feature.getFeatureData();
    assert s instanceof IonMobilogramTimeSeries;

    final IonMobilogramTimeSeries originalSeries = (IonMobilogramTimeSeries) s;
    final MobilogramDataAccess dataAccess = EfficientDataAccess.of(originalSeries, dataAccessType);

    List<IonMobilitySeries> smoothedMobilograms = new ArrayList<>();
    final SGIntensitySmoothing smoothing = new SGIntensitySmoothing(zht, weights);
    while (dataAccess.hasNext()) {
      final IonMobilitySeries mobilogram = dataAccess.next();
      double[] smoothedMobilogramIntensities = smoothing.smooth(dataAccess);

    }
    return new ArrayList<>();
  }

  private IonTimeSeries<? extends Scan> createNewSeries(@Nonnull final IntensitySeries dataAccess,
      @Nonnull final ModularFeature feature,
      @Nonnull final double[] smoothedIntensities, List<Scan> allScans) {

    final IonTimeSeries<? extends Scan> originalSeries = feature.getFeatureData();
    double[] originalIntensities = new double[originalSeries.getNumberOfValues()];
    originalSeries.getIntensityValues(originalIntensities);

    final List<Double> newIntensities = new ArrayList<>();
    final List<Double> mzs = new ArrayList<>();
    final List<Scan> newScans = new ArrayList<>();
    final List<IonMobilitySeries> mobilograms;
    final int someMobilityScanIndex;

    if (originalSeries instanceof IonMobilogramTimeSeries) {
      mobilograms = new ArrayList<>();
      someMobilityScanIndex = ((IonMobilogramTimeSeries) originalSeries).getMobilogram(0)
          .getSpectrum(0).getMobilityScanNumber();
    } else {
      mobilograms = null;
      someMobilityScanIndex = 0;
    }

    int oldSeriesIndex = 0;
    double prevIntensity = 0d;
    for (int i = 0; i < smoothedIntensities.length; i++) {
      // if the intensity is != 0, keep it
      // todo leading/trailing 0s
      if (Double.compare(smoothedIntensities[i], 0) == 0) {
        continue;
      }

      final Scan newScan = allScans.get(i);
      newIntensities.add(smoothedIntensities[i]);
      newScans.add(newScan);

      if (newScan == originalSeries.getSpectrum(oldSeriesIndex)) {
        mzs.add(originalSeries.getMZ(oldSeriesIndex));
        oldSeriesIndex++;
        if (originalSeries instanceof IonMobilogramTimeSeries) {
          mobilograms.add(((IonMobilogramTimeSeries) originalSeries).getMobilogram(oldSeriesIndex));
        }
      } else {
        // todo what do we do with mzs in this case? putting a new intensity with some mz will
        //  influence the features m/z in every case.
        mzs.add(feature.getMZ());
        // if this is an IonMobilogramTimeSeries, we also have to create an empty mobilogram
        if (originalSeries instanceof IonMobilogramTimeSeries) {
          final Frame frame = (Frame) newScan;
          final MobilityScan someMobilityScan;
          if (someMobilityScanIndex < frame.getNumberOfMobilityScans()) {
            someMobilityScan = frame.getMobilityScan(someMobilityScanIndex);
          } else {
            someMobilityScan = frame.getMobilityScans().get(frame.getNumberOfMobilityScans() - 1);
          }
          final IonMobilitySeries dummyMobilogram = new SimpleIonMobilitySeries(null,
              new double[]{feature.getMZ()}, new double[]{0}, List.of(someMobilityScan));
          mobilograms.add(dummyMobilogram);
        }
      }
    }

    // todo smooth mobilograms here if needed
    if (originalSeries instanceof IonMobilogramTimeSeries) {

      if (smoothMobility) {

      }

//      return ((IonMobilogramTimeSeries) originalSeries).copyAndReplace(getMemoryMapStorage(), mzs.toArray(), newIntensities.toArray(), mobilograms, );
    }
    return null;
  }

  public enum SmoothingDimension {
    RETENTION_TIME, MOBILITY
  }
}
