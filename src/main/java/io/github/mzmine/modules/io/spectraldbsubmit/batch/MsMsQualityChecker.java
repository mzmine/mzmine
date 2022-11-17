/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.io.spectraldbsubmit.batch;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.modules.tools.msmsscore.MSMSScore;
import io.github.mzmine.modules.tools.msmsscore.MSMSScoreCalculator;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.scans.ScanUtils;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public record MsMsQualityChecker(Integer minNumSignals, Double minExplainedSignals,
                                 Double minExplainedIntensity, MZTolerance msmsFormulaTolerance,
                                 boolean exportExplainedSignalsOnly,
                                 boolean exportFlistNameMatchOnly) {

  @NotNull
  private static IMolecularFormula getIonizedFormula(final FeatureAnnotation annotation,
      final String formula) {
    final IMolecularFormula molecularFormula = MolecularFormulaManipulator.getMajorIsotopeMolecularFormula(
        formula, SilentChemObjectBuilder.getInstance());

    try {
      FormulaUtils.replaceAllIsotopesWithoutExactMass(molecularFormula);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    final IonType adductType = annotation.getAdductType();
    if (adductType.getCDKFormula() != null) {
      molecularFormula.add(adductType.getCDKFormula());
    }
    return molecularFormula;
  }

  /**
   * Checks if feature list contains the name from {@link FeatureAnnotation#getCompoundName()}
   *
   * @param annotation name provider
   * @param flist      feature list name
   * @return true if annotation name is contained in feature list
   */
  public boolean matchesName(final FeatureAnnotation annotation, FeatureList flist) {
    return !exportFlistNameMatchOnly || (annotation.getCompoundName() != null && flist.getName()
        .contains(annotation.getCompoundName()));
  }

  /**
   * @param msmsScan   The msms scan to evaluate
   * @param annotation The annotation to base the evaluation on
   * @return MSMSScore or constants for failed or limited success. FAILED_FILTERS (failed).
   * SUCCESS_WITHOUT_FORMULA describes the case where all filters that do not rely on the formula
   * were successful
   */
  public @NotNull MSMSScore match(final Scan msmsScan, final FeatureAnnotation annotation) {

    if (minNumSignals != null && msmsScan.getNumberOfDataPoints() < minNumSignals) {
      return MSMSScore.FAILED_FILTERS;
    }

    final String formula = annotation != null ? annotation.getFormula() : null;
    if (formula == null) {
      return MSMSScore.SUCCESS_WITHOUT_FORMULA;
    }

    final IMolecularFormula molecularFormula = getIonizedFormula(annotation, formula);

    final DataPoint[] dataPoints = ScanUtils.extractDataPoints(msmsScan);

    Double precursorMz = msmsScan.getPrecursorMz();
    if (precursorMz == null) {
      precursorMz = annotation.getPrecursorMZ();
    }
    if (precursorMz == null) {
      return MSMSScore.SUCCESS_WITHOUT_PRECURSOR_MZ;
    }

    int precursorCharge = Objects.requireNonNullElse(msmsScan.getPrecursorCharge(), 1);
    MSMSScore score = MSMSScoreCalculator.evaluateMSMS(msmsFormulaTolerance, molecularFormula,
        dataPoints, precursorMz, precursorCharge, dataPoints.length);

    if (minExplainedIntensity != null) {
      if (score == null || score.explainedIntensity() < minExplainedIntensity) {
        return MSMSScore.FAILED_FILTERS;
      }
    }

    if (minExplainedSignals != null) {
      if (score == null || score.explainedSignals() < minExplainedSignals) {
        return MSMSScore.FAILED_FILTERS;
      }
    }

    // double check if we still match the minimum peaks if we export explained only
    if (exportExplainedSignalsOnly) {
      List<Set<DataPoint>> explainedSignals = List.of(score.annotation().keySet());
      if (minNumSignals != null && explainedSignals.size() < minNumSignals) {
        return MSMSScore.FAILED_FILTERS;
      } else {
        return score;
      }
    }

    return score;
  }
}
