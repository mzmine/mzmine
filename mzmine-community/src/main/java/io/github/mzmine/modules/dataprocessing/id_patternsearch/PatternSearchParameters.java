/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.id_patternsearch;

import io.github.mzmine.javafx.components.factories.FxTextFlows;
import io.github.mzmine.javafx.components.factories.FxTexts;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.files.ExtensionFilters;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.Nullable;

public class PatternSearchParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flists = new FeatureListsParameter();

  public static final FileNameParameter libraryFile = new FileNameParameter("Library file",
      "Library file containing the pattern search library.", ExtensionFilters.ALL_LIBRARY,
      FileSelectionType.OPEN);

  public static final DoubleParameter minScore = new DoubleParameter("Minimum score",
      "Minimum required score.", ConfigService.getGuiFormats().scoreFormat(), 0.9, 0d, 1d);

  public static final MZToleranceParameter isotopeMatchingTolerance = new MZToleranceParameter(
      "Pattern matching tolerance", "Tolerance to match the tolerance to the pattern.", 0.002, 2);

  public PatternSearchParameters() {
    super(flists, libraryFile, minScore /*, ms1MergingTolerance*/, isotopeMatchingTolerance);
  }

  @Override
  public @Nullable Region getMessage() {
    return FxTextFlows.newTextFlow(FxTexts.text("""
        This is an experimental/prototype module.
        The intended use case is to recognise intensity patterns in spectra that are paired as fragmentation spectra. \
        The pattern is specified in a spectral library. Each signal in the spectral library is shifted by the m/z of \
        the specific feature. If a match is achieved, the match is added as a spectral library match.
        To pair correlated MS1 signals, use the DIA correlation and set the scan filter to MS1."""));
  }
}
