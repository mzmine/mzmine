/*
 * Copyright 2006-2019 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.alignment.icpspeciation;

import java.util.Collection;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineRunnableModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

public class IcpSpeciationAlignerModule implements MZmineRunnableModule {

  @Override
  public String getName() {
    // TODO Auto-generated method stub
    return "ICP speciation aligner";
  }

  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    // TODO Auto-generated method stub
    return IcpSpeciationAlignerParameters.class;
  }

  @Override
  public String getDescription() {
    return "Align icp and molecule ms data files by peak shape correlation.";
  }

  @Override
  public ExitCode runModule(MZmineProject project, ParameterSet parameters,
      Collection<Task> tasks) {
    Task task = new IcpSpeciationAlignerTask(project,  parameters);
    tasks.add(task);
    return ExitCode.OK;
  }

  @Override
  public MZmineModuleCategory getModuleCategory() {
    // TODO Auto-generated method stub
    return MZmineModuleCategory.ALIGNMENT;
  }

}
