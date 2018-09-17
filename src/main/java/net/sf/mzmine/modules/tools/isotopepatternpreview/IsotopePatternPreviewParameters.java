/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.tools.isotopepatternpreview;

import java.awt.Window;
import net.sf.mzmine.modules.tools.isotopepatternpreview.customparameters.IsotopePatternPreviewCustomParameters;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.OptionalModuleParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.util.ExitCode;

/**
 * 
 * @author Steffen Heuckeroth steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class IsotopePatternPreviewParameters extends SimpleParameterSet {

  public static final StringParameter molecule = new StringParameter("Element/Molecule",
      "The element/molecule to calculate the isotope pattern of. Enter a sum formula.");

  public static final OptionalModuleParameter optionals =
      new OptionalModuleParameter("Use custom parameters",
          "If not checked, default parameters will be used to calculate the isotope pattern.\n"
          + "The standard values are:\n"
          + "  minimum Abundance = 1.0 %\n"
          + "  minimum Intensity = 0.05 / 1\n"
          + "  merge width = 0.0005 Da",
          new IsotopePatternPreviewCustomParameters());

  public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {
    if ((getParameters() == null) || (getParameters().length == 0))
      return ExitCode.OK;

    ParameterSetupDialog dialog = new IsotopePatternPreviewDialog(parent, valueCheckRequired, this);
    dialog.setVisible(true);
    return dialog.getExitCode();
  }

  public IsotopePatternPreviewParameters() {
    super(new Parameter[] {molecule, optionals});
  }
}