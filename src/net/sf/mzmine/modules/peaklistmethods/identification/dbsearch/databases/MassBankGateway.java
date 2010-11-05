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

public class MassBankGateway implements DBGateway {

	// Unfortunately, we have to list all the instrument types here
	public static final String massBankSearchAddress = "http://www.massbank.jp/jsp/Result.jsp?type=quick&inst=CE-ESI-TOF-MS&inst=ESI-IT-(MS)n&inst=ESI-IT-MS/MS&inst=ESI-QTOF-MS/MS&inst=ESI-QqQ-MS/MS&inst=ESI-QqTOF-MS/MS&inst=LC-ESI-FT-MS&inst=LC-ESI-IT-MS/MS&inst=LC-ESI-IT-TOF-MS&inst=LC-ESI-Q-MS&inst=LC-ESI-QTOF-MS/MS&inst=LC-ESI-QqQ-MS/MS&inst=LC-ESI-TOF-MS";

	public static final String massBankEntryAddress = "http://www.massbank.jp/jsp/FwdRecord.jsp?id=";

	/**
	 */
	public String[] findCompounds(double mass, double massTolerance,
			int numOfResults) throws IOException {

		String queryAddress = massBankSearchAddress + "&mz=" + mass + "&tol="
				+ massTolerance;

		URL queryURL = new URL(queryAddress);

		// Submit the query
		String queryResult = InetUtils.retrieveData(queryURL);

		Vector<String> results = new Vector<String>();

		// Find IDs in the HTML data
		Pattern pat = Pattern
				.compile("&nbsp;&nbsp;&nbsp;&nbsp;([A-Z]{2}[0-9]{6})&nbsp;");
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
	 * This method retrieves the details about the compound
	 * 
	 */
	public DBCompound getCompound(String ID) throws IOException {

		URL entryURL = new URL(massBankEntryAddress + ID);

		String massBankEntry = InetUtils.retrieveData(entryURL);

		String compoundName = null;
		String compoundFormula = null;
		URL structure2DURL = null;
		URL structure3DURL = null;
		URL databaseURL = entryURL;

		System.out.println(massBankEntry);
		// Find compound name
		Pattern patName = Pattern.compile("RECORD_TITLE: (.*)");
		Matcher matcherName = patName.matcher(massBankEntry);
		if (matcherName.find()) {
			compoundName = matcherName.group(1);
		}

		// Find compound formula
		Pattern patFormula = Pattern.compile("CH\\$FORMULA: (.*)");
		Matcher matcherFormula = patFormula.matcher(massBankEntry);
		if (matcherFormula.find()) {
			compoundFormula = matcherFormula.group(1);
		}

		if (compoundName == null) {
			throw (new IOException(
					"Could not parse compound name for compound " + ID));
		}

		DBCompound newCompound = new DBCompound(OnlineDatabase.MASSBANK, ID,
				compoundName, compoundFormula, databaseURL, structure2DURL,
				structure3DURL);

		return newCompound;

	}
}
