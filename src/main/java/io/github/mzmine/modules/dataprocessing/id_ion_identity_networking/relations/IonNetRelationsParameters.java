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

  public static final FeatureListsParameter featureLists = new FeatureListsParameter();

  public static final MZToleranceParameter mzTol = new MZToleranceParameter();

  public static final IonModificationParameter adducts = new IonModificationParameter("Adducts",
      "List of modifications");

  public static final BooleanParameter searchCondensedMultimer = new BooleanParameter(
      "Search condensed multimer",
      "Searches for condensed structures (loss of water) with regards to possible structure modifications",
      true);
  public static final BooleanParameter searchCondensedHeteroMultimer = new BooleanParameter(
      "Search condensed hetero",
      "Searches for condensed structures (loss of water) of two different neutral modifications",
      true);

  public IonNetRelationsParameters() {
    super(
        new Parameter[]{featureLists, mzTol, searchCondensedMultimer, searchCondensedHeteroMultimer,
            adducts});
  }

}
