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
package io.github.mzmine.modules.dataprocessing.filter_rowsfilter;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.types.annotations.GNPSSpectralLibraryMatchesType;
import io.github.mzmine.datamodel.features.types.numbers.IDType;
import io.github.mzmine.datamodel.features.types.otherdectectors.MsOtherCorrelationResultType;
import io.github.mzmine.modules.dataprocessing.id_gnpsresultsimport.GNPSLibraryMatch.ATT;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.MinimumSamplesFilter;
import io.github.mzmine.parameters.parametertypes.MinimumSamplesFilterConfig;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.massdefect.MassDefectFilter;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.annotations.CompoundAnnotationUtils;
import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
  private final boolean filterByMinIsotopePatternSize;
  private final boolean filterByMzRange;
  private final boolean filterByRtRange;
  private final boolean filterByDuration;
  private final boolean filterByFWHM;
  private final boolean filterByCharge;
  private final boolean filterByKMD;
  private final boolean filterByMS2;
  private final boolean onlyWithOtherCorrelated;
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
  private final FoldChangeSignificanceRowFilterParameters significanceFoldChangeFilterParameters;
  private final RsdFilterParameters cvFilterParameters;
  private final MinimumSamplesFilter minSamples;
  private final MinimumSamplesFilter minSamplesInGroup;
  private final boolean removeRedundantIsotopeRows;
  private final boolean keepAnnotated;
  private FeatureList filteredFeatureList;
  // Processed rows counter
  private int processedRows, totalRows;
  private FoldChangeSignificanceRowFilter significanceFoldChangeFilter;
  private RsdFilter cvFilter;


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
    final List<RawDataFile> rawFiles = origFeatureList.getRawDataFiles();
    filteredFeatureList = null;
    processedRows = 0;
    totalRows = 0;

    // Get parameters.
    keepAllWithMS2 = parameters.getValue(RowsFilterParameters.KEEP_ALL_MS2);
    keepAnnotated = parameters.getValue(RowsFilterParameters.KEEP_ALL_ANNOTATED);

    onlyIdentified = parameters.getValue(RowsFilterParameters.HAS_IDENTITIES);
    filterByIdentityText = parameters.getValue(RowsFilterParameters.IDENTITY_TEXT);
    filterByCommentText = parameters.getValue(RowsFilterParameters.COMMENT_TEXT);
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
    onlyWithOtherCorrelated = parameters.getValue(
        RowsFilterParameters.onlyCorrelatedWithOtherDetectors);

    // create min samples filter based on all files and on groups in column
    minSamples = parameters.getOptionalValue(RowsFilterParameters.MIN_FEATURE_COUNT)
        .map(min -> new MinimumSamplesFilterConfig(min).createFilter(rawFiles)).orElse(null);
    minSamplesInGroup = parameters.getOptionalValue(RowsFilterParameters.MIN_FEATURE_IN_GROUP_COUNT)
        .map(config -> config.createFilter(rawFiles)).orElse(null);

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

    this.cvFilterParameters = parameters.getEmbeddedParametersIfSelectedOrElse(
        RowsFilterParameters.cvFilter, null);

    this.significanceFoldChangeFilterParameters = parameters.getEmbeddedParametersIfSelectedOrElse(
        RowsFilterParameters.foldChangeFilter, null);

    // isotope filter
    filter13CIsotopes = parameters.getParameter(RowsFilterParameters.ISOTOPE_FILTER_13C).getValue();
    isotope13CFilter = parameters.getParameter(RowsFilterParameters.ISOTOPE_FILTER_13C)
        .getEmbeddedParameters().createFilter();

    removeRedundantIsotopeRows = parameters.getValue(RowsFilterParameters.removeRedundantRows);
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
        error(t.getMessage());
        logger.log(Level.SEVERE, "Feature list row filter error", t);
        return;
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
    // prepare filters that require a prepared data table
    if (significanceFoldChangeFilterParameters != null) {
      significanceFoldChangeFilter = significanceFoldChangeFilterParameters.createFilter(
          featureList.getRows(), featureList.getRawDataFiles());
    }

    if (cvFilterParameters != null) {
      cvFilter = cvFilterParameters.createFilter(featureList.getRows(),
          featureList.getRawDataFiles());
    }

    // if keep is selected we remove rows on failed criteria
    // otherwise we remove those that match all criteria
    boolean removeFailed = RowsFilterChoices.KEEP_MATCHING == filterOption;

    // check if min samples filter is valid
    if (minSamples != null) {
      String message = minSamples.getInvalidConfigMessage(
          RowsFilterParameters.MIN_FEATURE_COUNT.getName(), featureList);
      if (message != null) {
        error(message);
        return null;
      }
    }
    if (minSamplesInGroup != null) {
      String message = minSamplesInGroup.getInvalidConfigMessage(
          RowsFilterParameters.MIN_FEATURE_IN_GROUP_COUNT.getName(), featureList);
      if (message != null) {
        error(message);
        return null;
      }
    }

    // Filter rows.
    totalRows = featureList.getNumberOfRows();
    processedRows = 0;
    // requires copy of rows as there is no efficient way to remove rows from the list
    // the use setAll
    final ArrayList<FeatureListRow> rowsToAdd = new ArrayList<>((int) (totalRows * 0.75));

    // keep track of index
    int rowIndex = -1;
    for (final FeatureListRow row : featureList.getRows()) {
      rowIndex++;

      if (isCanceled()) {
        return null;
      }

      final boolean hasMS2 = row.hasMs2Fragmentation();
      final boolean annotated = row.isIdentified();

      // Only remove rows that match *all* of the criteria, so add
      // rows that fail any of the criteria.
      // Only add the row if none of the criteria have failed.
      boolean keepRow = (keepAllWithMS2 && hasMS2) || (keepAnnotated && annotated)
          || isFilterRowCriteriaFailed(row, rowIndex, hasMS2) != removeFailed;
      if (keepRow) {
        rowsToAdd.add(row);
      }

      processedRows++;
    }

    final ModularFeatureList newFeatureList;
    if (processInCurrentList) {
      newFeatureList = (ModularFeatureList) featureList;
      rowsToAdd.trimToSize();
      newFeatureList.setRowsApplySort(rowsToAdd);
      if (renumber) {
        for (int i = 0; i < rowsToAdd.size(); i++) {
          rowsToAdd.get(i).set(IDType.class, i + 1);
        }
      }
    } else {
      final String suffix = parameters.getValue(RowsFilterParameters.SUFFIX);
      // exact number of needed features and rows
      int totalRows = rowsToAdd.size();
      int totalFeatures = rowsToAdd.stream().mapToInt(FeatureListRow::getNumberOfFeatures).sum();

      newFeatureList = FeatureListUtils.createCopyWithoutRows(featureList, suffix,
          getMemoryMapStorage(), totalRows, totalFeatures);
      // add rows to new list
      for (int i = 0; i < rowsToAdd.size(); i++) {
        var row = rowsToAdd.get(i);
        FeatureListRow resetRow = new ModularFeatureListRow(newFeatureList,
            renumber ? i + 1 : row.getID(), (ModularFeatureListRow) row, true);
        newFeatureList.addRow(resetRow);
      }
    }

    // Add task description to featureList.
    newFeatureList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod(getTaskDescription(), RowsFilterModule.class, parameters,
            getModuleCallDate()));

    return newFeatureList;
  }

  private boolean isFilterRowCriteriaFailed(FeatureListRow row, int rowIndex, boolean hasMS2) {

    // Check ms2 filter .
    if (filterByMS2 && !hasMS2) {
      return true;
    }

    // Check number of features.
    final int featureCount = row.getNumberOfFeatures();
    if (minSamples != null) {
      if (!minSamples.matches(row)) {
        return true;
      }
    }
    if (minSamplesInGroup != null) {
      if (!minSamplesInGroup.matches(row)) {
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
      if (!foundText && !row.getCompoundAnnotations().isEmpty()) {
        if (CompoundAnnotationUtils.streamFeatureAnnotations(row)
            .map(FeatureAnnotation::getCompoundName).filter(Objects::nonNull)
            .anyMatch(name -> name.toLowerCase().trim().contains(searchText))) {
          foundText = true;
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

    // filter by correlated traces
    if (onlyWithOtherCorrelated) {
      boolean foundCorrelation = false;
      for (ModularFeature feature : row.getFeatures()) {
        if (feature.get(MsOtherCorrelationResultType.class) != null) {
          foundCorrelation = true;
          break;
        }
      }
      if (!foundCorrelation) {
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

    if (filterByMassDefect && !massDefectFilter.contains(row.getAverageMZ())) {
      return true;
    }

    if (cvFilter != null && !cvFilter.matches(row, rowIndex)) {
      return true;
    }

    if (significanceFoldChangeFilter != null && !significanceFoldChangeFilter.matches(rowIndex)) {
      return true;
    }

    return removeRedundantIsotopeRows && isRowRedundantDueToIsotopePattern(row,
        row.getBestIsotopePattern());
  }

  /**
   * @param row     The row
   * @param pattern An isotope pattern of that row
   * @return True if the row is not the most intense or first signal in that isotope pattern.
   */
  private boolean isRowRedundantDueToIsotopePattern(@NotNull FeatureListRow row,
      @Nullable final IsotopePattern pattern) {
    if (!removeRedundantIsotopeRows || pattern == null) {
      return false;
    }
    final int featureDpIndex = pattern.binarySearch(row.getAverageMZ(), DefaultTo.CLOSEST_VALUE);

    return featureDpIndex != 0 && featureDpIndex != Objects.requireNonNullElse(
        pattern.getBasePeakIndex(), -1);
  }
}
