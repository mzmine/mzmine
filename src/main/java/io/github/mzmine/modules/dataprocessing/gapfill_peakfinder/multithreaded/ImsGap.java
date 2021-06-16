package io.github.mzmine.modules.dataprocessing.gapfill_peakfinder.multithreaded;

import com.google.common.collect.Range;
import gnu.trove.list.array.TDoubleArrayList;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.MobilityScanDataAccess;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.dataprocessing.gapfill_peakfinder.Gap;
import io.github.mzmine.util.exceptions.MissingMassListException;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;

public class ImsGap extends Gap {

  private final Range<Float> mobilityRange;

  /**
   * Constructor: Initializes an empty gap
   *
   * @param peakListRow
   * @param rawDataFile
   * @param mzRange      M/Z coordinate of this empty gap
   * @param rtRange      RT coordinate of this empty gap
   * @param intTolerance
   */
  public ImsGap(FeatureListRow peakListRow, RawDataFile rawDataFile, Range<Double> mzRange,
      Range<Float> rtRange, Range<Float> mobilityRange, double intTolerance) {
    super(peakListRow, rawDataFile, mzRange, rtRange, intTolerance);
    this.mobilityRange = mobilityRange;
  }

  @Override
  public DataPoint findDataPoint(@NotNull final Scan scanAccess) {

    if(!(scanAccess instanceof MobilityScanDataAccess access)) {
      throw new IllegalArgumentException("Scan is not a MobilityScanDataAccess");
    }

    final Frame frame = access.getFrame();
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

      if (scan.getMobility() < mobilityRange.lowerEndpoint()) {
        continue;
      } else if (scan.getMobility() > mobilityRange.upperEndpoint()) {
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
        intensityValues.add(access.getMzValue(bestIndex));
        mobilogramScans.add(scan);
      }
    }

    if (!mobilogramScans.isEmpty()) {
      return new DataPointIonMobilitySeries(
          null, mzValues.toArray(), intensityValues.toArray(), mobilogramScans);
    }
    return null;
  }
}
