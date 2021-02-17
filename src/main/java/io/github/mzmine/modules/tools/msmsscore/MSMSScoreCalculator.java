/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.tools.msmsscore;

import java.util.Hashtable;
import java.util.Map;
import org.openscience.cdk.formula.MolecularFormulaGenerator;
import org.openscience.cdk.formula.MolecularFormulaRange;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;

public class MSMSScoreCalculator {

  /**
   * Returns a calculated similarity score of
   */
  public static MSMSScore evaluateMSMS(IMolecularFormula parentFormula, Scan msmsScan,
      ParameterSet parameters) {

    MZTolerance msmsTolerance =
        parameters.getParameter(MSMSScoreParameters.msmsTolerance).getValue();

    MassList massList = msmsScan.getMassList();

    if (massList == null) {
      throw new IllegalArgumentException("Scan #" + msmsScan.getScanNumber()
          + " does not have a mass list");
    }

    DataPoint msmsIons[] = massList.getDataPoints();

    if (msmsIons == null) {
      throw new IllegalArgumentException(
          "Mass list " + massList + " does not contain data for scan #" + msmsScan.getScanNumber());
    }

    MolecularFormulaRange msmsElementRange = new MolecularFormulaRange();
    for (IIsotope isotope : parentFormula.isotopes()) {
      msmsElementRange.addIsotope(isotope, 0, parentFormula.getIsotopeCount(isotope));
    }

    int totalMSMSpeaks = 0, interpretedMSMSpeaks = 0;
    Map<Integer, String> msmsAnnotations = new Hashtable<>();

    int dpIndex = 0;
    msmsCycle: for (DataPoint dp : msmsIons) {

      // Check if this is an isotope
      Range<Double> isotopeCheckRange = Range.closed(dp.getMZ() - 1.4, dp.getMZ() - 0.6);
      for (DataPoint dpCheck : msmsIons) {
        // If we have any MS/MS peak with 1 neutron mass smaller m/z
        // and higher intensity, it means the current peak is an
        // isotope and we should ignore it
        if (isotopeCheckRange.contains(dpCheck.getMZ())
            && (dpCheck.getIntensity() > dp.getIntensity())) {
          continue msmsCycle;
        }
      }

      // If getPrecursorCharge() returns 0, it means charge is unknown. In
      // that case let's assume charge 1
      int precursorCharge = msmsScan.getPrecursorCharge();
      if (precursorCharge == 0)
        precursorCharge = 1;

      // We don't know the charge of the fragment, so we will simply
      // assume 1
      double neutralLoss = msmsScan.getPrecursorMZ() * precursorCharge - dp.getMZ();

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
        msmsAnnotations.put(dpIndex, formulaString);
        interpretedMSMSpeaks++;
      }

      totalMSMSpeaks++;
      dpIndex++;

    }

    // If we did not evaluate any MS/MS peaks, we cannot calculate a score
    if (totalMSMSpeaks == 0) {
      return null;
    }

    double msmsScore = (double) interpretedMSMSpeaks / totalMSMSpeaks;

    MSMSScore result = new MSMSScore(msmsScore, msmsAnnotations);

    return result;

  }

}
