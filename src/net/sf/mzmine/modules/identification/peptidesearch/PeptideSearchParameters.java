/*
 * Copyright 2006-2010 The MZmine 2 Development Team
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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.identification.peptidesearch;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;

public class PeptideSearchParameters extends SimpleParameterSet {

	public static final Parameter proteinFile = new SimpleParameter(
			ParameterType.FILE_NAME, "Protein identification file",
			"Name of file that contains information for peptide identification");
	
	public static final Parameter significanceThreshold = new SimpleParameter(
			ParameterType.DOUBLE,
			"Significance threshold",
			"Threshold value to accept peptide's identity as true by peptide score",
			null, new Double(0.05), new Double(0.0), new Double(99.0), null);

	public PeptideSearchParameters() {
		super(new Parameter[] { proteinFile, significanceThreshold});
	}

}
