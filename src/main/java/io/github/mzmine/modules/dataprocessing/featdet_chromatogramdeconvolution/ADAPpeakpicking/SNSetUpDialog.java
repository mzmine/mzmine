/*
 * Copyright 2006-2021 The MZmine Development Team
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
 * author Owen Myers (Oweenm@gmail.com)
 */
package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ADAPpeakpicking;

import java.awt.Font;
import java.util.logging.Logger;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;

public class SNSetUpDialog extends ParameterSetupDialog {

  private static final long serialVersionUID = 1L;

  private static final Logger logger = Logger.getLogger(SNSetUpDialog.class.getName());

  // Combo-box font.
  private static final Font COMBO_FONT = new Font("SansSerif", Font.PLAIN, 10);

  private final ParameterSet parameters;

  public SNSetUpDialog(boolean valueCheckRequired, final ParameterSet SNParameters) {

    super(valueCheckRequired, SNParameters);

    parameters = SNParameters;
  }
}
