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

package io.github.mzmine.modules.io.projectsave;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.ExitCode;
import java.io.File;
import java.util.Objects;

public class ProjectSaveParameters extends ProjectSaveAsParameters {

  public ProjectSaveParameters() {
    super();
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    // see if current project already has a location
    // otherwise use parent SaveAs
    final MZmineProject project = MZmineCore.getProjectManager().getCurrentProject();
    final File currentProjectFile = project.getProjectFile();

    if ((currentProjectFile != null) && (currentProjectFile.canWrite())) {
      final ProjectSaveOption projectType =
          Objects.requireNonNullElse(project.isStandalone(), true) ? ProjectSaveOption.STANDALONE
              : ProjectSaveOption.REFERENCING;

      setParameter(projectFile, currentProjectFile);
      setParameter(option, projectType);
      return ExitCode.OK;
    } else {
      return super.showSetupDialog(valueCheckRequired);
    }
  }
}
