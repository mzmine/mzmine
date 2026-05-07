package io.github.mzmine.datamodel.features.compoundlist;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import org.jetbrains.annotations.NotNull;

public enum CompoundMemberRole implements UniqueIdSupplier {
  REPRESENTATIVE, ADDUCT, ISOTOPOLOGUE, IN_SOURCE_FRAGMENT, CORRELATED;

  public @NotNull String getLabel() {
    final String s = name().toLowerCase().replace('_', ' ');
    return Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case REPRESENTATIVE -> "representative";
      case ADDUCT -> "adduct";
      case ISOTOPOLOGUE -> "isotopologue";
      case IN_SOURCE_FRAGMENT -> "in_source_fragment";
      case CORRELATED -> "correlated";
    };
  }
}
