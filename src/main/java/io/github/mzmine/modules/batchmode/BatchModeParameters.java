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

package io.github.mzmine.modules.batchmode;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.AdvancedParametersParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameListSilentParameter;
import io.github.mzmine.util.ExitCode;

public class BatchModeParameters extends SimpleParameterSet {

  public static final FileNameListSilentParameter lastFiles = new FileNameListSilentParameter(
      "Last used files");
  public static final BatchQueueParameter batchQueue = new BatchQueueParameter();

  public static final AdvancedParametersParameter<AdvancedBatchModeParameters> advanced = new AdvancedParametersParameter<>(
      new AdvancedBatchModeParameters());

  public BatchModeParameters() {
    super(new Parameter[]{batchQueue, advanced, lastFiles});
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    BatchModeParameterSetupDialog dialog = new BatchModeParameterSetupDialog(this);
    dialog.showAndWait();
    return dialog.getExitCode();
  }
}
