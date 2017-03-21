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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.google.common.collect.Range;

import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.DBCompound;
import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.DBGateway;
import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.OnlineDatabase;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;

public class PubChemGateway implements DBGateway {

    public static final String pubchemEntryAddress = "https://pubchem.ncbi.nlm.nih.gov/summary/summary.cgi?cid=";
    public static final String searchURL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?usehistory=n&db=pccompound&sort=cida&retmax=";
    public static final String compoundURL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=pccompound&id=";
    public static final String pubchem2DStructureAddress = "https://pubchem.ncbi.nlm.nih.gov/summary/summary.cgi?disopt=SaveSDF&cid=";
    public static final String pubchem3DStructureAddress = "https://pubchem.ncbi.nlm.nih.gov/summary/summary.cgi?disopt=3DSaveSDF&cid=";

    /**
     * Searches for CIDs of PubChem compounds based on their exact
     * (monoisotopic) mass. Returns maximum numOfResults results sorted by the
     * CID. If chargedOnly parameter is set, returns only molecules with
     * non-zero charge.
     */
    public String[] findCompounds(double mass, MZTolerance mzTolerance,
            int numOfResults, ParameterSet parameters) throws IOException {

        Range<Double> toleranceRange = mzTolerance.getToleranceRange(mass);

        StringBuilder pubchemUrl = new StringBuilder();

        pubchemUrl.append(searchURL);
        pubchemUrl.append(numOfResults);
        pubchemUrl.append("&term=");
        pubchemUrl.append(toleranceRange.lowerEndpoint());
        pubchemUrl.append(":");
        pubchemUrl.append(toleranceRange.upperEndpoint());
        pubchemUrl.append("[MonoisotopicMass]");

        NodeList cidElements;

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document parsedResult = builder.parse(pubchemUrl.toString());

            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            XPathExpression expr = xpath.compile("//eSearchResult/IdList/Id");
            cidElements = (NodeList) expr.evaluate(parsedResult,
                    XPathConstants.NODESET);

        } catch (Exception e) {
            e.printStackTrace();
            throw (new IOException(e));
        }

        String cidArray[] = new String[cidElements.getLength()];
        for (int i = 0; i < cidElements.getLength(); i++) {
            Element cidElement = (Element) cidElements.item(i);
            cidArray[i] = cidElement.getTextContent();
        }

        return cidArray;

    }

    /**
     * This method retrieves the details about a PubChem compound
     * 
     */
    public DBCompound getCompound(String CID, ParameterSet parameters)
            throws IOException {

        String url = compoundURL + CID;

        Element nameElement, formulaElement;

        try {

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document parsedResult = builder.parse(url);

            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();

            XPathExpression expr = xpath.compile(
                    "//eSummaryResult/DocSum/Item[@Name='MeSHHeadingList']/Item");
            NodeList nameElementNL = (NodeList) expr.evaluate(parsedResult,
                    XPathConstants.NODESET);
            nameElement = (Element) nameElementNL.item(0);

            if (nameElement == null) {
                expr = xpath.compile(
                        "//eSummaryResult/DocSum/Item[@Name='SynonymList']/Item");
                nameElementNL = (NodeList) expr.evaluate(parsedResult,
                        XPathConstants.NODESET);
                nameElement = (Element) nameElementNL.item(0);
            }

            if (nameElement == null) {
                expr = xpath.compile(
                        "//eSummaryResult/DocSum/Item[@Name='IUPACName']");
                nameElementNL = (NodeList) expr.evaluate(parsedResult,
                        XPathConstants.NODESET);
                nameElement = (Element) nameElementNL.item(0);
            }

            if (nameElement == null)
                throw new IOException("Could not parse compound name");

            expr = xpath.compile(
                    "//eSummaryResult/DocSum/Item[@Name='MolecularFormula']");
            NodeList formulaElementNL = (NodeList) expr.evaluate(parsedResult,
                    XPathConstants.NODESET);
            formulaElement = (Element) formulaElementNL.item(0);

        } catch (Exception e) {
            throw new IOException(e);
        }

        String compoundName = nameElement.getTextContent();

        String compoundFormula = formulaElement.getTextContent();

        URL entryURL = new URL(pubchemEntryAddress + CID);
        URL structure2DURL = new URL(pubchem2DStructureAddress + CID);
        URL structure3DURL = new URL(pubchem3DStructureAddress + CID);

        DBCompound newCompound = new DBCompound(OnlineDatabase.PubChem, CID,
                compoundName, compoundFormula, entryURL, structure2DURL,
                structure3DURL);

        return newCompound;

    }

}
