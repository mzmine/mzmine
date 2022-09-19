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
      "Main mass tolerance",
      "Used to determine the main signal. Usually smaller than the precursor isolation "
          + "window and dependent on the accuracy and resolution of the instrument.", 0.005, 2);
  public static final MZToleranceParameter isolationWindow = new MZToleranceParameter(
      "Precursor isolation tolerance",
      "Real isolation window to check for co-isolated signals. This might be greater than "
          + "the actually set value. Especially some TOF-MS instruments isolate more than a unit "
          + "resolution, resulting in co isolated isotopes.", 0.4, 0);
  public static final PercentParameter allowedOtherSignals = new PercentParameter(
      "Allowed other signal sum (%)",
      "Only flag spectrum as chimeric if the sum of other signals is greater than X% of the target signal.",
      0.25);
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
