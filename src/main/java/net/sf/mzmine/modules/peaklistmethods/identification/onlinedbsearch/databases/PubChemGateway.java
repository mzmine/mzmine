/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.databases;

import java.io.IOException;
import java.net.URL;
import java.util.Hashtable;
import java.util.logging.Logger;

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
import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.OnlineDatabases;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;

public class PubChemGateway implements DBGateway {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  public static final String pubchemEntryAddress =
      "https://pubchem.ncbi.nlm.nih.gov/summary/summary.cgi?cid=";
  public static final String searchURL =
      "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?usehistory=y&db=pccompound&sort=cida&retmax=";
  public static final String compoundURL =
      "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=pccompound&rettype=xml";
  public static final String pubchem2DStructureAddress =
      "https://pubchem.ncbi.nlm.nih.gov/summary/summary.cgi?disopt=SaveSDF&cid=";
  public static final String pubchem3DStructureAddress =
      "https://pubchem.ncbi.nlm.nih.gov/summary/summary.cgi?disopt=3DSaveSDF&cid=";

  private final Hashtable<String, Element> compoundSummaryElements = new Hashtable<>();

  /**
   * Searches for CIDs of PubChem compounds based on their exact (monoisotopic) mass. Returns
   * maximum numOfResults results sorted by the CID. If chargedOnly parameter is set, returns only
   * molecules with non-zero charge.
   */
  public String[] findCompounds(double mass, MZTolerance mzTolerance, int numOfResults,
      ParameterSet parameters) throws IOException {

    final Range<Double> toleranceRange = mzTolerance.getToleranceRange(mass);

    try {

      final StringBuilder pubchemUrl = new StringBuilder();
      pubchemUrl.append(searchURL);
      pubchemUrl.append(numOfResults);
      pubchemUrl.append("&term=");
      pubchemUrl.append(toleranceRange.lowerEndpoint());
      pubchemUrl.append(":");
      pubchemUrl.append(toleranceRange.upperEndpoint());
      pubchemUrl.append("[MonoisotopicMass]");

      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = dbf.newDocumentBuilder();

      logger.finest("Searching PubChem via URL " + pubchemUrl.toString());
      Document parsedResult = builder.parse(pubchemUrl.toString());

      XPathFactory factory = XPathFactory.newInstance();
      XPath xpath = factory.newXPath();
      XPathExpression expr;

      expr = xpath.compile("//eSearchResult/WebEnv");
      NodeList webEnvNL = (NodeList) expr.evaluate(parsedResult, XPathConstants.NODESET);
      Element webEnvElement = (Element) webEnvNL.item(0);
      final String webEnvKey = webEnvElement.getTextContent();

      expr = xpath.compile("//eSearchResult/QueryKey");
      NodeList queryKeyNL = (NodeList) expr.evaluate(parsedResult, XPathConstants.NODESET);
      Element queryKeyElement = (Element) queryKeyNL.item(0);
      final String queryKey = queryKeyElement.getTextContent();

      expr = xpath.compile("//eSearchResult/IdList/Id");
      NodeList cidElements = (NodeList) expr.evaluate(parsedResult, XPathConstants.NODESET);
      String cidArray[] = new String[cidElements.getLength()];
      for (int i = 0; i < cidElements.getLength(); i++) {
        Element cidElement = (Element) cidElements.item(i);
        cidArray[i] = cidElement.getTextContent();
      }


      // Load the compound details. This is necessary to avoid generating too many queries to
      // PubChem. See the API Key section here: https://www.ncbi.nlm.nih.gov/books/NBK25497/
      final StringBuilder compoundUrl = new StringBuilder();
      compoundUrl.append(compoundURL);
      compoundUrl.append("&retmax=");
      compoundUrl.append(numOfResults);
      compoundUrl.append("&WebEnv=");
      compoundUrl.append(webEnvKey);
      compoundUrl.append("&query_key=");
      compoundUrl.append(queryKey);

      logger.finest("Loading compounds from PubChem via URL " + compoundUrl.toString());

      Document compoundSummaryDocument = builder.parse(compoundUrl.toString());
      NodeList docSumElements =
          compoundSummaryDocument.getDocumentElement().getElementsByTagName("DocSum");

      // Store the compound details
      for (int i = 0; i < docSumElements.getLength(); i++) {
        Element docSumElement = (Element) docSumElements.item(i);
        NodeList idElementNL = docSumElement.getElementsByTagName("Id");
        Element idElement = (Element) idElementNL.item(0);
        String id = idElement.getTextContent();
        compoundSummaryElements.put(id, docSumElement);
      }

      return cidArray;

    } catch (Exception e) {
      e.printStackTrace();
      throw (new IOException(e));
    }

  }

  /**
   * This method retrieves the details about a PubChem compound
   * 
   */
  public DBCompound getCompound(String CID, ParameterSet parameters) throws IOException {

    try {

      Element docSumElement = compoundSummaryElements.get(CID);
      if (docSumElement == null)
        throw new IOException("Missing data of compound CID " + CID);

      XPathFactory factory = XPathFactory.newInstance();
      XPath xpath = factory.newXPath();

      /*
       * XPathExpression expr = xpath.compile("Item[@Name='MeSHHeadingList']/Item"); NodeList
       * nameElementNL = (NodeList) expr.evaluate(docSumElement, XPathConstants.NODESET); Element
       * nameElement = (Element) nameElementNL.item(0);
       */

      XPathExpression expr = xpath.compile("Item[@Name='SynonymList']/Item");
      NodeList nameElementNL = (NodeList) expr.evaluate(docSumElement, XPathConstants.NODESET);
      Element nameElement = (Element) nameElementNL.item(0);

      if (nameElement == null) {
        expr = xpath.compile("Item[@Name='IUPACName']");
        nameElementNL = (NodeList) expr.evaluate(docSumElement, XPathConstants.NODESET);
        nameElement = (Element) nameElementNL.item(0);
      }

      if (nameElement == null)
        throw new IOException("Could not parse compound name");

      expr = xpath.compile("Item[@Name='MolecularFormula']");
      NodeList formulaElementNL = (NodeList) expr.evaluate(docSumElement, XPathConstants.NODESET);
      Element formulaElement = (Element) formulaElementNL.item(0);

      String compoundName = nameElement.getTextContent();
      String compoundFormula = formulaElement.getTextContent();

      URL entryURL = new URL(pubchemEntryAddress + CID);
      URL structure2DURL = new URL(pubchem2DStructureAddress + CID);
      URL structure3DURL = new URL(pubchem3DStructureAddress + CID);

      DBCompound newCompound = new DBCompound(OnlineDatabases.PubChem, CID, compoundName,
          compoundFormula, entryURL, structure2DURL, structure3DURL);

      return newCompound;

    } catch (Exception e) {
      e.printStackTrace();
      throw new IOException(e);
    }

  }

}
