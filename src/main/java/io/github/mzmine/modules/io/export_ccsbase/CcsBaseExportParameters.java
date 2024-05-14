/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.modules.io.export_ccsbase;

import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import java.util.List;

public class CcsBaseExportParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flists = new FeatureListsParameter();

  public static final FileNameParameter file = new FileNameParameter("Export file",
      "The file to export all annoteted compounds to.", FileSelectionType.SAVE);

  public static final ComboParameter<String> fallbackMoleculeInfo = new ComboParameter<>(
      "Fallback molecule type", """
      In case no molecular class is specified by the compound database,
      this type will be used. Valid choices are
      'small molecule', 'lipid', 'carbohydrate', or 'peptide'
      """, List.of("small molecule", "lipid", "carbohydrate", "peptide"), "small molecule");

  public static final ComboParameter<String> calibrationMethod = new ComboParameter<>(
      "Calibration method",
      "The method you used to calibrate the ion mobility device of the instrument.",
      List.of("single field, calibrated with Agilent Tune Mix", "single field, calibrated",
          "stepped field, calibrated with Agilent tune mix", "stepped-field"));

  public CcsBaseExportParameters() {
    super(flists, file, fallbackMoleculeInfo, calibrationMethod);
  }
}
