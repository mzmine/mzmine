package io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification;

import org.jetbrains.annotations.NotNull;

/**
 * How the formula of a fragmentation rule changes the {@link LipidRuleTarget}.
 */
public enum LipidRuleModification {

  NONE("No modification"), //
  PLUS_FORMULA("+ formula"), //
  MINUS_FORMULA("− formula");

  private final @NotNull String label;

  LipidRuleModification(@NotNull String label) {
    this.label = label;
  }

  @Override
  public @NotNull String toString() {
    return label;
  }
}
