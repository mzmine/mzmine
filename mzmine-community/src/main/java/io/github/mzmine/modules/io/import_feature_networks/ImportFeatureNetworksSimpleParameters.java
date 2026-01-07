/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.io.import_feature_networks;

import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.filenames.FileNamesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.util.files.ExtensionFilters;

public class ImportFeatureNetworksSimpleParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flist = new FeatureListsParameter(1, 1);

  public static final FileNamesParameter input = new FileNamesParameter("Input files (tsv/csv)", """
      Comma-separated files (csv) or Tab-separated files (tsv) with the following columns:
      ID1,ID2,EdgeType,EdgeAnnotation,EdgeScore
      ID1 and ID2 need to correspond to feature list row IDs in the selected feature lists.
      EdgeType and EdgeAnnotation are strings to define the method used and annotations of the edge
      EdgeScore is a floating point number
      """, ExtensionFilters.CSV_TSV_IMPORT);

  public ImportFeatureNetworksSimpleParameters() {
    super(flist, input);
  }
}
