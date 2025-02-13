/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
import io.github.mzmine.modules.dataprocessing.isolab_targeted.IsotopeLabelingTargetedTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import io.github.mzmine.util.annotations.CompoundAnnotationUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
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
  private final MobilityTolerance mobilityTolerance;
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

    // Get parameter values for easier use
    suffix = parameters.getParameter(IsotopeNaturalAbundanceParameters.suffix).getValue();
    handleOriginal = parameters.getValue(IsotopeNaturalAbundanceParameters.handleOriginal);
    mobilityTolerance = parameters.getParameter(IsotopeNaturalAbundanceParameters.mobilityTolerace)
        .getEmbeddedParameter().getValue();
    mzTolerance = parameters.getParameter(IsotopeNaturalAbundanceParameters.mzTolerance).getValue();
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
    //    DataTypeUtils.copyTypes(featureList, correctedFeatureList, true, true);

    final FeatureListRowSorter rowsMzSorter = new FeatureListRowSorter(SortingProperty.MZ,
        SortingDirection.Ascending);

    // use a second sorted list to limit the number of comparisons
    List<FeatureListRow> rowsFeatureList = new ArrayList<>(correctedFeatureList.getRows());

    // find the unlabeled compounds in the feature list; their names in the "Compound DB" column do not start with "13C"
    final List<FeatureListRow> unlabeledRows = new ArrayList<>();

    for (FeatureListRow row : rowsFeatureList) {
      if (row.getPreferredAnnotationName() == null || !row.getPreferredAnnotationName()
          .startsWith("13C")) {
        unlabeledRows.add(row);
      }
    }

    // For each unlabeled compound, find the corresponding labeled compounds in the original feature list. Their names will be "13C" followed by a number and "-" followed by the compound name.
    for (FeatureListRow unlabeledRow : unlabeledRows) {
      final String compoundName = unlabeledRow.getPreferredAnnotationName();
      final List<FeatureListRow> labeledRows = new ArrayList<>();

      for (FeatureListRow row : rowsFeatureList) {
        if (row.getPreferredAnnotationName() != null && row.getPreferredAnnotationName()
            .startsWith("13C") && row.getPreferredAnnotationName().contains("-" + compoundName)) {
          labeledRows.add(row);
        }
      }

      double mzValue = unlabeledRow.getAverageMZ();
      double deltaMDalton = mzTolerance.getMzTolerance(); // Absolute tolerance in Dalton
      double ppmTolerance = mzTolerance.getPpmTolerance(); // Tolerance in ppm

      // Calculate resolution using Dalton tolerance
      double resolutionDalton = calculateResolutionFromDalton(mzValue, deltaMDalton);
      // System.out.println("Resolution (from Dalton): " + resolutionDalton);

      // Calculate resolution using ppm tolerance
      double resolutionPPM = calculateResolutionFromPPM(mzValue, ppmTolerance);
      //System.out.println("Resolution (from ppm): " + resolutionPPM);
      // check how many carbon atoms are in the unlabeled compound. Each carbon atom that does not have a measured isotope peak will be replaced by a 0 in the array.
      // get the formula of the compound
      String formula = Arrays.toString(
          CompoundAnnotationUtils.streamFeatureAnnotations(unlabeledRow)
              .map(FeatureAnnotation::getFormula).toArray());
      int carbonNumber = IsotopeLabelingTargetedTask.getAtomCount(formula, "C");

      // create an array of measurement for the unlabeled compound and the measurements for the labeled compounds. If a compound does not have a measured isotope peak, the corresponding value in the array will be 0.
      double[] measurements = new double[carbonNumber + 1];
      measurements[0] = unlabeledRow.getMaxArea();
      int counter = 1;
      for (FeatureListRow labeledRow : labeledRows) {
        int index = Integer.parseInt(
            labeledRow.getPreferredAnnotationName().split("-")[0].substring(3));
        if (index != counter) {
          for (int i = counter; i < index; i++) {
            measurements[i] = 0.0;
          }
        } else {
          measurements[index] = labeledRow.getMaxArea();
        }
        counter++;
      }
      // print everything to the console to check if the code is working as expected
      System.out.println("Unlabeled compound: " + compoundName);
      System.out.println("Carbon number: " + carbonNumber);
      for (double measurement : measurements) {
        System.out.println(measurement);
      }
      // create a new row for each unlabeled compound, change the value that you can access with .getMaxArea() to 0.0 and overwrite the corresponding row in rowsFeatureList
      //RawDataFile file : unlabeledRow.getRawDataFiles();
      //Feature originalFeature = unlabeledRow.getFeature(file);
      //if (originalFeature != null) {
      ModularFeature normalizedFeature = new ModularFeature(correctedFeatureList,
          unlabeledRow.getFeature(unlabeledRow.getRawDataFiles().get(0)));
      //}
      float normalizedHeight = 1000000;
      float normalizedArea = 1000000;
      normalizedFeature.setHeight(normalizedHeight);
      normalizedFeature.setArea(normalizedArea);

      unlabeledRow.getFeatures().get(0).setHeight(normalizedHeight);
      unlabeledRow.getFeatures().get(0).setArea(normalizedArea);
      rowsFeatureList.set(rowsFeatureList.indexOf(unlabeledRow), unlabeledRow);
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

