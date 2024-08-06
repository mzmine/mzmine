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

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreCalculator;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreParameters;
import io.github.mzmine.modules.tools.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.submodules.ValueWithParameters;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import io.github.mzmine.util.exceptions.ExceptionUtils;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.interfaces.IMolecularFormula;

/**
 * @deprecated because of old API usage. Hard to maintain. This was removed from the interfaces and
 * is only here as reference point
 */
@Deprecated
public class PeakListIdentificationTask extends AbstractTask {

  // Logger.
  private static final Logger logger = Logger.getLogger(PeakListIdentificationTask.class.getName());

  // Minimum abundance.
  private static final double MIN_ABUNDANCE = 0.001;
  private final Double minIsotopeScore;
  private final Double isotopeNoiseLevel;
  private final MZTolerance isotopeMZTolerance;
  private final MZTolerance mzTolerance;
  private final int numOfResults;
  private final FeatureList peakList;
  private final boolean isotopeFilter;
  private final IonizationType ionType;
  private final ParameterSet parameters;
  private final ValueWithParameters<OnlineDatabases> db;
  // Counters.
  private int finishedItems;
  private int numItems;
  private DBGateway gateway;
  private FeatureListRow currentRow;

  /**
   * Create the identification task.
   *
   * @param parameters task parameters.
   * @param list       feature list to operate on.
   */
  PeakListIdentificationTask(final ParameterSet parameters, final FeatureList list,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null

    peakList = list;
    numItems = 0;
    finishedItems = 0;
    gateway = null;
    currentRow = null;

    db = parameters.getParameter(SingleRowIdentificationParameters.DATABASE)
        .getValueWithParameters();
    mzTolerance = parameters.getParameter(SingleRowIdentificationParameters.MZ_TOLERANCE)
        .getValue();
    numOfResults = parameters.getParameter(SingleRowIdentificationParameters.MAX_RESULTS)
        .getValue();
    isotopeFilter = parameters.getParameter(SingleRowIdentificationParameters.ISOTOPE_FILTER)
        .getValue();
    ionType = parameters.getParameter(PeakListIdentificationParameters.ionizationType).getValue();
    this.parameters = parameters;

    final ParameterSet isoParam = parameters.getParameter(
        SingleRowIdentificationParameters.ISOTOPE_FILTER).getEmbeddedParameters();

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

  @Override
  public double getFinishedPercentage() {

    return numItems == 0 ? 0.0 : (double) finishedItems / (double) numItems;
  }

  @Override
  public String getTaskDescription() {

    return "Identification of peaks in " + peakList + (currentRow == null ? " using " + db
        : " (" + MZmineCore.getConfiguration().getMZFormat().format(currentRow.getAverageMZ())
          + " m/z) using " + db);
  }

  @Override
  public void run() {

    if (!isCanceled()) {
      try {

        setStatus(TaskStatus.PROCESSING);

        // Create database gateway.
        gateway = db.value().getGatewayClass().getDeclaredConstructor().newInstance();

        // Identify the feature list rows starting from the biggest
        // peaks.
        final FeatureListRow[] rows = peakList.getRows().toArray(FeatureListRow[]::new);
        Arrays.sort(rows,
            new FeatureListRowSorter(SortingProperty.Area, SortingDirection.Descending));

        // Initialize counters.
        numItems = rows.length;

        // Process rows.
        for (finishedItems = 0; !isCanceled() && finishedItems < numItems; finishedItems++) {

          // Retrieve results for each row.
          retrieveIdentification(rows[finishedItems]);
        }

        if (!isCanceled()) {
          setStatus(TaskStatus.FINISHED);
        }
      } catch (Throwable t) {

        final String msg = "Could not search " + db;
        logger.log(Level.WARNING, msg, t);
        setStatus(TaskStatus.ERROR);
        setErrorMessage(msg + ": " + ExceptionUtils.exceptionToString(t));
      }
    }

    peakList.getAppliedMethods().add(
        new SimpleFeatureListAppliedMethod(OnlineDBSearchModule.class, parameters,
            getModuleCallDate()));
  }

  /**
   * Search the database for the peak's identity.
   *
   * @param row the feature list row.
   * @throws IOException if there are i/o problems.
   */
  private void retrieveIdentification(final FeatureListRow row) throws IOException {

    currentRow = row;

    // Determine peak charge.
    final Feature bestPeak = row.getBestFeature();
    int charge = bestPeak.getCharge();
    if (charge <= 0) {
      charge = 1;
    }

    // Calculate mass value.

    final double massValue = row.getAverageMZ() * charge - ionType.getAddedMass();

    // Isotope pattern.
    final IsotopePattern rowIsotopePattern = bestPeak.getIsotopePattern();

    // Process each one of the result ID's.
    final String[] findCompounds = gateway.findCompounds(massValue, mzTolerance, numOfResults,
        db.parameters());

    for (int i = 0; !isCanceled() && i < findCompounds.length; i++) {

      final CompoundDBAnnotation compound = gateway.getCompound(findCompounds[i], db.parameters());

      // In case we failed to retrieve data, skip this compound
      if (compound == null) {
        continue;
      }

      final String formula = compound.getFormula();

      // If required, check isotope score.
      if (isotopeFilter && rowIsotopePattern != null && formula != null) {

        // First modify the formula according to ionization.
        final IMolecularFormula ionizedFormula = ionType.ionizeFormula(formula);

        logger.finest(
            "Calculating isotope pattern for compound formula " + formula + " adjusted to "
            + ionizedFormula);

        // Generate IsotopePattern for this compound
        final IsotopePattern compoundIsotopePattern = IsotopePatternCalculator.calculateIsotopePattern(
            ionizedFormula, MIN_ABUNDANCE, charge, ionType.getPolarity());

        // Check isotope pattern match
        boolean check = IsotopePatternScoreCalculator.checkMatch(rowIsotopePattern,
            compoundIsotopePattern, isotopeMZTolerance, isotopeNoiseLevel, minIsotopeScore);

        if (!check) {
          continue;
        }
      }

      // Add the retrieved identity to the feature list row
      row.addCompoundAnnotation(compound);

    }
  }
}
