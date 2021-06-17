package io.github.mzmine.modules.dataprocessing.gapfill_peakfinder.multithreaded;

import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilitySeries;
import io.github.mzmine.modules.dataprocessing.gapfill_peakfinder.GapDataPoint;
import io.github.mzmine.util.ArrayUtils;
import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.scans.SpectraMerging;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DataPointIonMobilitySeries extends SimpleIonMobilitySeries implements GapDataPoint {

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

  @Override
  public double getRT() {
    return getSpectrum(0).getRetentionTime();
  }

  @Override
  public Scan getScan() {
    return getSpectrum(0).getFrame();
  }
}
