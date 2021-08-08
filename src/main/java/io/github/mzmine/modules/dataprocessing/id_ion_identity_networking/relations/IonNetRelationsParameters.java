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

package io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.relations;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ionidentity.IonModificationParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class IonNetRelationsParameters extends SimpleParameterSet {
  public static final FeatureListsParameter PEAK_LISTS = new FeatureListsParameter();

  public static final MZToleranceParameter MZ_TOL = new MZToleranceParameter();

  public static final IonModificationParameter ADDUCTS =
      new IonModificationParameter("Adducts", "List of modifications");

  public static final BooleanParameter SEARCH_CONDENSED_MOL = new BooleanParameter(
      "Search condensed",
      "Searches for condensed structures (loss of water) with regards to possible structure modifications",
      true);
  public static final BooleanParameter SEARCH_CONDENSED_HETERO_MOL = new BooleanParameter(
      "Search hetero condensed",
      "Searches for condensed structures (loss of water) of two different neutral modifications",
      true);

  public IonNetRelationsParameters() {
    super(new Parameter[] {PEAK_LISTS, MZ_TOL, SEARCH_CONDENSED_MOL, SEARCH_CONDENSED_HETERO_MOL,
        ADDUCTS});
  }

}
