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

package io.github.mzmine.modules.tools.msmsscore;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.scans.ScanUtils;
import java.util.Hashtable;
import java.util.Map;
import org.openscience.cdk.formula.MolecularFormulaGenerator;
import org.openscience.cdk.formula.MolecularFormulaRange;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class MSMSScoreCalculator {

  /**
   * Returns a calculated similarity score of
   */
  public static MSMSScore evaluateMSMS(IMolecularFormula parentFormula, Scan msmsScan,
      MZTolerance msmsTolerance, int topNSignals) {

    MassList massList = msmsScan.getMassList();

    if (massList == null) {
      throw new IllegalArgumentException(
          "Scan #" + msmsScan.getScanNumber() + " does not have a mass list");
    }

    DataPoint[] msmsIons = massList.getDataPoints();

    if (msmsIons == null) {
      throw new IllegalArgumentException(
          "Mass list " + massList + " does not contain data for scan #" + msmsScan.getScanNumber());
    }

    double precursorMZ = msmsScan.getPrecursorMz() != null ? msmsScan.getPrecursorMz() : 0d;
    int precursorCharge = msmsScan.getPrecursorCharge() != null ? msmsScan.getPrecursorCharge() : 0;
    return evaluateMSMS(msmsTolerance, parentFormula, msmsIons, precursorMZ, precursorCharge,
        topNSignals);
  }


  /**
   * @param parentFormula
   * @param msmsIons
   * @param precursorCharge
   * @param maxSignals      if > 0; only use top n signals
   * @return
   */
  public static MSMSScore evaluateMSMS(MZTolerance msmsTolerance, IMolecularFormula parentFormula,
      DataPoint[] msmsIons, double precursorMZ, int precursorCharge, int maxSignals) {
    if (maxSignals <= 0) {
      return evaluateMSMS(msmsTolerance, parentFormula, msmsIons, precursorMZ, precursorCharge);
    } else {
      DataPoint[] dps = ScanUtils.getMostAbundantSignals(msmsIons, maxSignals);
      return evaluateMSMS(msmsTolerance, parentFormula, dps, precursorMZ, precursorCharge);
    }
  }

  public static MSMSScore evaluateMSMS(MZTolerance msmsTolerance, IMolecularFormula parentFormula,
      DataPoint[] msmsIons, double precursorMZ, int precursorCharge) {
    MolecularFormulaRange msmsElementRange = new MolecularFormulaRange();
    for (IIsotope isotope : parentFormula.isotopes()) {
      msmsElementRange.addIsotope(isotope, 0, parentFormula.getIsotopeCount(isotope));
    }

    int totalMSMSpeaks = 0, interpretedMSMSpeaks = 0;
    Map<DataPoint, String> msmsAnnotations = new Hashtable<>();

    // If getPrecursorCharge() returns 0, it means charge is unknown. In
    // that case let's assume charge 1
    if (precursorCharge == 0) {
      precursorCharge = 1;
    }

    msmsCycle:
    for (DataPoint dp : msmsIons) {

      // Check if this is an isotope
      Range<Double> isotopeCheckRange = Range.closed(dp.getMZ() - 1.4, dp.getMZ() - 0.6);
      for (DataPoint dpCheck : msmsIons) {
        // If we have any MS/MS peak with 1 neutron mass smaller m/z
        // and higher intensity, it means the current peak is an
        // isotope and we should ignore it
        if (isotopeCheckRange.contains(dpCheck.getMZ()) && (dpCheck.getIntensity() > dp
            .getIntensity())) {
          continue msmsCycle;
        }
      }

      // We don't know the charge of the fragment, so we will simply
      // assume 1
      double neutralLoss = precursorMZ * precursorCharge - dp.getMZ();

      // Ignore negative neutral losses and parent ion, <5 may be a
      // good threshold
      if (neutralLoss < 5) {
        continue;
      }

      Range<Double> msmsTargetRange = msmsTolerance.getToleranceRange(neutralLoss);
      IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
      MolecularFormulaGenerator msmsEngine = new MolecularFormulaGenerator(builder,
          msmsTargetRange.lowerEndpoint(), msmsTargetRange.upperEndpoint(), msmsElementRange);

      IMolecularFormula formula = msmsEngine.getNextFormula();
      if (formula != null) {
        String formulaString = MolecularFormulaManipulator.getString(formula);
        msmsAnnotations.put(dp, String.format("[M-%s]", formulaString));
        interpretedMSMSpeaks++;
      }
      totalMSMSpeaks++;
    }

    // If we did not evaluate any MS/MS peaks, we cannot calculate a score
    if (totalMSMSpeaks == 0) {
      return null;
    }

    float msmsScore = interpretedMSMSpeaks / (float) totalMSMSpeaks;
    return new MSMSScore(msmsScore, msmsAnnotations);
  }

}
