package io.github.mzmine.modules.dataprocessing.featdet_smoothing2;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.FeatureDataType;
import io.github.mzmine.datamodel.data_access.FeatureDataAccess;
import io.github.mzmine.datamodel.featuredata.IntensitySeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SavitzkyGolayFilter;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing2.SGIntensitySmoothing.ZeroHandlingType;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SmoothingTask extends AbstractTask {

  public enum SmoothingDimension {
    RETENTION_TIME, MOBILITY
  }

  private static final Logger logger = Logger.getLogger(SmoothingTask.class.getName());

  private final ModularFeatureList flist;
  private final ParameterSet parameters;
  private final AtomicInteger processedFeatures = new AtomicInteger(0);
  private final SmoothingDimension dimension = SmoothingDimension.RETENTION_TIME;
  private final int numFeatures;
  private final int filterWidth;
  private final ZeroHandlingType zht;

  public SmoothingTask(@Nonnull ModularFeatureList flist, @Nullable MemoryMapStorage storage,
      @Nonnull
          ParameterSet parameters) {
    super(storage);

    this.flist = flist;
    this.parameters = parameters;
    numFeatures = flist.getNumberOfRows();
    zht = ZeroHandlingType.KEEP;
    filterWidth = 9;
  }

  @Override
  public String getTaskDescription() {
    return null;
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

    final double[] normalizedWeights = SavitzkyGolayFilter.getNormalizedWeights(filterWidth);
    // include zeros
    final FeatureDataAccess dataAccess = EfficientDataAccess
        .of(flist, FeatureDataType.INCLUDE_ZEROS);
    while (dataAccess.hasNextFeature()) {
      final ModularFeature feature = (ModularFeature) dataAccess.nextFeature();

      final SGIntensitySmoothing smoother = new SGIntensitySmoothing(dataAccess,
          ZeroHandlingType.KEEP, normalizedWeights);
      double[] smoothedIntensities = smoother.smooth();
    }
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
    }

    double[] originalMzs = new double[originalSeries.getNumberOfValues()];
    return (IonTimeSeries<? extends Scan>) originalSeries
        .copyAndReplace(getMemoryMapStorage(), originalMzs, newIntensities);
  }

  private IonTimeSeries<? extends Scan> createNewSeries(@Nonnull final IntensitySeries dataAccess,
      @Nonnull final ModularFeature feature,
      @Nonnull final double[] smoothedIntensities, List<Scan> allScans) {

    final IonTimeSeries<? extends Scan> originalSeries = feature.getFeatureData();
    double[] originalIntensities = new double[originalSeries.getNumberOfValues()];
    originalSeries.getIntensityValues(originalIntensities);

    List<Double> newIntensities = new ArrayList<>();
    List<Scan> newScans = new ArrayList<>();

    double prevIntensity = 0d;
    for (int i = 0; i < smoothedIntensities.length; i++) {
      if(Double.compare(smoothedIntensities[i], 0) != 0) {
        newIntensities.add(smoothedIntensities[i]);
        newScans.add(allScans.get(i));
      }
    }

    double[] originalMzs = new double[originalSeries.getNumberOfValues()];
    return (IonTimeSeries<? extends Scan>) originalSeries
        .copyAndReplace(getMemoryMapStorage(), originalMzs, newIntensities);
  }
}
