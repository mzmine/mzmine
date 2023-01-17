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

package io.github.mzmine.modules.io.export_msmsquality;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;

public class MsMsQualityExportParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flists = new FeatureListsParameter();

  public static final FileNameParameter file = new FileNameParameter("Export File",
      "File to put the quality analysis files in.", List.of(new ExtensionFilter("csv", "*.csv")),
      FileSelectionType.SAVE);

  public static final BooleanParameter onlyCompoundAnnotated = new BooleanParameter(
      "Only compound matches", "Only export MSMS spectra from features with compound matches",
      true);

  public static final MZToleranceParameter formulaTolerance = new MZToleranceParameter(
      "Sub formula m/z tolerance", "Tolerance for scoring MS/MS peaks with sub formulas.", 0.003,
      10);

  public static final BooleanParameter matchCompoundNameToFlist = new BooleanParameter(
      "Match compound name to feature list name",
      "Only MS2 spectra whose compound name is contained in the feature lsit name are exported.");

  public MsMsQualityExportParameters() {
    super(new Parameter[]{flists, file, onlyCompoundAnnotated, formulaTolerance,
        matchCompoundNameToFlist});
  }
}
