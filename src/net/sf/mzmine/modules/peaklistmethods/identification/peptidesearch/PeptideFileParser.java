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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */


package net.sf.mzmine.modules.peaklistmethods.identification.peptidesearch;

import net.sf.mzmine.data.proteomics.PeptideIdentityDataFile;

public interface PeptideFileParser {

	/**
	 * Returns the number of queries (possible identifications) in the file.
	 * 
	 * @return int number of queries
	 */
	public int getNumOfQueries();

	/**
	 * Parse the parameters section of the file
	 * 
	 * @param pepDataFile
	 */
	public void parseParameters(PeptideIdentityDataFile pepDataFile);

	/**
	 * Parse the masses values used in the calculation of fragment ions. 
	 * The values are stored in the peptide identification data file
	 * 
	 * @param pepDataFile
	 */
	public void parseDefaultMasses(PeptideIdentityDataFile pepDataFile);

	/**
	 * Parse the query according with the number and sets the information in the peptide identification file.
	 * 
	 * @param queryNumber
	 * @param pepDataFile
	 */
	public void parseQuery(int queryNumber, PeptideIdentityDataFile pepDataFile);

}
