/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.modules.dataprocessing.group_metacorrelate.test;

import io.github.mzmine.main.MZmineCore;
import java.io.File;

public class TestMSEGroupingModule {

  public static void main(String[] args) {
    // TODO Auto-generated method stub
    MZmineCore core = new MZmineCore();
    core.main(args);

    // sample project
    File file = new File("D://Daten/RAW/Master/STC1_test.mzmine");
    // open project
    ProjectLoaderParameters par = new ProjectLoaderParameters();
    par.getParameter(ProjectLoaderParameters.projectFile).setValue(file);
    ProjectOpeningTask newTask = new ProjectOpeningTask(par);
    core.getTaskController().addTask(newTask);
  }

}
