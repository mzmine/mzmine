/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.modules.tools.batchwizard;

import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;

public class BatchWizardController {

  public GridPane pnParameters;
  public RadioButton rbOrbitrap;
  public RadioButton rbTOF;
  public ToggleGroup massSpec;
  public RadioButton rbHPLC;
  public RadioButton rbUHPLC;
  public ToggleGroup hplc;

  public void initialize() {

    final ParameterSet hplcParameters = new BatchWizardHPLCParameters();
    final ParameterSetupDialog hplcDialog = new ParameterSetupDialog(false, hplcParameters);
    pnParameters.add(hplcDialog.getParamsPane(), 1, 2);

    final ParameterSet msParameters = new BatchWizardMassSpectrometerParameters();
    final ParameterSetupDialog msDialog = new ParameterSetupDialog(false, msParameters);
    pnParameters.add(msDialog.getParamsPane(), 0, 2);

  }
}
