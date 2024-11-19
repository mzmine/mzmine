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

package io.github.mzmine.modules.dataprocessing.featdet_smoothing;

import com.google.common.collect.Range;
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
import io.github.mzmine.datamodel.featuredata.IonTimeSeriesUtils;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilitySeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.otherdectectors.MrmTransitionListType;
import io.github.mzmine.datamodel.otherdetectors.MrmTransition;
import io.github.mzmine.datamodel.otherdetectors.MrmTransitionList;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.DataTypeUtils;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SmoothingTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(SmoothingTask.class.getName());

  private final ModularFeatureList flist;
  private final ParameterSet parameters;
  private final MZmineProject project;

  private final AtomicInteger processedFeatures = new AtomicInteger(0);
  private final SmoothingDimension dimension = SmoothingDimension.RETENTION_TIME;

  private final int numFeatures;
  private final ZeroHandlingType zht;
  private final String suffix;
  private final OriginalFeatureListOption handleOriginal;

  public SmoothingTask(@NotNull MZmineProject project, @NotNull ModularFeatureList flist,
      @Nullable MemoryMapStorage storage, @NotNull ParameterSet parameters,
      @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    this.flist = flist;
    this.parameters = parameters;
    this.project = project;
    numFeatures = flist.getNumberOfRows();
    zht = ZeroHandlingType.KEEP;

    suffix = parameters.getParameter(SmoothingParameters.suffix).getValue();

    handleOriginal = parameters.getParameter(SmoothingParameters.handleOriginal).getValue();
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

    final ModularFeatureList smoothedList = flist.createCopy(flist.getName() + " " + suffix,
        getMemoryMapStorage(), false);
    DataTypeUtils.copyTypes(flist, smoothedList, true, true);
    // init a new smoother instance, since the parameters have to be stored in the smoother itself.
    final SmoothingAlgorithm smoother = FeatureSmoothingOptions.createSmoother(parameters);
    if (smoother == null) {
      return;
    }

    // include zeros
    final FeatureDataAccess dataAccess = EfficientDataAccess.of(smoothedList,
        FeatureDataType.INCLUDE_ZEROS);

    while (dataAccess.hasNextFeature()) {
      final ModularFeature feature = (ModularFeature) dataAccess.nextFeature();

      final IonTimeSeries<? extends Scan> smoothedSeries = smoother.smoothFeature(
          getMemoryMapStorage(), dataAccess, feature, zht);
      feature.set(io.github.mzmine.datamodel.features.types.FeatureDataType.class, smoothedSeries);
      FeatureDataUtils.recalculateIonSeriesDependingTypes(feature);

      handleMrmTraces(feature, smoother);

      processedFeatures.getAndIncrement();
    }

    if (isCanceled()) {
      return;
    }

    smoothedList.getAppliedMethods().add(
        new SimpleFeatureListAppliedMethod(SmoothingModule.class, parameters, getModuleCallDate()));

    // add new / remove old
    handleOriginal.reflectNewFeatureListToProject(suffix, project, smoothedList, flist);

    setStatus(TaskStatus.FINISHED);
  }

  private void handleMrmTraces(ModularFeature feature, SmoothingAlgorithm smoother) {
    if (!(feature.get(MrmTransitionListType.class) instanceof MrmTransitionList transitions)) {
      return;
    }

    List<MrmTransition> smoothedTransitions = new ArrayList<>();
    for (MrmTransition transition : transitions.transitions()) {
      final IonTimeSeries<Scan> remapped = IonTimeSeriesUtils.remapRtAxis(transition.chromatogram(),
          flist.getSeletedScans(feature.getRawDataFile()));
      final @Nullable double[] intensities = smoother.smoothRt(remapped);
      if (intensities == null) {
        throw new RuntimeException("Error while smoothing MRMs of feature %s".formatted(
            FeatureUtils.featureToString(feature)));
      }

      final Range<Float> rtRange = feature.getRawDataPointsRTRange();
      // replace the intensities in the full series, cut to original length right after. Only need
      // to save the second one.
      final IonTimeSeries<Scan> smoothed = remapped.copyAndReplace(null, intensities)
          .subSeries(getMemoryMapStorage(), rtRange.lowerEndpoint(), rtRange.upperEndpoint());

      smoothedTransitions.add(transition.with(smoothed));
    }
    final MrmTransitionList mrmTransitionList = new MrmTransitionList(smoothedTransitions);
    feature.set(MrmTransitionListType.class, mrmTransitionList);
    mrmTransitionList.setQuantifier(mrmTransitionList.quantifier(), feature);
  }

  // -----------------------------
  // todo: these are not used yet due to questions regarding the actual implementation
  //  1. if new intensities are added on the peak edges - what do we do on the mobilogram level? We
  //   need the same number of mobilograms as for rt data points
  //  2. Which m/z do we put for newly created intensities? they will influence the overall m/z of
  //   the feature
  private List<IonMobilitySeries> smoothMobilograms(@NotNull final ModularFeature feature,
      @NotNull final double[] smoothedRtIntensities,
      @NotNull final MobilogramAccessType dataAccessType, @NotNull final ZeroHandlingType zht,
      @NotNull final double[] weights) {
    final IonTimeSeries<? extends Scan> s = feature.getFeatureData();
    assert s instanceof IonMobilogramTimeSeries;

    final IonMobilogramTimeSeries originalSeries = (IonMobilogramTimeSeries) s;
    final MobilogramDataAccess dataAccess = EfficientDataAccess.of(originalSeries, dataAccessType);

    List<IonMobilitySeries> smoothedMobilograms = new ArrayList<>();
//    final SavitzkyGolaySmoothing smoothing = new SavitzkyGolaySmoothing(zht, weights);
    while (dataAccess.hasNext()) {
      final IonMobilitySeries mobilogram = dataAccess.next();
//      double[] smoothedMobilogramIntensities = smoothing.smooth(dataAccess);

    }
    return new ArrayList<>();
  }

  // -----------------------------
  // todo: these are not used yet due to questions regarding the actual implementation
  //  1. if new intensities are added on the peak edges - what do we do on the mobilogram level? We
  //   need the same number of mobilograms as for rt data points
  //  2. Which m/z do we put for newly created intensities? they will influence the overall m/z of
  //   the feature
  private IonTimeSeries<? extends Scan> createNewSeries(@NotNull final IntensitySeries dataAccess,
      @NotNull final ModularFeature feature, @NotNull final double[] smoothedIntensities,
      List<Scan> allScans) {

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
            someMobilityScan = frame.getMobilityScan(frame.getNumberOfMobilityScans() - 1);
          }
          final IonMobilitySeries dummyMobilogram = new SimpleIonMobilitySeries(null,
              new double[]{feature.getMZ()}, new double[]{0}, List.of(someMobilityScan));
          mobilograms.add(dummyMobilogram);
        }
      }
    }

    // todo smooth mobilograms here if needed
    if (originalSeries instanceof IonMobilogramTimeSeries) {

    }
    return null;
  }

  public enum SmoothingDimension {
    RETENTION_TIME("Retention time"), MOBILITY("Mobility");
    private final String name;

    SmoothingDimension(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }
  }
}
