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
package net.sf.mzmine.modules.rawdata.datasetfilters.preview;

import java.util.logging.Logger;
import net.sf.mzmine.modules.rawdata.datasetfilters.RawDataFilteringParameters;
import net.sf.mzmine.util.dialogs.ParameterSetupDialogWithChromatogramPreview;

/**
 * This class extends ParameterSetupDialog class, including a spectraPlot. This
 * is used to preview how the selected raw data filter and his parameters works
 * over the raw data file.
 */
public class RawDataFilterSetupDialog extends ParameterSetupDialogWithChromatogramPreview {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	
	/**
	 * @param parameters
	 * @param rawDataFilterTypeNumber
	 */
	public RawDataFilterSetupDialog(RawDataFilteringParameters parameters,
			int rawDataFilterTypeNumber) {

		super(
				RawDataFilteringParameters.rawDataFilterNames[rawDataFilterTypeNumber] + "'s parameter setup dialog ",
				parameters.getRawDataFilteringParameters(rawDataFilterTypeNumber),
				RawDataFilteringParameters.rawDataFilterHelpFiles[rawDataFilterTypeNumber]);

		
	}


	private void reloadPreview() {
		
	}

	
	
}