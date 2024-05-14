/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.id_onlinecompounddb.databases;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.SimpleCompoundDBAnnotation;
import io.github.mzmine.modules.dataprocessing.id_onlinecompounddb.DBGateway;
import io.github.mzmine.modules.dataprocessing.id_onlinecompounddb.OnlineDatabases;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.InetUtils;
import java.io.IOException;
import java.io.StringReader;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class HMDBGateway implements DBGateway {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  public static final String hmdbEntryAddress = "http://www.hmdb.ca/metabolites/";
  private static final String hmdbSeachAddress =
      "http://www.hmdb.ca/structures/search/metabolites/mass?search_type=monoisotopic&";
  private static final String hmdbStructureAddress = "http://www.hmdb.ca/structures/metabolites/";

  @Override
  public String[] findCompounds(double mass, MZTolerance mzTolerance, int numOfResults,
      ParameterSet parameters) throws IOException {

    Range<Double> toleranceRange = mzTolerance.getToleranceRange(mass);

    String queryAddress = hmdbSeachAddress + "&query_from=" + toleranceRange.lowerEndpoint()
        + "&query_to=" + toleranceRange.upperEndpoint();

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
  @Override
  public CompoundDBAnnotation getCompound(String ID, ParameterSet parameters) throws IOException {

    logger.finest("Obtaining information about HMDB compound id " + ID);

    Element nameElement, formulaElement;

    try {

      final String url = hmdbEntryAddress + ID + ".xml";
      logger.finest("Loading URL " + url);
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = dbf.newDocumentBuilder();
      String compoundXML = InetUtils.retrieveData(new URL(url));
      InputSource is = new InputSource(new StringReader(compoundXML));
      Document parsedResult = builder.parse(is);

      XPathFactory factory = XPathFactory.newInstance();
      XPath xpath = factory.newXPath();

      XPathExpression expr = xpath.compile("//metabolite/name");
      NodeList nameElementNL = (NodeList) expr.evaluate(parsedResult, XPathConstants.NODESET);
      nameElement = (Element) nameElementNL.item(0);

      if (nameElement == null)
        throw new IOException("Could not parse compound name");

      expr = xpath.compile("//metabolite/chemical_formula");
      NodeList formulaElementNL = (NodeList) expr.evaluate(parsedResult, XPathConstants.NODESET);
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

    CompoundDBAnnotation newCompound = new SimpleCompoundDBAnnotation(OnlineDatabases.HMDB, ID, compoundName, compoundFormula,
        entryURL, structure2DURL, structure3DURL);

    return newCompound;

  }
}
