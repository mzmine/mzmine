/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.io.export_scans;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameSuffixExportParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;

public class ExportScansFromRawFilesParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();
  public static final ScanSelectionParameter scanSelect = new ScanSelectionParameter();
  public static final FileNameSuffixExportParameter file = new FileNameSuffixExportParameter("File",
      "file destination", "rawscans");
  public static final ComboParameter<ScanFormats> formats = new ComboParameter<>("Format",
      "Export formats. mgf: MASCOT, SIRIUS;  txt: plain text", ScanFormats.values(),
      ScanFormats.mgf);
  public static final BooleanParameter export_masslist = new BooleanParameter(
      "Export centroid mass list", "Exports the centroid mass list instead of raw data", true);

  public ExportScansFromRawFilesParameters() {
    super(new Parameter[]{dataFiles, scanSelect, file, formats, export_masslist});
  }

}
