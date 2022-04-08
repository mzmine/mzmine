package io.github.mzmine.datamodel.features.types.annotations.compounddb.sirius;

import io.github.mzmine.datamodel.features.types.numbers.abstr.ScoreType;
import org.jetbrains.annotations.NotNull;

public class SiriusFingerIdScoreType extends ScoreType {

  @Override
  public @NotNull String getUniqueID() {
    return "sirius_finger_id_score";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "FingerID Score (Sirius)";
  }
}
