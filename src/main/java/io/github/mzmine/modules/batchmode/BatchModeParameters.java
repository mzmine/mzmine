/*
 * Copyright 2006-2020 The MZmine Development Team
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

package io.github.mzmine.modules.batchmode;

import java.awt.Window;
import java.util.logging.Logger;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameListSilentParameter;
import io.github.mzmine.util.ExitCode;

public class BatchModeParameters extends SimpleParameterSet {
    // Logger.
    private static final Logger LOG = Logger
            .getLogger(BatchModeParameters.class.getName());

    public static final FileNameListSilentParameter lastFiles = new FileNameListSilentParameter(
            "Last used files");
    public static final BatchQueueParameter batchQueue = new BatchQueueParameter();

    public BatchModeParameters() {
        super(new Parameter[] { batchQueue, lastFiles });
    }

    @Override
    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {
        ParameterSetupDialog dialog = new ParameterSetupDialog(parent,
                valueCheckRequired, this);
        // set lastUsed files list
        final BatchSetupComponent batchComponent = ((BatchSetupComponent) dialog
                .getComponentForParameter(batchQueue));

        // new last used files are inserted to this list in the component
        batchComponent.setLastFiles(lastFiles.getValue());

        dialog.setVisible(true);
        return dialog.getExitCode();
    }
}
