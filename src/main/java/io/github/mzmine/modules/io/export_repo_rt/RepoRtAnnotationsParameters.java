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

package io.github.mzmine.modules.io.export_repo_rt;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import java.util.List;
import javafx.stage.FileChooser;

public class RepoRtAnnotationsParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter(1);

  private static final List<FileChooser.ExtensionFilter> extensions = List.of( //
      new FileChooser.ExtensionFilter("tab-separated values", "*.tsv"), //
      new FileChooser.ExtensionFilter("All files", "*.*") //
  );

  public static final FileNameParameter filename = new FileNameParameter("Filename",
      "Name of the output TSV file. "
      + "Use pattern \"{}\" in the file name to substitute with feature list name. "
      + "(i.e. \"blah{}blah.csv\" would become \"blahSourceFeatureListNameblah.csv\"). "
      + "If the file already exists, it will be overwritten.", extensions, FileSelectionType.SAVE);

  public static final StringParameter datasetId = new StringParameter("RepoRT dataset ID",
      "An ID needs to be requested first. Otherwise use a unique placeholder that can be replaced later by the ID.",
      "", true);

  public RepoRtAnnotationsParameters() {
    super(new Parameter[]{featureLists, filename, datasetId},
        "https://mzmine.github.io/mzmine_documentation/module_docs/io/feat-list-export.html");
  }

}
