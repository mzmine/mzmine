/*
    Copyright 2005 VTT Biotechnology

    This file is part of MZmine.

    MZmine is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    MZmine is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MZmine; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/

package net.sf.mzmine.methods.normalization.linear;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.Parameter.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;


public class LinearNormalizerParameters extends SimpleParameterSet {

	protected static final String NormalizationTypeAverageIntensity = "Average intensity";
	protected static final String NormalizationTypeAverageSquaredIntensity = "Average squared intensity";
	protected static final String NormalizationTypeMaximumPeakHeight = "Maximum peak intensity";
	protected static final String NormalizationTypeTotalRawSignal = "Total raw signal";
	
	protected static final Object[] NormalizationTypePossibleValues = { NormalizationTypeAverageIntensity, NormalizationTypeAverageSquaredIntensity, NormalizationTypeMaximumPeakHeight, NormalizationTypeTotalRawSignal };

	protected static final Parameter NormalizationType = new SimpleParameter(	ParameterType.STRING,
			"Normalization type",
			"Normalize intensities by...",
			NormalizationTypeAverageIntensity,
			NormalizationTypePossibleValues);
	

	public Parameter[] getParameters() {
		return new Parameter[] { NormalizationType };
	}

}