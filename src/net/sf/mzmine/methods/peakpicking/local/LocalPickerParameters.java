/*
 * Copyright 2006 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.methods.peakpicking.local;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.Parameter.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.methods.MethodParameters;

class LocalPickerParameters extends MethodParameters {

    protected static final Parameter binSize = new SimpleParameter(	ParameterType.DOUBLE,
			"M/Z bin width",
			"Width of M/Z range for each precalculated XIC",
			"Da",
			new Double(0.25));

    protected static final Parameter chromatographicThresholdLevel = new SimpleParameter(	
			ParameterType.DOUBLE,
			"Chromatographic threshold level",
			"Used in defining threshold level value from an XIC",
			"%",
			new Double(0.0));

    protected static final Parameter noiseLevel = new SimpleParameter(	
			ParameterType.DOUBLE,
			"Nouse level",
			"Intensities less than this value are interpreted as noise",
			"absolute",
			new Double(4.0));

    protected static final Parameter minimumPeakHeight = new SimpleParameter(	
			ParameterType.DOUBLE,
			"Min peak height",
			"Minimum acceptable peak height",
			"absolute",
			new Double(15.0));

    protected static final Parameter minimumPeakDuration = new SimpleParameter(	
			ParameterType.DOUBLE,
			"Min peak duration",
			"Minimum acceptable peak duration",
			"seconds",
			new Double(3.0));

    protected static final Parameter mzTolerance = new SimpleParameter(	
			ParameterType.DOUBLE,
			"M/Z tolerance",
			"Maximum allowed distance in M/Z between centroid peaks in successive scans",
			"Da",
			new Double(0.050));

    protected static final Parameter intTolerance = new SimpleParameter(	
			ParameterType.DOUBLE,
			"Intensity tolerance",
			"Maximum allowed deviation from expected /\\ shape of a peak in chromatographic direction",
			"%",
			new Double(0.20)); 
    
    
	public Parameter[] getParameters() {
		return new Parameter[] {binSize,chromatographicThresholdLevel,noiseLevel,minimumPeakHeight,minimumPeakDuration,mzTolerance,intTolerance};
	}

}