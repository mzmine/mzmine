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
 * Code created was by or on behalf of Syngenta and is released under the open source license in use
 * for the pre-existing code or project. Syngenta does not assert ownership or copyright any over
 * pre-existing work.
 */

package io.github.mzmine.modules.dataprocessing.id_adductsearch;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Params to add custom adducts.
 */
public class AddAdductParameters extends SimpleParameterSet implements Supplier<AdductType> {

  // Adduct name.
  public static final StringParameter NAME =
      new StringParameter("Name", "A name to identify the new adduct");
  // Adduct mass difference.
  public static final DoubleParameter MASS_DIFFERENCE = new DoubleParameter("Mass difference",
      "Mass difference for the new adduct", MZmineCore.getConfiguration().getMZFormat());
  private static final Logger logger = Logger.getLogger(AddAdductParameters.class.getName());

  public AddAdductParameters() {
    super(new Parameter[]{NAME, MASS_DIFFERENCE});
  }

  @Override
  public AdductType get() {
    try {
      return new AdductType(getParameter(AddAdductParameters.NAME).getValue(),
          getParameter(AddAdductParameters.MASS_DIFFERENCE).getValue());
    } catch (Exception ex) {
      logger.log(Level.WARNING, ex.getMessage(), ex);
      return null;
    }
  }
}
