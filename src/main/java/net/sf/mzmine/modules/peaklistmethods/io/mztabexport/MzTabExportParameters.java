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

package net.sf.mzmine.modules.peaklistmethods.io.mztabexport;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;

public class MzTabExportParameters extends SimpleParameterSet {

    public static final PeakListsParameter peakLists = new PeakListsParameter(1);

    public static final FileNameParameter filename = new FileNameParameter(
	    "Filename",
            "Use pattern \"{}\" in the file name to substitute with peak list name. " +
            "(i.e. \"blah{}blah.mzTab\" would become \"blahSourcePeakListNameblah.mzTab\"). " +
            "If the file already exists, it will be overwritten.",
	    "mzTab",
	    32);
    
    public static final BooleanParameter exportall = new BooleanParameter(
	    "Include all peaks", "Includes peaks with unknown identity"); 

    public MzTabExportParameters() {
	super(new Parameter[] { peakLists, filename, exportall});
    }
}
