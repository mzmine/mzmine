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
package io.github.mzmine.modules.io.export_features_msp;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.util.scans.ScanUtils.IntegerMode;
import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;

/**
 * @author Du-Lab Team <dulab.binf@gmail.com>
 */
public class AdapMspExportParameters extends SimpleParameterSet {

  private static final List<ExtensionFilter> extensions = List.of( //
      new ExtensionFilter("msp NIST library format", "*.msp") //
  );


  public static final FeatureListsParameter FEATURE_LISTS = new FeatureListsParameter();

  public static final FileNameParameter FILENAME = new FileNameParameter("Filename",
      "Name of the output MSP file. "
      + "Use pattern \"{}\" in the file name to substitute with feature list name. "
      + "(i.e. \"blah{}blah.msp\" would become \"blahSourceFeatureListNameblah.msp\"). "
      + "If the file already exists, it will be overwritten.",
      extensions, FileSelectionType.SAVE);

  public static final OptionalParameter<StringParameter> ADD_RET_TIME =
      new OptionalParameter<>(
          new StringParameter("Add retention time",
              "If selected, each MSP record will contain the feature's retention time", "RT"),
          true);

  public static final OptionalParameter<StringParameter> ADD_ANOVA_P_VALUE =
      new OptionalParameter<>(new StringParameter("Add ANOVA p-value (if calculated)",
          "If selected, each MSP record will contain the One-way ANOVA p-value (if calculated)",
          "ANOVA_P_VALUE"), true);

  public static final OptionalParameter<ComboParameter<IntegerMode>> INTEGER_MZ =
      new OptionalParameter<>(
          new ComboParameter<IntegerMode>("Integer m/z",
              "Merging mode for fractional m/z to unit mass", IntegerMode.values()),
          false);

  public AdapMspExportParameters() {
    super(new Parameter[] {FEATURE_LISTS, FILENAME, ADD_RET_TIME, ADD_ANOVA_P_VALUE, INTEGER_MZ});
  }
}
