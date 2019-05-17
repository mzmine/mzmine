/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.batchmode;

import java.awt.Window;
import java.util.logging.Logger;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameListComponent;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameListParameter;
import net.sf.mzmine.util.ExitCode;

public class BatchModeParameters extends SimpleParameterSet {
  // Logger.
  private static final Logger LOG = Logger.getLogger(BatchModeParameters.class.getName());

  public static final FileNameListParameter lastFiles =
      new FileNameListParameter("Last used files", "Last used batch files");
  public static final BatchQueueParameter batchQueue = new BatchQueueParameter();

  public BatchModeParameters() {
    super(new Parameter[] {batchQueue, lastFiles});
  }

  @Override
  public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {
    ParameterSetupDialog dialog = new ParameterSetupDialog(parent, valueCheckRequired, this);
    //
    final BatchSetupComponent batchComponent =
        ((BatchSetupComponent) dialog.getComponentForParameter(batchQueue));

    // listen for clicks on a last used file
    // load this file to batch
    FileNameListComponent lastUsedFilesComponent =
        ((FileNameListComponent) dialog.getComponentForParameter(lastFiles));

    // listen for load/save and add to last used files
    batchComponent.setLastUsedFilesComponent(lastUsedFilesComponent);

    dialog.setVisible(true);
    return dialog.getExitCode();
  }
}
