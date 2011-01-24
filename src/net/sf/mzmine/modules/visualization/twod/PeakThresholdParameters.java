/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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
package net.sf.mzmine.modules.visualization.twod;


import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;

public class PeakThresholdParameters extends SimpleParameterSet {

	public static final Parameter intensityThreshold = new SimpleParameter(
			ParameterType.DOUBLE, "Intensity threshold",
			"user-defined intensity threshold", "Intensity threshold",
			new Double(0.0), null, null, MZmineCore.getIntensityFormat());
	public static final Parameter topThreshold = new SimpleParameter(            
			ParameterType.INTEGER, "Top peaks threshold", "User-defined top peaks threshold", 0);

	public static final Parameter topThresholdArea = new SimpleParameter(            
			ParameterType.INTEGER, "Top peaks threshold in the displayed area", "User-defined top peaks threshold in the displayed area", 0);

	public static final Parameter comboBoxIndexThreshold = new SimpleParameter(            
			ParameterType.INTEGER, "Index threshold combo box", "Index threshold combo box", 0);

	public PeakThresholdParameters() {
		super(new Parameter[] { intensityThreshold, topThreshold, topThresholdArea,comboBoxIndexThreshold});
	}
}
