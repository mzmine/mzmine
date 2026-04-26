package io.github.mzmine.datamodel.features.compoundlist;

import org.jetbrains.annotations.NotNull;

public enum CompoundMemberRole {
  REPRESENTATIVE, ADDUCT, ISOTOPOLOGUE, IN_SOURCE_FRAGMENT, CORRELATED;

  public @NotNull String getLabel() {
    final String s = name().toLowerCase().replace('_', ' ');
    return Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }
}
