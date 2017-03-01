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
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.DBCompound;
import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.DBGateway;
import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.OnlineDatabase;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.util.InetUtils;

import com.google.common.collect.Range;

public class YMDBGateway implements DBGateway {

    private static final String ymdbSeachAddress = "http://www.ymdb.ca/structures/search/compounds/mass?";
    private static final String ymdbEntryAddress = "http://www.ymdb.ca/structures/structures/compounds/";

    public String[] findCompounds(double mass, MZTolerance mzTolerance,
	    int numOfResults, ParameterSet parameters) throws IOException {

	Range<Double> toleranceRange = mzTolerance.getToleranceRange(mass);

	String queryAddress = ymdbSeachAddress + "query_from="
		+ toleranceRange.lowerEndpoint() + "&query_to="
		+ toleranceRange.upperEndpoint();

	URL queryURL = new URL(queryAddress);

	// Submit the query
	String queryResult = InetUtils.retrieveData(queryURL);

	// Organize the IDs as a TreeSet to keep them sorted
	TreeSet<String> results = new TreeSet<String>();

	// Find IDs in the HTML data
	Pattern pat = Pattern.compile("/compounds/(YMDB[0-9]{5})");
	Matcher matcher = pat.matcher(queryResult);
	while (matcher.find()) {
	    String ymdbID = matcher.group(1);
	    results.add(ymdbID);
	}

	// Remove all except first numOfResults IDs. The reason why we first
	// retrieve all results and then remove those above numOfResults is to
	// keep the lowest YDMB IDs - these may be the most interesting ones.
	while (results.size() > numOfResults) {
	    String lastItem = results.last();
	    results.remove(lastItem);
	}

	return results.toArray(new String[0]);

    }

    /**
     * This method retrieves the details about YMDB compound
     * 
     */
    public DBCompound getCompound(String ID, ParameterSet parameters)
	    throws IOException {

	// We will parse the name and formula from the SDF file, it seems like
	// the easiest way
	URL sdfURL = new URL(ymdbEntryAddress + ID + ".sdf");

	String sdfRecord = InetUtils.retrieveData(sdfURL);
	String lines[] = sdfRecord.split("\n");

	String compoundName = null;
	String compoundFormula = null;
	URL entryURL = new URL(ymdbEntryAddress + ID);
	URL structure2DURL = sdfURL;
	URL structure3DURL = sdfURL;

	for (int i = 0; i < lines.length - 1; i++) {

	    if (lines[i].contains("> <GENERIC_NAME>")) {
		compoundName = lines[i + 1];
	    }

	    if (lines[i].contains("> <JCHEM_FORMULA>")) {
		compoundFormula = lines[i + 1];
	    }
	}

	if (compoundName == null) {
	    throw (new IOException("Could not parse compound name"));
	}

	DBCompound newCompound = new DBCompound(OnlineDatabase.YMDB, ID,
		compoundName, compoundFormula, entryURL, structure2DURL,
		structure3DURL);

	return newCompound;

    }
}
