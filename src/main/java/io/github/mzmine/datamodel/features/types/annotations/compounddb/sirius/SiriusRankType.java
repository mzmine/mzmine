package io.github.mzmine.datamodel.features.types.annotations.compounddb.sirius;

import io.github.mzmine.datamodel.features.types.numbers.abstr.IntegerType;
import org.jetbrains.annotations.NotNull;

public class SiriusRankType extends IntegerType {

  @Override
  public @NotNull String getUniqueID() {
    return "sirius_rank";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Rank (Sirius)";
  }
}
