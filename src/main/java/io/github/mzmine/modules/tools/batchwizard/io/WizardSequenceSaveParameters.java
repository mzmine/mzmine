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

package io.github.mzmine.modules.tools.batchwizard.io;

import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.MultiChoiceParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.filenames.DirectoryParameter;
import java.io.File;
import java.util.Optional;

public class WizardSequenceSaveParameters extends SimpleParameterSet {

  public static final DirectoryParameter directory = new DirectoryParameter("Directory",
      "The default preset directory is the user folder/.mzmine/wizard/",
      Optional.ofNullable(WizardSequenceIOUtils.getWizardSettingsPath()).map(File::getAbsolutePath)
          .orElse(""));

  public static final StringParameter fileName = new StringParameter("Filename", "", "");

  public static final MultiChoiceParameter<WizardPart> exportParts = new MultiChoiceParameter<>(
      "Export", """
      Parts to export. This way the different instruments and methods for LC, GC, MS, ...
      can be defined and combined later. When importing a preset that only defines some parts of the workflow,
      the rest of the workflow is kept at the current definitions in the UI.""",
      WizardPart.values(), WizardPart.values());

  public WizardSequenceSaveParameters() {
    super(directory, fileName, exportParts);
  }

}
