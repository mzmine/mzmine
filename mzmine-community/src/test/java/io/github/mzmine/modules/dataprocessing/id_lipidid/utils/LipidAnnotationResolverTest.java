/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_lipidid.utils;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRuleRating;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRuleType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidAnnotationLevel;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidClasses;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidFragment;
import java.util.Arrays;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class LipidAnnotationResolverTest {

  private static final LipidFactory LIPID_FACTORY = new LipidFactory();

  @Test
  void relativeIntensityScoreFavorsClassCloserToObservedTwoAcylRatio() {
    final MatchedLipid pgMatch = createMatchedLipid(
        LipidClasses.DIACYLGLYCEROPHOSPHOGLYCEROLS, 100d, 40d);
    final MatchedLipid bmpMatch = createMatchedLipid(
        LipidClasses.MONOACYLGLYCEROPHOSPHOMONORADYLGLYCEROLS, 100d, 40d);

    final Double pgScore = LipidAnnotationResolver.computeRelativeRuleIntensityPreferenceScore(
        pgMatch);
    final Double bmpScore = LipidAnnotationResolver.computeRelativeRuleIntensityPreferenceScore(
        bmpMatch);

    Assertions.assertNotNull(pgScore);
    Assertions.assertNotNull(bmpScore);

    final double observedTwoAcylRatio = 100d / (100d + 40d);
    final double pgDistance = Math.abs(observedTwoAcylRatio - expectedTwoAcylRatio(
        LipidClasses.DIACYLGLYCEROPHOSPHOGLYCEROLS));
    final double bmpDistance = Math.abs(observedTwoAcylRatio - expectedTwoAcylRatio(
        LipidClasses.MONOACYLGLYCEROPHOSPHOMONORADYLGLYCEROLS));
    if (pgDistance < bmpDistance) {
      Assertions.assertTrue(pgScore > bmpScore,
          () -> "Expected PG score > BMP score but got PG=" + pgScore + ", BMP=" + bmpScore);
    } else if (bmpDistance < pgDistance) {
      Assertions.assertTrue(bmpScore > pgScore,
          () -> "Expected BMP score > PG score but got BMP=" + bmpScore + ", PG=" + pgScore);
    } else {
      Assertions.assertEquals(pgScore, bmpScore, 1e-9,
          "Expected equal scores for equal expected distance to observed ratio.");
    }
  }

  @Test
  void relativeIntensityScorePrefersPgWhenOnlyAcylFragmentIsDetected() {
    final MatchedLipid pgMatch = createMatchedLipidWithSingleFragment(
        LipidClasses.DIACYLGLYCEROPHOSPHOGLYCEROLS,
        LipidFragmentationRuleType.ACYLCHAIN_PLUS_FORMULA_FRAGMENT, "C3H5O", 100d);
    final MatchedLipid bmpMatch = createMatchedLipidWithSingleFragment(
        LipidClasses.MONOACYLGLYCEROPHOSPHOMONORADYLGLYCEROLS,
        LipidFragmentationRuleType.ACYLCHAIN_PLUS_FORMULA_FRAGMENT, "C3H5O", 100d);

    final Double pgScore = LipidAnnotationResolver.computeRelativeRuleIntensityPreferenceScore(
        pgMatch);
    final Double bmpScore = LipidAnnotationResolver.computeRelativeRuleIntensityPreferenceScore(
        bmpMatch);

    Assertions.assertNotNull(pgScore);
    Assertions.assertNotNull(bmpScore);
    final int pgWeight = findRelativeIntensityWeight(LipidClasses.DIACYLGLYCEROPHOSPHOGLYCEROLS,
        LipidFragmentationRuleType.ACYLCHAIN_PLUS_FORMULA_FRAGMENT, "C3H5O");
    final int bmpWeight = findRelativeIntensityWeight(
        LipidClasses.MONOACYLGLYCEROPHOSPHOMONORADYLGLYCEROLS,
        LipidFragmentationRuleType.ACYLCHAIN_PLUS_FORMULA_FRAGMENT, "C3H5O");
    assertScoreOrderMatchesWeight(pgScore, bmpScore, pgWeight, bmpWeight, "acyl");
  }

  @Test
  void relativeIntensityScorePrefersBmpWhenOnlyTwoAcylFragmentIsDetected() {
    final MatchedLipid pgMatch = createMatchedLipidWithSingleFragment(
        LipidClasses.DIACYLGLYCEROPHOSPHOGLYCEROLS,
        LipidFragmentationRuleType.TWO_ACYLCHAINS_PLUS_FORMULA_FRAGMENT, "C3H3", 100d);
    final MatchedLipid bmpMatch = createMatchedLipidWithSingleFragment(
        LipidClasses.MONOACYLGLYCEROPHOSPHOMONORADYLGLYCEROLS,
        LipidFragmentationRuleType.TWO_ACYLCHAINS_PLUS_FORMULA_FRAGMENT, "C3H3", 100d);

    final Double pgScore = LipidAnnotationResolver.computeRelativeRuleIntensityPreferenceScore(
        pgMatch);
    final Double bmpScore = LipidAnnotationResolver.computeRelativeRuleIntensityPreferenceScore(
        bmpMatch);

    Assertions.assertNotNull(pgScore);
    Assertions.assertNotNull(bmpScore);
    final int pgWeight = findRelativeIntensityWeight(LipidClasses.DIACYLGLYCEROPHOSPHOGLYCEROLS,
        LipidFragmentationRuleType.TWO_ACYLCHAINS_PLUS_FORMULA_FRAGMENT, "C3H3");
    final int bmpWeight = findRelativeIntensityWeight(
        LipidClasses.MONOACYLGLYCEROPHOSPHOMONORADYLGLYCEROLS,
        LipidFragmentationRuleType.TWO_ACYLCHAINS_PLUS_FORMULA_FRAGMENT, "C3H3");
    assertScoreOrderMatchesWeight(pgScore, bmpScore, pgWeight, bmpWeight, "two-acyl");
  }

  private static @NotNull MatchedLipid createMatchedLipid(final @NotNull LipidClasses lipidClass,
      final double twoAcylIntensity, final double acylIntensity) {
    final ILipidAnnotation lipidAnnotation = LIPID_FACTORY.buildSpeciesLevelLipid(lipidClass, 40, 5,
        0);
    Assertions.assertNotNull(lipidAnnotation);
    final int twoAcylWeight = findRelativeIntensityWeight(lipidClass,
        LipidFragmentationRuleType.TWO_ACYLCHAINS_PLUS_FORMULA_FRAGMENT, "C3H3");
    final int acylWeight = findRelativeIntensityWeight(lipidClass,
        LipidFragmentationRuleType.ACYLCHAIN_PLUS_FORMULA_FRAGMENT, "C3H5O");
    final Set<LipidFragment> fragments = Set.of(
        createFragment(LipidFragmentationRuleType.TWO_ACYLCHAINS_PLUS_FORMULA_FRAGMENT,
            twoAcylIntensity, lipidClass, twoAcylWeight, "C3H3"),
        createFragment(LipidFragmentationRuleType.ACYLCHAIN_PLUS_FORMULA_FRAGMENT, acylIntensity,
            lipidClass, acylWeight, "C3H5O"));
    return new MatchedLipid(lipidAnnotation, 0d, IonizationType.AMMONIUM, fragments, 0.5d);
  }

  private static @NotNull MatchedLipid createMatchedLipidWithSingleFragment(
      final @NotNull LipidClasses lipidClass, final @NotNull LipidFragmentationRuleType ruleType,
      final @NotNull String ruleFormula, final double intensity) {
    final ILipidAnnotation lipidAnnotation = LIPID_FACTORY.buildSpeciesLevelLipid(lipidClass, 40, 5,
        0);
    Assertions.assertNotNull(lipidAnnotation);
    final int relativeIntensityWeight = findRelativeIntensityWeight(lipidClass, ruleType,
        ruleFormula);
    final LipidFragment fragment = createFragment(ruleType, intensity, lipidClass,
        relativeIntensityWeight, ruleFormula);
    return new MatchedLipid(lipidAnnotation, 0d, IonizationType.AMMONIUM, Set.of(fragment), 0.5d);
  }

  private static @NotNull LipidFragment createFragment(
      final @NotNull LipidFragmentationRuleType ruleType, final double intensity,
      final @NotNull LipidClasses lipidClass, final int relativeIntensityWeight,
      final @NotNull String ruleFormula) {
    return new LipidFragment(ruleType, LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL,
        LipidFragmentationRuleRating.MAJOR, 100d, "C", new SimpleDataPoint(100d, intensity),
        lipidClass, null, null, null, null, null, relativeIntensityWeight, ruleFormula);
  }

  private static int findRelativeIntensityWeight(final @NotNull LipidClasses lipidClass,
      final @NotNull LipidFragmentationRuleType ruleType, final @NotNull String molecularFormula) {
    return Arrays.stream(lipidClass.getFragmentationRules())
        .filter(rule -> rule.getPolarityType() == PolarityType.POSITIVE)
        .filter(rule -> rule.getIonizationType() == IonizationType.AMMONIUM)
        .filter(rule -> rule.getLipidFragmentationRuleType() == ruleType)
        .filter(rule -> molecularFormula.equals(rule.getMolecularFormula()))
        .mapToInt(LipidFragmentationRule::getRelativeIntensityWeight).findFirst().orElse(0);
  }

  private static double expectedTwoAcylRatio(final @NotNull LipidClasses lipidClass) {
    final int twoAcylWeight = findRelativeIntensityWeight(lipidClass,
        LipidFragmentationRuleType.TWO_ACYLCHAINS_PLUS_FORMULA_FRAGMENT, "C3H3");
    final int acylWeight = findRelativeIntensityWeight(lipidClass,
        LipidFragmentationRuleType.ACYLCHAIN_PLUS_FORMULA_FRAGMENT, "C3H5O");
    final int sum = twoAcylWeight + acylWeight;
    if (sum <= 0) {
      return 0d;
    }
    return (double) twoAcylWeight / sum;
  }

  private static void assertScoreOrderMatchesWeight(final double pgScore, final double bmpScore,
      final int pgWeight, final int bmpWeight, final @NotNull String fragmentLabel) {
    if (pgWeight > bmpWeight) {
      Assertions.assertTrue(pgScore > bmpScore,
          () -> "Expected higher PG score for " + fragmentLabel + " fragment weight (" + pgWeight
              + " > " + bmpWeight + "), but got PG=" + pgScore + ", BMP=" + bmpScore);
    } else if (bmpWeight > pgWeight) {
      Assertions.assertTrue(bmpScore > pgScore,
          () -> "Expected higher BMP score for " + fragmentLabel + " fragment weight (" + bmpWeight
              + " > " + pgWeight + "), but got BMP=" + bmpScore + ", PG=" + pgScore);
    } else {
      Assertions.assertEquals(pgScore, bmpScore, 1e-9,
          "Expected equal scores for equal " + fragmentLabel + " fragment weights.");
    }
  }
}
