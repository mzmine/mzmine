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

package io.github.mzmine.modules.dataprocessing.process_fragmentsanalysis;

import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameSuffixExportParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;

/**
 * Define any parameters here (see io.github.mzmine.parameters for parameter types) static is needed
 * here to use this parameter as a key to lookup values
 * <p>
 * var flists = parameters.getValue(EmptyFeatureListParameters.featureLists);
 */
public class FragmentsAnalysisParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter();

  public static final MZToleranceParameter tolerance = new MZToleranceParameter();

  public static final ComboParameter<ScanDataType> scanDataType = new ComboParameter<>(
      "MS data selection",
      "Show either raw data or filtered centroid data (after mass detection and other filters).\n"
      + "RAW on profile mode spectra may result in unwanted results, apply mass detection and choose centroid instead. ",
      ScanDataType.values(), ScanDataType.MASS_LIST);

  private static final List<ExtensionFilter> extensions = List.of( //
      new ExtensionFilter("mgf format that contains MS1 and MS2 data", "*.mgf")); //

  // those are just example parameters and should be exchanged
  public static final FileNameSuffixExportParameter outFile = new FileNameSuffixExportParameter(
      "Filename", "Name of the output MGF file. "
                  + "Use pattern \"{}\" in the file name to substitute with feature list name. "
                  + "(i.e. \"blah{}blah.mgf\" would become \"blahSourceFeatureListNameblah.mgf\"). "
                  + "If the file already exists, it will be overwritten.", extensions,
      "fragments_analysis");

  public FragmentsAnalysisParameters() {
    /*
     * The order of the parameters is used to construct the parameter dialog automatically
     */
    super(featureLists, scanDataType, outFile, tolerance);
  }

}
