/*
 * Copyright 2006-2022 The MZmine Development Team
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
