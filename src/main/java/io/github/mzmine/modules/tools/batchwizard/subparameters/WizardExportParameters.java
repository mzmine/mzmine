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

package io.github.mzmine.modules.tools.batchwizard.subparameters;

import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;

public class WizardExportParameters extends SimpleParameterSet {

  public static final BooleanParameter exportSirius = new BooleanParameter("Export for SIRIUS", "",
      true);
  public static final BooleanParameter exportGnps = new BooleanParameter(
      "Export for GNPS FBMN/IIMN",
      "Export to Feature-based Molecular Networking (FBMN) and Ion Identity Molecular Networking (IIMN) on GNPS",
      true);

  public static final OptionalParameter<FileNameParameter> exportPath = new OptionalParameter<>(
      new FileNameParameter("Export path",
          "If checked, export results for different tools, e.g., GNPS IIMN, SIRIUS, ...",
          FileSelectionType.SAVE, false), false);

  /**
   * the part category of presets - is used in all wizard parameter classes
   */
  public static final WizardPartParameter wizardPartCategory = new WizardPartParameter(
      WizardPart.EXPORT);

  public WizardExportParameters() {
    super(new Parameter[]{wizardPartCategory, exportPath, exportGnps, exportSirius});
  }
}
