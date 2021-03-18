package io.github.mzmine.modules.dataprocessing.featdet_imsbuilder;

import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilitySeries;
import io.github.mzmine.util.ArrayUtils;
import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.maths.CenterMeasure;
import io.github.mzmine.util.maths.Weighting;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BuildingIonMobilitySeries extends SimpleIonMobilitySeries {

  protected final double summedIntensity;
  protected final double avgMZ;
  protected final int frameNumber;

  /**
   * @param storage         May be null if forceStoreInRam is true.
   * @param mzValues
   * @param intensityValues
   * @param scans
   */
  public BuildingIonMobilitySeries(@Nullable MemoryMapStorage storage, @Nonnull double[] mzValues,
      @Nonnull double[] intensityValues,
      @Nonnull List<MobilityScan> scans) {
    super(storage, mzValues, intensityValues, scans);

    frameNumber = scans.get(0).getFrame().getFrameId();
    summedIntensity = ArrayUtils.sum(intensityValues);
    avgMZ = MathUtils.calcCenter(CenterMeasure.AVG, mzValues, intensityValues, Weighting.LINEAR);
  }

  public double getSummedIntensity() {
    return summedIntensity;
  }

  public double getAvgMZ() {
    return avgMZ;
  }

  public int getFrameNumber() {
    return frameNumber;
  }
}
