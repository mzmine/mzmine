package io.github.mzmine.datamodel.features.types.numbers;

import io.github.mzmine.datamodel.features.types.numbers.abstr.FloatType;
import io.github.mzmine.main.MZmineCore;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import org.jetbrains.annotations.NotNull;

public class RtRelativeErrorType extends FloatType {

  private static final NumberFormat format = MZmineCore.getConfiguration().getRTFormat();

  protected RtRelativeErrorType() {
    super(new DecimalFormat("0.00"));
  }

  @Override
  public @NotNull String getUniqueID() {
    return "rt_relative_error";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Rt error / %";
  }

  @Override
  public NumberFormat getFormatter() {
    return format;
  }
}
