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

package io.github.mzmine.modules.dataprocessing.filter_merge;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;

public class RawFileMergeParameters extends SimpleParameterSet {

  public enum MODE {
    MERGE_SELECTED("Merge selected"),
    MERGE_PATTERN("Merge pattern");

    private String label;

    MODE(String label) {
      this.label = label;
    }

    @Override
    public String toString() {
//      return super.toString().replaceAll("_", " ");
      return this.label;
    }
  }

  public enum POSITION {
    BEFORE_FIRST ("Before first"),
    AFTER_LAST("After last");

    private String label;

    POSITION(String label) {
      this.label=label;
    }
    @Override
    public String toString() {
//      return super.toString().replaceAll("_", " ");
      return this.label;
    }
  }

  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

  public static final ComboParameter<MODE> mode = new ComboParameter<MODE>("Mode",
      "Merge all selected to one or all file that have a matching suffix or prefix", MODE.values(),
      MODE.MERGE_PATTERN);

  public static final ComboParameter<POSITION> position =
      new ComboParameter<POSITION>("Grouping identifier position",
          "Define position of the identifier to use for grouping (e.g., a number after the last _)",
          POSITION.values(), POSITION.AFTER_LAST);

  public static final StringParameter posMarker =
      new StringParameter("Position marker", "e.g., the last _ or any string", "_");

  public static final OptionalParameter<StringParameter> MS2_marker =
      new OptionalParameter<>(new StringParameter("MS2 marker",
          "Raw data files that contain this marker in their name will only be used as a source for MS2 scans.",
          ""));

  public static final StringParameter suffix =
      new StringParameter("Suffix to new name", "The suffix to describe the new file", "_merged");

  public RawFileMergeParameters() {
    super(new Parameter[] {dataFiles, mode, position, posMarker, MS2_marker, suffix},
        "https://mzmine.github.io/mzmine_documentation/module_docs/featdet_file_merging/raw-files-merging.html");
  }

}
