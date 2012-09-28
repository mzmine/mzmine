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
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.mzmine.modules.peaklistmethods.identification.dbsearch.DBCompound;
import net.sf.mzmine.modules.peaklistmethods.identification.dbsearch.DBGateway;
import net.sf.mzmine.modules.peaklistmethods.identification.dbsearch.OnlineDatabase;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.MZTolerance;
import net.sf.mzmine.util.InetUtils;
import net.sf.mzmine.util.Range;

public class HMDBGateway implements DBGateway {

    private static final String hmdbSeachAddress = "http://www.hmdb.ca/chemquery/mass?";
    private static final String hmdbStructureAddress = "http://structures.wishartlab.com/molecules/";
    private static final String hmdbEntryAddress = "http://www.hmdb.ca/metabolites/";

    public String[] findCompounds(double mass, MZTolerance mzTolerance,
	    int numOfResults, ParameterSet parameters) throws IOException {

	Range toleranceRange = mzTolerance.getToleranceRange(mass);

	String queryAddress = hmdbSeachAddress + "&query_from="
		+ toleranceRange.getMin() + "&query_to="
		+ toleranceRange.getMax();

	URL queryURL = new URL(queryAddress);

	// Submit the query
	String queryResult = InetUtils.retrieveData(queryURL);

	// Organize the IDs as a TreeSet to keep them sorted
	TreeSet<String> results = new TreeSet<String>();

	// Find IDs in the HTML data
	Pattern pat = Pattern.compile("metabolites/(HMDB[0-9]{5})");
	Matcher matcher = pat.matcher(queryResult);
	while (matcher.find()) {
	    String hmdbID = matcher.group(1);
	    results.add(hmdbID);
	}

	// Remove all except first numOfResults IDs. The reason why we first
	// retrieve all results and then remove those above numOfResults is to
	// keep the lowest HDMB IDs - these may be the most interesting ones.
	while (results.size() > numOfResults) {
	    String lastItem = results.last();
	    results.remove(lastItem);
	}

	return results.toArray(new String[0]);

    }

    /**
     * This method retrieves the details about HMDB compound
     * 
     */
    public DBCompound getCompound(String ID, ParameterSet parameters)
	    throws IOException {

	final URL entryURL = new URL(hmdbEntryAddress + ID);

	String metaboCard = InetUtils.retrieveData(entryURL);
	String lines[] = metaboCard.split("\n");

	String compoundName = null;
	String compoundFormula = null;
	final URL structure2DURL = new URL(hmdbStructureAddress + ID + ".sdf");
	final URL structure3DURL = new URL(hmdbStructureAddress + ID + ".pdb");

	for (int i = 0; i < lines.length - 1; i++) {

	    if (lines[i].contains("<td>Common Name</td>")) {
		Pattern pat = Pattern
			.compile("<td><strong>([^<]+)</strong></td>");
		Matcher matcher = pat.matcher(lines[i + 1]);
		if (matcher.find()) {
		    compoundName = matcher.group(1);
		}
	    }

	    if (lines[i].contains("<td>Chemical Formula</td>")) {
		Pattern pat = Pattern.compile("<td>(.+)</td>");
		Matcher matcher = pat.matcher(lines[i + 1]);
		if (matcher.find()) {
		    String htmlFormula = matcher.group(1);
		    compoundFormula = htmlFormula.replaceAll("<[^>]+>", "");
		}
	    }

	}

	if (compoundName == null) {
	    throw (new IOException("Could not parse compound name"));
	}

	DBCompound newCompound = new DBCompound(OnlineDatabase.HMDB, ID,
		compoundName, compoundFormula, entryURL, structure2DURL,
		structure3DURL);

	return newCompound;

    }
}
