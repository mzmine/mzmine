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

package io.github.mzmine.modules.dataprocessing.id_onlinecompounddb;

import static io.github.mzmine.modules.dataprocessing.id_onlinecompounddb.SingleRowIdentificationParameters.DATABASE;
import static io.github.mzmine.modules.dataprocessing.id_onlinecompounddb.SingleRowIdentificationParameters.ISOTOPE_FILTER;
import static io.github.mzmine.modules.dataprocessing.id_onlinecompounddb.SingleRowIdentificationParameters.MAX_RESULTS;
import static io.github.mzmine.modules.dataprocessing.id_onlinecompounddb.SingleRowIdentificationParameters.MZ_TOLERANCE;
import static io.github.mzmine.modules.dataprocessing.id_onlinecompounddb.SingleRowIdentificationParameters.NEUTRAL_MASS;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.IsotopePatternType;
import io.github.mzmine.datamodel.features.types.numbers.scores.IsotopePatternScoreType;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreCalculator;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreParameters;
import io.github.mzmine.modules.tools.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.submodules.ValueWithParameters;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.exceptions.ExceptionUtils;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

/**
 * @deprecated because of old API usage. Hard to maintain. This was removed from the interfaces and
 * is only here as reference point
 */
@Deprecated
public class SingleRowIdentificationTask extends AbstractTask {

  public static final NumberFormat massFormater = MZmineCore.getConfiguration().getMZFormat();
  private static final Logger logger = Logger.getLogger(
      SingleRowIdentificationTask.class.getName());
  private final Double minIsotopeScore;
  private final Double isotopeNoiseLevel;
  private final MZTolerance isotopeMZTolerance;
  private final ValueWithParameters<OnlineDatabases> db;
  private final double searchedMass;
  private final MZTolerance mzTolerance;
  private final int charge;
  private final int numOfResults;
  private final FeatureListRow peakListRow;
  private final IonizationType ionType;
  private final boolean isotopeFilter;
  private int finishedItems = 0, numItems;
  private DBGateway gateway;
  private ResultWindowFX resultWindowFX;

  /**
   * Create the task.
   *
   * @param parameters  task parameters.
   * @param peakListRow peak-list row to identify.
   */
  public SingleRowIdentificationTask(ParameterSet parameters, FeatureListRow peakListRow,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null

    this.peakListRow = peakListRow;

    db = parameters.getParameter(DATABASE).getValueWithParameters();

    try {
      gateway = db.value().getGatewayClass().newInstance();
    } catch (Exception e) {
      e.printStackTrace();
    }

    searchedMass = parameters.getParameter(NEUTRAL_MASS).getValue();
    mzTolerance = parameters.getParameter(MZ_TOLERANCE).getValue();
    numOfResults = parameters.getParameter(MAX_RESULTS).getValue();

    ionType = parameters.getParameter(NEUTRAL_MASS).getIonType();
    charge = parameters.getParameter(NEUTRAL_MASS).getCharge();

    isotopeFilter = parameters.getParameter(ISOTOPE_FILTER).getValue();
    final ParameterSet isoParam = parameters.getParameter(ISOTOPE_FILTER).getEmbeddedParameters();

    if (isotopeFilter) {
      minIsotopeScore = isoParam.getValue(
          IsotopePatternScoreParameters.isotopePatternScoreThreshold);
      isotopeNoiseLevel = isoParam.getValue(IsotopePatternScoreParameters.isotopeNoiseLevel);
      isotopeMZTolerance = isoParam.getValue(IsotopePatternScoreParameters.mzTolerance);
    } else {
      minIsotopeScore = null;
      isotopeNoiseLevel = null;
      isotopeMZTolerance = null;
    }
  }

  public double getFinishedPercentage() {
    if (numItems == 0) {
      return 0;
    }
    return ((double) finishedItems) / numItems;
  }

  public String getTaskDescription() {
    return "Peak identification of " + massFormater.format(searchedMass) + " using " + db;
  }

  public void run() {

    setStatus(TaskStatus.PROCESSING);

    Platform.runLater(() -> {
      resultWindowFX = new ResultWindowFX(peakListRow, searchedMass, this);
      resultWindowFX.show();
      //close button handle
      resultWindowFX.getScene().getWindow().setOnCloseRequest(e -> {
        if (getStatus() == TaskStatus.WAITING || getStatus() == TaskStatus.PROCESSING) {
          cancel();
        }
      });
    });

    NumberFormat massFormatter = MZmineCore.getConfiguration().getMZFormat();

    IsotopePattern detectedPattern = peakListRow.getBestIsotopePattern();
    if ((isotopeFilter) && (detectedPattern == null)) {
      final String msg = "Cannot calculate isotope pattern scores, because selected"
                         + " peak does not have any isotopes. Have you run the isotope peak grouper?";
      MZmineCore.getDesktop().displayMessage(null, msg);
    }

    try {
      String[] compoundIDs = gateway.findCompounds(searchedMass, mzTolerance, numOfResults,
          db.parameters());

      // Get the number of results
      numItems = compoundIDs.length;

      if (numItems == 0) {
        //
        FxThread.runLater(() -> resultWindowFX.setTitle(
            "Searching for " + massFormatter.format(searchedMass) + " amu: no results found"));
      }

      // Process each one of the result ID's.
      for (int i = 0; i < numItems; i++) {

        if (getStatus() != TaskStatus.PROCESSING) {
          return;
        }

        CompoundDBAnnotation compound = gateway.getCompound(compoundIDs[i], db.parameters());

        // In case we failed to retrieve data, skip this compound
        if (compound == null) {
          continue;
        }

        String formula = compound.getFormula();

        if (formula != null) {

          // First modify the formula according to the ionization
          final IMolecularFormula ionizedFormula = ionType.ionizeFormula(formula);

          logger.finest(
              "Calculating isotope pattern for compound formula " + formula + " adjusted to "
              + MolecularFormulaManipulator.getString(ionizedFormula));

          // Generate IsotopePattern for this compound
          IsotopePattern compoundIsotopePattern = IsotopePatternCalculator.calculateIsotopePattern(
              ionizedFormula, 0.001, charge, ionType.getPolarity());

          compound.put(IsotopePatternType.class, compoundIsotopePattern);

          IsotopePattern rawDataIsotopePattern = peakListRow.getBestIsotopePattern();

          // If required, check isotope score
          if (isotopeFilter && (rawDataIsotopePattern != null) && (compoundIsotopePattern
                                                                   != null)) {

            double score = IsotopePatternScoreCalculator.getSimilarityScore(rawDataIsotopePattern,
                compoundIsotopePattern, isotopeMZTolerance, isotopeNoiseLevel);
            compound.put(IsotopePatternScoreType.class, (float) score);

            if (score < minIsotopeScore) {
              finishedItems++;
              continue;
            }
          }

        }

        // Add compound to the list of possible candidate and
        // display it in window of results.

        int finalI = i;
        Platform.runLater(() -> {
          resultWindowFX.addNewListItem(compound);
          // Update window title

          resultWindowFX.setTitle(
              "Searching for " + massFormatter.format(searchedMass) + " amu (" + (finalI + 1) + "/"
              + numItems + ")");
        });

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
