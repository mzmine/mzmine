package io.github.mzmine.datamodel.features.types.annotations.resolver;

import org.jetbrains.annotations.NotNull;
import io.github.mzmine.datamodel.features.types.numbers.abstr.IntegerType;

public class SignChangesType  extends IntegerType {

  public SignChangesType() {
    super();
  }

  @Override
  public @NotNull String getUniqueID() {
    return "sign_count";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "sign changes";
  }
}
