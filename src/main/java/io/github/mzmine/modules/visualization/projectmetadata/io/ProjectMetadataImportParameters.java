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

package io.github.mzmine.modules.visualization.projectmetadata.io;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.filenames.FileNamesParameter;
import io.github.mzmine.util.ExitCode;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class ProjectMetadataImportParameters extends SimpleParameterSet {

  private static final List<ExtensionFilter> extensions = List.of( //
      new ExtensionFilter("tsv files", "*.tsv"), //
      new ExtensionFilter("csv files", "*.csv"), //
      new ExtensionFilter("All files", "*.*") //
  );

  public static final FileNamesParameter fileNames = new FileNamesParameter("File names", "",
      extensions);

  public ProjectMetadataImportParameters() {
    super(new Parameter[]{fileNames});
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {

    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Import project metadata");
    fileChooser.getExtensionFilters().addAll(extensions);

    // setting an initial directory
    File[] lastFiles = getParameter(fileNames).getValue();
    Arrays.stream(Objects.requireNonNullElse(lastFiles, new File[0])).filter(Objects::nonNull)
        .map(File::getParentFile).filter(Objects::nonNull).filter(File::exists).findFirst()
        .ifPresent(fileChooser::setInitialDirectory);

    List<File> selectedFiles = fileChooser.showOpenMultipleDialog(null);
    if (selectedFiles == null) {
      return ExitCode.CANCEL;
    }
    getParameter(fileNames).setValue(selectedFiles.toArray(new File[0]));

    return ExitCode.OK;
  }
}
