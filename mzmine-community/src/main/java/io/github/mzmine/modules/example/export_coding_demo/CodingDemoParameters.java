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

package io.github.mzmine.modules.example.export_coding_demo;

import io.github.mzmine.modules.io.download.ExternalAsset;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameSuffixExportParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameWithDownloadParameter;
import io.github.mzmine.parameters.parametertypes.ranges.MZRangeParameter;
import io.github.mzmine.parameters.parametertypes.ranges.RTRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.util.files.ExtensionFilters;
import java.util.List;

/**
 * Define any parameters here (see io.github.mzmine.parameters for parameter types) static is needed
 * here to use this parameter as a key to lookup values
 * <p>
 * var flists = parameters.getValue(EmptyFeatureListParameters.featureLists);
 */
public class CodingDemoParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter();

  // those are just example parameters and should be exchanged
  public static final FileNameSuffixExportParameter outFile = new FileNameSuffixExportParameter(
      "Export file", "Save results to csv file", ExtensionFilters.CSV_TSV_EXPORT, "coding_demo");

  // optional parameters just add a checkbox in front of another parameter
  public static final OptionalParameter<MZRangeParameter> mzRange = new OptionalParameter<>(
      new MZRangeParameter());
  public static final OptionalParameter<RTRangeParameter> rtRange = new OptionalParameter<>(
      new RTRangeParameter());


  public static final FileNameWithDownloadParameter testDownload = new FileNameWithDownloadParameter(
      "Test download", "Try out the new download file parameter", List.of(ExtensionFilters.ZIP),
      ExternalAsset.ThermoRawFileParser);


  public static final FileNameWithDownloadParameter testDownloadLibrary = new FileNameWithDownloadParameter(
      "Test download", "Try out the new download file parameter", List.of(ExtensionFilters.MGF),
      ExternalAsset.MSnLib);

  public CodingDemoParameters() {
    /*
     * The order of the parameters is used to construct the parameter dialog automatically
     */
    super(featureLists, outFile, mzRange, rtRange, testDownload, testDownloadLibrary);
  }

}
