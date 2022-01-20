package io.github.mzmine.datamodel.features.types.numbers;

import io.github.mzmine.datamodel.features.types.numbers.abstr.FloatType;
import io.github.mzmine.gui.preferences.UnitFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import org.jetbrains.annotations.NotNull;

public class CCSRelativeErrorType extends FloatType {

  private static final NumberFormat defaultFormat = new DecimalFormat("0.00 %");
  private static final String headerString = UnitFormat.DIVIDE.format("\u0394 CCS", "%");

  public CCSRelativeErrorType() {
    super(defaultFormat);
  }

  @Override
  public @NotNull String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "collisional_cross_section_relative_error";
  }

  @Override
  public @NotNull String getHeaderString() {
    return headerString;
  }

  @Override
  public NumberFormat getFormatter() {
    return defaultFormat;
  }
}
