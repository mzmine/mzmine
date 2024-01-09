/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.lipidfragmentannotation;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.LipidFragmentationRuleType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.lipidchain.LipidChainType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidannotationmodules.LipidAnnotationChainParameters;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.List;

;

public class SphingolipidFragmentFactory extends LipidFragmentFactory implements
    ILipidFragmentFactory {

  public SphingolipidFragmentFactory(MZTolerance mzToleranceMS2, ILipidAnnotation lipidAnnotation,
      IonizationType ionizationType, LipidFragmentationRule[] rules, Scan msMsScan,
      LipidAnnotationChainParameters chainParameters) {
    super(mzToleranceMS2, lipidAnnotation, ionizationType, rules, msMsScan, chainParameters);
  }

  @Override
  public List<LipidFragment> findLipidFragments() {
    return findCommonLipidFragments();
  }

  private List<LipidFragment> checkForSphingolipidSpecificRuleTypes(LipidFragmentationRule rule) {
    LipidFragmentationRuleType ruleType = rule.getLipidFragmentationRuleType();
    return switch (ruleType) {
      case SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN_FRAGMENT ->
          findChainFragment(rule, lipidAnnotation, msMsScan,
              LipidChainType.SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN);
      case SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_FRAGMENT ->
          findChainFragment(rule, lipidAnnotation, msMsScan,
              LipidChainType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN);
      case SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN_FRAGMENT ->
          findChainFragment(rule, lipidAnnotation, msMsScan,
              LipidChainType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN);
      case SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT ->
          findChainMinusFormulaFragment(rule, lipidAnnotation, msMsScan,
              LipidChainType.SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN);
      case SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT ->
          findChainMinusFormulaFragment(rule, lipidAnnotation, msMsScan,
              LipidChainType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN);
      case SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT ->
          findChainMinusFormulaFragment(rule, lipidAnnotation, msMsScan,
              LipidChainType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN);
      default -> List.of();
    };
  }

}