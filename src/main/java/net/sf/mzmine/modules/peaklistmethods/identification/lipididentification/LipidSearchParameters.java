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

package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification;

import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.MultiChoiceParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;

public class LipidSearchParameters extends SimpleParameterSet {

  public static final PeakListsParameter peakLists = new PeakListsParameter();

  public static final MultiChoiceParameter<LipidType> lipidTypes =
      new MultiChoiceParameter<LipidType>("Type of lipids", "Selection of lipid backbones",
          LipidType.values());

  public static final IntegerParameter minChainLength = new IntegerParameter(
      "Minimum number of Carbon in fatty acids", "Minimum number of Carbon in fatty acids");

  public static final IntegerParameter maxChainLength = new IntegerParameter(
      "maximum number of Carbon in fatty acids", "Maximum number of Carbon in fatty acids");

  public static final IntegerParameter maxDoubleBonds = new IntegerParameter(
      "Maximum number of double bonds", "Maximum number of double bonds in all fatty acid chains");

  public static final IntegerParameter maxOxidationValue =
      new IntegerParameter("Maximum number of additional oxygen due to oxidation [M+xO]",
          "Maximum number of additional oxygen due to oxidation [M+xO]");

  public static final MZToleranceParameter mzTolerance =
      new MZToleranceParameter("m/z tolerance MS1 level:",
          "Enter m/z tolerance for exact mass database matching on MS1 level");

  public static final ComboParameter<IonizationType> ionizationMethod =
      new ComboParameter<IonizationType>("Ionization method",
          "Type of ion used to calculate the ionized mass", IonizationType.values());

  public static final BooleanParameter searchForIsotopes =
      new BooleanParameter("Search for 13C isotopes", "Search for 13C isotopes");

  public static final RTToleranceParameter isotopeRetentionTimeTolerance = new RTToleranceParameter(
      "Isotope RT tolerance", "Set the RT tolerance in min to search for lipid 13C isotopes");

  public static final IntegerParameter relativeIsotopeIntensityTolerance =
      new IntegerParameter("Relative intensity tolerance of 13C isotope [%]",
          "Relative intensity tolerance of 13C isotope compared to calculated 13C feature intensity"
              + "of predicted lipid");

  public static final BooleanParameter searchForFAinMSMS =
      new BooleanParameter("Search for lipid class specific fragments in MS/MS spectra",
          "Search for lipid class specific fragments in MS/MS spectra");

  public static final MZToleranceParameter mzToleranceMS2 =
      new MZToleranceParameter("m/z tolerance MS2 level:",
          "Enter m/z tolerance for exact mass database matching on MS2 level");

  public static final DoubleParameter noiseLevel = new DoubleParameter(
      "Noise level for MS/MS scans", "Intensities less than this value are interpreted as noise.");

  public LipidSearchParameters() {
    super(new Parameter[] {peakLists, lipidTypes, minChainLength, maxChainLength, maxDoubleBonds,
        maxOxidationValue, mzTolerance, ionizationMethod, searchForIsotopes,
        isotopeRetentionTimeTolerance, relativeIsotopeIntensityTolerance, searchForFAinMSMS,
        mzToleranceMS2, noiseLevel});
  }

}
