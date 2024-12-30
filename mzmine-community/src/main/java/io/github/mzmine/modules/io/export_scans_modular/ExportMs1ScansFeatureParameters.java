/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.io.export_scans_modular;

import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;

public class ExportMs1ScansFeatureParameters extends SimpleParameterSet {

  public static final ComboParameter<Ms1ScanSelection> ms1Selection = new ComboParameter<>(
      "Scan selection",
      "MS1 scan or correlated MS1 scan of signals from modules like isotope finder, metaCorrelate, and others.",
      Ms1ScanSelection.values(), Ms1ScanSelection.MS1_AND_CORRELATED);

  public static final BooleanParameter separateMs1File = new BooleanParameter(
      "MS1 in separate file",
      "Create a separate file for MS1 - otherwise MS1 scans will be mixed with MS2 scans into one file.",
      false);

  public static final BooleanParameter ms1RequiresFragmentScan = new BooleanParameter(
      "MS1 requires fragment scans",
      "Only export MS1 scans if there are corresponding fragment scans.", true);

  public ExportMs1ScansFeatureParameters() {
    super(ms1Selection, ms1RequiresFragmentScan, separateMs1File);
  }
}
