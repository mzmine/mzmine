package io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification;

import org.jetbrains.annotations.NotNull;

/**
 * How a fragmentation rule is observed in the MS2 spectrum.
 */
public enum LipidRuleObservation {

  FRAGMENT_ION("Fragment ion"), //
  NEUTRAL_LOSS("Neutral loss");

  private final @NotNull String label;

  LipidRuleObservation(@NotNull String label) {
    this.label = label;
  }

  @Override
  public @NotNull String toString() {
    return label;
  }
}
