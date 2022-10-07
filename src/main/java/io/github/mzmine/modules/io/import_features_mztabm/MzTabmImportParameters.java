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

package io.github.mzmine.modules.io.import_features_mztabm;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNamesParameter;
import javafx.stage.FileChooser.ExtensionFilter;

import java.util.List;

public class MzTabmImportParameters extends SimpleParameterSet {

  private static final List<ExtensionFilter> filters =
      List.of(new ExtensionFilter("mzTab-m files", "*.mztab"),
          new ExtensionFilter("mzTab-m files", "*.mzTab"));

  public static final FileNamesParameter file =
      new FileNamesParameter("mzTab-m files", "mzTab-m files to import", filters);

  public static final BooleanParameter importRawFiles = new BooleanParameter(
      "Import raw files too?",
      "If selected, raw data files will also be imported if they are available.\nFiles will be loaded from the msrun location defined in the mzTab-m file or from the same folder containing the mzTab-m file.\nIf raw data files cannot be found, empty files will be generated instead.\nPlease note that missing files may lead to issues with other modules that need access to scan data.",
      Boolean.TRUE
  );

  public MzTabmImportParameters() {
    super(new Parameter[]{file, importRawFiles});
  }

}
