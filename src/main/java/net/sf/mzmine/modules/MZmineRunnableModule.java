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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules;

import java.util.Collection;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

/**
 * Interface representing a module that can be executed from the GUI through a
 * menu item. This can be either a data processing method (@see
 * MZmineProcessingModule) or a visualization/data analysis method.
 */
public interface MZmineRunnableModule extends MZmineModule {

    /**
     * Returns a brief module description for quick tooltips in the GUI
     * 
     * @return Module description
     */
    @Nonnull
    public String getDescription();

    /**
     * Run this module with given parameters. The module may create new Tasks
     * and add them to the 'tasks' collection. The module is not supposed to
     * submit the tasks to the TaskController by itself.
     * 
     * @param project
     *            Project to apply this module on.
     * @param parameters
     *            ParameterSet to invoke this module with. The ParameterSet has
     *            already been cloned for exclusive use by this module,
     *            therefore the module does not need to clone it again. Upon
     *            invocation of the runModule() method it is guaranteed that the
     *            ParameterSet is of the proper class as returned by
     *            getParameterSetClass(). Also, it is guaranteed that the
     *            ParameterSet is checked by checkParameters(), therefore the
     *            module does not need to perform these checks again.
     * @param tasks
     *            A collection where the module should add its newly created
     *            Tasks, if it creates any.
     * @return Exit code of the operation. ExitCode.OK means the module was
     *         started properly, however it does not guarantee that the Tasks
     *         will finish without error. ExitCode.ERROR means there was a
     *         problem starting the module.
     */
    @Nonnull
    public ExitCode runModule(@Nonnull MZmineProject project,
	    @Nonnull ParameterSet parameters, @Nonnull Collection<Task> tasks);

    /**
     * Returns the category of the module (e.g. raw data processing, peak
     * picking etc.). A menu item for this module will be created according to
     * the category.
     */
    @Nonnull
    public MZmineModuleCategory getModuleCategory();

}
