/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.project.impl;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.MZmineGUI;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.projectload.ProjectLoadModule;
import io.github.mzmine.modules.io.projectload.ProjectLoaderParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.ProjectManager;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import java.io.File;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Project manager implementation
 */
public class ProjectManagerImpl implements ProjectManager {

  private static final Logger logger = Logger.getLogger(ProjectManagerImpl.class.getName());
  private static final ProjectManagerImpl myInstance = new ProjectManagerImpl();

  private MZmineProject currentProject;

  private ProjectManagerImpl() {
    this.currentProject = new MZmineProjectImpl();
  }

  public static ProjectManagerImpl getInstance() {
    return myInstance;
  }

  @Override
  public @NotNull MZmineProject getCurrentProject() {
    return currentProject;
  }

  @Override
  public void setCurrentProject(@NotNull MZmineProject project) {

    if (project == currentProject) {
      return;
    }

    // Close previous data files
    if (currentProject != null) {
      RawDataFile[] prevDataFiles = currentProject.getDataFiles();
      for (RawDataFile prevDataFile : prevDataFiles) {
        prevDataFile.close();
      }
    }

    this.currentProject = project;

    // This is a hack to keep correct value of last opened directory (this
    // value was overwritten when configuration file was loaded from the new
    // project)
    if (project.getProjectFile() != null) {
      File projectFile = project.getProjectFile();
      ParameterSet loaderParams = MZmineCore.getConfiguration()
          .getModuleParameters(ProjectLoadModule.class);
      loaderParams.getParameter(ProjectLoaderParameters.projectFile).setValue(projectFile);
    }

    // Notify the GUI about project structure change
    if (!MZmineCore.isHeadLessMode()) {
      MZmineGUI.activateProject(project);
    }
  }

  @Override
  public void clearProject() {
    // Create a new, empty project
    MZmineProject old = getCurrentProject();
    setCurrentProject(new MZmineProjectImpl());
    // keep libraries
    currentProject.addSpectralLibrary(
        old.getCurrentSpectralLibraries().toArray(new SpectralLibrary[0]));
    logger.info("Project cleared");
  }

}
