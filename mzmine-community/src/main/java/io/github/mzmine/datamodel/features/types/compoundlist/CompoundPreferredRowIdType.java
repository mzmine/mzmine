package io.github.mzmine.datamodel.features.types.compoundlist;

import io.github.mzmine.datamodel.features.types.numbers.abstr.IntegerType;
import org.jetbrains.annotations.NotNull;

public class CompoundPreferredRowIdType extends IntegerType {

  @Override
  public @NotNull String getUniqueID() {
    return "compound_preferred_row_id";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Preferred Row ID";
  }

  @Override
  public boolean getDefaultVisibility() {
    return false;
  }
}
