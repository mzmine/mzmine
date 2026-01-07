/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.batchmode.change_outfiles;

import static io.github.mzmine.javafx.components.factories.FxTexts.text;

import io.github.mzmine.javafx.components.factories.FxTextFlows;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameSuffixExportParameter;
import io.github.mzmine.util.ExitCode;
import javafx.application.Platform;
import javafx.scene.layout.Region;

/**
 * Parameters to change all output files at once
 */
public class ChangeOutputFilesParameters extends SimpleParameterSet {

  public static final FileNameSuffixExportParameter outBaseFile = new FileNameSuffixExportParameter(
      "Base output filename", """
      Defines the base output file path and name without any format.
      This will be used to change all output files by adding module specific suffixes.""");

  public ChangeOutputFilesParameters() {
    super(outBaseFile);
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    assert Platform.isFxApplicationThread();

    final Region message = FxTextFlows.newTextFlowInAccordion("Change all output filenames", true,
        text("""
            Define a new output file path and base filename without any format to change all steps that export files. \
            Make sure to select a fully qualified file path. After pressing ok, all export file names will change to the \
            base filename with module specific suffixes added."""));

    ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired, this, message);
    dialog.showAndWait();
    return dialog.getExitCode();
  }
}
