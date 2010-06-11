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

package net.sf.mzmine.modules.peaklistmethods.identification.dbsearch.databases;

import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.mzmine.modules.peaklistmethods.identification.dbsearch.DBCompound;
import net.sf.mzmine.modules.peaklistmethods.identification.dbsearch.DBGateway;
import net.sf.mzmine.modules.peaklistmethods.identification.dbsearch.OnlineDatabase;
import net.sf.mzmine.util.InetUtils;

public class MetLinGateway implements DBGateway {

	public static final String metLinEntryAddress = "http://metlin.scripps.edu/metabo_info.php?molid=";
	public static final String metLinStructureAddress1 = "http://metlin.scripps.edu/structure/";
	public static final String metLinStructureAddress2 = ".mol";

	/**
	 */
	public String[] findCompounds(double mass, double massTolerance,
			int numOfResults) throws IOException {

		String queryAddress = "http://metlin.scripps.edu/metabo_list.php?mass_min="
				+ (mass - massTolerance)
				+ "&mass_max="
				+ (mass + massTolerance);

		URL queryURL = new URL(queryAddress);

		// Submit the query
		String queryResult = InetUtils.retrieveData(queryURL);

		Vector<String> results = new Vector<String>();

		// Find IDs in the HTML data
		Pattern pat = Pattern.compile("\"metabo_info.php\\?molid=([0-9]+)\">\\1");
		Matcher matcher = pat.matcher(queryResult);
		while (matcher.find()) {
			String MID = matcher.group(1);
			results.add(MID);
			if (results.size() == numOfResults)
				break;
		}

		return results.toArray(new String[0]);

	}

	/**
	 * This method retrieves the details about METLIN compound
	 * 
	 */
	public DBCompound getCompound(String ID) throws IOException {

		URL entryURL = new URL(metLinEntryAddress + ID);

		String metLinEntry = InetUtils.retrieveData(entryURL);

		String compoundName = null;
		String compoundFormula = null;
		URL structure2DURL = null;
		URL structure3DURL = null;

		// Find compound name
		Pattern patName = Pattern.compile(
				"Name</td>.*?<td.*?return false\">(.+?)</a></td>",
				Pattern.DOTALL);
		Matcher matcherName = patName.matcher(metLinEntry);
		if (matcherName.find()) {
			compoundName = matcherName.group(1);
		}

		// Find compound formula
		Pattern patFormula = Pattern.compile(
				"Formula</td>.*?<td.*?</script>(.+?)</td>", Pattern.DOTALL);
		Matcher matcherFormula = patFormula.matcher(metLinEntry);
		if (matcherFormula.find()) {
			String htmlFormula = matcherFormula.group(1);
			compoundFormula = htmlFormula.replaceAll("<[^>]+>", "");
		}

		// Unfortunately, 2D structures provided by METLIN cannot be loaded
		// into CDK (throws CDKException). They can be loaded into JMol, so
		// we can show 3D structure.
		structure3DURL = new URL(metLinStructureAddress1 + ID
				+ metLinStructureAddress2);

		if (compoundName == null) {
			throw (new IOException(
					"Could not parse compound name for compound " + ID));
		}

		DBCompound newCompound = new DBCompound(OnlineDatabase.METLIN, ID,
				compoundName, compoundFormula, entryURL, structure2DURL,
				structure3DURL);

		return newCompound;

	}
}
