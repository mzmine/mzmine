package io.github.mzmine.datamodel.features.types.numbers;

import io.github.mzmine.datamodel.features.types.numbers.abstr.PercentType;
import io.github.mzmine.gui.preferences.UnitFormat;
import org.jetbrains.annotations.NotNull;

public class RtRelativeErrorType extends PercentType {

  protected RtRelativeErrorType() {
    super();
  }

  @Override
  public @NotNull String getUniqueID() {
    return "rt_relative_error";
  }

  @Override
  public @NotNull String getHeaderString() {
    return UnitFormat.DIVIDE.format("Rt error", "%");
  }
}
