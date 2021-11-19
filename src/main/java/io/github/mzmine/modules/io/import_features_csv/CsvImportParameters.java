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

package io.github.mzmine.modules.io.import_features_csv;

import java.util.List;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.filenames.FileNamesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import javafx.stage.FileChooser.ExtensionFilter;

public class CsvImportParameters extends SimpleParameterSet {

  private static final List<ExtensionFilter> filters = List.of(new ExtensionFilter("csv", "*.csv"));

  public static final FileNamesParameter filename =
      new FileNamesParameter("csv", "CSV files to import", filters);

  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();


  public CsvImportParameters() {
    super(new Parameter[] {filename, dataFiles});
  }
}
