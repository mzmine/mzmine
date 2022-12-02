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
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.modules.tools.msmsscore.MSMSIntensityScoreCalculator;
import io.github.mzmine.modules.tools.msmsscore.MSMSScore;
import io.github.mzmine.modules.tools.msmsscore.MSMSScoreCalculator;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.scans.ScanUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public record MsMsQualityChecker(Integer minNumSignals, Double minExplainedSignals,
                                 Double minExplainedIntensity, MZTolerance msmsFormulaTolerance,
                                 boolean exportExplainedSignalsOnly,
                                 boolean exportFlistNameMatchOnly) {

  /**
   * @param msmsScan   The msms scan to evaluate
   * @param annotation The annotation to base the evaluation on
   * @return The list of explained signals. Null if this spectrum did not match the quality
   * parameters. Empty list if formula parameters are disabled but the number of signals matched the
   * requirements.
   */
  public List<DataPoint> matchAndGetExplainedSignals(final Scan msmsScan,
      final FeatureAnnotation annotation, FeatureListRow f) {

    if (minNumSignals != null && msmsScan.getNumberOfDataPoints() < minNumSignals) {
      return null;
    }

    final String formula = annotation != null ? annotation.getFormula() : null;
    if (formula == null || annotation.getCompoundName() == null) {
      return null;
    }

    if (exportFlistNameMatchOnly && !f.getFeatureList().getName()
        // annotations may have unsafe characters, flists not
        .contains(FileAndPathUtil.safePathEncode(annotation.getCompoundName()))) {
      return null;
    }

    final IMolecularFormula molecularFormula = MolecularFormulaManipulator.getMajorIsotopeMolecularFormula(
        formula, SilentChemObjectBuilder.getInstance());
    final List<DataPoint> explainedSignals = new ArrayList<>();

    try {
      FormulaUtils.replaceAllIsotopesWithoutExactMass(molecularFormula);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    final IonType adductType = annotation.getAdductType();
    if (adductType.getCDKFormula() != null) {
      molecularFormula.add(adductType.getCDKFormula());
    }

    final DataPoint[] dataPoints = ScanUtils.extractDataPoints(msmsScan);

    if (minExplainedIntensity != null) {
      MSMSScore intensityFormulaScore = MSMSIntensityScoreCalculator.evaluateMSMS(
          msmsFormulaTolerance, molecularFormula, dataPoints, msmsScan.getPrecursorMz(),
          msmsScan.getPrecursorCharge(), dataPoints.length);
      if (intensityFormulaScore == null
          || intensityFormulaScore.getScore() < minExplainedIntensity.floatValue()) {
        return null;
      }
      explainedSignals.addAll(intensityFormulaScore.getAnnotation().keySet());
    }

    if (minExplainedSignals != null) {
      MSMSScore peakFormulaScore = MSMSScoreCalculator.evaluateMSMS(msmsFormulaTolerance,
          molecularFormula, dataPoints, msmsScan.getPrecursorMz(), msmsScan.getPrecursorCharge(),
          dataPoints.length);
      if (peakFormulaScore == null
          || peakFormulaScore.getScore() < minExplainedSignals.floatValue()) {
        return null;
      }
      explainedSignals.clear(); // clear if we have previous annotations, they are the same
      explainedSignals.addAll(peakFormulaScore.getAnnotation().keySet());
    }

    // double check if we still match the minimum peaks if we export explained only
    if (minNumSignals != null && exportExplainedSignalsOnly
        && explainedSignals.size() < minNumSignals) {
      return null;
    }

    return explainedSignals;
  }
}
