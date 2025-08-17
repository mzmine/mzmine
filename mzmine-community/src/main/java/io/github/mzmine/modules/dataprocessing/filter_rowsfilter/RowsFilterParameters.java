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
import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataanalysis.significance.SignificanceTests;
import io.github.mzmine.modules.dataanalysis.utils.imputation.ImputationFunctions;
import io.github.mzmine.modules.visualization.projectmetadata.SampleType;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.dialogs.GroupedParameterSetupDialog;
import io.github.mzmine.parameters.dialogs.GroupedParameterSetupPane.GroupView;
import io.github.mzmine.parameters.dialogs.GroupedParameterSetupPane.ParameterGroup;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.MinimumSamplesFilterConfig;
import io.github.mzmine.parameters.parametertypes.MinimumSamplesInAnyMetadataGroupParameter;
import io.github.mzmine.parameters.parametertypes.MinimumSamplesInOneMetadataGroupParameter;
import io.github.mzmine.parameters.parametertypes.MinimumSamplesParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.absoluterelative.AbsoluteAndRelativeInt;
import io.github.mzmine.parameters.parametertypes.absoluterelative.AbsoluteAndRelativeInt.Mode;
import io.github.mzmine.parameters.parametertypes.massdefect.MassDefectParameter;
import io.github.mzmine.parameters.parametertypes.metadata.Metadata2GroupsSelection;
import io.github.mzmine.parameters.parametertypes.metadata.MetadataGroupSelection;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import io.github.mzmine.parameters.parametertypes.ranges.IntRangeParameter;
import io.github.mzmine.parameters.parametertypes.ranges.MZRangeParameter;
import io.github.mzmine.parameters.parametertypes.ranges.RTRangeParameter;
import io.github.mzmine.parameters.parametertypes.row_type_filter.RowTypeFilterParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelectionType;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.ExitCode;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.Element;

public class RowsFilterParameters extends SimpleParameterSet {

  // general parameters
  public static final FeatureListsParameter FEATURE_LISTS = new FeatureListsParameter();

  public static final StringParameter SUFFIX = new StringParameter("Name suffix",
      "Suffix to be added to feature list name", "filtered");

  public static final OptionalParameter<MinimumSamplesParameter> MIN_FEATURE_COUNT = new OptionalParameter<>(
      new MinimumSamplesParameter(), false);

  public static final OptionalParameter<MinimumSamplesInAnyMetadataGroupParameter> MIN_FEATURE_IN_GROUP_COUNT = new OptionalParameter<>(
      new MinimumSamplesInAnyMetadataGroupParameter(), false);

  public static final OptionalParameter<MinimumSamplesInOneMetadataGroupParameter> MIN_FEATURE_IN_ONE_GROUP_COUNT = new OptionalParameter<>(
      new MinimumSamplesInOneMetadataGroupParameter(), false);

  public static final OptionalParameter<IntegerParameter> MIN_ISOTOPE_PATTERN_COUNT = new OptionalParameter<>(
      new IntegerParameter("Minimum signals in an isotope pattern",
          "Minimum number of detected signals required in an isotope pattern", 2), false);

  public static final OptionalModuleParameter<Isotope13CFilterParameters> ISOTOPE_FILTER_13C = new OptionalModuleParameter<>(
      "Validate 13C isotope pattern",
      "Searches for an +1 13C signal (considering possible charge states) \n"
          + "within estimated range of carbon atoms. Optionally: Detect and filter rows \n"
          + "that are 13C isotopes by searching for preceding -1 signal.",
      new Isotope13CFilterParameters(), false);

  public static final BooleanParameter removeRedundantRows = new BooleanParameter(
      "Remove redundant isotope rows",
      "Removes rows that are not the most intense or the monoisotopic peak in an isotope pattern.",
      false);

  public static final OptionalParameter<MZRangeParameter> MZ_RANGE = new OptionalParameter<>(
      new MZRangeParameter(), false);

  public static final OptionalParameter<RTRangeParameter> RT_RANGE = new OptionalParameter<>(
      new RTRangeParameter(), false);

  public static final OptionalParameter<DoubleRangeParameter> FEATURE_DURATION = new OptionalParameter<>(
      new DoubleRangeParameter("Chromatographic width",
          "Permissible range of (average) row retention time widths. The full width including all detected data points.",
          MZmineCore.getConfiguration().getRTFormat(), Range.closed(0.0, 3d)), false);

  public static final OptionalParameter<DoubleRangeParameter> FWHM = new OptionalParameter<>(
      new DoubleRangeParameter("Chromatographic FWHM",
          "Permissible range of chromatographic FWHM per row",
          MZmineCore.getConfiguration().getRTFormat(), Range.closed(0.0, 1.0)), false);
  public static final OptionalParameter<IntRangeParameter> CHARGE = new OptionalParameter<>(
      new IntRangeParameter("Charge", "Filter by charge, run isotopic features grouper first", true,
          Range.closed(1, 2)), false);

  public static final OptionalModuleParameter<KendrickMassDefectFilterParameters> KENDRICK_MASS_DEFECT = new OptionalModuleParameter<>(
      "Kendrick mass defect", "Permissible range of a Kendrick mass defect per row",
      new KendrickMassDefectFilterParameters(), false);

  public static final BooleanParameter HAS_IDENTITIES = new BooleanParameter("Only identified?",
      "Select to filter only identified compounds", false);

  public static final OptionalParameter<StringParameter> IDENTITY_TEXT = new OptionalParameter<>(
      new StringParameter("Text in identity",
          "Only rows that contain this text in their feature identity field will be retained.", ""),
      false);

  public static final OptionalParameter<StringParameter> COMMENT_TEXT = new OptionalParameter<>(
      new StringParameter("Text in comment",
          "Only rows that contain this text in their comment field will be retained.", ""), false);

  public static final ComboParameter<RowsFilterChoices> REMOVE_ROW = new ComboParameter<>(
      "Keep or remove rows", "If selected, rows will be removed based on criteria instead of kept",
      RowsFilterChoices.values(), RowsFilterChoices.KEEP_MATCHING);

  public static final OptionalModuleParameter<RsdFilterParameters> cvFilter = new OptionalModuleParameter<>(
      "RSD filter",
      "Filter rows based on relative standard deviation (coefficient of variation, CV) in a specific sample group.",
      (RsdFilterParameters) new RsdFilterParameters().cloneParameterSet(), false);

  public static final OptionalModuleParameter<FoldChangeSignificanceRowFilterParameters> foldChangeFilter = new OptionalModuleParameter<>(
      "Significance/fold-change filter",
      "Filter that works similar to the volcano plot on both the significance and fold-change.",
      new FoldChangeSignificanceRowFilterParameters(), false);

  public static final OriginalFeatureListHandlingParameter handleOriginal = new OriginalFeatureListHandlingParameter(
      true);

  public static final BooleanParameter MS2_Filter = new BooleanParameter("Require MS2 scan",
      "If checked, the rows that don't contain MS2 scan will be removed.", false);

  public static final BooleanParameter KEEP_ALL_MS2 = new BooleanParameter(
      "Never remove rows with MS2",
      "If checked, all rows with MS2 are retained without applying any further filters on them.",
      true);

  public static final OptionalParameter<RowTypeFilterParameter> ROW_TYPE_FILTER = new OptionalParameter<>(
      new RowTypeFilterParameter());

  public static final BooleanParameter KEEP_ALL_ANNOTATED = new BooleanParameter(
      "Never remove annotated rows",
      "If checked, a feature that is annotated will never be removed from the feature list.",
      false);

  public static final BooleanParameter Reset_ID = new BooleanParameter("Reset the row ID",
      "If checked, the row number of original feature list will be reset.", false);

  public static final OptionalParameter<MassDefectParameter> massDefect = new OptionalParameter<>(
      new MassDefectParameter("Mass defect",
          "Filters for mass defects of features.\nValid inputs: 0.314-0.5 or 0.90-0.15",
          MZmineCore.getConfiguration().getMZFormat()));

  public static final BooleanParameter onlyCorrelatedWithOtherDetectors = new BooleanParameter(
      "Require other detector correlation",
      "If checked, the rows that do not have at least one feature that is correlated to a signal of another detector will be removed.",
      false);

  // resorted parameters to be more grouped
  public RowsFilterParameters() {
    super(new Parameter[]{
            // general parameters
            FEATURE_LISTS, SUFFIX, REMOVE_ROW, handleOriginal,
            // sample filtering
            MIN_FEATURE_COUNT, MIN_FEATURE_IN_GROUP_COUNT, MIN_FEATURE_IN_ONE_GROUP_COUNT, cvFilter,
            foldChangeFilter,
            // isotopes
            // TODO what does redundant do?
            MIN_ISOTOPE_PATTERN_COUNT, ISOTOPE_FILTER_13C, removeRedundantRows,
            // feature properties
            MZ_RANGE, RT_RANGE, FEATURE_DURATION, FWHM, CHARGE, massDefect, KENDRICK_MASS_DEFECT,
            // identities / annotations
            ROW_TYPE_FILTER, HAS_IDENTITIES, IDENTITY_TEXT, COMMENT_TEXT, MS2_Filter,
            onlyCorrelatedWithOtherDetectors, KEEP_ALL_MS2, KEEP_ALL_ANNOTATED, Reset_ID},
        "https://mzmine.github.io/mzmine_documentation/module_docs/feature_list_row_filter/feature_list_rows_filter.html");
  }


  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    return showSetupDialog(valueCheckRequired, "");
  }

  public ExitCode showSetupDialog(boolean valueCheckRequired, @Nullable String filterParameters) {
    assert Platform.isFxApplicationThread();

    final List<UserParameter<?, ? extends Region>> fixed = List.of(FEATURE_LISTS, SUFFIX,
        REMOVE_ROW, handleOriginal);

    final List<ParameterGroup> groups = List.of( //
        new ParameterGroup("Sample-based filters", MIN_FEATURE_COUNT, MIN_FEATURE_IN_GROUP_COUNT,
            MIN_FEATURE_IN_ONE_GROUP_COUNT, cvFilter, foldChangeFilter), //
        new ParameterGroup("Isotope filters", MIN_ISOTOPE_PATTERN_COUNT, ISOTOPE_FILTER_13C,
            removeRedundantRows), //
        new ParameterGroup("Feature properties", MZ_RANGE, RT_RANGE, FEATURE_DURATION, FWHM, CHARGE,
            massDefect, KENDRICK_MASS_DEFECT), //
        new ParameterGroup("Annotations & MS2 filter", KEEP_ALL_MS2, MS2_Filter, KEEP_ALL_ANNOTATED,
            ROW_TYPE_FILTER, HAS_IDENTITIES, IDENTITY_TEXT, COMMENT_TEXT), //
        new ParameterGroup("Other options", onlyCorrelatedWithOtherDetectors, Reset_ID) //
    );

    GroupedParameterSetupDialog dialog = new GroupedParameterSetupDialog(valueCheckRequired, this,
        true, fixed, groups, GroupView.GROUPED);

    // add groups
    dialog.setFilterText(filterParameters);

    dialog.setWidth(800);
    dialog.setHeight(800);

    // check
    dialog.showAndWait();
    return dialog.getExitCode();
  }


  /**
   * A default parameter set with all options off
   */
  public static ParameterSet createDefaultAllOff() {
    final ParameterSet param = new RowsFilterParameters().cloneParameterSet();
    param.setParameter(RowsFilterParameters.FEATURE_LISTS,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(RowsFilterParameters.SUFFIX, "row_filtered");
    param.setParameter(RowsFilterParameters.MIN_FEATURE_COUNT, false,
        new AbsoluteAndRelativeInt(1, 0, Mode.ROUND_DOWN));
    param.setParameter(RowsFilterParameters.MIN_FEATURE_IN_GROUP_COUNT, false,
        MinimumSamplesFilterConfig.DEFAULT);
    // just set the filter to false and allow to use the recent parameters internally
    param.setParameter(RowsFilterParameters.MIN_FEATURE_IN_ONE_GROUP_COUNT, false);

    param.setParameter(RowsFilterParameters.MIN_ISOTOPE_PATTERN_COUNT, false);
    param.setParameter(RowsFilterParameters.ISOTOPE_FILTER_13C, false);

    final Isotope13CFilterParameters filterIsoParam = param.getParameter(
        RowsFilterParameters.ISOTOPE_FILTER_13C).getEmbeddedParameters();
    filterIsoParam.setParameter(Isotope13CFilterParameters.mzTolerance,
        MZTolerance.FIFTEEN_PPM_OR_FIVE_MDA);
    filterIsoParam.setParameter(Isotope13CFilterParameters.maxCharge, 2);
    filterIsoParam.setParameter(Isotope13CFilterParameters.applyMinCEstimation, true);
    filterIsoParam.setParameter(Isotope13CFilterParameters.removeIfMainIs13CIsotope, true);
    filterIsoParam.setParameter(Isotope13CFilterParameters.elements, List.of(new Element("O")));

    param.setParameter(RowsFilterParameters.cvFilter, false);
    final RsdFilterParameters cvFilter = param.getParameter(RowsFilterParameters.cvFilter)
        .getEmbeddedParameters();
    cvFilter.setAll(AbundanceMeasure.Area, ImputationFunctions.GLOBAL_LIMIT_OF_DETECTION, 0.2, 0.2,
        false,
        new MetadataGroupSelection(MetadataColumn.SAMPLE_TYPE_HEADER, SampleType.QC.toString()));

    param.setParameter(RowsFilterParameters.foldChangeFilter, false);
    final FoldChangeSignificanceRowFilterParameters fcParams = param.getParameter(
        RowsFilterParameters.foldChangeFilter).getEmbeddedParameters();

    fcParams.setAll(AbundanceMeasure.Area, ImputationFunctions.GLOBAL_LIMIT_OF_DETECTION,
        Metadata2GroupsSelection.NONE, SignificanceTests.WELCHS_T_TEST, 0.05, 1d,
        FoldChangeFilterSides.ABS_BOTH_SIDES);

    //
    param.setParameter(RowsFilterParameters.removeRedundantRows, false);
    param.setParameter(RowsFilterParameters.MZ_RANGE, false);
    param.setParameter(RowsFilterParameters.RT_RANGE, false);
    param.setParameter(RowsFilterParameters.FEATURE_DURATION, false);
    param.setParameter(RowsFilterParameters.FWHM, false);
    param.setParameter(RowsFilterParameters.CHARGE, false);
    param.setParameter(RowsFilterParameters.KENDRICK_MASS_DEFECT, false);
    param.setParameter(RowsFilterParameters.HAS_IDENTITIES, false);
    param.setParameter(RowsFilterParameters.IDENTITY_TEXT, false);
    param.setParameter(RowsFilterParameters.COMMENT_TEXT, false);
    param.setParameter(RowsFilterParameters.REMOVE_ROW, RowsFilterChoices.KEEP_MATCHING);
    param.setParameter(RowsFilterParameters.MS2_Filter, false);
    param.setParameter(RowsFilterParameters.KEEP_ALL_MS2, true);
    param.setParameter(RowsFilterParameters.KEEP_ALL_ANNOTATED, false);
    param.setParameter(RowsFilterParameters.ROW_TYPE_FILTER, false);
    param.setParameter(RowsFilterParameters.Reset_ID, false);
    param.setParameter(RowsFilterParameters.massDefect, false);
    param.setParameter(RowsFilterParameters.onlyCorrelatedWithOtherDetectors, false);
    param.setParameter(RowsFilterParameters.handleOriginal, OriginalFeatureListOption.KEEP);
    return param;
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }

  @Override
  public @Nullable String getVersionMessage(int version) {
    return switch (version) {
      case 3 -> """
          "%s" has changed internally. Missing value imputation was added.
          "%s" was added as an additional filtering option.""".formatted(cvFilter.getName(),
          foldChangeFilter.getName());
      default -> null;
    };
  }

  @Override
  public int getVersion() {
    return 3;
  }

  @Override
  public void handleLoadedParameters(Map<String, Parameter<?>> loadedParams, int loadedVersion) {
    super.handleLoadedParameters(loadedParams, loadedVersion);

    // deactivate new parameter that may not be available
    if (!loadedParams.containsKey(ROW_TYPE_FILTER.getName())) {
      setParameter(ROW_TYPE_FILTER, false);
    }
    if (!loadedParams.containsKey(MIN_FEATURE_IN_GROUP_COUNT.getName())) {
      setParameter(MIN_FEATURE_IN_GROUP_COUNT, false);
    }
    if (!loadedParams.containsKey(MIN_FEATURE_IN_ONE_GROUP_COUNT.getName())) {
      setParameter(MIN_FEATURE_IN_ONE_GROUP_COUNT, false);
    }
    if (!loadedParams.containsKey(cvFilter.getName())) {
      setParameter(cvFilter, false);
    }
    if (!loadedParams.containsKey(foldChangeFilter.getName())) {
      setParameter(foldChangeFilter, false);
    }
  }

  @Override
  public Map<String, Parameter<?>> getNameParameterMap() {
    var map = super.getNameParameterMap();
    map.put("Only other detector correlated", getParameter(onlyCorrelatedWithOtherDetectors));
    map.put("Minimum aligned features (samples)", getParameter(MIN_FEATURE_COUNT));
    map.put("Reset the feature number ID", getParameter(Reset_ID));
    map.put("Never remove feature with MS2", getParameter(KEEP_ALL_MS2));
    map.put("Feature with MS2 scan", getParameter(MS2_Filter));
    map.put("Minimum features in an isotope pattern", getParameter(MIN_ISOTOPE_PATTERN_COUNT));
    map.put("features duration range", getParameter(FEATURE_DURATION));
    return map;
  }
}
