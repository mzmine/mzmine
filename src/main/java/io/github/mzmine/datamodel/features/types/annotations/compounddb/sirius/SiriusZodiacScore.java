package io.github.mzmine.datamodel.features.types.annotations.compounddb.sirius;

import io.github.mzmine.datamodel.features.types.numbers.abstr.ScoreType;
import org.jetbrains.annotations.NotNull;

public class SiriusZodiacScore extends ScoreType {

  @Override
  public @NotNull String getUniqueID() {
    return "sirius_zodiac_score";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Zodiac Score";
  }
}
