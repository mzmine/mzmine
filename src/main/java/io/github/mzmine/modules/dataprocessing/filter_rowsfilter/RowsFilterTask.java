/*
 * Copyright 2006-2022 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
import io.github.mzmine.datamodel.features.types.numbers.IDType;
import io.github.mzmine.modules.dataprocessing.id_gnpsresultsimport.GNPSLibraryMatch.ATT;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.MatchedLipid;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.massdefect.MassDefectFilter;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RangeUtils;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
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
  private final boolean filter13CIsotopes;
  private final boolean keepAllWithMS2;
  private final boolean onlyIdentified;
  private final boolean filterByIdentityText;
  private final boolean filterByCommentText;
  private final String groupingParameter;
  private final boolean filterByMinFeatureCount;
  private final boolean filterByMinIsotopePatternSize;
  private final boolean filterByMzRange;
  private final boolean filterByRtRange;
  private final boolean filterByDuration;
  private final boolean filterByFWHM;
  private final boolean filterByCharge;
  private final boolean filterByKMD;
  private final boolean filterByMS2;
  private final RowsFilterChoices filterOption;
  private final boolean renumber;
  private final boolean filterByMassDefect;
  private final MassDefectFilter massDefectFilter;
  private final KendrickMassDefectFilterParameters kendrickParam;
  private final Range<Double> rangeKMD;
  private final String kendrickMassBase;
  private final Double shift;
  private final Integer kendrickCharge;
  private final Integer divisor;
  private final Boolean useRemainderOfKendrickMass;
  private final Range<Integer> chargeRange;
  private final Range<Double> durationRange;
  private final Integer minIsotopePatternSize;
  private final String commentSearchText;
  private final String searchText;
  private final Range<Double> mzRange;
  private final Range<Float> rtRange;
  private final Range<Float> fwhmRange;
  private final Isotope13CFilter isotope13CFilter;
  private Double minCount;
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

    // Get parameters.
    keepAllWithMS2 = parameters.getValue(RowsFilterParameters.KEEP_ALL_MS2);

    onlyIdentified = parameters.getValue(RowsFilterParameters.HAS_IDENTITIES);
    filterByIdentityText = parameters.getValue(RowsFilterParameters.IDENTITY_TEXT);
    filterByCommentText = parameters.getValue(RowsFilterParameters.COMMENT_TEXT);
    groupingParameter = (String) parameters.getValue(RowsFilterParameters.GROUPSPARAMETER);
    filterByMinFeatureCount = parameters.getValue(RowsFilterParameters.MIN_FEATURE_COUNT);
    filterByMinIsotopePatternSize = parameters.getValue(
        RowsFilterParameters.MIN_ISOTOPE_PATTERN_COUNT);
    filterByMzRange = parameters.getValue(RowsFilterParameters.MZ_RANGE);
    filterByRtRange = parameters.getValue(RowsFilterParameters.RT_RANGE);
    filterByDuration = parameters.getValue(RowsFilterParameters.FEATURE_DURATION);
    filterByFWHM = parameters.getValue(RowsFilterParameters.FWHM);
    filterByCharge = parameters.getValue(RowsFilterParameters.CHARGE);
    filterByKMD = parameters.getValue(RowsFilterParameters.KENDRICK_MASS_DEFECT);
    filterByMS2 = parameters.getValue(RowsFilterParameters.MS2_Filter);
    filterOption = parameters.getValue(RowsFilterParameters.REMOVE_ROW);
    minCount = parameters.getParameter(RowsFilterParameters.MIN_FEATURE_COUNT)
        .getEmbeddedParameter().getValue();
    renumber = parameters.getValue(RowsFilterParameters.Reset_ID);
    filterByMassDefect = parameters.getValue(RowsFilterParameters.massDefect);
    massDefectFilter = filterByMassDefect ? parameters.getParameter(RowsFilterParameters.massDefect)
        .getEmbeddedParameter().getValue() : MassDefectFilter.ALL;

    // get embedded parameters
    kendrickParam = parameters.getParameter(RowsFilterParameters.KENDRICK_MASS_DEFECT)
        .getEmbeddedParameters();
    rangeKMD = kendrickParam.getParameter(
        KendrickMassDefectFilterParameters.kendrickMassDefectRange).getValue();
    kendrickMassBase = kendrickParam.getParameter(
        KendrickMassDefectFilterParameters.kendrickMassBase).getValue();
    shift = kendrickParam.getParameter(KendrickMassDefectFilterParameters.shift).getValue();
    kendrickCharge = kendrickParam.getParameter(KendrickMassDefectFilterParameters.charge)
        .getValue();
    divisor = kendrickParam.getParameter(KendrickMassDefectFilterParameters.divisor).getValue();
    useRemainderOfKendrickMass = kendrickParam.getParameter(
        KendrickMassDefectFilterParameters.useRemainderOfKendrickMass).getValue();
    chargeRange =
        filterByCharge ? parameters.getParameter(RowsFilterParameters.CHARGE).getEmbeddedParameter()
            .getValue() : null;
    durationRange =
        filterByDuration ? parameters.getParameter(RowsFilterParameters.FEATURE_DURATION)
            .getEmbeddedParameter().getValue() : null;
    minIsotopePatternSize = parameters.getParameter(RowsFilterParameters.MIN_ISOTOPE_PATTERN_COUNT)
        .getEmbeddedParameter().getValue();
    commentSearchText = parameters.getParameter(RowsFilterParameters.COMMENT_TEXT)
        .getEmbeddedParameter().getValue().toLowerCase().trim();
    searchText = parameters.getParameter(RowsFilterParameters.IDENTITY_TEXT).getEmbeddedParameter()
        .getValue().toLowerCase().trim();
    mzRange = filterByMzRange ? parameters.getParameter(RowsFilterParameters.MZ_RANGE)
        .getEmbeddedParameter().getValue() : null;
    rtRange = filterByRtRange ? RangeUtils.toFloatRange(
        parameters.getParameter(RowsFilterParameters.RT_RANGE).getEmbeddedParameter().getValue())
        : null;
    fwhmRange = filterByFWHM ? RangeUtils.toFloatRange(
        parameters.getParameter(RowsFilterParameters.FWHM).getEmbeddedParameter().getValue())
        : null;

    // isotope filter
    filter13CIsotopes = parameters.getParameter(RowsFilterParameters.ISOTOPE_FILTER_13C).getValue();
    isotope13CFilter = parameters.getParameter(RowsFilterParameters.ISOTOPE_FILTER_13C)
        .getEmbeddedParameters().createFilter();
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

        final OriginalFeatureListOption originalFeatureListOption = parameters.getValue(
            RowsFilterParameters.handleOriginal);

        switch (originalFeatureListOption) {
          case KEEP -> logger.finer("Create new feature List");
          case REMOVE -> logger.finer("Remove original feature list");
          case PROCESS_IN_PLACE -> logger.finer("Process in place");
        }

        // Filter the feature list.
        filteredFeatureList = filterFeatureListRows(origFeatureList,
            originalFeatureListOption == OriginalFeatureListOption.PROCESS_IN_PLACE);

        if (!isCanceled()) {
          final String suffix = parameters.getValue(RowsFilterParameters.SUFFIX);
          originalFeatureListOption.reflectNewFeatureListToProject(suffix, project,
              filteredFeatureList, origFeatureList);
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
   * @param featureList          feature list to filter.
   * @param processInCurrentList use the current list and filter it
   * @return a new feature list with rows of the original feature list that pass the filtering.
   */
  private FeatureList filterFeatureListRows(final FeatureList featureList,
      boolean processInCurrentList) {

    // Create new feature list.

    final ModularFeatureList newFeatureList;
    if (processInCurrentList) {
      newFeatureList = (ModularFeatureList) featureList;
    } else {
      newFeatureList = new ModularFeatureList(
          featureList.getName() + ' ' + parameters.getParameter(RowsFilterParameters.SUFFIX)
              .getValue(), getMemoryMapStorage(), featureList.getRawDataFiles());
      // Copy previous applied methods.
      for (final FeatureListAppliedMethod method : featureList.getAppliedMethods()) {
        newFeatureList.addDescriptionOfAppliedTask(method);
      }

      featureList.getRawDataFiles().forEach(
          file -> newFeatureList.setSelectedScans(file, featureList.getSeletedScans(file)));
    }

    // Add task description to featureList.
    newFeatureList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod(getTaskDescription(), RowsFilterModule.class, parameters,
            getModuleCallDate()));

    int rowsCount = 0;
    // if keep is selected we remove rows on failed criteria
    // otherwise we remove those that match all criteria
    boolean removeFailed = RowsFilterChoices.KEEP_MATCHING == filterOption;

    // Handle < 1 values for minFeatureCount
    if ((minCount == null) || (minCount < 1)) {
      minCount = 1.0;
    }
    // Round value down to nearest hole number
    int intMinCount = minCount.intValue();

    // Filter rows.
    totalRows = featureList.getNumberOfRows();
    processedRows = 0;
    final ListIterator<FeatureListRow> iterator = featureList.getRows().listIterator();
    while (iterator.hasNext()) {
      if (isCanceled()) {
        return null;
      }

      final FeatureListRow row = iterator.next();

      final boolean hasMS2 = row.hasMs2Fragmentation();

      // Only remove rows that match *all* of the criteria, so add
      // rows that fail any of the criteria.
      // Only add the row if none of the criteria have failed.
      boolean keepRow = (keepAllWithMS2 && hasMS2)
          || isFilterRowCriteriaFailed(intMinCount, row, hasMS2) != removeFailed;
      if (processInCurrentList) {
        if (keepRow) {
          rowsCount++;
          if (renumber) {
            row.set(IDType.class, rowsCount);
          }
        } else {
          iterator.remove();
        }
      } else if (keepRow) {
        rowsCount++;
        FeatureListRow resetRow = new ModularFeatureListRow(newFeatureList,
            renumber ? rowsCount : row.getID(), (ModularFeatureListRow) row, true);
        newFeatureList.addRow(resetRow);
      }

      processedRows++;
    }

    return newFeatureList;
  }

  private boolean isFilterRowCriteriaFailed(int intMinCount, FeatureListRow row, boolean hasMS2) {

    // Check ms2 filter .
    if (filterByMS2 && !hasMS2) {
      return true;
    }

    // Check number of features.
    final int featureCount = getFeatureCount(row, groupingParameter);
    if (filterByMinFeatureCount) {
      if (featureCount < intMinCount) {
        return true;
      }
    }

    // Check identities.
    if (onlyIdentified && !row.isIdentified()) {
      return true;
    }

    // Check average m/z.
    if (filterByMzRange) {
      if (!mzRange.contains(row.getAverageMZ())) {
        return true;
      }
    }

    // Check average RT.
    if (filterByRtRange) {
      if (!rtRange.contains(row.getAverageRT())) {
        return true;
      }
    }

    // Search feature identity text.
    if (filterByIdentityText) {
      boolean foundText = false;
      if (row.getPeakIdentities() != null) {
        for (var id : row.getPeakIdentities()) {
          if (id != null && id.getName().toLowerCase().trim().contains(searchText)) {
            foundText = true;
            break;
          }
        }
      }
      List<MatchedLipid> matchedLipids = row.get(LipidMatchListType.class);
      if (matchedLipids != null && !foundText) {
        for (var id : matchedLipids) {
          if (id != null && id.getLipidAnnotation().getAnnotation().toLowerCase().trim()
              .contains(searchText)) {
            foundText = true;
            break;
          }
        }
      }
      if (!foundText) {
        for (var id : row.getSpectralLibraryMatches()) {
          if (id != null && id.getCompoundName().toLowerCase().trim().contains(searchText)) {
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
        return true;
      }
    }

    // Search feature comment text.
    if (filterByCommentText) {

      if (row.getComment() == null) {
        return true;
      }
      if (row.getComment() != null) {
        final String rowText = row.getComment().toLowerCase().trim();
        if (!rowText.contains(commentSearchText)) {
          return true;
        }
      }
    }

    // Calculate average duration and isotope pattern count.
    int maxIsotopePatternSizeOnRow = 1;
    double avgDuration = 0.0;
    boolean matches13Cisotopes = false;
    final Feature[] features = row.getFeatures().toArray(new Feature[0]);
    for (final Feature p : features) {

      final IsotopePattern pattern = p.getIsotopePattern();
      if (pattern != null) {
        // check isotope pattern - only one match for a feature needed
        if (filter13CIsotopes && !matches13Cisotopes) {
          matches13Cisotopes = isotope13CFilter.accept(pattern, p.getMZ());
        }

        if (maxIsotopePatternSizeOnRow < pattern.getNumberOfDataPoints()) {
          maxIsotopePatternSizeOnRow = pattern.getNumberOfDataPoints();
        }
      }
      avgDuration += RangeUtils.rangeLength(p.getRawDataPointsRTRange());
    }

    // filter 13C istope pattern - needs to be true in one feature
    if (filter13CIsotopes && !matches13Cisotopes) {
      return true;
    }
    // Check isotope pattern count.
    if (filterByMinIsotopePatternSize) {
      if (maxIsotopePatternSizeOnRow < minIsotopePatternSize) {
        return true;
      }
    }

    // Check average duration.
    avgDuration /= featureCount;
    if (filterByDuration) {
      if (!durationRange.contains(avgDuration)) {
        return true;
      }
    }

    // Filter by FWHM range
    if (filterByFWHM) {
      // If any of the features fail the FWHM criteria,
      Float FWHM_value = row.getBestFeature().getFWHM();
      if (FWHM_value != null && !fwhmRange.contains(FWHM_value)) {
        return true;
      }
    }

    // Filter by charge range
    if (filterByCharge) {
      int charge = row.getBestFeature().getCharge();
      if (charge == 0 || !chargeRange.contains(charge)) {
        return true;
      }
    }

    // Filter by KMD or RKM range
    if (filterByKMD) {
      // get m/z
      Double valueMZ = row.getBestFeature().getMZ();

      // calc exact mass of Kendrick mass base
      double exactMassFormula = FormulaUtils.calculateExactMass(kendrickMassBase);

      // calc exact mass of Kendrick mass factor
      double kendrickMassFactor =
          Math.round(exactMassFormula / divisor) / (exactMassFormula / divisor);

      double defectOrRemainder;

      if (!useRemainderOfKendrickMass) {
        // calc Kendrick mass defect
        defectOrRemainder = Math.ceil(kendrickCharge * (valueMZ * kendrickMassFactor)) //
            - kendrickCharge * (valueMZ * kendrickMassFactor);
      } else {
        // calc Kendrick mass remainder
        defectOrRemainder = (kendrickCharge * (divisor - Math.round(
            FormulaUtils.calculateExactMass(kendrickMassBase))) * valueMZ)
            / FormulaUtils.calculateExactMass(kendrickMassBase) - Math.floor(
            (kendrickCharge * (divisor - Math.round(
                FormulaUtils.calculateExactMass(kendrickMassBase))) * valueMZ)
                / FormulaUtils.calculateExactMass(kendrickMassBase));
      }

      // shift Kendrick mass defect or remainder of Kendrick mass
      double kendrickMassDefectShifted =
          defectOrRemainder + shift - Math.floor(defectOrRemainder + shift);

      // check if shifted Kendrick mass defect or remainder of
      // Kendrick mass is in range
      if (!rangeKMD.contains(kendrickMassDefectShifted)) {
        return true;
      }
    }

    if (filterByMassDefect) {
      return !massDefectFilter.contains(row.getAverageMZ());
    }
    return false;
  }

  private int getFeatureCount(FeatureListRow row, String groupingParameter) {
    if (groupingParameter.contains("Filtering by ")) {
      HashMap<String, Integer> groups = new HashMap<>();
      for (RawDataFile file : project.getDataFiles()) {
        UserParameter<?, ?>[] params = project.getParameters();
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
