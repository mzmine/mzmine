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
