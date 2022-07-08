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
package io.github.mzmine.modules.dataprocessing.featdet_dfbuilder;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;
import weka.core.pmml.jaxbbindings.True;

public class DiagnosticFilterParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

  /**
   * Scan selection. MSn level must be set through MS Level field. Default MS2.
   */
  public static final ScanSelectionParameter scanSelection =
      new ScanSelectionParameter(new ScanSelection(2));

  /**
   * MZ tolerance for precursor chromatogram building.
   */
  public static final MZToleranceParameter mzDifference = new MZToleranceParameter();

  /**
   * RT tolerance for precursor chromatogram building.
   */
  public static final RTToleranceParameter rtTolerance = new RTToleranceParameter();

  /**
   * CSV extension for file import and export
   */
  private static final List<ExtensionFilter> extensions = List.of( //
      new ExtensionFilter("comma-separated values", "*.csv") //
  );

  /**
   * Import path of diagnostic filters table.
   */
  public static final FileNameParameter diagnosticFile = new FileNameParameter("Diagnostic feature list file",
      "CSV file containing diagnostic filter targets. See Help for more info.",
      extensions,
      FileSelectionType.OPEN);

  /**
   * Import path of exclusion list. Optional parameter.
   */
  public static final OptionalParameter<FileNameParameter> exclusionFile =
      new OptionalParameter<>(new FileNameParameter("(Optional) Exclusion feature list file",
          "Optional CSV file of mass-RT combinations to exclude. See Help for more info.",
          extensions,
          FileSelectionType.OPEN));

  /**
   * Export path of detected scans. Optional parameter.
   */
  public static final OptionalParameter<FileNameParameter> exportFile =
      new OptionalParameter<>(new FileNameParameter("(Optional) Export detected precursor list",
          "Optional export of precursor hits of interest. See Help for more info.",
          extensions,
          FileSelectionType.SAVE));

  /**
   * Fragment ion tolerance formatted as percent of base peak.
   */
  public static final DoubleParameter basePeakPercent = new DoubleParameter(
      "Minimum ion intensity (% base peak)",
      "Minimum ion intensity for screening, scaled to base peak. Will choose non-zero maximum between %base peak and relative abundance.",
      MZmineCore.getConfiguration().getRTFormat(), 1.0, 0.0, 100.0);

  public DiagnosticFilterParameters() {
    super(new Parameter[] {dataFiles, scanSelection, mzDifference,
        diagnosticFile, basePeakPercent, rtTolerance,
        exclusionFile, exportFile});
  }
}