package io.github.mzmine.datamodel.features.types.compoundlist;

import io.github.mzmine.datamodel.features.types.numbers.abstr.ScoreType;
import org.jetbrains.annotations.NotNull;

public class CompoundConfidenceType extends ScoreType {

  public CompoundConfidenceType() {
    super();
  }

  @Override
  public @NotNull String getUniqueID() {
    return "compound_confidence";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Confidence";
  }
}