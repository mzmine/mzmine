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
import java.util.Hashtable;
import java.util.Map;

import javax.xml.rpc.ServiceException;

import metlin.LineInfo;
import metlin.MetaboliteSearchRequest;
import metlin.MetlinPortType;
import metlin.MetlinServiceLocator;
import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.DBCompound;
import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.DBGateway;
import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.OnlineDatabase;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.util.RangeUtils;

import org.apache.axis.AxisFault;

import com.google.common.collect.Range;

public class MetLinGateway implements DBGateway {

    private static final String adduct[] = { "M" };

    private static final String metLinEntryAddress = "http://metlin.scripps.edu/metabo_info.php?molid=";
    private static final String metLinStructureAddress1 = "http://metlin.scripps.edu/structure/";
    private static final String metLinStructureAddress2 = ".mol";

    private Map<String, LineInfo> retrievedMolecules = new Hashtable<String, LineInfo>();

    public synchronized String[] findCompounds(double mass,
	    MZTolerance mzTolerance, int numOfResults, ParameterSet parameters)
	    throws IOException {

	Range<Double> toleranceRange = mzTolerance.getToleranceRange(mass);

	MetlinServiceLocator locator = new MetlinServiceLocator();
	MetlinPortType serv;
	try {
	    serv = locator.getMetlinPort();
	} catch (ServiceException e) {
	    throw (new IOException(e));
	}

	// Search mass as float[]
	float searchMass[] = new float[] { (float) RangeUtils
		.rangeCenter(toleranceRange) };
	float searchTolerance = (float) (RangeUtils.rangeLength(toleranceRange) / 2.0);

	final String token = parameters.getParameter(
		MetLinParameters.SECURITY_TOKEN).getValue();

	MetaboliteSearchRequest searchParams = new MetaboliteSearchRequest(
		token, searchMass, adduct, searchTolerance, "Da");
	LineInfo resultsData[][];
	try {
	    resultsData = serv.metaboliteSearch(searchParams);
	} catch (AxisFault e) {
	    // For some reason, the METLIN SOAP gateway throws AxisFault caused
	    // by ArrayStoreException if no result is found. I suspect their
	    // SOAP response is malformed and Axis does not like it.
	    resultsData = new LineInfo[1][0];
	}

	if (resultsData.length == 0) {
	    throw (new IOException("Results could not be retrieved from METLIN"));

	}
	final int totalResults = Math.min(resultsData[0].length, numOfResults);
	String metlinIDs[] = new String[totalResults];

	for (int i = 0; i < totalResults; i++) {
	    LineInfo metlinEntry = resultsData[0][i];
	    String metlinID = metlinEntry.getMolid();
	    retrievedMolecules.put(metlinID, metlinEntry);
	    metlinIDs[i] = metlinID;
	}

	return metlinIDs;

    }

    /**
     * This method retrieves the details about METLIN compound
     * 
     */
    public DBCompound getCompound(String ID, ParameterSet parameters)
	    throws IOException {

	URL entryURL = new URL(metLinEntryAddress + ID);

	LineInfo metlinEntry = retrievedMolecules.get(ID);

	if (metlinEntry == null) {
	    throw new IOException("Unknown ID " + ID);
	}

	String compoundName = metlinEntry.getName();

	if (compoundName == null) {
	    throw (new IOException(
		    "Could not parse compound name for compound " + ID));
	}

	String compoundFormula = metlinEntry.getFormula();

	URL structure2DURL = new URL(metLinStructureAddress1 + ID
		+ metLinStructureAddress2);

	URL structure3DURL = null;

	/*
	DBCompound newCompound = new DBCompound(OnlineDatabase.METLIN, ID,
		compoundName, compoundFormula, entryURL, structure2DURL,
		structure3DURL);

	return newCompound;
	
	*/
	return null;

    }
}
