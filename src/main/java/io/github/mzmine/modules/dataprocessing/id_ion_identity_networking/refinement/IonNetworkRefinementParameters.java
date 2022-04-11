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

package io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.refinement;


import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import org.jetbrains.annotations.NotNull;

/**
 * Refinement to MS annotation
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class IonNetworkRefinementParameters extends SimpleParameterSet {

  // NOT INCLUDED in sub
  // General parameters
  public static final FeatureListsParameter PEAK_LISTS = new FeatureListsParameter();


  public static final OptionalParameter<IntegerParameter> MIN_NETWORK_SIZE =
      new OptionalParameter<>(new IntegerParameter("Minimum size", "Minimum network size", 2));
  public static final OptionalParameter<IntegerParameter> TRUE_THRESHOLD = // todo - better description
      new OptionalParameter<>(new IntegerParameter("Delete smaller networks: Link threshold",
          "links>=true threshold, then delete all other occurance in annotation networks", 4));

  public static final BooleanParameter DELETE_SMALL_NO_MAJOR = new BooleanParameter(
      "Delete small networks without major ion",
      "Delete small networks without H+, Na+, NH4+, H-, Cl-, formic acid- adduct. \nMaximum allowed in-source modifications: H+(3, e.g., -H2O), Na(0), NH4(1), H-(2), Cl(0), FA(0).",
      false);
  public static final BooleanParameter DELETE_ROWS_WITHOUT_ID =
      new BooleanParameter("Delete rows witout ion id", "Keeps only rows with ion ids", false);
  public static final BooleanParameter DELETE_WITHOUT_MONOMER =
      new BooleanParameter("Delete networks without monomer",
          "Deletes all networks without monomer or with 1 monomer and >=3 multimers", true);
  // public static final BooleanParameter DELETE_XMERS_ON_MSMS = new BooleanParameter(
  // "Use MS/MS xmer verification to exclude",
  // "If an xmer was identified by MS/MS annotation of fragment xmers, then use this identification
  // and delete rest",
  // true);


  // Constructor
  public IonNetworkRefinementParameters() {
    this(false);
  }

  public IonNetworkRefinementParameters(boolean isSub) {
    super(isSub ? // no peak list and rt tolerance
        new Parameter[] {MIN_NETWORK_SIZE, DELETE_SMALL_NO_MAJOR, TRUE_THRESHOLD,
            DELETE_WITHOUT_MONOMER, DELETE_ROWS_WITHOUT_ID}
        : new Parameter[] {PEAK_LISTS, MIN_NETWORK_SIZE, DELETE_SMALL_NO_MAJOR, TRUE_THRESHOLD,
            DELETE_WITHOUT_MONOMER, DELETE_ROWS_WITHOUT_ID});
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
