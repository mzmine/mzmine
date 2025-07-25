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
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.MinimumSamplesInMetadataParameter;
import io.github.mzmine.parameters.parametertypes.MinimumSamplesParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.massdefect.MassDefectParameter;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import io.github.mzmine.parameters.parametertypes.ranges.IntRangeParameter;
import io.github.mzmine.parameters.parametertypes.ranges.MZRangeParameter;
import io.github.mzmine.parameters.parametertypes.ranges.RTRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RowsFilterParameters extends SimpleParameterSet {

  // general parameters
  public static final FeatureListsParameter FEATURE_LISTS = new FeatureListsParameter();

  public static final StringParameter SUFFIX = new StringParameter("Name suffix",
      "Suffix to be added to feature list name", "filtered");

  public static final OptionalParameter<MinimumSamplesParameter> MIN_FEATURE_COUNT = new OptionalParameter<>(
      new MinimumSamplesParameter(), false);

  public static final OptionalParameter<MinimumSamplesInMetadataParameter> MIN_FEATURE_IN_GROUP_COUNT = new OptionalParameter<>(
      new MinimumSamplesInMetadataParameter(), false);

  public static final OptionalParameter<IntegerParameter> MIN_ISOTOPE_PATTERN_COUNT = new OptionalParameter<>(
      new IntegerParameter("Minimum features in an isotope pattern",
          "Minimum number of features required in an isotope pattern", 2), false);

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
      new DoubleRangeParameter("features duration range",
          "Permissible range of (average) feature durations per row",
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

  public static final BooleanParameter MS2_Filter = new BooleanParameter("Feature with MS2 scan",
      "If checked, the rows that don't contain MS2 scan will be removed.", false);

  public static final BooleanParameter KEEP_ALL_MS2 = new BooleanParameter(
      "Never remove feature with MS2",
      "If checked, all rows with MS2 are retained without applying any further filters on them.",
      true);

  public static final BooleanParameter KEEP_ALL_ANNOTATED = new BooleanParameter(
      "Never remove annotated rows",
      "If checked, a feature that is annotated will never be removed from the feature list.",
      false);

  public static final BooleanParameter Reset_ID = new BooleanParameter(
      "Reset the feature number ID",
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
  // TODO maybe make the dialog similar to the preferences by grouping up parameters
  public RowsFilterParameters() {
    super(new Parameter[]{
            // general parameters
            FEATURE_LISTS, SUFFIX, REMOVE_ROW, handleOriginal,
            // sample filtering
            MIN_FEATURE_COUNT, MIN_FEATURE_IN_GROUP_COUNT, cvFilter, foldChangeFilter,
            // isotopes
            // TODO what does redundant do?
            MIN_ISOTOPE_PATTERN_COUNT, ISOTOPE_FILTER_13C, removeRedundantRows,
            // feature properties
            MZ_RANGE, RT_RANGE, FEATURE_DURATION, FWHM, CHARGE, massDefect, KENDRICK_MASS_DEFECT,
            // identities / annotations
            HAS_IDENTITIES, IDENTITY_TEXT, COMMENT_TEXT, MS2_Filter, onlyCorrelatedWithOtherDetectors,
            KEEP_ALL_MS2, KEEP_ALL_ANNOTATED, Reset_ID},
        "https://mzmine.github.io/mzmine_documentation/module_docs/feature_list_row_filter/feature_list_rows_filter.html");
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
    if (!loadedParams.containsKey(MIN_FEATURE_IN_GROUP_COUNT.getName())) {
      setParameter(MIN_FEATURE_IN_GROUP_COUNT, false);
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
    return map;
  }
}
