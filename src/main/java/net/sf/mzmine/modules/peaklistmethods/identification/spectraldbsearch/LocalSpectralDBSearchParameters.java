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

package net.sf.mzmine.modules.peaklistmethods.identification.spectraldbsearch;

import java.text.DecimalFormat;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.MassListParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;

/**
 * 
 */
public class LocalSpectralDBSearchParameters extends SimpleParameterSet {

  public static final PeakListsParameter peakLists = new PeakListsParameter();

  public static final FileNameParameter dataBaseFile = new FileNameParameter("Database file",
      "(GNPS json, MONA json, NIST msp) Name of file that contains information for peak identification");

  public static final MassListParameter massList =
      new MassListParameter("MassList (MS2)", "MassList for MS/MS scans to match");

  public static final OptionalParameter<RTToleranceParameter> rtTolerance =
      new OptionalParameter<>(new RTToleranceParameter());

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();
  public static final DoubleParameter noiseLevelMS2 =
      new DoubleParameter("Noise level", "Noise level to filter MS2 mass lists",
          MZmineCore.getConfiguration().getIntensityFormat(), 0d);
  public static final DoubleParameter minCosine = new DoubleParameter("Min cos similarity",
      "Minimum cosine similarity. (All MS2 signals in the masslist against the spectral library entry. "
          + "Considers only signals which were found in both the masslist and the library entry)",
      new DecimalFormat("0.000"), 0.7);
  public static final IntegerParameter minMatch = new IntegerParameter("Min matched signals",
      "Minimum number of matched signals in MS2 masslist and spectral library entry (within mz tolerance)",
      4);

  public LocalSpectralDBSearchParameters() {
    super(new Parameter[] {peakLists, massList, dataBaseFile, mzTolerance, rtTolerance,
        noiseLevelMS2, minCosine, minMatch});
  }

}
