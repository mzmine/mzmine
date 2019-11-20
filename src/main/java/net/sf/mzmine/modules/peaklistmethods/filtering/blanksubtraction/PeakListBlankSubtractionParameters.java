/*
 * Copyright 2006-2018 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.filtering.blanksubtraction;

import net.sf.mzmine.modules.peaklistmethods.filtering.blanksubtraction.PeakListBlankSubtractionMasterTask.SubtractionType;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalParameter;
import net.sf.mzmine.parameters.parametertypes.PercentParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;

public class PeakListBlankSubtractionParameters extends SimpleParameterSet {

  public static final PeakListsParameter alignedPeakList =
      new PeakListsParameter("Aligned peak list", 1, 1);

  public static final RawDataFilesParameter blankRawDataFiles =
      new RawDataFilesParameter("Blank peak list(s)", 1, 100);

  /*public static final MZToleranceParameter mzTolerance = new MZToleranceParameter("m/z tolerance",
      "m/z tolerance allowed to be considered the same peak.");

  public static final RTToleranceParameter rtTolerance = new RTToleranceParameter("Rt tolerance",
      "Rt tolerance allowed to be considered the same compound");

  public static final ComboParameter<SubtractionType> subtractionType =
      new ComboParameter<SubtractionType>("Blank substraction type",
          "Defines if all peaks detected in the combined blank should be sustracted, or if only peaks found"
              + " in all blanks will be substracted.",
          SubtractionType.values());*/

  public static final IntegerParameter minBlanks = new IntegerParameter(
      "Minimum # of detection in blanks",
      "Specifies in how many of the blank files a peak has to be detected, if 'Combined' is selected "
          + "in the 'Blank substraction type' parameter.");

  public static final OptionalParameter<PercentParameter> foldChange = new OptionalParameter<>(new PercentParameter("Fold change increase",
      "Specifies a percentage of increase of the intensity of a feature. If the intensity in the list to be"
          + " filtered increases more than the given percentage to the blank, it will not be deleted from "
          + "the feature list.",
      3.0, 1.0, 1E5));

  public PeakListBlankSubtractionParameters() {
    super(new Parameter[] {alignedPeakList, blankRawDataFiles,/* mzTolerance, rtTolerance, 
        subtractionType, */minBlanks, foldChange});
  };
}
