package io.github.mzmine.datamodel.features.types.compoundlist;

import io.github.mzmine.datamodel.features.types.numbers.abstr.IntegerType;
import org.jetbrains.annotations.NotNull;

public class CompoundSizeType extends IntegerType {

  @Override
  public @NotNull String getUniqueID() {
    return "compound_size";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "#Members";
  }
}
