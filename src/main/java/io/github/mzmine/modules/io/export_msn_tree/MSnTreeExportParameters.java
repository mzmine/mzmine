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

/*
 * This module was prepared by Abi Sarvepalli, Christopher Jensen, and Zheng Zhang at the Dorrestein
 * Lab (University of California, San Diego).
 *
 * It is freely available under the GNU GPL licence of MZmine2.
 *
 * For any questions or concerns, please refer to:
 * https://groups.google.com/forum/#!forum/molecular_networking_bug_reports
 *
 * Credit to the Du-Lab development team for the initial commitment to the MGF export module.
 */

package io.github.mzmine.modules.io.export_msn_tree;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;
import org.jetbrains.annotations.NotNull;

public class MSnTreeExportParameters extends SimpleParameterSet {

  public static final MZToleranceParameter MZ_TOL = new MZToleranceParameter("m/z tolerance",
      "Tolerance for building MSn trees to pair MSn on each level", 0.001, 5);
  public static final RawDataFilesParameter RAW_FILES = new RawDataFilesParameter();

  public static final StringParameter SEPARATOR = new StringParameter("Separator",
      "Separator used in tabular data file", "\t");
  private static final List<ExtensionFilter> extensions = List.of( //
      new ExtensionFilter("tab separated file", "*.tsv"), //
      new ExtensionFilter("comma separated file", "*.csv") //
  );
  public static final FileNameParameter FILENAME = new FileNameParameter("Filename",
      "Name of the raw data files to be exported "
          + "Use pattern \"{}\" in the file name to substitute with raw data filename. "
          + "(i.e. \"blah{}blah.mgf\" would become \"blahSOURCE_DATAFILE_Nameblah.mgf\"). "
          + "If the file already exists, it will be overwritten.", extensions,
      FileSelectionType.SAVE);

  public MSnTreeExportParameters() {
    super(new Parameter[]{RAW_FILES, FILENAME, SEPARATOR, MZ_TOL});
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.UNTESTED;
  }
}
