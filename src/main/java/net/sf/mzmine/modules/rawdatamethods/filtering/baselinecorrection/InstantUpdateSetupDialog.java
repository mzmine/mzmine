/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */
package net.sf.mzmine.modules.rawdatamethods.filtering.baselinecorrection;

import java.awt.Window;

import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;

/**
 * @description Same as "ParameterSetupDialog", but any change on the parameters
 *              is immediately recorded to the related ParameterSet, such that
 *              those parameters can be obtained at any time, even before having
 *              started running the module.
 * 
 */
@SuppressWarnings("serial")
public class InstantUpdateSetupDialog extends ParameterSetupDialog {

    public InstantUpdateSetupDialog(Window parent, boolean valueCheckRequired,
	    ParameterSet parameters) {
	super(parent, valueCheckRequired, parameters);
    }

    @Override
    protected void parametersChanged() {
	this.updateParameterSetFromComponents();
    }

}
