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

/* Code created was by or on behalf of Syngenta and is released under the open source license in use for the
 * pre-existing code or project. Syngenta does not assert ownership or copyright any over pre-existing work.
 */

package net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.databases;

import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.xml.rpc.ServiceException;

import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.DBCompound;
import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.DBGateway;
import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.OnlineDatabase;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.util.RangeUtils;

import com.chemspider.www.ExtendedCompoundInfo;
import com.chemspider.www.MassSpecAPILocator;
import com.chemspider.www.MassSpecAPISoap;
import com.google.common.collect.Range;

/**
 * Searches the ChemSpider database.
 * 
 */
public class ChemSpiderGateway implements DBGateway {

    // Logger.
    private static final Logger LOG = Logger.getLogger(ChemSpiderGateway.class
	    .getName());

    // Compound names.
    private static final String UNKNOWN_NAME = "Unknown name";
    private static final String ERROR_MESSAGE = "Error fetching compound info (possible invalid search token or deprecated structure?)";

    // Pattern for chemical structure URLs - replace CSID.
    private static final String STRUCTURE_URL_PATTERN = "http://www.chemspider.com/Chemical-Structure.CSID.html";
    private static final String STRUCTURE2D_URL_PATTERN = "http://www.chemspider.com/FilesHandler.ashx?type=str&id=CSID";
    private static final String STRUCTURE3D_URL_PATTERN = "http://www.chemspider.com/FilesHandler.ashx?type=str&3d=yes&id=CSID";

    // Pattern to clean-up formulas.
    private static final Pattern FORMULA_PATTERN = Pattern.compile("[\\W_]*");

    @Override
    public String[] findCompounds(final double mass,
	    final MZTolerance mzTolerance, final int numOfResults,
	    ParameterSet parameters) throws IOException {

	LOG.finest("Searching by mass...");

	// Get search range
	final Range<Double> mzRange = mzTolerance.getToleranceRange(mass);

	// These are returned in #CSID (numerical) order.
	final String[] results = createMassSpecAPI().searchByMass2(
		RangeUtils.rangeCenter(mzRange),
		RangeUtils.rangeLength(mzRange) / 2.0);

	// Copy results.
	final int len = Math.min(numOfResults, results.length);
	final String[] ids = new String[len];
	System.arraycopy(results, 0, ids, 0, len);

	return ids;
    }

    @Override
    public DBCompound getCompound(final String ID, ParameterSet parameters)
	    throws IOException {

	LOG.finest("Fetching compound info for CSID #" + ID);

	final MassSpecAPISoap massSpec = createMassSpecAPI();

	// Get security token.
	final String token = parameters.getParameter(
		ChemSpiderParameters.SECURITY_TOKEN).getValue();

	// Fetch compound info.
	ExtendedCompoundInfo info = null;
	try {
	    info = massSpec.getExtendedCompoundInfo(Integer.valueOf(ID), token);
	} catch (final RemoteException e) {

	    // We need to catch exceptions here - usually from deprecated
	    // structures in the ChemSpider database or
	    // invalid security token.
	    LOG.log(Level.WARNING, "Failed to fetch compound info for CSID #"
		    + ID, e);
	}

	// Determine name and formula.
	final String name;
	final String formula;
	if (info != null) {

	    // Use returned info.
	    final String commonName = info.getCommonName();
	    name = commonName == null ? UNKNOWN_NAME : commonName;
	    formula = FORMULA_PATTERN.matcher(info.getMF()).replaceAll("");

	} else {

	    // An error occurred.
	    name = ERROR_MESSAGE;
	    formula = null;
	}

	// Create and return the compound record.
	return new DBCompound(OnlineDatabase.CHEMSPIDER, ID, name, formula,
		new URL(STRUCTURE_URL_PATTERN.replaceFirst("CSID", ID)),
		new URL(STRUCTURE2D_URL_PATTERN.replaceFirst("CSID", ID)),
		new URL(STRUCTURE3D_URL_PATTERN.replaceFirst("CSID", ID)));
    }

    /**
     * Create a Mass Spec API handle.
     * 
     * @return the newly created handle.
     * @throws IOException
     *             if there were any problems.
     */
    private static MassSpecAPISoap createMassSpecAPI() throws IOException {

	LOG.finest("Create mass-spec API handle...");

	// Create API handles.
	final MassSpecAPISoap handle;
	try {
	    handle = new MassSpecAPILocator().getMassSpecAPISoap();
	} catch (ServiceException e) {
	    LOG.log(Level.WARNING,
		    "Problem initializing ChemSpider Mass-Spec API", e);
	    throw new IOException("Problem initializing ChemSpider API", e);
	}
	return handle;
    }

}
