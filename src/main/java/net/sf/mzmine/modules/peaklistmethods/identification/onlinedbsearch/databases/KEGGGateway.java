/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.databases;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.DBCompound;
import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.DBGateway;
import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.OnlineDatabase;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.util.InetUtils;

import com.google.common.collect.Range;

public class KEGGGateway implements DBGateway {

		public static final String keggEntryAddress = "http://www.kegg.jp/entry/";
    private static final String keggFindAddress1 = "http://rest.kegg.jp/find/compound/";
    private static final String keggFindAddress2 = "/exact_mass";
    private static final String keggGetAddress = "http://rest.kegg.jp/get/";

    private static final String kegg2DStructureAddress = "https://www.genome.jp/dbget-bin/www_bget?-f+m+compound+";
    private static final String met3DStructureAddress1 = "http://www.3dmet.dna.affrc.go.jp/pdb/";
    private static final String met3DStructureAddress2 = ".pdb";

    public String[] findCompounds(double mass, MZTolerance mzTolerance,
	    int numOfResults, ParameterSet parameters) throws IOException {

	Range<Double> toleranceRange = mzTolerance.getToleranceRange(mass);

	String queryAddress = keggFindAddress1 + toleranceRange.lowerEndpoint()
		+ "-" + toleranceRange.upperEndpoint() + keggFindAddress2;

	URL queryURL = new URL(queryAddress);

	String queryResult = InetUtils.retrieveData(queryURL);

	ArrayList<String> results = new ArrayList<String>();

	Pattern pat = Pattern.compile("cpd:(C[0-9]+)");
	Matcher matcher = pat.matcher(queryResult);
	while (matcher.find()) {
	    String keggID = matcher.group(1);
	    results.add(keggID);
	}

	return results.toArray(new String[0]);

    }

    /**
     * This method retrieves the details about a KEGG compound
     * 
     */
    public DBCompound getCompound(String ID, ParameterSet parameters)
	    throws IOException {

	String queryAddress = keggGetAddress + ID;

	URL queryURL = new URL(queryAddress);

	String compoundData = InetUtils.retrieveData(queryURL);

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
