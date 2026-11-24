/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification;

import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.LipidChainType;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum LipidFragmentationRuleType {

  HEADGROUP_FRAGMENT, //
  HEADGROUP_FRAGMENT_NL, //
  ACYLCHAIN_FRAGMENT, //
  ACYLCHAIN_FRAGMENT_NL, //
  ACYLCHAIN_PLUS_FORMULA_FRAGMENT, //
  ACYLCHAIN_PLUS_FORMULA_FRAGMENT_NL, //
  ACYLCHAIN_MINUS_FORMULA_FRAGMENT, //
  ACYLCHAIN_MINUS_FORMULA_FRAGMENT_NL, //
  ACYL_MONO_HYDROXY_CHAIN_FRAGMENT, //
  ACYL_MONO_HYDROXY_CHAIN_FRAGMENT_NL, //
  ACYL_MONO_HYDROXY_CHAIN_PLUS_FORMULA_FRAGMENT, //
  ACYL_MONO_HYDROXY_CHAIN_PLUS_FORMULA_FRAGMENT_NL, //
  ACYL_MONO_HYDROXY_CHAIN_MINUS_FORMULA_FRAGMENT, //
  ACYL_MONO_HYDROXY_CHAIN_MINUS_FORMULA_FRAGMENT_NL, //
  TWO_ACYLCHAINS_FRAGMENT, //
  TWO_ACYLCHAINS_FRAGMENT_NL, //
  TWO_ACYLCHAINS_PLUS_FORMULA_FRAGMENT, //
  TWO_ACYLCHAINS_PLUS_FORMULA_FRAGMENT_NL, //
  TWO_ACYLCHAINS_MINUS_FORMULA_FRAGMENT, //
  TWO_ACYLCHAINS_MINUS_FORMULA_FRAGMENT_NL, //
  ALKYLCHAIN_FRAGMENT, //
  ALKYLCHAIN_FRAGMENT_NL, //
  ALKYLCHAIN_PLUS_FORMULA_FRAGMENT, //
  ALKYLCHAIN_PLUS_FORMULA_FRAGMENT_NL, //
  ALKYLCHAIN_MINUS_FORMULA_FRAGMENT, //
  ALKYLCHAIN_MINUS_FORMULA_FRAGMENT_NL, //
  SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN_FRAGMENT, //
  SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_FRAGMENT, //
  SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN_FRAGMENT, //
  SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN_FRAGMENT_NL, //
  SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_FRAGMENT_NL, //
  SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN_FRAGMENT_NL, //
  SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN_PLUS_FORMULA_FRAGMENT_NL, //
  SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_PLUS_FORMULA_FRAGMENT_NL, //
  SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN_PLUS_FORMULA_FRAGMENT_NL, //
  SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT_NL, //
  SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT_NL, //
  SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT_NL, //
  SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN_PLUS_FORMULA_FRAGMENT, //
  SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_PLUS_FORMULA_FRAGMENT, //
  SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN_PLUS_FORMULA_FRAGMENT, //
  SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT, //
  SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT, //
  SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT, //
  AMID_CHAIN_FRAGMENT, //
  AMID_CHAIN_FRAGMENT_NL, //
  AMID_CHAIN_PLUS_FORMULA_FRAGMENT, //
  AMID_CHAIN_PLUS_FORMULA_FRAGMENT_NL, //
  AMID_CHAIN_MINUS_FORMULA_FRAGMENT, //
  AMID_CHAIN_MINUS_FORMULA_FRAGMENT_NL, //
  AMID_MONO_HYDROXY_CHAIN_FRAGMENT, //
  AMID_MONO_HYDROXY_CHAIN_FRAGMENT_NL, //
  AMID_MONO_HYDROXY_CHAIN_PLUS_FORMULA_FRAGMENT, //
  AMID_MONO_HYDROXY_CHAIN_PLUS_FORMULA_FRAGMENT_NL, //
  AMID_MONO_HYDROXY_CHAIN_MINUS_FORMULA_FRAGMENT, //
  AMID_MONO_HYDROXY_CHAIN_MINUS_FORMULA_FRAGMENT_NL, //
  PRECURSOR;

  @Override
  public String toString() {
    return name().charAt(0) + name().substring(1).toLowerCase().replace('_', ' ');
  }

  /**
   * For custom lipids we need to check if this fragment was given a formula in the gui.
   */
  public boolean requiresFromulaFragment() {
    if (this == HEADGROUP_FRAGMENT || this == HEADGROUP_FRAGMENT_NL) {
      return true;
    }
    return isChainFormulaModification();
  }

  /**
   * @return true if this rule adds a formula to or removes a formula from a chain. The formula of
   * these rules may be a signed formula, see {@link LipidChainFormulaModification}.
   */
  public boolean isChainFormulaModification() {
    return name().contains("_PLUS_FORMULA_") || name().contains("_MINUS_FORMULA_");
  }

  /**
   * @return true if this rule adds a formula to a chain
   */
  public boolean isChainPlusFormula() {
    return name().contains("_PLUS_FORMULA_");
  }

  /**
   * @return true if the fragment is observed as neutral loss from the precursor ion
   */
  public boolean isNeutralLoss() {
    return name().endsWith("_NL");
  }

  /**
   * @return true if this rule only applies to sphingolipid backbone chains
   */
  public boolean isSphingolipidSpecific() {
    return name().startsWith("SPHINGOLIPID_");
  }

  /**
   * @return the chain type this rule refers to or null for head group and precursor rules
   */
  public @Nullable LipidChainType getChainType() {
    return switch (this) {
      case HEADGROUP_FRAGMENT, HEADGROUP_FRAGMENT_NL, PRECURSOR -> null;
      case ACYLCHAIN_FRAGMENT, ACYLCHAIN_FRAGMENT_NL, ACYLCHAIN_PLUS_FORMULA_FRAGMENT,
           ACYLCHAIN_PLUS_FORMULA_FRAGMENT_NL, ACYLCHAIN_MINUS_FORMULA_FRAGMENT,
           ACYLCHAIN_MINUS_FORMULA_FRAGMENT_NL -> LipidChainType.ACYL_CHAIN;
      case ACYL_MONO_HYDROXY_CHAIN_FRAGMENT, ACYL_MONO_HYDROXY_CHAIN_FRAGMENT_NL,
           ACYL_MONO_HYDROXY_CHAIN_PLUS_FORMULA_FRAGMENT,
           ACYL_MONO_HYDROXY_CHAIN_PLUS_FORMULA_FRAGMENT_NL,
           ACYL_MONO_HYDROXY_CHAIN_MINUS_FORMULA_FRAGMENT,
           ACYL_MONO_HYDROXY_CHAIN_MINUS_FORMULA_FRAGMENT_NL ->
          LipidChainType.ACYL_MONO_HYDROXY_CHAIN;
      case TWO_ACYLCHAINS_FRAGMENT, TWO_ACYLCHAINS_FRAGMENT_NL,
           TWO_ACYLCHAINS_PLUS_FORMULA_FRAGMENT, TWO_ACYLCHAINS_PLUS_FORMULA_FRAGMENT_NL,
           TWO_ACYLCHAINS_MINUS_FORMULA_FRAGMENT, TWO_ACYLCHAINS_MINUS_FORMULA_FRAGMENT_NL ->
          LipidChainType.TWO_ACYL_CHAINS_COMBINED;
      case ALKYLCHAIN_FRAGMENT, ALKYLCHAIN_FRAGMENT_NL, ALKYLCHAIN_PLUS_FORMULA_FRAGMENT,
           ALKYLCHAIN_PLUS_FORMULA_FRAGMENT_NL, ALKYLCHAIN_MINUS_FORMULA_FRAGMENT,
           ALKYLCHAIN_MINUS_FORMULA_FRAGMENT_NL -> LipidChainType.ALKYL_CHAIN;
      case SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN_FRAGMENT,
           SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN_FRAGMENT_NL,
           SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN_PLUS_FORMULA_FRAGMENT,
           SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN_PLUS_FORMULA_FRAGMENT_NL,
           SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT,
           SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT_NL ->
          LipidChainType.SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN;
      case SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_FRAGMENT,
           SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_FRAGMENT_NL,
           SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_PLUS_FORMULA_FRAGMENT,
           SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_PLUS_FORMULA_FRAGMENT_NL,
           SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT,
           SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT_NL ->
          LipidChainType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN;
      case SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN_FRAGMENT,
           SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN_FRAGMENT_NL,
           SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN_PLUS_FORMULA_FRAGMENT,
           SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN_PLUS_FORMULA_FRAGMENT_NL,
           SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT,
           SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT_NL ->
          LipidChainType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN;
      case AMID_CHAIN_FRAGMENT, AMID_CHAIN_FRAGMENT_NL, AMID_CHAIN_PLUS_FORMULA_FRAGMENT,
           AMID_CHAIN_PLUS_FORMULA_FRAGMENT_NL, AMID_CHAIN_MINUS_FORMULA_FRAGMENT,
           AMID_CHAIN_MINUS_FORMULA_FRAGMENT_NL -> LipidChainType.AMID_CHAIN;
      case AMID_MONO_HYDROXY_CHAIN_FRAGMENT, AMID_MONO_HYDROXY_CHAIN_FRAGMENT_NL,
           AMID_MONO_HYDROXY_CHAIN_PLUS_FORMULA_FRAGMENT,
           AMID_MONO_HYDROXY_CHAIN_PLUS_FORMULA_FRAGMENT_NL,
           AMID_MONO_HYDROXY_CHAIN_MINUS_FORMULA_FRAGMENT,
           AMID_MONO_HYDROXY_CHAIN_MINUS_FORMULA_FRAGMENT_NL ->
          LipidChainType.AMID_MONO_HYDROXY_CHAIN;
    };
  }

  /**
   * @return the part of the lipid this rule describes
   */
  public @NotNull LipidRuleTarget getTarget() {
    final LipidChainType chainType = getChainType();
    if (chainType != null) {
      final LipidRuleTarget target = LipidRuleTarget.ofChainType(chainType);
      if (target != null) {
        return target;
      }
    }
    return this == PRECURSOR ? LipidRuleTarget.PRECURSOR : LipidRuleTarget.HEAD_GROUP;
  }

  /**
   * @return how the rule formula modifies the target
   */
  public @NotNull LipidRuleModification getModification() {
    if (isChainPlusFormula()) {
      return LipidRuleModification.PLUS_FORMULA;
    }
    return isChainFormulaModification() ? LipidRuleModification.MINUS_FORMULA
        : LipidRuleModification.NONE;
  }

  /**
   * @return how the rule is observed in the spectrum
   */
  public @NotNull LipidRuleObservation getObservation() {
    return isNeutralLoss() ? LipidRuleObservation.NEUTRAL_LOSS : LipidRuleObservation.FRAGMENT_ION;
  }

  /**
   * Finds the rule type for the combination of target, modification and observation.
   *
   * @return the rule type or empty if the combination is not supported, e.g., a modified head group
   */
  public static @NotNull Optional<LipidFragmentationRuleType> of(@NotNull LipidRuleTarget target,
      @NotNull LipidRuleModification modification, @NotNull LipidRuleObservation observation) {
    for (final LipidFragmentationRuleType type : values()) {
      if (type.getTarget() == target && type.getModification() == modification
          && type.getObservation() == observation) {
        return Optional.of(type);
      }
    }
    return Optional.empty();
  }

  /**
   * @param target the part of the lipid
   * @return the modifications that exist for the target, e.g., none for head groups
   */
  public static @NotNull List<LipidRuleModification> validModifications(
      @NotNull LipidRuleTarget target) {
    return Arrays.stream(LipidRuleModification.values()).filter(
        m -> Arrays.stream(LipidRuleObservation.values())
            .anyMatch(o -> of(target, m, o).isPresent())).toList();
  }

  /**
   * @return the observations that exist for the target and modification, e.g., only fragment ion
   * for the precursor
   */
  public static @NotNull List<LipidRuleObservation> validObservations(
      @NotNull LipidRuleTarget target, @NotNull LipidRuleModification modification) {
    return Arrays.stream(LipidRuleObservation.values())
        .filter(o -> of(target, modification, o).isPresent()).toList();
  }
}
