package io.github.mzmine.datamodel.features.types.annotations.compounddb.sirius;

import io.github.mzmine.datamodel.features.types.abstr.StringType;
import org.jetbrains.annotations.NotNull;

public class SiriusIdType extends StringType {

  @Override
  public @NotNull String getUniqueID() {
    return "sirius_id_type";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Sirius ID";
  }
}
