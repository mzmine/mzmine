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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.modules.dataprocessing.filter_rowsfilter;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.annotations.GNPSSpectralLibraryMatchesType;
import io.github.mzmine.datamodel.features.types.annotations.LipidMatchListType;
import io.github.mzmine.modules.dataprocessing.id_gnpsresultsimport.GNPSLibraryMatch;
import io.github.mzmine.modules.dataprocessing.id_gnpsresultsimport.GNPSLibraryMatch.ATT;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.MatchedLipid;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.massdefect.MassDefectFilter;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.spectraldb.entry.SpectralDBFeatureIdentity;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Filters out feature list rows.
 */
public class RowsFilterTask extends AbstractTask {

  // Logger.
  private static final Logger logger = Logger.getLogger(RowsFilterTask.class.getName());
  // Feature lists.
  private final MZmineProject project;
  private final FeatureList origFeatureList;
  // Parameters.
  private final ParameterSet parameters;
  private FeatureList filteredFeatureList;
  // Processed rows counter
  private int processedRows, totalRows;

  /**
   * Create the task.
   *
   * @param list         feature list to process.
   * @param parameterSet task parameters.
   */
  public RowsFilterTask(final MZmineProject project, final FeatureList list,
      final ParameterSet parameterSet, @Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    // Initialize.
    this.project = project;
    parameters = parameterSet;
    origFeatureList = list;
    filteredFeatureList = null;
    processedRows = 0;
    totalRows = 0;
  }

  @Override
  public double getFinishedPercentage() {

    return totalRows == 0 ? 0.0 : (double) processedRows / (double) totalRows;

  }

  @Override
  public String getTaskDescription() {

    return "Filtering feature list rows";
  }

  @Override
  public void run() {

    if (!isCanceled()) {

      try {
        setStatus(TaskStatus.PROCESSING);
        logger.info("Filtering feature list rows");

        // Filter the feature list.
        filteredFeatureList = filterFeatureListRows(origFeatureList);

        if (!isCanceled()) {

          // Add new feature list to the project
          project.addFeatureList(filteredFeatureList);

          // Remove the original feature list if requested
          if (parameters.getParameter(RowsFilterParameters.AUTO_REMOVE).getValue()) {

            project.removeFeatureList(origFeatureList);
          }
          setStatus(TaskStatus.FINISHED);
          logger.info("Finished feature list rows filter");
        }
      } catch (Throwable t) {

        setErrorMessage(t.getMessage());
        setStatus(TaskStatus.ERROR);
        logger.log(Level.SEVERE, "Feature list row filter error", t);
      }
    }
  }

  /**
   * Filter the feature list rows.
   *
   * @param featureList feature list to filter.
   * @return a new feature list with rows of the original feature list that pass the filtering.
   */
  private FeatureList filterFeatureListRows(final FeatureList featureList) {

    // Create new feature list.

    final ModularFeatureList newFeatureList = new ModularFeatureList(
        featureList.getName() + ' ' + parameters.getParameter(RowsFilterParameters.SUFFIX)
            .getValue(), getMemoryMapStorage(), featureList.getRawDataFiles());

    // Copy previous applied methods.
    for (final FeatureListAppliedMethod method : featureList.getAppliedMethods()) {
      newFeatureList.addDescriptionOfAppliedTask(method);
    }

    // Add task description to featureList.
    newFeatureList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod(getTaskDescription(), RowsFilterModule.class, parameters,
            getModuleCallDate()));

    featureList.getRawDataFiles()
        .forEach(file -> newFeatureList.setSelectedScans(file, featureList.getSeletedScans(file)));

    // Get parameters.
    final boolean onlyIdentified = parameters.getParameter(RowsFilterParameters.HAS_IDENTITIES)
        .getValue();
    final boolean filterByIdentityText = parameters.getParameter(RowsFilterParameters.IDENTITY_TEXT)
        .getValue();
    final boolean filterByCommentText = parameters.getParameter(RowsFilterParameters.COMMENT_TEXT)
        .getValue();
    final String groupingParameter = (String) parameters.getParameter(
        RowsFilterParameters.GROUPSPARAMETER).getValue();
    final boolean filterByMinFeatureCount = parameters.getParameter(
        RowsFilterParameters.MIN_FEATURE_COUNT).getValue();
    final boolean filterByMinIsotopePatternSize = parameters.getParameter(
        RowsFilterParameters.MIN_ISOTOPE_PATTERN_COUNT).getValue();
    final boolean filterByMzRange = parameters.getParameter(RowsFilterParameters.MZ_RANGE)
        .getValue();
    final boolean filterByRtRange = parameters.getParameter(RowsFilterParameters.RT_RANGE)
        .getValue();
    final boolean filterByDuration = parameters.getParameter(RowsFilterParameters.FEATURE_DURATION)
        .getValue();
    final boolean filterByFWHM = parameters.getParameter(RowsFilterParameters.FWHM).getValue();
    final boolean filterByCharge = parameters.getParameter(RowsFilterParameters.CHARGE).getValue();
    final boolean filterByKMD = parameters.getParameter(RowsFilterParameters.KENDRICK_MASS_DEFECT)
        .getValue();
    final boolean filterByMS2 = parameters.getParameter(RowsFilterParameters.MS2_Filter).getValue();
    final String removeRowString = parameters.getParameter(RowsFilterParameters.REMOVE_ROW)
        .getValue();
    Double minCount = parameters.getParameter(RowsFilterParameters.MIN_FEATURE_COUNT)
        .getEmbeddedParameter().getValue();
    final boolean renumber = parameters.getParameter(RowsFilterParameters.Reset_ID).getValue();
    final boolean filterByMassDefect = parameters.getValue(RowsFilterParameters.massDefect);
    final MassDefectFilter massDefectFilter =
        filterByMassDefect ? parameters.getParameter(RowsFilterParameters.massDefect)
            .getEmbeddedParameter().getValue() : MassDefectFilter.ALL;

    int rowsCount = 0;
    boolean removeRow = false;

    removeRow = !removeRowString.equals(RowsFilterParameters.removeRowChoices[0]);

    // Keep rows that don't match any criteria. Keep by default.
    boolean filterRowCriteriaFailed = false;

    // Handle < 1 values for minFeatureCount
    if ((minCount == null) || (minCount < 1)) {
      minCount = 1.0;
    }
    // Round value down to nearest hole number
    int intMinCount = minCount.intValue();

    // Filter rows.
    final ModularFeatureListRow[] rows = featureList.getRows()
        .toArray(ModularFeatureListRow[]::new);
    totalRows = rows.length;
    for (processedRows = 0; !isCanceled() && processedRows < totalRows; processedRows++) {

      filterRowCriteriaFailed = false;

      final ModularFeatureListRow row = rows[processedRows];

      final int featureCount = getFeatureCount(row, groupingParameter);

      // Check number of features.
      if (filterByMinFeatureCount) {
        if (featureCount < intMinCount) {
          filterRowCriteriaFailed = true;
        }
      }

      // Check identities.
      List<MatchedLipid> matchedLipids = row.get(LipidMatchListType.class);
      if (onlyIdentified) {
        List<SpectralDBFeatureIdentity> matches = row.getSpectralLibraryMatches();
        List<GNPSLibraryMatch> gnps = row.get(GNPSSpectralLibraryMatchesType.class);

        boolean noIdentity = (row.getPreferredFeatureIdentity() == null);
        boolean noLipid = matchedLipids == null || matchedLipids.isEmpty();
        boolean noGNPS = gnps == null || gnps.isEmpty();
        boolean noMatch = matches == null || matches.isEmpty();

        if (noIdentity && noLipid && noGNPS && noMatch) {
          filterRowCriteriaFailed = true;
        }
      }

      // Check average m/z.
      if (filterByMzRange) {
        final Range<Double> mzRange = parameters.getParameter(RowsFilterParameters.MZ_RANGE)
            .getEmbeddedParameter().getValue();
        if (!mzRange.contains(row.getAverageMZ())) {
          filterRowCriteriaFailed = true;
        }
      }

      // Check average RT.
      if (filterByRtRange) {

        final Range<Float> rtRange = RangeUtils.toFloatRange(
            parameters.getParameter(RowsFilterParameters.RT_RANGE).getEmbeddedParameter()
                .getValue());

        if (!rtRange.contains(row.getAverageRT())) {
          filterRowCriteriaFailed = true;
        }
      }

      // Search feature identity text.
      if (filterByIdentityText) {
        final String searchText = parameters.getParameter(RowsFilterParameters.IDENTITY_TEXT)
            .getEmbeddedParameter().getValue().toLowerCase().trim();

        boolean foundText = false;
        if (row.getPeakIdentities() != null) {
          for (var id : row.getPeakIdentities()) {
            if (id != null && id.getName().toLowerCase().trim().contains(searchText)) {
              foundText = true;
              break;
            }
          }
        }
        if (matchedLipids != null && !foundText) {
          for (var id : matchedLipids) {
            if (id != null && id.getLipidAnnotation().getAnnotation().toLowerCase().trim()
                .contains(searchText)) {
              foundText = true;
              break;
            }
          }
        }
        if (!foundText && row.getSpectralLibraryMatches() != null) {
          for (var id : row.getSpectralLibraryMatches()) {
            if (id != null && id.getName().toLowerCase().trim().contains(searchText)) {
              foundText = true;
              break;
            }
          }
        }
        if (!foundText && row.get(GNPSSpectralLibraryMatchesType.class) != null) {
          for (var id : row.get(GNPSSpectralLibraryMatchesType.class)) {
            if (id != null && id.getResultOr(ATT.COMPOUND_NAME, "").toLowerCase().trim()
                .contains(searchText)) {
              foundText = true;
              break;
            }
          }
        }

        if (!foundText) {
          filterRowCriteriaFailed = true;
        }
      }

      // Search feature comment text.
      if (filterByCommentText) {

        if (row.getComment() == null) {
          filterRowCriteriaFailed = true;
        }
        if (row.getComment() != null) {
          final String searchText = parameters.getParameter(RowsFilterParameters.COMMENT_TEXT)
              .getEmbeddedParameter().getValue().toLowerCase().trim();
          final String rowText = row.getComment().toLowerCase().trim();
          if (!rowText.contains(searchText)) {
            filterRowCriteriaFailed = true;
          }

        }
      }

      // Calculate average duration and isotope pattern count.
      int maxIsotopePatternSizeOnRow = 1;
      double avgDuration = 0.0;
      final Feature[] features = row.getFeatures().toArray(new Feature[0]);
      for (final Feature p : features) {

        final IsotopePattern pattern = p.getIsotopePattern();
        if (pattern != null && maxIsotopePatternSizeOnRow < pattern.getNumberOfDataPoints()) {

          maxIsotopePatternSizeOnRow = pattern.getNumberOfDataPoints();
        }

        avgDuration += RangeUtils.rangeLength(p.getRawDataPointsRTRange());
      }

      // Check isotope pattern count.
      if (filterByMinIsotopePatternSize) {

        final int minIsotopePatternSize = parameters.getParameter(
            RowsFilterParameters.MIN_ISOTOPE_PATTERN_COUNT).getEmbeddedParameter().getValue();
        if (maxIsotopePatternSizeOnRow < minIsotopePatternSize) {
          filterRowCriteriaFailed = true;
        }
      }

      // Check average duration.
      avgDuration /= featureCount;
      if (filterByDuration) {

        final Range<Double> durationRange = parameters.getParameter(
            RowsFilterParameters.FEATURE_DURATION).getEmbeddedParameter().getValue();
        if (!durationRange.contains(avgDuration)) {
          filterRowCriteriaFailed = true;
        }

      }

      // Filter by FWHM range
      if (filterByFWHM) {

        final Range<Float> FWHMRange = RangeUtils.toFloatRange(
            parameters.getParameter(RowsFilterParameters.FWHM).getEmbeddedParameter().getValue());
        // If any of the features fail the FWHM criteria,
        Float FWHM_value = row.getBestFeature().getFWHM();

        if (FWHM_value != null && !FWHMRange.contains(FWHM_value)) {
          filterRowCriteriaFailed = true;
        }
      }

      // Filter by charge range
      if (filterByCharge) {

        final Range<Integer> chargeRange = parameters.getParameter(RowsFilterParameters.CHARGE)
            .getEmbeddedParameter().getValue();
        int charge = row.getBestFeature().getCharge();
        if (charge == 0 || !chargeRange.contains(charge)) {
          filterRowCriteriaFailed = true;
        }
      }

      // Filter by KMD or RKM range
      if (filterByKMD) {

        // get embedded parameters
        final Range<Double> rangeKMD = parameters.getParameter(
                RowsFilterParameters.KENDRICK_MASS_DEFECT).getEmbeddedParameters()
            .getParameter(KendrickMassDefectFilterParameters.kendrickMassDefectRange).getValue();
        final String kendrickMassBase = parameters.getParameter(
                RowsFilterParameters.KENDRICK_MASS_DEFECT).getEmbeddedParameters()
            .getParameter(KendrickMassDefectFilterParameters.kendrickMassBase).getValue();
        final double shift = parameters.getParameter(RowsFilterParameters.KENDRICK_MASS_DEFECT)
            .getEmbeddedParameters().getParameter(KendrickMassDefectFilterParameters.shift)
            .getValue();
        final int charge = parameters.getParameter(RowsFilterParameters.KENDRICK_MASS_DEFECT)
            .getEmbeddedParameters().getParameter(KendrickMassDefectFilterParameters.charge)
            .getValue();
        final int divisor = parameters.getParameter(RowsFilterParameters.KENDRICK_MASS_DEFECT)
            .getEmbeddedParameters().getParameter(KendrickMassDefectFilterParameters.divisor)
            .getValue();
        final boolean useRemainderOfKendrickMass = parameters.getParameter(
                RowsFilterParameters.KENDRICK_MASS_DEFECT).getEmbeddedParameters()
            .getParameter(KendrickMassDefectFilterParameters.useRemainderOfKendrickMass).getValue();

        // get m/z
        Double valueMZ = row.getBestFeature().getMZ();

        // calc exact mass of Kendrick mass base
        double exactMassFormula = FormulaUtils.calculateExactMass(kendrickMassBase);

        // calc exact mass of Kendrick mass factor
        double kendrickMassFactor =
            Math.round(exactMassFormula / divisor) / (exactMassFormula / divisor);

        double defectOrRemainder = 0.0;

        if (!useRemainderOfKendrickMass) {

          // calc Kendrick mass defect
          defectOrRemainder = Math.ceil(charge * (valueMZ * kendrickMassFactor)) - charge * (valueMZ
              * kendrickMassFactor);
        } else {

          // calc Kendrick mass remainder
          defectOrRemainder =
              (charge * (divisor - Math.round(FormulaUtils.calculateExactMass(kendrickMassBase)))
                  * valueMZ) / FormulaUtils.calculateExactMass(kendrickMassBase)//
                  - Math.floor((charge * (divisor - Math.round(
                  FormulaUtils.calculateExactMass(kendrickMassBase))) * valueMZ)
                  / FormulaUtils.calculateExactMass(kendrickMassBase));
        }

        // shift Kendrick mass defect or remainder of Kendrick mass
        double kendrickMassDefectShifted =
            defectOrRemainder + shift - Math.floor(defectOrRemainder + shift);

        // check if shifted Kendrick mass defect or remainder of
        // Kendrick mass is in range
        if (!rangeKMD.contains(kendrickMassDefectShifted)) {
          filterRowCriteriaFailed = true;
        }
      }

      // Check ms2 filter .
      if (filterByMS2) {
        // iterates the features
        int failCounts = 0;
        for (int i = 0; i < featureCount; i++) {
          if (row.getFeatures().get(i).getMostIntenseFragmentScan() == null) {
            failCounts++;
            // filterRowCriteriaFailed = true;
            // break;
          }
        }
        if (failCounts == featureCount) {
          filterRowCriteriaFailed = true;
        }
      }

      if (filterByMassDefect) {
        if (!massDefectFilter.contains(row.getAverageMZ())) {
          filterRowCriteriaFailed = true;
        }
      }

      if (!filterRowCriteriaFailed && !removeRow) {
        // Only add the row if none of the criteria have failed.
        rowsCount++;
        FeatureListRow resetRow = new ModularFeatureListRow(newFeatureList,
            renumber ? rowsCount : row.getID(), row, true);
        newFeatureList.addRow(resetRow);
      }

      if (filterRowCriteriaFailed && removeRow) {
        // Only remove rows that match *all* of the criteria, so add
        // rows that fail any of the criteria.
        rowsCount++;
        FeatureListRow resetRow = new ModularFeatureListRow(newFeatureList,
            renumber ? rowsCount : row.getID(), row, true);
        newFeatureList.addRow(resetRow);
      }

    }

    return newFeatureList;
  }

  private int getFeatureCount(FeatureListRow row, String groupingParameter) {
    if (groupingParameter.contains("Filtering by ")) {
      HashMap<String, Integer> groups = new HashMap<>();
      for (RawDataFile file : project.getDataFiles()) {
        UserParameter<?, ?> params[] = project.getParameters();
        for (UserParameter<?, ?> p : params) {
          groupingParameter = groupingParameter.replace("Filtering by ", "");
          if (groupingParameter.equals(p.getName())) {
            String parameterValue = String.valueOf(project.getParameterValue(p, file));
            if (row.hasFeature(file)) {
              if (groups.containsKey(parameterValue)) {
                groups.put(parameterValue, groups.get(parameterValue) + 1);
              } else {
                groups.put(parameterValue, 1);
              }
            } else {
              groups.put(parameterValue, 0);
            }
          }
        }
      }

      Set<String> ref = groups.keySet();
      Iterator<String> it = ref.iterator();
      int min = Integer.MAX_VALUE;
      while (it.hasNext()) {
        String name = it.next();
        int val = groups.get(name);
        if (val < min) {
          min = val;
        }
      }
      return min;

    } else {
      return row.getNumberOfFeatures();
    }
  }
}
