/*
 * Copyright 2006-2021 The MZmine Development Team
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
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
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
import io.github.mzmine.util.ExitCode;
import java.text.DecimalFormat;

public class RowsFilterParameters extends SimpleParameterSet {

  public static final String defaultGrouping = "No parameters defined";

  public static final FeatureListsParameter FEATURE_LISTS = new FeatureListsParameter();

  public static final StringParameter SUFFIX = new StringParameter("Name suffix",
      "Suffix to be added to feature list name", "filtered");

  public static final OptionalParameter<DoubleParameter> MIN_FEATURE_COUNT = new OptionalParameter<>(
      new DoubleParameter("Minimum features in a row (abs or %)",
          "Minimum number of feature detections required per row.\nValues <1 will be "
              + "interpreted as a %-value of the total # samples in the feature list. The value will be rounded down to the nearest whole number.",
          new DecimalFormat("0.000"), 0.1), false);

  public static final OptionalParameter<IntegerParameter> MIN_ISOTOPE_PATTERN_COUNT = new OptionalParameter<>(
      new IntegerParameter("Minimum features in an isotope pattern",
          "Minimum number of features required in an isotope pattern", 2), false);

  public static final OptionalModuleParameter<Isotope13CFilterParameters> ISOTOPE_FILTER_13C = new OptionalModuleParameter<>(
      "Validate 13C isotope pattern",
      "Searches for an +1 13C signal (considering possible charge states) \n"
      + "within estimated range of carbon atoms. Optionally: Detect and filter rows \n"
      + "that are 13C isotopes by searching for preceding -1 signal.",
      new Isotope13CFilterParameters(), false);

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
  public static final ComboParameter<Object> GROUPSPARAMETER = new ComboParameter<Object>(
      "Parameter", "Paremeter defining the group of each sample.", new Object[]{defaultGrouping},
      defaultGrouping);

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


  public static final OriginalFeatureListHandlingParameter handleOriginal = new OriginalFeatureListHandlingParameter(
      true);

  public static final BooleanParameter MS2_Filter = new BooleanParameter("Feature with MS2 scan",
      "If checked, the rows that don't contain MS2 scan will be removed.", false);
  public static final BooleanParameter KEEP_ALL_MS2 = new BooleanParameter(
      "Never remove feature with MS2",
      "If checked, all rows with MS2 are retained without applying any further filters on them.",
      true);

  public static final BooleanParameter Reset_ID = new BooleanParameter(
      "Reset the feature number ID",
      "If checked, the row number of original feature list will be reset.", false);


  public static final OptionalParameter<MassDefectParameter> massDefect = new OptionalParameter<>(
      new MassDefectParameter("Mass defect",
          "Filters for mass defects of features.\nValid inputs: 0.314-0.5 or 0.90-0.15",
          MZmineCore.getConfiguration().getMZFormat()));

  public RowsFilterParameters() {
    super(new Parameter[]{FEATURE_LISTS, SUFFIX, MIN_FEATURE_COUNT, MIN_ISOTOPE_PATTERN_COUNT,
        ISOTOPE_FILTER_13C, MZ_RANGE, RT_RANGE, FEATURE_DURATION, FWHM, CHARGE,
        KENDRICK_MASS_DEFECT, GROUPSPARAMETER, HAS_IDENTITIES, IDENTITY_TEXT, COMMENT_TEXT,
        REMOVE_ROW, MS2_Filter, KEEP_ALL_MS2, Reset_ID, massDefect, handleOriginal},
        "https://mzmine.github.io/mzmine_documentation/module_docs/feature_list_row_filter/feature_list_rows_filter.html");
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {

    // Update the parameter choices
    UserParameter<?, ?> newChoices[] = MZmineCore.getProjectManager().getCurrentProject()
        .getParameters();
    String[] choices;
    if (newChoices == null || newChoices.length == 0) {
      choices = new String[1];
      choices[0] = defaultGrouping;
    } else {
      choices = new String[newChoices.length + 1];
      choices[0] = "Ignore groups";
      for (int i = 0; i < newChoices.length; i++) {
        choices[i + 1] = "Filtering by " + newChoices[i].getName();
      }
    }

    getParameter(RowsFilterParameters.GROUPSPARAMETER).setChoices(choices);
    ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired, this);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

}
