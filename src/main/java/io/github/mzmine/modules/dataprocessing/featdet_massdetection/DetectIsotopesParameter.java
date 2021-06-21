/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.featdet_massdetection;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.elements.ElementsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class DetectIsotopesParameter extends SimpleParameterSet {

  public static final ElementsParameter elements = new ElementsParameter(
      "Elements", "Chemical elements which isotopes will be considered", true,
      MassDetectionUtils.DEFAULT_ELEMENTS_LIST);

  public static final MZToleranceParameter isotopeMzTolerance = new MZToleranceParameter();

  public static final DoubleParameter isotopeAbundanceLowBound
      = new DoubleParameter("Isotope abundance strict lower bound", "Isotope "
      + "abundance strict lower bound given as the value from [0, 1] interval. For example, a value "
      + "of 0 means that only isotopes with natural abundance strictly higher than 0 will be considered.",
      MZmineCore.getConfiguration().getMZFormat(), 0d);

  public static final IntegerParameter charge = new IntegerParameter("Charge",
      "Ion charge", 1, true, 1, null);

  public DetectIsotopesParameter() {
    super(new UserParameter[] {elements, isotopeAbundanceLowBound, isotopeMzTolerance, charge});
  }

}
