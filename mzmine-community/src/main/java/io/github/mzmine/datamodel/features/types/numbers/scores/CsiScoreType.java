package io.github.mzmine.datamodel.features.types.numbers.scores;

import io.github.mzmine.datamodel.features.types.numbers.abstr.ScoreType;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import org.jetbrains.annotations.NotNull;

public class CsiScoreType extends ScoreType {

  private static final NumberFormat FORMAT = new DecimalFormat("#.##");

  @Override
  public NumberFormat getFormat() {
    return FORMAT;
  }

  @Override
  public String getUniqueID() {
    return "sirius_csi_score";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "CSI:FingerId";
  }
}
