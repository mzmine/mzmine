package io.github.mzmine.datamodel.features.types;

import io.github.mzmine.datamodel.features.types.abstr.StringType;
import org.jetbrains.annotations.NotNull;

public class MaldiSpotType extends StringType {

  @Override
  public @NotNull String getUniqueID() {
    return "maldi_spot_type";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Spot";
  }
}
