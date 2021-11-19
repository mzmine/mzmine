/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package io.github.mzmine.parameters.parametertypes.ionidentity;


import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;

public class IonLibraryParameterSet extends SimpleParameterSet {

  public static final ComboParameter<String> POSITIVE_MODE = new ComboParameter<>("MS mode",
      "Positive or negative mode?", new String[] {"POSITIVE", "NEGATIVE"}, "POSITIVE");

  public static final IntegerParameter MAX_CHARGE = new IntegerParameter("Maximum charge",
      "Maximum charge to be used for adduct search.", 2, 1, 100);
  public static final IntegerParameter MAX_MOLECULES = new IntegerParameter(
      "Maximum molecules/cluster", "Maximum molecules per cluster (f.e. [2M+Na]+).", 3, 1, 10);

  public static final IonModificationParameter ADDUCTS = new IonModificationParameter("Adducts",
      "List of adducts, each one refers a specific distance in m/z axis between related peaks");

  public IonLibraryParameterSet() {
    super(new Parameter[] {POSITIVE_MODE, MAX_CHARGE, MAX_MOLECULES, ADDUCTS});
  }
}
