/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

import static io.github.mzmine.javafx.components.factories.FxTexts.boldText;
import static io.github.mzmine.javafx.components.factories.FxTexts.linebreak;
import static io.github.mzmine.javafx.components.factories.FxTexts.text;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.javafx.components.factories.FxTextFlows;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameSuffixExportParameter;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.files.ExtensionFilters;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser.ExtensionFilter;

public class ProjectSaveAsParameters extends SimpleParameterSet {

  public static final List<ExtensionFilter> extensions = List.of(ExtensionFilters.MZ_PROJECT,
      ExtensionFilters.ALL_FILES);
  public static final ComboParameter<ProjectSaveOption> option = new ComboParameter<>(
      "Project type", """
      Referencing projects point to the original directory of raw data files as absolute and 
      relative paths.  Projects can be shared if the raw files are located in exactly the same absolute 
      path (e.g. starting with C:\\\\...) or the same path relative to the project file (e.g. saving the
      project in the same folder as the raw files will achieve a portable project - requires mzmine 4.3 or higher). 
      
      Standalone copies the raw data files into the project, 
      creating a larger, but flexible project that can be shared without any additional requirements.""",
      ProjectSaveOption.values(), ProjectSaveOption.REFERENCING);

  public static final FileNameSuffixExportParameter projectFile = new FileNameSuffixExportParameter(
      "Project file", "File name of project to be saved", extensions, null);
  private static final Logger logger = Logger.getLogger(ProjectSaveAsParameters.class.getName());

  public ProjectSaveAsParameters() {
    super(new Parameter[]{projectFile, option});
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    assert Platform.isFxApplicationThread();

    final Region message = FxTextFlows.newTextFlowInAccordion("Important note", true,
        text("There are currently two project formats supported:"), linebreak(),
        boldText("Standalone: "),
        text("Adds the raw data files into a project (large but flexible)"), linebreak(),
        boldText("Referencing: "), text(
            "The project will point to the currently used files. Renaming, moving, or removing"
                + " a file from its current directory might lead to incompatibility of the project. "
                + "The project will keep absolute and relative (from mzmine 4.4) paths the the raw files."),
        linebreak(), boldText("WARNING: "),
        text("If this is an existing project, it is recommended to save it in the same way."));

    // set parameters to current project if already saved to file
    final MZmineProject project = ProjectService.getProjectManager().getCurrentProject();
    final File currentProjectFile = project.getProjectFile();

    if ((currentProjectFile != null) && (currentProjectFile.canWrite())) {
      final ProjectSaveOption projectType =
          Objects.requireNonNullElse(project.isStandalone(), true) ? ProjectSaveOption.STANDALONE
              : ProjectSaveOption.REFERENCING;
      setParameter(projectFile, currentProjectFile);
      setParameter(option, projectType);
    }

    ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired, this, message);
    dialog.showAndWait();
    final ExitCode exitCode = dialog.getExitCode();

    if (!ExitCode.OK.equals(exitCode)) {
      return exitCode;
    }

    // check if file exists and alert
    try {
      File file = getValue(projectFile);
      if (file == null) {
        return ExitCode.CANCEL;
      }

      if (!file.getName().endsWith(".mzmine")) {
        file = FileAndPathUtil.getRealFilePath(file, ".mzmine");
      }

      if (file.exists()) {
        Alert alert = new Alert(AlertType.CONFIRMATION,
            file.getName() + " already exists, overwrite ?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait();

        if (alert.getResult() != ButtonType.YES) {
          return ExitCode.CANCEL;
        }
      }
      setParameter(projectFile, file);
    } catch (Exception e) {
      logger.log(Level.WARNING, "Problem with checking project file during saving", e);
    }
    return ExitCode.OK;
  }
}
