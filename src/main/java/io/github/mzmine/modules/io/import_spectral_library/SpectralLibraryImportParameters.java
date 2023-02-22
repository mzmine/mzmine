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

package io.github.mzmine.modules.io.import_spectral_library;

import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.filenames.FileNamesParameter;
import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;

public class SpectralLibraryImportParameters extends SimpleParameterSet {

  public static final List<ExtensionFilter> extensions = List.of( //
      new ExtensionFilter("json files from MoNA or GNPS", "*.json"), //
      new ExtensionFilter("mgf files", "*.mgf"), //
      new ExtensionFilter("msp files from NIST", "*.msp"), //
      new ExtensionFilter("JCAM-DX files", "*.jdx"), //
      new ExtensionFilter("All files", "*.*") //
  );

  public static final FileNamesParameter dataBaseFiles = new FileNamesParameter(
      "Spectral library files",
      "Name of file that contains information for peak identification\n(GNPS json, MONA json, NIST msp, mgf, JCAMP-DX jdx)",
      extensions);

  public SpectralLibraryImportParameters() {
    super(dataBaseFiles);
  }

}
