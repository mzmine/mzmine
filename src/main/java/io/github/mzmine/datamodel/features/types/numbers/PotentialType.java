package io.github.mzmine.datamodel.features.types.numbers;

import io.github.mzmine.datamodel.features.types.numbers.abstr.FloatType;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import org.jetbrains.annotations.NotNull;

public class PotentialType extends FloatType {

  private static final DecimalFormat format = new DecimalFormat("0.00");

  public PotentialType() {
    super(format);
  }

  @Override
  public @NotNull String getUniqueID() {
    return "ec_ms_potential";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Potential / V";
  }

  @Override
  public NumberFormat getFormatter() {
    return format;
  }
}
