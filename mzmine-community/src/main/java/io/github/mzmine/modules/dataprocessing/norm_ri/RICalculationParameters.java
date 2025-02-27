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

package io.github.mzmine.modules.dataprocessing.norm_ri;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNamesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.util.files.ExtensionFilters;

import java.util.List;

/**
 * Define any parameters here (see io.github.mzmine.parameters for parameter types) static is needed
 * here to use this parameter as a key to lookup values
 * <p>
 * var flists = parameters.getValue(EmptyFeatureListParameters.featureLists);
 */
public class RICalculationParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter("Feature lists", 1, Integer.MAX_VALUE);
  public static final FileNamesParameter alkaneFiles = new FileNamesParameter("Alkane scale files", "List of files containing retention times for straight-chain alkanes." +
      "Expects CSV files with two columns with the header containing \"Carbon #\" and \"RT\". + " +
      "Compares the dates in the filenames to dates in the raw data files' names and uses the most recent scale. ",
      List.of(ExtensionFilters.CSV));
  public static final BooleanParameter extrapolate = new BooleanParameter("Extrapolation", "Linear extrapolation past the endpoints of the scale", false);
  public static final BooleanParameter addSummary = new BooleanParameter("Add row summaries", "Inserts the minimum and maximum retention index values for each row", false);
  public static final OriginalFeatureListHandlingParameter handleOriginal = new OriginalFeatureListHandlingParameter(false);
  public static final StringParameter suffix = new StringParameter("Name suffix",
      "Suffix to be added to feature list name", "ri");


  public RICalculationParameters() {
    /*
     * The order of the parameters is used to construct the parameter dialog automatically
     */
    super(featureLists, alkaneFiles, extrapolate, addSummary, handleOriginal, suffix);
  }

}
