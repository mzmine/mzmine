/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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
package net.sf.mzmine.modules.alignment.ransac;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;

public class RansacAlignerParameters extends SimpleParameterSet {

	public static final Parameter peakListName = new SimpleParameter(
			ParameterType.STRING, "Peak list name", "Peak list name", null,
			"Aligned peak list", null);

	public static final Parameter MZTolerance = new SimpleParameter(
			ParameterType.DOUBLE, "m/z tolerance",
			"Maximum allowed M/Z difference", "m/z", new Double(0.02),
			new Double(0.0), null, MZmineCore.getMZFormat());

	public static final Parameter RTTolerance = new SimpleParameter(
			ParameterType.DOUBLE, "RT tolerance",
			"Maximum allowed absolute RT difference", null, new Double(15.0),
			new Double(0.0), null, MZmineCore.getRTFormat());

	public static final Parameter OptimizationIterations = new SimpleParameter(
			ParameterType.INTEGER, "RANSAC Iterations",
			"Maximum number of iterations allowed in the algorithm", new Integer(0));

	public static final Parameter NMinPoints = new SimpleParameter(
			ParameterType.DOUBLE, "NMinPoints",
			"Number Minimun of Points", new Double(0.2));

	public static final Parameter Margin = new SimpleParameter(
			ParameterType.DOUBLE, "Threshold value",
			"Threshold value for determining when a data point fits a model", new Double(3.0));	

	public static final Parameter curve = new SimpleParameter(
			ParameterType.BOOLEAN, "Curve model",
			"Switch between curve model or lineal model", new Boolean(false));
	
	
	public RansacAlignerParameters() {
		super(new Parameter[]{peakListName, MZTolerance, RTTolerance, OptimizationIterations, NMinPoints, Margin, curve});
	}
}
