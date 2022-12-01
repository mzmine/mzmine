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

package io.github.mzmine.modules.visualization.spectra.spectralmatchresults;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;

/**
 * Saves the export paths of the SpectraIdentificationResultsWindow
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class SpectraIdentificationResultsParameters extends SimpleParameterSet {

  public static final FileNameParameter file =
      new FileNameParameter("file", "file without extension", FileSelectionType.SAVE);

  public static final BooleanParameter all =
      new BooleanParameter("Show export all", "Show button in panel", true);
  public static final BooleanParameter pdf =
      new BooleanParameter("Show export pdf", "Show button in panel", true);
  public static final BooleanParameter emf =
      new BooleanParameter("Show export emf", "Show button in panel", true);
  public static final BooleanParameter eps =
      new BooleanParameter("Show export eps", "Show button in panel", true);
  public static final BooleanParameter svg =
      new BooleanParameter("Show export svg", "Show button in panel", true);

  public SpectraIdentificationResultsParameters() {
    super(new Parameter[] {file, all, pdf, emf, eps, svg});
  }

}
