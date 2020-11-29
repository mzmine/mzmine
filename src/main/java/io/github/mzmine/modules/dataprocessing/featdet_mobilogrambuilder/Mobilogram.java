package io.github.mzmine.modules.dataprocessing.featdet_mobilogrambuilder;


import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MobilityType;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface Mobilogram {

  public double getMZ();

  public double getMobility();

  @Nullable
  public Range<Double> getMobilityRange();

  @Nonnull
  List<MobilityDataPoint> getDataPoints();

  @Nonnull
  List<Integer> getScanNumbers();

  public MobilityType getMobilityType();
}
