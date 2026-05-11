package io.github.mzmine.datamodel.features.types.compoundlist;

import io.github.mzmine.datamodel.features.types.numbers.abstr.IntegerType;
import org.jetbrains.annotations.NotNull;

public class CompoundIdType extends IntegerType {

  @Override
  public @NotNull String getUniqueID() {
    return "compound_id";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Compound ID";
  }

  @Override
  public boolean getDefaultVisibility() {
    return true;
  }


}
