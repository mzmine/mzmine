/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.isolab_natabundance;

import static io.github.mzmine.modules.dataprocessing.isolab_natabundance.MassResolutionCalculator.calculateResolutionFromDalton;
import static io.github.mzmine.modules.dataprocessing.isolab_natabundance.MassResolutionCalculator.calculateResolutionFromPPM;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.modules.dataprocessing.isolab_natabundance.LowResMetaboliteCorrector.CorrectedResult;
import io.github.mzmine.modules.dataprocessing.isolab_targeted.IsotopeLabelingTargetedTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.annotations.CompoundAnnotationUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 *
 */
class IsotopeNaturalAbundanceTask extends AbstractTask {

  /**
   * Actual weight of 1 neutron is 1.008665 Da, but part of this mass is consumed as binding energy
   * to other protons/neutrons. Actual mass increase of isotopes depends on chemical formula of the
   * molecule. Since we don't know the formula, we can assume the distance to be ~1.0033 Da, with
   * user-defined tolerance.
   */
  private static final Logger logger = Logger.getLogger(
      IsotopeNaturalAbundanceTask.class.getName());
  private final MZmineProject project;
  private final ModularFeatureList featureList;
  // parameter values
  private final String suffix;
  private final MZTolerance mzTolerance;
  private final Double resolution;
  private final Double mzOfResolution;
  private final String resolutionFormulaCode;
  private final MobilityTolerance mobilityTolerance;
  private final Double backgroundValue;
  private final Boolean correct_NA_tracer;
  private final double[] tracerPurity;
  private final String tracerIsotope;
  private final ParameterSet parameters;
  private final OriginalFeatureListOption handleOriginal;
  // peaks counter
  private int processedRows, totalRows;

  /**
   *
   */
  IsotopeNaturalAbundanceTask(MZmineProject project, ModularFeatureList featureList,
      ParameterSet parameters, @Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    this.project = project;
    this.featureList = featureList;
    this.parameters = parameters;

    suffix = parameters.getParameter(IsotopeNaturalAbundanceParameters.suffix).getValue();
    handleOriginal = parameters.getValue(IsotopeNaturalAbundanceParameters.handleOriginal);
    mobilityTolerance = parameters.getParameter(IsotopeNaturalAbundanceParameters.mobilityTolerace)
        .getEmbeddedParameter().getValue();
    mzTolerance = parameters.getParameter(IsotopeNaturalAbundanceParameters.mzTolerance).getValue();
    if (parameters.getParameter(IsotopeNaturalAbundanceParameters.backgroundValue).getValue()) {
      this.backgroundValue = parameters.getParameter(
          IsotopeNaturalAbundanceParameters.backgroundValue).getEmbeddedParameter().getValue();
    } else {
      this.backgroundValue = null;
    }
    correct_NA_tracer = true; // parameters.getParameter(IsotopeNaturalAbundanceParameters.correct_NA_tracer).getValue();
    double purityValue = parameters.getParameter(IsotopeNaturalAbundanceParameters.tracerPurity)
        .getValue();
    tracerPurity = new double[]{1.0 - purityValue, purityValue};
    tracerIsotope = parameters.getParameter(IsotopeNaturalAbundanceParameters.tracerIsotope)
        .getValue();
    resolution = parameters.getParameter(IsotopeNaturalAbundanceParameters.resolution)
        .getEmbeddedParameter().getValue();
    mzOfResolution = parameters.getParameter(IsotopeNaturalAbundanceParameters.mzOfResolution)
        .getEmbeddedParameter().getValue();
    resolutionFormulaCode = parameters.getParameter(
        IsotopeNaturalAbundanceParameters.resolutionFormulaCode).getEmbeddedParameter().getValue();
  }

  @Override
  public String getTaskDescription() {
    return "Isotope natural abundance correction on " + featureList;
  }

  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0) {
      return 0.0f;
    }
    return (double) processedRows / (double) totalRows;
  }

  @Override
  public void run() {
    // Continue with your existing code...
    setStatus(TaskStatus.PROCESSING);
    logger.info("Running isotope natural abundance correction on " + featureList);

    // We assume source peakList contains one datafile
    if (featureList.getRawDataFiles().size() > 1) {
      setErrorMessage(
          "Cannot perform isotope natural abundance correction on aligned feature list.");
      setStatus(TaskStatus.ERROR);
      return;
    }

    // create copy or work on same list
    ModularFeatureList correctedFeatureList = switch (handleOriginal) {
      case KEEP, REMOVE ->
          featureList.createCopy(featureList.getName() + " " + suffix, getMemoryMapStorage(),
              false);
      case PROCESS_IN_PLACE -> featureList;
    };

    // use a second sorted list to limit the number of comparisons
    List<FeatureListRow> rowsFeatureList = new ArrayList<>(correctedFeatureList.getRows());

    // find the unlabeled compounds in the feature list; their names in the "Compound DB" column do not start with "13C"
    final List<FeatureListRow> unlabeledRows = new ArrayList<>();

    for (FeatureListRow row : rowsFeatureList) {
      if (row.getPreferredAnnotationName() == null || !row.getPreferredAnnotationName()
          .startsWith(tracerIsotope)) {
        unlabeledRows.add(row);
      }
    }

    // For each unlabeled compound, find the corresponding labeled compounds in the original feature list. Their names will be "13C" followed by a number and "-" followed by the compound name.
    for (FeatureListRow unlabeledRow : unlabeledRows) {
      final String compoundName = unlabeledRow.getPreferredAnnotationName();
      final List<FeatureListRow> labeledRows = new ArrayList<>();

      for (FeatureListRow row : rowsFeatureList) {
        if (row.getPreferredAnnotationName() != null && row.getPreferredAnnotationName()
            .startsWith(tracerIsotope) && row.getPreferredAnnotationName()
            .contains("-" + compoundName)) {
          labeledRows.add(row);
        }
      }

      double mzValue = unlabeledRow.getAverageMZ();
      double deltaMDalton = mzTolerance.getMzTolerance(); // Absolute tolerance in Dalton
      double ppmTolerance = mzTolerance.getPpmTolerance(); // Tolerance in ppm

      // Calculate resolution using Dalton tolerance
      double resolutionDalton = calculateResolutionFromDalton(mzValue, deltaMDalton);

      // Calculate resolution using ppm tolerance
      double resolutionPPM = calculateResolutionFromPPM(mzValue, ppmTolerance);
      // check how many element atoms are in the unlabeled compound. Each element atom that does not have a measured isotope peak will be replaced by a 0 in the array.
      // get the formula of the compound
      java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([A-Z][a-z]*)");
      java.util.regex.Matcher matcher = pattern.matcher(tracerIsotope);
      if (!matcher.find()) {
        throw new IllegalArgumentException("Invalid tracer format: " + tracerIsotope);
      }
      String tracerElement = matcher.group(1);
      String formula = CompoundAnnotationUtils.streamFeatureAnnotations(unlabeledRow)
          .map(FeatureAnnotation::getFormula).collect(Collectors.joining());
      int elementNumber = IsotopeLabelingTargetedTask.getAtomCount(formula, tracerElement);

      // create an array of measurement for the unlabeled compound and the measurements for the labeled compounds. If a compound does not have a measured isotope peak, the corresponding value in the array will be 0.
      double[] measurements = new double[elementNumber + 1];
      measurements[0] = unlabeledRow.getMaxArea();
      int charge = unlabeledRow.getRowCharge();
      int counter = 1;
      for (FeatureListRow labeledRow : labeledRows) {
        int index = Integer.parseInt(
            labeledRow.getPreferredAnnotationName().split("-")[0].substring(3));
        if (index != counter) {
          for (int i = counter; i < index; i++) {
            if (backgroundValue == null || backgroundValue < 0.0) {
              measurements[i] = 0.0;
            } else {
              measurements[i] = backgroundValue;
            }
          }
        } else {
          if (labeledRow.getMaxArea() == 0.0) {
            measurements[index] = backgroundValue;
          } else {
            measurements[index] = labeledRow.getMaxArea();
          }
        }

        counter++;
      }

      double[] correctedMeasurements;
      // define the options for the corrector
      HashMap options = new HashMap<>();

      // compare the measurement resolution with the calculated dalton and ppm resolution to check which one is the lowest of the three.
      if (resolution != null && mzOfResolution != null) {
        options.put("mzOfResolution", mzOfResolution);
        if (resolution > 0 && mzOfResolution > 0) {
          // compare resolution with the others
          double lowestCalculatedResolution = Math.min(resolutionDalton, resolutionPPM);
          if (resolution < lowestCalculatedResolution) {
            options.put("resolution", resolution);
          } else {
            options.put("resolution", lowestCalculatedResolution);
            // notify the user that the tolerance parameters decreased the resolution below the measurement resolution
            logger.warning(
                "The tolerance parameters decreased the resolution below the measurement resolution. "
                    + "The resolution will be set to " + lowestCalculatedResolution);
          }
        } else {
          // notify the user that the input was invalid and that the fallback low resolution correction is applied
          logger.warning(
              "The input resolution was invalid. The low resolution correction will be applied.");
        }
      } else {
        // notify the user that the low resolution correction is applied
        logger.warning("The low resolution correction is applied.");
      }
      options.put("charge", charge);
      options.put("tracerPurity", tracerPurity);
      options.put("correct_NA_tracer", correct_NA_tracer);
      options.put("resolutionFormulaCode", resolutionFormulaCode);
      MetaboliteCorrector corrector = MetaboliteCorrectorFactory.createCorrector(formula,
          tracerIsotope, options);
      // correct the measurements
      CorrectedResult result = corrector.correct(measurements);
      correctedMeasurements = result.getCorrectedMeasurements();
      double[] isotopologueFraction = result.getIsotopologueFraction();
      double[] meanEnrichment = new double[]{result.getMeanEnrichment()};
      double[] residuum = result.getResiduum();

      // set the new values for each row
      for (int i = 0; i < correctedMeasurements.length; i++) {
        if (i == 0) {
          // set the new value for the unlabeled compound
          ModularFeature unlabeledFeature = new ModularFeature(correctedFeatureList,
              unlabeledRow.getFeature(unlabeledRow.getRawDataFiles().get(0)));
          unlabeledFeature.setHeight((float) correctedMeasurements[i]);
          unlabeledFeature.setArea((float) correctedMeasurements[i]);
          unlabeledRow.getFeatures().get(0).setHeight((float) correctedMeasurements[i]);
          unlabeledRow.getFeatures().get(0).setArea((float) correctedMeasurements[i]);
        } else {
          // set the new value for each isotopologue
          String labeledCompoundName = tracerIsotope + i + "-" + compoundName;
          for (FeatureListRow labeledRow : rowsFeatureList) {
            if (labeledCompoundName.equals(labeledRow.getPreferredAnnotationName())) {
              ModularFeature labeledFeature = new ModularFeature(correctedFeatureList,
                  labeledRow.getFeature(labeledRow.getRawDataFiles().get(0)));
              labeledFeature.setHeight((float) correctedMeasurements[i]);
              labeledFeature.setArea((float) correctedMeasurements[i]);
              labeledRow.getFeatures().get(0).setHeight((float) correctedMeasurements[i]);
              labeledRow.getFeatures().get(0).setArea((float) correctedMeasurements[i]);
              rowsFeatureList.set(rowsFeatureList.indexOf(labeledRow), labeledRow);
            }
          }
        }
      }

      logger.info("Isotopologue fraction of " + compoundName + ": " + Arrays.toString(
          isotopologueFraction));
      logger.info("Mean enrichment of " + compoundName + ": " + Arrays.toString(meanEnrichment));
      logger.info("Residuum of " + compoundName + ": " + Arrays.toString(residuum));
    }

    // Loop through all peaks
    totalRows = rowsFeatureList.size();

    // Add task description to peakList
    correctedFeatureList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod(IsotopeNaturalAbundanceModule.MODULE_NAME,
            IsotopeNaturalAbundanceModule.class, parameters, getModuleCallDate()));

    // sort by RT
    rowsFeatureList.sort(FeatureListRowSorter.DEFAULT_RT);

    // replace rows in list
    correctedFeatureList.setRows(rowsFeatureList);

    // Remove the original peakList if requested, or add, or work in place
    handleOriginal.reflectNewFeatureListToProject(suffix, project, correctedFeatureList,
        featureList);

    logger.info("Finished isotope natural abundance correction on " + featureList);
    setStatus(TaskStatus.FINISHED);
  }

  private boolean checkCandidateMobility(Float mainMobility, FeatureListRow row) {
    Float candidateMobility = row.getAverageMobility();
    return candidateMobility == null || mobilityTolerance.checkWithinTolerance(mainMobility,
        candidateMobility);
  }
}

