/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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
