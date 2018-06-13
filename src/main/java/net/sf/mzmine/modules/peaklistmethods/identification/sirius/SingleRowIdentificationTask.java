/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.peaklistmethods.identification.sirius;

import static net.sf.mzmine.modules.peaklistmethods.identification.sirius.SiriusParameters.ELEMENTS;
import static net.sf.mzmine.modules.peaklistmethods.identification.sirius.SiriusParameters.MZ_TOLERANCE;
import static net.sf.mzmine.modules.peaklistmethods.identification.sirius.SiriusParameters.ISOTOPE_FILTER;
import static net.sf.mzmine.modules.peaklistmethods.identification.sirius.SiriusParameters.MAX_RESULTS;
import static net.sf.mzmine.modules.peaklistmethods.identification.sirius.SiriusParameters.PARENT_MASS;
import static net.sf.mzmine.modules.peaklistmethods.identification.sirius.SiriusParameters.NEUTRAL_MASS;

import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineProcessingStep;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepatternscore.IsotopePatternScoreCalculator;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepatternscore.IsotopePatternScoreParameters;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopeprediction.IsotopePatternCalculator;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.ExceptionUtils;
import net.sf.mzmine.util.FormulaUtils;
import org.openscience.cdk.formula.MolecularFormulaRange;

public class SingleRowIdentificationTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  public static final NumberFormat massFormater = MZmineCore.getConfiguration().getMZFormat();

  private int finishedItems = 0, numItems;

  private MZmineProcessingStep<OnlineDatabase> db;
  private double searchedMass;
  private MZTolerance mzTolerance;
  private int charge;
  private int numOfResults;
  private PeakListRow peakListRow;
  private IonizationType ionType;
  private boolean isotopeFilter = false;
  private ParameterSet isotopeFilterParameters;
  private DBGateway gateway;
  private MolecularFormulaRange formulaRange;
  private Double parentMass;

  /**
   * Create the task.
   * 
   * @param parameters task parameters.
   * @param peakListRow peak-list row to identify.
   */
  public SingleRowIdentificationTask(ParameterSet parameters, PeakListRow peakListRow) {

    this.peakListRow = peakListRow;

    db = null;

    try {
      gateway = db.getModule().getGatewayClass().newInstance();
    } catch (Exception e) {
      e.printStackTrace();
    }

    searchedMass = parameters.getParameter(NEUTRAL_MASS).getValue();
    mzTolerance = parameters.getParameter(MZ_TOLERANCE).getValue();
    numOfResults = parameters.getParameter(MAX_RESULTS).getValue();

    ionType = parameters.getParameter(NEUTRAL_MASS).getIonType();
    charge = parameters.getParameter(NEUTRAL_MASS).getCharge();

    isotopeFilter = parameters.getParameter(ISOTOPE_FILTER).getValue();
    isotopeFilterParameters = parameters.getParameter(ISOTOPE_FILTER).getEmbeddedParameters();
    formulaRange = parameters.getParameter(ELEMENTS).getValue();
    parentMass = parameters.getParameter(PARENT_MASS).getValue();

  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  public double getFinishedPercentage() {
    if (numItems == 0)
      return 0;
    return ((double) finishedItems) / numItems;
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
   */
  public String getTaskDescription() {
    return "Peak identification of " + massFormater.format(searchedMass) + " using " + db;
  }

  /**
   * @see Runnable#run()
   */
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    NumberFormat massFormater = MZmineCore.getConfiguration().getMZFormat();

    ResultWindow window = new ResultWindow(peakListRow, searchedMass, this);
    window.setTitle("Searching for " + massFormater.format(searchedMass) + " amu");
    window.setVisible(true);

    IsotopePattern detectedPattern = peakListRow.getBestIsotopePattern();
    if ((isotopeFilter) && (detectedPattern == null)) {
      final String msg = "Cannot calculate isotope pattern scores, because selected"
          + " peak does not have any isotopes. Have you run the isotope peak grouper?";
      MZmineCore.getDesktop().displayMessage(window, msg);
    }

    try {
      String compoundIDs[] =
          gateway.findCompounds(searchedMass, mzTolerance, numOfResults, db.getParameterSet());

      // Get the number of results
      numItems = compoundIDs.length;

      if (numItems == 0) {
        window.setTitle(
            "Searching for " + massFormater.format(searchedMass) + " amu: no results found");
      }

      // Process each one of the result ID's.
      for (int i = 0; i < numItems; i++) {

        if (getStatus() != TaskStatus.PROCESSING) {
          return;
        }

        DBCompound compound = gateway.getCompound(compoundIDs[i], db.getParameterSet());

        // In case we failed to retrieve data, skip this compound
        if (compound == null)
          continue;

        String formula = compound.getPropertyValue(PeakIdentity.PROPERTY_FORMULA);

        if (formula != null) {

          // First modify the formula according to the ionization
          String adjustedFormula = FormulaUtils.ionizeFormula(formula, ionType, charge);

          logger.finest("Calculating isotope pattern for compound formula " + formula
              + " adjusted to " + adjustedFormula);

          // Generate IsotopePattern for this compound
          IsotopePattern compoundIsotopePattern = IsotopePatternCalculator
              .calculateIsotopePattern(adjustedFormula, 0.001, charge, ionType.getPolarity());

          compound.setIsotopePattern(compoundIsotopePattern);

          IsotopePattern rawDataIsotopePattern = peakListRow.getBestIsotopePattern();

          // If required, check isotope score
          if (isotopeFilter && (rawDataIsotopePattern != null)
              && (compoundIsotopePattern != null)) {

            double score = IsotopePatternScoreCalculator.getSimilarityScore(rawDataIsotopePattern,
                compoundIsotopePattern, isotopeFilterParameters);
            compound.setIsotopePatternScore(score);

            double minimumScore = isotopeFilterParameters
                .getParameter(IsotopePatternScoreParameters.isotopePatternScoreThreshold)
                .getValue();

            if (score < minimumScore) {
              finishedItems++;
              continue;
            }
          }

        }

        // Add compound to the list of possible candidate and
        // display it in window of results.
        window.addNewListItem(compound);

        // Update window title
        window.setTitle("Searching for " + massFormater.format(searchedMass) + " amu (" + (i + 1)
            + "/" + numItems + ")");

        finishedItems++;

      }

    } catch (Exception e) {
      e.printStackTrace();
      logger.log(Level.WARNING, "Could not connect to " + db, e);
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Could not connect to " + db + ": " + ExceptionUtils.exceptionToString(e));
      return;
    }

    setStatus(TaskStatus.FINISHED);

  }

}
