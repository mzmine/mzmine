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

package io.github.mzmine.modules.io.spectraldbsubmit.param;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.util.scans.sorting.ScanSortMode;

/**
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class LibrarySubmitParameters extends SimpleParameterSet {

  // scan selection and preprocessing
  public static final DoubleParameter noiseLevel = new DoubleParameter("Noise level",
      "Noise level to filter masslists", MZmineCore.getConfiguration().getIntensityFormat(), 0d);
  public static final IntegerParameter minSignals = new IntegerParameter("Min signals",
      "Minimum signals in a masslist (all other masslists are discarded)", 3);

  public static final ComboParameter<ScanSortMode> sorting = new ComboParameter<>("Sorting",
      "Sorting mode for filtered mass lists", ScanSortMode.values(), ScanSortMode.MAX_TIC);

  // submission and creation of libraries
  // save to local file
  public static final OptionalParameter<FileNameParameter> LOCALFILE = new OptionalParameter<>(
      new FileNameParameter("Local file", "Local library file", FileSelectionType.SAVE), false);
  public static final BooleanParameter EXPORT_GNPS_JSON = new BooleanParameter(
      "Export GNPS json file", "The GNPS library submission json format", true);
  public static final BooleanParameter EXPORT_MSP = new BooleanParameter("Export NIST msp file",
      "The NIST msp library format", true);
  public static final BooleanParameter EXPORT_MGF = new BooleanParameter("Export mgf file",
      "The mgf library format", true);
  // user and password
  public static final OptionalModuleParameter<GnpsLibrarySubmitParameters> SUBMIT_GNPS = new OptionalModuleParameter<>(
      "Submit to GNPS (MS2)",
      "Submit new entry to GNPS library (Only for fragmentation data of MS level >1)",
      new GnpsLibrarySubmitParameters(), true);

  public LibrarySubmitParameters() {
    super(new Parameter[]{noiseLevel, minSignals, sorting,
        // save to local file
        LOCALFILE, EXPORT_GNPS_JSON, EXPORT_MSP, EXPORT_MGF,
        // submit to online library
        SUBMIT_GNPS});
  }
}
