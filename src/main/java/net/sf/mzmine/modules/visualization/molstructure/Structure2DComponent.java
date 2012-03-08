/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.visualization.molstructure;

import java.io.StringReader;

import org.openscience.cdk.ChemModel;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.io.MDLReader;
import org.openscience.jchempaint.JChemPaintViewerPanel;

public class Structure2DComponent extends JChemPaintViewerPanel {

	public Structure2DComponent(String structure) throws CDKException {

		super(new ChemModel(), 0, 0, true, false, null);

		// Load the structure
		StringReader reader = new StringReader(structure);

		MDLReader molReader = new MDLReader(reader);
		ChemModel chemModel = new ChemModel();
		chemModel = (ChemModel) molReader.read(chemModel);

		setChemModel(chemModel);

	}

}
