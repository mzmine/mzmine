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
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.DBCompound;
import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.DBGateway;
import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.OnlineDatabase;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.util.InetUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.google.common.collect.Range;

public class HMDBGateway implements DBGateway {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private static final String hmdbSeachAddress = "http://www.hmdb.ca/structures/search/metabolites/mass?search_type=monoisotopic&";
    private static final String hmdbStructureAddress = "http://structures.wishartlab.com/molecules/";
    private static final String hmdbEntryAddress = "http://www.hmdb.ca/metabolites/";

    public String[] findCompounds(double mass, MZTolerance mzTolerance,
	    int numOfResults, ParameterSet parameters) throws IOException {

	Range<Double> toleranceRange = mzTolerance.getToleranceRange(mass);

	String queryAddress = hmdbSeachAddress + "&query_from="
		+ toleranceRange.lowerEndpoint() + "&query_to="
		+ toleranceRange.upperEndpoint();

	URL queryURL = new URL(queryAddress);

	// Submit the query
	logger.finest("Loading URL " + queryAddress);
	String queryResult = InetUtils.retrieveData(queryURL);

	// Organize the IDs as a TreeSet to keep them sorted
	TreeSet<String> results = new TreeSet<String>();

	// Find IDs in the HTML data
	Pattern pat = Pattern.compile("metabolites/(HMDB[0-9]{5,})");
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

	logger.finest("Obtaining information about HMDB compound id " + ID);

	Element nameElement, formulaElement;

	try {

	    final String url = hmdbEntryAddress + ID + ".xml";
	    logger.finest("Loading URL " + url);
	    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	    DocumentBuilder builder = dbf.newDocumentBuilder();
	    Document parsedResult = builder.parse(url);

	    XPathFactory factory = XPathFactory.newInstance();
	    XPath xpath = factory.newXPath();

	    XPathExpression expr = xpath.compile("//metabolite/name");
	    NodeList nameElementNL = (NodeList) expr.evaluate(parsedResult,
		    XPathConstants.NODESET);
	    nameElement = (Element) nameElementNL.item(0);

	    if (nameElement == null)
		throw new IOException("Could not parse compound name");

	    expr = xpath.compile("//metabolite/chemical_formula");
	    NodeList formulaElementNL = (NodeList) expr.evaluate(parsedResult,
		    XPathConstants.NODESET);
	    formulaElement = (Element) formulaElementNL.item(0);
	    if (formulaElement == null)
		throw new IOException("Could not parse compound formula");

	} catch (Exception e) {
	    throw new IOException(e);
	}

	final String compoundName = nameElement.getTextContent();
	final String compoundFormula = formulaElement.getTextContent();
	final URL structure2DURL = new URL(hmdbStructureAddress + ID + ".sdf");
	final URL structure3DURL = new URL(hmdbStructureAddress + ID + ".pdb");
	final URL entryURL = new URL(hmdbEntryAddress + ID);

	DBCompound newCompound = new DBCompound(OnlineDatabase.HMDB, ID,
		compoundName, compoundFormula, entryURL, structure2DURL,
		structure3DURL);

	return newCompound;

    }
}
