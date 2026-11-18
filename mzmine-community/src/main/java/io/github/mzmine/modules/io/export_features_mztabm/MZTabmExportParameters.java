/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.io.export_features_mztabm;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameSuffixExportParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.util.ExitCode;
import java.io.File;
import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MZTabmExportParameters extends SimpleParameterSet {

  private static final List<ExtensionFilter> extensions = List.of( //
      new ExtensionFilter("mzTab files for feature lists", "*.mzTab") //
  );


  public static final FeatureListsParameter featureLists = new FeatureListsParameter();

  public static final FileNameSuffixExportParameter filename = new FileNameSuffixExportParameter(
      "Filename", "Use pattern \"{}\" in the file name to substitute with feature list name. "
                  + "(i.e. \"blah{}blah.mzTab\" would become \"blahSourcePeakListNameblah.mzTab\"). "
                  + "The dialog defaults to the saved project's directory. "
                  + "If the file already exists, it will be overwritten.", extensions, "quant", false);

  public static final BooleanParameter exportAll = new BooleanParameter("Include all peaks"//
      , "Include peaks with unknown identity");

  public MZTabmExportParameters() {
    super(new Parameter[]{featureLists, filename, exportAll});
  }

  /**
   * Opens the standard export dialog with a destination in the current project's directory.
   *
   * @param valueCheckRequired whether the dialog validates settings before accepting them
   * @return the user's dialog choice
   */
  @Override
  public @NotNull ExitCode showSetupDialog(final boolean valueCheckRequired) {
    initializeOutputFile(ProjectService.getProject().getProjectFile());
    return super.showSetupDialog(valueCheckRequired);
  }

  /**
   * Prepares the destination for a new interactive export without changing batch settings.
   *
   * @param projectFile saved project archive, or null when the project has not been saved
   */
  void initializeOutputFile(@Nullable final File projectFile) {
    // decision: Opening the GUI starts in this project's directory, not a previous project's folder.
    final File output = projectFile == null ? null
        : new File(projectFile.getAbsoluteFile().getParentFile(), "{}_quant.mzTab");
    setParameter(filename, output);
  }
}
