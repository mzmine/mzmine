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
import java.net.URL;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YMDBGateway implements DBGateway {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private static final String ymdbSearchAddress =
      "http://www.ymdb.ca/structures/search/compounds/mass?";
  public static final String ymdbSDFAddress = "http://www.ymdb.ca/structures/compounds/";
  public static final String ymdbEntryAddress = "http://www.ymdb.ca/compounds/";

  public String[] findCompounds(double mass, MZTolerance mzTolerance, int numOfResults,
      ParameterSet parameters) throws IOException {

    Range<Double> toleranceRange = mzTolerance.getToleranceRange(mass);

    String queryAddress = ymdbSearchAddress + "query_from=" + toleranceRange.lowerEndpoint()
        + "&query_to=" + toleranceRange.upperEndpoint();

    URL queryURL = new URL(queryAddress);

    // Submit the query
    logger.finest("Querying YMDB URL " + queryURL);
    String queryResult = InetUtils.retrieveData(queryURL);

    // Organize the IDs as a TreeSet to keep them sorted
    TreeSet<String> results = new TreeSet<String>();

    // Find IDs in the HTML data
    Pattern pat = Pattern.compile("/compounds/(YMDB[0-9]{5})");
    Matcher matcher = pat.matcher(queryResult);
    while (matcher.find()) {
      String ymdbID = matcher.group(1);
      results.add(ymdbID);
    }

    // Remove all except first numOfResults IDs. The reason why we first
    // retrieve all results and then remove those above numOfResults is to
    // keep the lowest YDMB IDs - these may be the most interesting ones.
    while (results.size() > numOfResults) {
      String lastItem = results.last();
      results.remove(lastItem);
    }

    return results.toArray(new String[0]);

  }

  /**
   * This method retrieves the details about YMDB compound
   * 
   */
  public CompoundDBAnnotation getCompound(String ID, ParameterSet parameters) throws IOException {

    // We will parse the name and formula from the SDF file, it seems like
    // the easiest way
    URL sdfURL = new URL(ymdbSDFAddress + ID + ".sdf");

    logger.finest("Querying YMDB URL " + sdfURL);
    String sdfRecord = InetUtils.retrieveData(sdfURL);
    String lines[] = sdfRecord.split("\n");

    String compoundName = null;
    String compoundFormula = null;
    URL entryURL = new URL(ymdbEntryAddress + ID);
    URL structure2DURL = sdfURL;
    URL structure3DURL = new URL(ymdbSDFAddress + ID + ".sdf?dim=3d");

    for (int i = 0; i < lines.length - 1; i++) {

      if (lines[i].contains("> <GENERIC_NAME>")) {
        compoundName = lines[i + 1];
      }

      if (lines[i].contains("> <FORMULA>")) {
        compoundFormula = lines[i + 1];
      }
    }

    if (compoundName == null) {
      throw (new IOException("Could not parse compound name"));
    }

    CompoundDBAnnotation newCompound = new SimpleCompoundDBAnnotation(OnlineDatabases.YMDB, ID, compoundName, compoundFormula,
        entryURL, structure2DURL, structure3DURL);

    return newCompound;

  }
}
