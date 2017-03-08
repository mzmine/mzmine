/* 
 * Copyright (C) 2016 Du-Lab Team <dulab.binf@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package net.sf.mzmine.modules.peaklistmethods.io.mspexport;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;

/**
 *
 * @author Du-Lab Team <dulab.binf@gmail.com>
 */


public class MSPExportParameters extends SimpleParameterSet 
{
    public static final String ROUND_MODE_MAX = "Maximum";
    public static final String ROUND_MODE_SUM = "Sum";
    
    public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();
    
    public static final FileNameParameter FILENAME = new FileNameParameter(
	    "Filename",
	    "Name of the output MSP file. " +
	    "Use pattern \"{}\" in the file name to substitute with peak list name. " +
	    "(i.e. \"blah{}blah.msp\" would become \"blahSourcePeakListNameblah.msp\"). " +
	    "If the file already exists, it will be overwritten.",
	    "msp");
    
    public static final BooleanParameter FRACTIONAL_MZ = new BooleanParameter(
            "Fractional m/z values", "If checked, write fractional m/z values", 
            false);
   
    public static final ComboParameter <String> ROUND_MODE = 
            new ComboParameter <> (
                    "Merging Mode", 
                    "Determines how to merge intensities with the same m/z values",
                    new String[] {ROUND_MODE_MAX, ROUND_MODE_SUM},
                    ROUND_MODE_MAX);
    
    public MSPExportParameters() {
	super(new Parameter[] {PEAK_LISTS, FILENAME, FRACTIONAL_MZ, ROUND_MODE});
    }
}
