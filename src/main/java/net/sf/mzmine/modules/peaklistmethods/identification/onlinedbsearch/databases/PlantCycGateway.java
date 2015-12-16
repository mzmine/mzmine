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
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.DBCompound;
import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.DBGateway;
import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.OnlineDatabase;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.util.InetUtils;

public class PlantCycGateway implements DBGateway {

    private static final String plantCycEntryAddress = "http://pmn.plantcyc.org/PLANT/NEW-IMAGE?type=COMPOUND&object=";

    private Map<String, String> retrievedNames = new Hashtable<String, String>();
    private Map<String, String> retrievedFormulas = new Hashtable<String, String>();

    public String[] findCompounds(double mass, MZTolerance mzTolerance,
	    int numOfResults, ParameterSet parameters) throws IOException {

	final double ppmTolerance = mzTolerance.getPpmToleranceForMass(mass);

	final String queryAddress = "http://pmn.plantcyc.org/PLANT/search-query?type=COMPOUND&monoisomw="
		+ mass + "&monoisotol=" + ppmTolerance;

	final URL queryURL = new URL(queryAddress);

	// Submit the query
	final String queryResult = InetUtils.retrieveData(queryURL);

	final List<String> results = new ArrayList<String>();

	// Find IDs in the HTML data
	Pattern pat = Pattern
		.compile("/PLANT/NEW-IMAGE\\?type=COMPOUND&object=([^\"]+)\">([^<]*)</A></TD><TD ALIGN=LEFT>([^<]*)</TD>");
	Matcher matcher = pat.matcher(queryResult);
	while (matcher.find()) {
	    String id = matcher.group(1);
	    String name = matcher.group(2);
	    String formula = matcher.group(3);
	    results.add(id);
	    retrievedNames.put(id, name);
	    retrievedFormulas.put(id, formula);
	    if (results.size() == numOfResults)
		break;
	}

	return results.toArray(new String[0]);

    }

    /**
     * This method retrieves the details about PlantCyc compound
     * 
     */
    public DBCompound getCompound(String ID, ParameterSet parameters)
	    throws IOException {

	final URL entryURL = new URL(plantCycEntryAddress + ID);

	final String compoundName = retrievedNames.get(ID);
	final String compoundFormula = retrievedFormulas.get(ID);

	// Unfortunately PlantCyc does not contain structures in MOL format
	URL structure2DURL = null;
	URL structure3DURL = null;

	if (compoundName == null) {
	    throw (new IOException("Invalid compound ID " + ID));
	}

	DBCompound newCompound = new DBCompound(OnlineDatabase.PLANTCYC, ID,
		compoundName, compoundFormula, entryURL, structure2DURL,
		structure3DURL);

	return newCompound;

    }
}
