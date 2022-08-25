/*
 * Copyright 2006-2022 The MZmine Development Team
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

package io.github.mzmine.modules.batchmode;

import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameListSilentParameter;

public class BatchModeParameterSetupDialog extends ParameterSetupDialog {

  public BatchModeParameterSetupDialog(final ParameterSet parameters) {
    super(true, parameters);
    BatchQueueParameter batchQueue = parameters.getParameter(BatchModeParameters.batchQueue);
    FileNameListSilentParameter lastFiles = parameters.getParameter(BatchModeParameters.lastFiles);

    BatchComponentController controller = batchQueue.getController();
    controller.setLastFiles(lastFiles.getValue());
  }

}
