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

package io.github.mzmine.modules.io.export_library_analysis_csv;

import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.isotopes.MassListDeisotoperParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.SpectralLibrarySelectionParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.util.scans.similarity.Weights;
import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;

public class LibraryAnalysisCSVExportParameters extends SimpleParameterSet {

  public static final SpectralLibrarySelectionParameter libraries = new SpectralLibrarySelectionParameter();
  public static final StringParameter fieldSeparator = new StringParameter("Field separator",
      "Character(s) used to separate fields in the exported file", ",");
  public static final ComboParameter<Weights> weight = new ComboParameter<>("Weights",
      "Weights for m/z and intensity", Weights.VALUES, Weights.SQRT);
  public static final OptionalParameter<MZToleranceParameter> removePrecursorRange = new OptionalParameter<>(
      new MZToleranceParameter("Remove +-m/z around precursor", "Remove residual precursor ion",
          17d, 0d), true);
  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter(
      "Spectral m/z tolerance",
      "Spectral m/z tolerance is used to match all signals in the query and library spectra (usually higher than precursor m/z tolerance)",
      0.01, 10);
  public static final IntegerParameter minMatch = new IntegerParameter("Minimum matched signals",
      "Minimum number of matched signals in masslist and spectral library entry (within mz tolerance)",
      6);
  public static final OptionalModuleParameter<MassListDeisotoperParameters> deisotoping = new OptionalModuleParameter<>(
      "13C deisotoping", "Removes 13C isotope signals from mass lists",
      new MassListDeisotoperParameters(), false);
  private static final List<ExtensionFilter> extensions = List.of( //
      new ExtensionFilter("comma-separated values", "*.csv"), //
      new ExtensionFilter("All files", "*.*") //
  );
  public static final FileNameParameter filename = new FileNameParameter("Filename",
      "Name of the output CSV file. " + "If the file already exists, it will be overwritten.",
      extensions, FileSelectionType.SAVE);


  public LibraryAnalysisCSVExportParameters() {
    super(new Parameter[]{libraries, filename, fieldSeparator, weight, removePrecursorRange,
        deisotoping, minMatch, mzTolerance});
  }

}
