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

package io.github.mzmine.modules.batchmode;

import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameListSilentParameter;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

public class BatchModeParameterSetupDialog extends ParameterSetupDialog {

  private static final Logger logger = Logger.getLogger(
      BatchModeParameterSetupDialog.class.getName());
  private final BatchComponentController controller;

  public BatchModeParameterSetupDialog(final ParameterSet parameters) {
    super(true, parameters);
    BatchQueueParameter batchQueue = parameters.getParameter(BatchModeParameters.batchQueue);
    FileNameListSilentParameter lastFiles = parameters.getParameter(BatchModeParameters.lastFiles);

    controller = batchQueue.getController();
    controller.setLastFiles(lastFiles.getValue());

    paramPane.setAskApplyParameterSet(presetParams -> {
      final var queue = presetParams.getParameter(BatchModeParameters.batchQueue);
      controller.askApplyBatchQueue(queue.getValue(), queue.getLastParsingErrorMessages());
    });

    super.addCheckParametersButton();
  }

  public void loadFile(File file) {
    try {
      controller.loadBatchSteps(file);
    } catch (ParserConfigurationException | IOException | SAXException e) {
      logger.log(Level.WARNING, "Error in loading batch: " + e.getMessage(), e);
    }
  }

}
