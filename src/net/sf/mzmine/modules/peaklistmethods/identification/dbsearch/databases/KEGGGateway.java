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

package net.sf.mzmine.modules.peaklistmethods.identification.dbsearch.databases;

import java.io.IOException;
import java.net.URL;

import javax.xml.rpc.ServiceException;

import keggapi.KEGGLocator;
import keggapi.KEGGPortType;
import net.sf.mzmine.modules.peaklistmethods.identification.dbsearch.DBCompound;
import net.sf.mzmine.modules.peaklistmethods.identification.dbsearch.DBGateway;
import net.sf.mzmine.modules.peaklistmethods.identification.dbsearch.OnlineDatabase;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.MZTolerance;
import net.sf.mzmine.util.Range;

public class KEGGGateway implements DBGateway {

    private static final String keggEntryAddress = "http://www.genome.jp/dbget-bin/www_bget?";
    private static final String kegg2DStructureAddress = "http://www.genome.jp/dbget-bin/www_bget?-f+m+";
    private static final String met3DStructureAddress1 = "http://www.3dmet.dna.affrc.go.jp/pdb2/";
    private static final String met3DStructureAddress2 = ".pdb";

    public String[] findCompounds(double mass, MZTolerance mzTolerance,
	    int numOfResults, ParameterSet parameters) throws IOException {

	Range toleranceRange = mzTolerance.getToleranceRange(mass);

	KEGGLocator locator = new KEGGLocator();
	KEGGPortType serv;
	try {
	    serv = locator.getKEGGPort();
	} catch (ServiceException e) {
	    throw (new IOException(e));
	}

	String[] results = serv.search_compounds_by_mass(
		(float) toleranceRange.getAverage(),
		(float) toleranceRange.getSize() / 2);

	return results;

    }

    /**
     * This method retrieves the details about a KEGG compound
     * 
     */
    public DBCompound getCompound(String ID, ParameterSet parameters)
	    throws IOException {

	KEGGLocator locator = new KEGGLocator();
	KEGGPortType serv;
	try {
	    serv = locator.getKEGGPort();
	} catch (ServiceException e) {
	    throw (new IOException(e));
	}

	String compoundData = serv.bget(ID);
	String dataLines[] = compoundData.split("\n");

	String compoundName = null, compoundFormula = null, ID3DMet = null;

	for (String line : dataLines) {
	    if (line.startsWith("NAME")) {
		compoundName = line.substring(12);
		if (compoundName.endsWith(";")) {
		    compoundName = compoundName.substring(0,
			    compoundName.length() - 1);
		}
	    }

	    if (line.startsWith("FORMULA")) {
		compoundFormula = line.substring(12);
	    }

	    // 3DMET id is last 6 characters on the line
	    if (line.contains("3DMET")) {
		ID3DMet = line.substring(line.length() - 6);
	    }

	}

	if ((compoundName == null) || (compoundFormula == null)) {
	    throw (new IOException("Could not obtain compound name and formula"));
	}

	URL entryURL = new URL(keggEntryAddress + ID);
	URL structure2DURL = new URL(kegg2DStructureAddress + ID);

	URL structure3DURL = null;

	if (ID3DMet != null) {
	    structure3DURL = new URL(met3DStructureAddress1 + ID3DMet
		    + met3DStructureAddress2);
	}

	DBCompound newCompound = new DBCompound(OnlineDatabase.KEGG, ID,
		compoundName, compoundFormula, entryURL, structure2DURL,
		structure3DURL);

	return newCompound;

    }

}
