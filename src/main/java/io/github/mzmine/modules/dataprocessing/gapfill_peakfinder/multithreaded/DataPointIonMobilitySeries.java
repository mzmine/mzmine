package io.github.mzmine.modules.dataprocessing.gapfill_peakfinder.multithreaded;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilitySeries;
import io.github.mzmine.util.ArrayUtils;
import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.scans.SpectraMerging;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DataPointIonMobilitySeries extends SimpleIonMobilitySeries implements DataPoint {

  private final double mz;
  private final double intensity;

  /**
   * @param storage         May be null if forceStoreInRam is true.
   * @param mzValues
   * @param intensityValues
   * @param scans
   */
  public DataPointIonMobilitySeries(@Nullable MemoryMapStorage storage, @NotNull double[] mzValues,
      @NotNull double[] intensityValues, @NotNull List<MobilityScan> scans) {
    super(storage, mzValues, intensityValues, scans);

    mz = MathUtils.calcCenter(SpectraMerging.DEFAULT_CENTER_MEASURE, mzValues, intensityValues,
        SpectraMerging.DEFAULT_WEIGHTING);
    intensity = ArrayUtils.sum(intensityValues);
  }

  @Override
  public double getMZ() {
    return mz;
  }

  @Override
  public double getIntensity() {
    return intensity;
  }
}
