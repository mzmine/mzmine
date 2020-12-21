/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.modules.dataprocessing.group_metacorrelate.export;


import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.rowfilter.RowFilter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;

public class ExportCorrAnnotationParameters extends SimpleParameterSet {

  // NOT INCLUDED in sub
  // General parameters
  public static final FeatureListsParameter PEAK_LISTS = new FeatureListsParameter();

  public static final FileNameParameter FILENAME =
      new FileNameParameter("Filename", "File name", "csv", FileSelectionType.SAVE);
  public static final BooleanParameter EX_ANNOT =
      new BooleanParameter("Export ion identity networking (IIN) edges", "", true);
  public static final BooleanParameter EX_CORR =
      new BooleanParameter("Export correlation edges", "", false);
  public static final BooleanParameter EX_IIN_RELATIONSHIP =
      new BooleanParameter("Export IIN relationship edges", "", false);
  public static final BooleanParameter EX_MS2_SIMILARITY =
      new BooleanParameter("Export MS2 similarity edges", "", false);
  public static final BooleanParameter EX_MS2_DIFF_SIMILARITY =
      new BooleanParameter("Export MS2 neutral loss similarity edges", "", false);



  public static final PercentParameter MIN_R =
      new PercentParameter("Min correlation (r)", "Minimum Pearson correlation", 0.9);

  public static final ComboParameter<RowFilter> FILTER = new ComboParameter<>(
      "Filter rows", "Limit the exported rows to those with MS/MS data or annotated rows",
      RowFilter.values(), RowFilter.ONLY_WITH_MS2_OR_ANNOTATION);

  // Constructor
  public ExportCorrAnnotationParameters() {
    super(new Parameter[] {PEAK_LISTS, FILENAME, EX_ANNOT, EX_CORR, EX_IIN_RELATIONSHIP,
        EX_MS2_SIMILARITY, EX_MS2_DIFF_SIMILARITY, FILTER, MIN_R});
  }

}
