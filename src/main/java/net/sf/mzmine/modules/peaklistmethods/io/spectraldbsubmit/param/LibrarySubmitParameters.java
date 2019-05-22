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

/*
 * This module was prepared by Abi Sarvepalli, Christopher Jensen, and Zheng Zhang at the Dorrestein
 * Lab (University of California, San Diego).
 * 
 * It is freely available under the GNU GPL licence of MZmine2.
 * 
 * For any questions or concerns, please refer to:
 * https://groups.google.com/forum/#!forum/molecular_networking_bug_reports
 * 
 * Credit to the Du-Lab development team for the initial commitment to the MGF export module.
 */

package net.sf.mzmine.modules.peaklistmethods.io.spectraldbsubmit.param;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.MassListParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import net.sf.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import net.sf.mzmine.util.scans.sorting.ScanSortMode;

/**
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class LibrarySubmitParameters extends SimpleParameterSet {

  // scan selection and preprocessing
  public static final MassListParameter massList = new MassListParameter();
  public static final DoubleParameter noiseLevel = new DoubleParameter("Noise level",
      "Noise level to filter masslists", MZmineCore.getConfiguration().getIntensityFormat(), 0d);
  public static final IntegerParameter minSignals = new IntegerParameter("Min signals",
      "Minimum signals in a masslist (all other masslists are discarded)", 3);

  public static final ComboParameter<ScanSortMode> sorting = new ComboParameter<>("Sorting",
      "Sorting mode for filtered mass lists", ScanSortMode.values(), ScanSortMode.MAX_TIC);

  // submission and creation of libraries
  // save to local file
  public static final OptionalParameter<FileNameParameter> LOCALFILE =
      new OptionalParameter<>(new FileNameParameter("Local file", "Local library file"), false);
  public static final BooleanParameter EXPORT_GNPS_JSON = new BooleanParameter(
      "Export GNPS json file", "The GNPS library submission json format", true);
  public static final BooleanParameter EXPORT_MSP =
      new BooleanParameter("Export NIST msp file", "The NIST msp library format", true);
  // user and password
  public static final OptionalModuleParameter<GnpsLibrarySubmitParameters> SUBMIT_GNPS =
      new OptionalModuleParameter<>("Submit to GNPS", "Submit new entry to GNPS library",
          new GnpsLibrarySubmitParameters(), true);


  public LibrarySubmitParameters() {
    super(new Parameter[] {massList, noiseLevel, minSignals, sorting,
        // save to local file
        LOCALFILE, EXPORT_GNPS_JSON, EXPORT_MSP,
        // submit to online library
        SUBMIT_GNPS});
  }
}
