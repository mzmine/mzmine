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

package io.github.mzmine.modules.tools.batchwizard_maldimsms;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNamesParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;

public class MaldiWizardParameters extends SimpleParameterSet {

  public static final DoubleParameter frameNoiseLevel = new DoubleParameter("Frame noise level",
      "The noise level for frames, equals the minimum intensity for a peak to be recognised as feature.",
      MZmineCore.getConfiguration().getIntensityFormat(), 1E3);

  public static final DoubleParameter mobilityScanNoiseLevel = new DoubleParameter(
      "Mobility scan noise level", "The noise level for mobility scans.",
      MZmineCore.getConfiguration().getIntensityFormat(), 1E2);

  public static final OptionalParameter<FileNameParameter> spotNameFile = new OptionalParameter<>(
      new FileNameParameter("Spot names",
          "Csv file file that contains semicolon separated columns called 'spot' and 'name' with the sample names.",
          List.of(new ExtensionFilter("csv", "*.csv")), FileSelectionType.OPEN));

  public static final OptionalParameter<FileNameParameter> compoundDbFile = new OptionalParameter<>(
      new FileNameParameter("Compound database",
          "Csv file file that contains semicolon separated columns called 'compound' and 'formula'.",
          List.of(new ExtensionFilter("csv", "*.csv")), FileSelectionType.OPEN));

  public static final FileNamesParameter importFiles = new FileNamesParameter("MS data files");

  public MaldiWizardParameters() {
    super(new Parameter[]{frameNoiseLevel, mobilityScanNoiseLevel, spotNameFile, compoundDbFile,
        importFiles});
  }
}
