package io.github.mzmine.modules.dataprocessing.gapfill_peakfinder.multithreaded;

import com.google.common.collect.Range;
import gnu.trove.list.array.TDoubleArrayList;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.data_access.MobilityScanDataAccess;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.IonMobilogramTimeSeriesFactory;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.dataprocessing.gapfill_peakfinder.Gap;
import io.github.mzmine.util.exceptions.MissingMassListException;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;

public class ImsGap extends Gap {

  private final Range<Float> mobilityRange;

  private List<DataPointIonMobilitySeries> currentPeakDataPoints;
  private @org.jetbrains.annotations.NotNull List<IonMobilitySeries> bestPeakDataPoints;
  protected double bestPeakHeight;
  private final BinningMobilogramDataAccess mobilogramBinning;

  /**
   * Constructor: Initializes an empty gap
   *
   * @param peakListRow
   * @param rawDataFile
   * @param mzRange           M/Z coordinate of this empty gap
   * @param rtRange           RT coordinate of this empty gap
   * @param intTolerance
   * @param mobilogramBinning
   */
  public ImsGap(FeatureListRow peakListRow, RawDataFile rawDataFile, Range<Double> mzRange,
      Range<Float> rtRange, Range<Float> mobilityRange, double intTolerance,
      BinningMobilogramDataAccess mobilogramBinning) {
    super(peakListRow, rawDataFile, mzRange, rtRange, intTolerance);
    this.mobilityRange = mobilityRange;
    this.mobilogramBinning = mobilogramBinning;
  }

  @Override
  public void offerNextScan(Scan scan) {
    if (!(scan instanceof MobilityScanDataAccess access)) {
      throw new IllegalArgumentException("Scan is not a MobilityScanDataAccess");
    }

    double scanRT = scan.getRetentionTime();

    // If not yet inside the RT range
    if (scanRT < rtRange.lowerEndpoint()) {
      return;
    }

    // If we have passed the RT range and finished processing last peak
    if ((scanRT > rtRange.upperEndpoint()) && (currentPeakDataPoints == null)) {
      return;
    }

    DataPointIonMobilitySeries mobilogram = (DataPointIonMobilitySeries) findDataPoint(access);

//    if(mobilogram == null) {
//      mobilogram = new DataPointIonMobilitySeries(null, new double[] {peakListRow.getAverageMZ()}, new double[] {0d}, )
//    }

    if (mobilogram == null) {
      return;
    }

    if (currentPeakDataPoints == null) {
      currentPeakDataPoints = new ArrayList<>();
      currentPeakDataPoints.add(mobilogram);
      return;
    }

    // Check if this continues previous peak?
    if (checkRTShape(mobilogram)) {
      // Yes, continue this peak.
      currentPeakDataPoints.add(mobilogram);
    } else {

      // No, new peak is starting
      // Check peak formed so far
      if (currentPeakDataPoints != null) {
        checkCurrentPeak();
        currentPeakDataPoints = null;
      }
    }

  }

  private DataPointIonMobilitySeries findDataPoint(@NotNull final MobilityScanDataAccess access) {

    final Frame frame = access.getFrame();
    final MobilityType mobilityType = frame.getMobilityType();
    final double featureMz = peakListRow.getAverageMZ();

    /*if (frame.getRetentionTime() < rtRange.lowerEndpoint()) {
      return null;
    } else if (frame.getRetentionTime() > rtRange.upperEndpoint()) {
      return null;
    }*/

    final List<MobilityScan> mobilogramScans = new ArrayList<>();
    final TDoubleArrayList mzValues = new TDoubleArrayList();
    final TDoubleArrayList intensityValues = new TDoubleArrayList();

    while (access.hasNextMobilityScan()) {
      final MobilityScan scan;
      try {
        scan = access.nextMobilityScan();
      } catch (MissingMassListException e) {
        e.printStackTrace();
        return null;
      }

      if ((mobilityType != MobilityType.TIMS && scan.getMobility() < mobilityRange.lowerEndpoint())
          || (mobilityType == MobilityType.TIMS && scan.getMobility() > mobilityRange
          .upperEndpoint())) {
        continue;
      } else if (
          (mobilityType != MobilityType.TIMS && scan.getMobility() > mobilityRange.upperEndpoint())
              || (mobilityType == MobilityType.TIMS && scan.getMobility() < mobilityRange
              .lowerEndpoint())) {
        break;
      }

      int bestIndex = -1;
      double bestDelta = Double.POSITIVE_INFINITY;
      for (int i = 0; i < access.getNumberOfDataPoints(); i++) {
        final double mz = access.getMzValue(i);
        if (mz < mzRange.lowerEndpoint()) {
          continue;
        } else if (mz > mzRange.upperEndpoint()) {
          break;
        }

        final double delta = Math.abs(mz - featureMz);
        if (delta < bestDelta) {
          bestDelta = delta;
          bestIndex = i;
        }
      }

      if (bestIndex != -1) {
        mzValues.add(access.getMzValue(bestIndex));
        intensityValues.add(access.getIntensityValue(bestIndex));
        mobilogramScans.add(scan);
      }
    }

    if (!mobilogramScans.isEmpty()) {
      return new DataPointIonMobilitySeries(null, mzValues.toArray(), intensityValues.toArray(),
          mobilogramScans);
    }
    return null;
  }

  protected boolean checkRTShape(DataPointIonMobilitySeries dp) {

    if (dp.getSpectrum(0).getRetentionTime() < rtRange.lowerEndpoint()) {
      double prevInt = currentPeakDataPoints.get(currentPeakDataPoints.size() - 1).getIntensity();
      if (dp.getIntensity() > (prevInt * (1 - intTolerance))) {
        return true;
      }
    }

    if (rtRange.contains((float) dp.getSpectrum(0).getRetentionTime())) {
      return true;
    }

    if (dp.getSpectrum(0).getRetentionTime() > rtRange.upperEndpoint()) {
      double prevInt = currentPeakDataPoints.get(currentPeakDataPoints.size() - 1).getIntensity();
      if (dp.getIntensity() < (prevInt * (1 + intTolerance))) {
        return true;
      }
    }

    return false;
  }

  private void checkCurrentPeak() {

    // 1) Check if currentpeak has a local maximum inside the search range
    int highestMaximumInd = -1;
    double currentMaxHeight = 0f;
    for (int i = 1; i < currentPeakDataPoints.size() - 1; i++) {

      if (rtRange
          .contains((float) currentPeakDataPoints.get(i).getSpectrum(0).getRetentionTime())) {

        if ((currentPeakDataPoints.get(i).getIntensity() >= currentPeakDataPoints.get(i + 1)
            .getIntensity()) && (currentPeakDataPoints.get(i).getIntensity()
            >= currentPeakDataPoints.get(i - 1).getIntensity())) {

          if (currentPeakDataPoints.get(i).getIntensity() > currentMaxHeight) {

            currentMaxHeight = currentPeakDataPoints.get(i).getIntensity();
            highestMaximumInd = i;
          }
        }
      }
    }

    // If no local maximum, return
    if (highestMaximumInd == -1) {
      return;
    }

    // 2) Find elution start and stop
    int startInd = highestMaximumInd;
    double currentInt = currentPeakDataPoints.get(startInd).getIntensity();
    while (startInd > 0) {
      double nextInt = currentPeakDataPoints.get(startInd - 1).getIntensity();
      if (currentInt < (nextInt * (1 - intTolerance))) {
        break;
      }
      startInd--;
      if (nextInt == 0) {
        break;
      }
      currentInt = nextInt;
    }

    // Since subList does not include toIndex value then find highest
    // possible value of stopInd+1 and currentPeakDataPoints.size()
    int stopInd = highestMaximumInd, toIndex = highestMaximumInd;
    currentInt = currentPeakDataPoints.get(stopInd).getIntensity();
    while (stopInd < (currentPeakDataPoints.size() - 1)) {
      double nextInt = currentPeakDataPoints.get(stopInd + 1).getIntensity();
      if (nextInt > (currentInt * (1 + intTolerance))) {
        toIndex = Math.min(currentPeakDataPoints.size(), stopInd + 1);
        break;
      }
      stopInd++;
      toIndex = Math.min(currentPeakDataPoints.size(), stopInd + 1);
      if (nextInt == 0) {
        stopInd++;
        toIndex = stopInd;
        break;
      }
      currentInt = nextInt;
    }

    // 3) Check if this is the best candidate for a peak
    if ((bestPeakDataPoints == null) || (bestPeakHeight < currentMaxHeight)) {
      bestPeakDataPoints = (List<IonMobilitySeries>) (List<? extends IonMobilitySeries>) currentPeakDataPoints
          .subList(startInd, toIndex);
    }
  }

  @Override
  public void noMoreOffers() {
    // Check peak that was last constructed
    if (currentPeakDataPoints != null) {
      checkCurrentPeak();
      currentPeakDataPoints = null;
    }

    if (bestPeakDataPoints == null) {
      return;
    }

    final IonMobilogramTimeSeries trace = IonMobilogramTimeSeriesFactory
        .of(((ModularFeatureList) peakListRow.getFeatureList()).getMemoryMapStorage(),
            bestPeakDataPoints, mobilogramBinning);

    ModularFeature f = new ModularFeature((ModularFeatureList) peakListRow.getFeatureList(),
        rawDataFile, 0d, 0f, trace, FeatureStatus.MANUAL, null, null, null);
    FeatureDataUtils.recalculateIonSeriesDependingTypes(f);

    peakListRow.addFeature(rawDataFile, f);
  }
}
