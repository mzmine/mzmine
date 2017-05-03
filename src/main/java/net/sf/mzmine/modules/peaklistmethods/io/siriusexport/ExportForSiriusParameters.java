/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.io.siriusexport;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.MassListParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;

public class ExportForSiriusParameters extends SimpleParameterSet 
{
    public static final String ROUND_MODE_MAX = "Maximum";
    public static final String ROUND_MODE_SUM = "Sum";
  
    public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();
    
    public static final FileNameParameter FILENAME = new FileNameParameter(
	    "Filename",
	    "Name of the output MGF file. " +
	    "Use pattern \"{}\" in the file name to substitute with peak list name. " +
	    "(i.e. \"blah{}blah.mgf\" would become \"blahSourcePeakListNameblah.mgf\"). " +
	    "If the file already exists, it will be overwritten.",
	    "mgf");
    
    public static final BooleanParameter FRACTIONAL_MZ = new BooleanParameter(
            "Fractional m/z values", "If checked, write fractional m/z values", 
            true);
   
    public static final ComboParameter <String> ROUND_MODE = 
            new ComboParameter <> (
                    "Merging Mode", 
                    "Determines how to merge intensities with the same m/z values",
                    new String[] {ROUND_MODE_MAX, ROUND_MODE_SUM},
                    ROUND_MODE_MAX);
    
    public static final MassListParameter MASS_LIST = new MassListParameter();
    
    public ExportForSiriusParameters() {
	super(new Parameter[] {PEAK_LISTS, FILENAME, FRACTIONAL_MZ, ROUND_MODE, MASS_LIST});
    }
}
