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

package io.github.mzmine.modules.io.spectraldbsubmit.batch;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

/**
 * How to handle chimeric MS/MS spectra, where the isolation selected multiple precursor
 *
 * @author Robin Schmid <a href="https://github.com/robinschmid">https://github.com/robinschmid</a>
 */
public class HandleChimericMsMsParameters extends SimpleParameterSet {

  public static final MZToleranceParameter mainMassWindow = new MZToleranceParameter(
      "Target ion m/z tolerance", """
      Used to determine the main precursor signal in the MS1 scan.
      The tolerance is usually smaller than the precursor isolation window and depends on the accuracy and resolution of the instrument.
      """, 0.005, 3);

  public static final MZToleranceParameter isolationWindow = new MZToleranceParameter(
      "Precursor m/z isolation tolerance", """
      Real isolation window to check MS1 scan for co-isolated signals. This might be greater than
      the actually set m/z window. Especially some TOF-MS instruments isolate more than a unit
      resolution, resulting in co-isolated isotopes.""", 0.4, 0);
  public static final PercentParameter allowedOtherSignals = new PercentParameter(
      "Allowed other signal sum (%)",
      "Only flag spectrum as chimeric if the sum of other signals within precursor "
          + "isolation tolerance is greater than X% of the main signal.", 0.25);

  public static final ComboParameter<ChimericMsOption> option = new ComboParameter<>(
      "Handle chimeric spectra",
      "Options to handle spectra with multiple signals in the isolation range",
      ChimericMsOption.values(), ChimericMsOption.FLAG);

  public HandleChimericMsMsParameters() {
    super(new Parameter[]{mainMassWindow, isolationWindow, allowedOtherSignals, option});
  }


  public enum ChimericMsOption {
    SKIP, FLAG;

    @Override
    public String toString() {
      return super.toString().toLowerCase().replaceAll("_", " ");
    }
  }

}
