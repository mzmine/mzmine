package io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification;

import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.LipidChainType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * What part of a lipid a fragmentation rule describes. Together with
 * {@link LipidRuleModification} and {@link LipidRuleObservation} it defines a
 * {@link LipidFragmentationRuleType} in terms that are easier to understand in the GUI.
 */
public enum LipidRuleTarget {

  PRECURSOR("Precursor ion", null), //
  HEAD_GROUP("Head group", null), //
  ACYL_CHAIN("Acyl chain", LipidChainType.ACYL_CHAIN), //
  ACYL_MONO_HYDROXY_CHAIN("Acyl chain with OH", LipidChainType.ACYL_MONO_HYDROXY_CHAIN), //
  TWO_ACYL_CHAINS("Two acyl chains combined", LipidChainType.TWO_ACYL_CHAINS_COMBINED), //
  ALKYL_CHAIN("Alkyl chain", LipidChainType.ALKYL_CHAIN), //
  AMID_CHAIN("Amid chain", LipidChainType.AMID_CHAIN), //
  AMID_MONO_HYDROXY_CHAIN("Amid chain with OH", LipidChainType.AMID_MONO_HYDROXY_CHAIN), //
  SPHINGOID_BASE_MONO_HYDROXY("Sphingoid base (1 OH)",
      LipidChainType.SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN), //
  SPHINGOID_BASE_DI_HYDROXY("Sphingoid base (2 OH)",
      LipidChainType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN), //
  SPHINGOID_BASE_TRI_HYDROXY("Sphingoid base (3 OH)",
      LipidChainType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN);

  private final @NotNull String label;
  private final @Nullable LipidChainType chainType;

  LipidRuleTarget(@NotNull String label, @Nullable LipidChainType chainType) {
    this.label = label;
    this.chainType = chainType;
  }

  /**
   * @return the chain type or null for precursor and head group
   */
  public @Nullable LipidChainType getChainType() {
    return chainType;
  }

  /**
   * @param chainType a chain type
   * @return the target of the chain type or null if no rule refers to it
   */
  public static @Nullable LipidRuleTarget ofChainType(@NotNull LipidChainType chainType) {
    for (final LipidRuleTarget target : values()) {
      if (target.chainType == chainType) {
        return target;
      }
    }
    return null;
  }

  @Override
  public @NotNull String toString() {
    return label;
  }
}
