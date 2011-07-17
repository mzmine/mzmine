/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.io.xmlexport;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;

public class XMLExportModule implements MZmineProcessingModule {

	public static final String MODULE_NAME = "Export to XML file";

	private XMLExportParameters parameters = new XMLExportParameters();

	public ParameterSet getParameterSet() {
		return parameters;
	}

	public Task[] runModule(ParameterSet parameters) {

		XMLExportTask task = new XMLExportTask(parameters);

		MZmineCore.getTaskController().addTask(task);

		return new Task[] { task };

	}

	public MZmineModuleCategory getModuleCategory() {
		return MZmineModuleCategory.PEAKLISTEXPORT;
	}

	public String toString() {
		return MODULE_NAME;
	}

}
