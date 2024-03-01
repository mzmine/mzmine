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

package io.github.mzmine.modules.io.import_rawdata_bruker_tdf;

import java.io.File;
import java.util.List;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.filenames.FileNamesParameter;
import io.github.mzmine.util.ExitCode;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class TDFImportParameters extends SimpleParameterSet {

  private static final List<ExtensionFilter> extensions = List.of( //
      new ExtensionFilter("Bruker tdf files", "*.d"), //
      new ExtensionFilter("All files", "*.*") //
  );

  public static final FileNamesParameter fileNames =
      new FileNamesParameter("File names", "", extensions);

  public TDFImportParameters() {
    super(new Parameter[] {fileNames});
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {

    DirectoryChooser fileChooser = new DirectoryChooser();
    fileChooser.setTitle("Import raw data files");

    File lastFiles[] = getParameter(fileNames).getValue();
    if ((lastFiles != null) && (lastFiles.length > 0)) {
      File currentDir = lastFiles[0].getParentFile();
      if ((currentDir != null) && (currentDir.exists())) {
        fileChooser.setInitialDirectory(currentDir);
      }
    }

    File selectedFile = fileChooser.showDialog(null);
    if (selectedFile == null) {
      return ExitCode.CANCEL;
    }
    getParameter(fileNames).setValue(new File[] {selectedFile});

    return ExitCode.OK;


  }

}
