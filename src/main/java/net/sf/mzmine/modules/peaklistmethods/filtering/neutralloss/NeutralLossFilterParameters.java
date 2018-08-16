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

package net.sf.mzmine.modules.peaklistmethods.filtering.neutralloss;

import java.text.NumberFormat;
import java.util.Locale;
import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.elements.ElementsParameter;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.restrictions.elements.ElementalHeuristicParameters;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.MassListParameter;
import net.sf.mzmine.parameters.parametertypes.MultiChoiceParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalModuleParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalParameter;
import net.sf.mzmine.parameters.parametertypes.PercentParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;

public class NeutralLossFilterParameters extends SimpleParameterSet {

  public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

  public static final BooleanParameter checkRT =
      new BooleanParameter("Check RT", "Compare rt of peaks to parent.");

  public static final RTToleranceParameter rtTolerance = new RTToleranceParameter();

  public static final DoubleParameter minHeight = new DoubleParameter("Minimum height",
      "Minimum peak height to be considered as an isotope peak.",
      NumberFormat.getNumberInstance(Locale.ENGLISH), 0.0);

  public static final DoubleParameter neutralLoss =
      new DoubleParameter("Neutral loss", "Enter exact mass of neutral loss.",
          NumberFormat.getNumberInstance(Locale.ENGLISH), 0.0);
  
  public static final StringParameter molecule =
      new StringParameter("Molecule", "String of a neutral loss compound (e.g. HI). If this textbox is not empty, the \"Neutral loss\" field will be ignored.", "");
  
  public static final StringParameter suffix =
      new StringParameter("Name suffix", "Suffix to be added to peak list name. If \"auto\" then this module will create a suffix.", "auto");

  public NeutralLossFilterParameters() {
    super(new Parameter[] {PEAK_LISTS, mzTolerance, checkRT, rtTolerance, minHeight, neutralLoss, molecule,
        suffix});
  }
}
