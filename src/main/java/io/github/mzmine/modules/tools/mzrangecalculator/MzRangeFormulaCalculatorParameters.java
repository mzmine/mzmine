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

package io.github.mzmine.modules.tools.mzrangecalculator;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.FormulaParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class MzRangeFormulaCalculatorParameters extends SimpleParameterSet {

  static final FormulaParameter formula = new FormulaParameter();

  static final ComboParameter<IonizationType> ionType =
      new ComboParameter<IonizationType>("Ionization type",
          "Please choose the type of ion to produce from the formula", IonizationType.values());

  static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

  public MzRangeFormulaCalculatorParameters() {
    super(new Parameter[] {formula, ionType, mzTolerance});
  }

}
