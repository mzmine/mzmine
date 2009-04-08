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

package net.sf.mzmine.modules.visualization.twod;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.Range;
import org.dom4j.Element;

/**
 * 2D visualizer parameter set
 */
public class TwoDParameters extends SimpleParameterSet {

	public static final Integer[] msLevels = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

	public static final Parameter msLevel = new SimpleParameter(
			ParameterType.INTEGER, "MS level", "MS level of plotted scans", 1,
			msLevels);

	public static final Parameter retentionTimeRange = new SimpleParameter(
			ParameterType.RANGE, "Retention time",
			"Retention time (X axis) range", null, new Range(0, 600),
			new Double(0), null, MZmineCore.getRTFormat());

	public static final Parameter mzRange = new SimpleParameter(
			ParameterType.RANGE, "m/z range", "m/z (Y axis) range", "m/z",
			new Range(0, 1000), new Double(0), null, MZmineCore.getMZFormat());

	public TwoDParameters() {
		super(new Parameter[] { msLevel, retentionTimeRange, mzRange });
	}    


	public void exportValuesToXML(Element element) {
		super.exportValuesToXML(element);
		TwoDVisualizer.getThresholdParameters().exportValuesToXML(element);
	}  

	public void importValuesFromXML(Element element) {
		super.importValuesFromXML(element);
		TwoDVisualizer.getThresholdParameters().importValuesFromXML(element);      
	}

}
