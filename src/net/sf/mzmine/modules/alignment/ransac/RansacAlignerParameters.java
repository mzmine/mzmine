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
			"Maximum allowed M/Z difference", "m/z", new Double(0.2),
			new Double(0.0), null, MZmineCore.getMZFormat());

	public static final Parameter RTToleranceValueAbs = new SimpleParameter(
			ParameterType.DOUBLE, "Absolute RT tolerance",
			"Maximum allowed absolute RT difference", null, new Double(15.0),
			new Double(0.0), null, MZmineCore.getRTFormat());

	public static final Parameter OptimizationIterations = new SimpleParameter(
			ParameterType.INTEGER, "RANSAC Iterations",
			"Min optimization iterations", new Integer(100));

	public static final Parameter NMinPoints = new SimpleParameter(
			ParameterType.DOUBLE, "NMinPoints",
			"Number Minimun of Points", new Double(0.2));

	public static final Parameter Margin = new SimpleParameter(
			ParameterType.DOUBLE, "Margin",
			"Margin", new Double(2.0));

	public static final Parameter curve = new SimpleParameter(
			ParameterType.BOOLEAN, "Curve",
			"Switch between curve model or lineal", new Boolean(false));

	public static final Parameter chart = new SimpleParameter(
			ParameterType.BOOLEAN, "Show Charts",
			"Show the aligment on some charts", new Boolean(false));

	public RansacAlignerParameters() {
		super(new Parameter[]{peakListName, MZTolerance, RTToleranceValueAbs, OptimizationIterations, NMinPoints, Margin, curve, chart});
	}
}
