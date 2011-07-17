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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules;

import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;

/**
 * Interface representing a data processing method which can be executed in a
 * batch
 */
public interface MZmineProcessingModule extends MZmineModule {

	/**
	 * Runs this method on a given items, and calls another task listener after
	 * task is complete and results have been processed.
	 * 
	 */
	public Task[] runModule(ParameterSet parameters);

	/**
	 * Returns the category of the module (e.g. raw data processing, peak
	 * picking etc.). Menu item will be created according to the category.
	 */
	public MZmineModuleCategory getModuleCategory();

}
