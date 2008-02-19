/*
 * Copyright 2006-2008 The MZmine Development Team
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

package net.sf.mzmine.modules.identification.qbixlipiddb;

import java.text.NumberFormat;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;

/**
 * 
 */
public class QBIXLipidDBSearchParameters extends SimpleParameterSet {

	public static final String QBIXInternalLipidDatabase = "QBIX internal lipid database";

	public static final Parameter databaseURI = new SimpleParameter(
			ParameterType.STRING, "Tamino server URI",
			"Address of the Tamino database server", null,
			"http://sbtamino1.ad.vtt.fi:8060/tamino/BfxDB03", null);

	public static final Parameter MZTolerance = new SimpleParameter(
			ParameterType.FLOAT, "ppm", "m/z tolerance in ppm", "ppm",
			new Float(50.0), new Float(0), null, NumberFormat.getNumberInstance());

	// TODO: Remove if unused parameter?
	public static final Parameter MassResolution = new SimpleParameter(
			ParameterType.FLOAT, "Mass resolution", "Mass resolution", "",
			new Float(0.2), new Float(0.0), null, NumberFormat.getNumberInstance());

	public QBIXLipidDBSearchParameters() {
		super(new Parameter[] { MZTolerance, MassResolution });
	}

}
