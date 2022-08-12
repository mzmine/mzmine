/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
