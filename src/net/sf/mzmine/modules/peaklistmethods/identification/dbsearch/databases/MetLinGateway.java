/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.rpc.ServiceException;

import metlinapi.MetaboliteRequest;
import metlinapi.MetlinPortType;
import metlinapi.MetlinServiceLocator;
import net.sf.mzmine.modules.peaklistmethods.identification.dbsearch.DBCompound;
import net.sf.mzmine.modules.peaklistmethods.identification.dbsearch.DBGateway;
import net.sf.mzmine.parameters.parametertypes.MZTolerance;
import net.sf.mzmine.util.InetUtils;
import net.sf.mzmine.util.Range;

public class MetLinGateway implements DBGateway {

	private static final String adduct[] = { "M" };

	public static final String metLinSearchAddress = "http://metlin.scripps.edu/metabo_list.php?";
	public static final String metLinEntryAddress = "http://metlin.scripps.edu/metabo_info.php?molid=";
	public static final String metLinStructureAddress1 = "http://metlin.scripps.edu/structure/";
	public static final String metLinStructureAddress2 = ".mol";

	/**
	 */
	public String[] findCompounds(double mass, MZTolerance mzTolerance,
			int numOfResults) throws IOException {

		Range toleranceRange = mzTolerance.getToleranceRange(mass);

		MetlinServiceLocator locator = new MetlinServiceLocator();
		MetlinPortType serv;
		try {
			serv = locator.getMetlinPort();
		} catch (ServiceException e) {
			throw (new IOException(e));
		}

		// Search mass as float[]
		float[] searchMass = new float[] { (float) toleranceRange.getAverage() };

		Float searchTolerance = (float) toleranceRange.getSize() / 2;

		MetaboliteRequest requestParameters = new MetaboliteRequest(searchMass,
				adduct, searchTolerance, "Da");

		String[] results = serv.metaboliteSearch(requestParameters);

		System.out.println(results.length + Arrays.toString(results) + " "
				+ results[0]);
		return adduct;

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
				"</iframe>',1,450,300\\)\">(.+?)&nbsp;&nbsp;&nbsp;&nbsp",
				Pattern.DOTALL);
		Matcher matcherName = patName.matcher(metLinEntry);
		if (matcherName.find()) {
			compoundName = matcherName.group(1);
		}

		// Find compound formula
		Pattern patFormula = Pattern.compile(
				"Formula.*?<td.*?</script>(.+?)</td>", Pattern.DOTALL);
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

		DBCompound newCompound = new DBCompound(
				// OnlineDatabase.METLIN,
				null, ID, compoundName, compoundFormula, entryURL,
				structure2DURL, structure3DURL);

		return newCompound;

	}
}
