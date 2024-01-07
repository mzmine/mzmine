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
import io.github.mzmine.util.RangeUtils;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MassBankEuropeGateway implements DBGateway {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private static final String massBankSearchAddress =
      "https://massbank.eu/MassBank/Result.jsp?type=quick&formula=&compound=&"
          + "ms=all&ms=MS&ms=MS2&ms=MS3&ms=MS4&ion=0&op1=and&"
          + "inst_grp=ESI&inst=CE-ESI-TOF&inst=ESI-ITFT&inst=ESI-ITTOF&inst=ESI-QTOF&inst=ESI-TOF&"
          + "inst=LC-ESI-IT&inst=LC-ESI-ITFT&inst=LC-ESI-ITTOF&inst=LC-ESI-Q&inst=LC-ESI-QFT&"
          + "inst=LC-ESI-QIT&inst=LC-ESI-QQ&inst=LC-ESI-QTOF&inst=LC-ESI-TOF";

  private static final String massBankEntryAddress =
      "https://massbank.eu/MassBank/RecordDisplay.jsp?id=";

  public String[] findCompounds(double mass, MZTolerance mzTolerance, int numOfResults,
      ParameterSet parameters) throws IOException {

    Range<Double> toleranceRange = mzTolerance.getToleranceRange(mass);

    StringBuilder queryAddress = new StringBuilder(massBankSearchAddress);

    queryAddress.append("&mz=");
    queryAddress.append(RangeUtils.rangeCenter(toleranceRange));
    queryAddress.append("&tol=");
    queryAddress.append(RangeUtils.rangeLength(toleranceRange) / 2.0);

    URL queryURL = new URL(queryAddress.toString());

    // Submit the query
    logger.finest("Querying MassBank.eu URL " + queryURL);
    String queryResult = InetUtils.retrieveData(queryURL);

    Vector<String> results = new Vector<String>();

    // Find IDs in the HTML data
    Pattern pat = Pattern.compile("&nbsp;&nbsp;&nbsp;&nbsp;([A-Z0-9]{8})&nbsp;");
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
  public CompoundDBAnnotation getCompound(String ID, ParameterSet parameters) throws IOException {

    URL entryURL = new URL(massBankEntryAddress + ID);

    // Retrieve data
    logger.finest("Querying MassBank.eu URL " + entryURL);
    String massBankEntry = InetUtils.retrieveData(entryURL);

    String compoundName = null;
    String compoundFormula = null;
    URL structure2DURL = null;
    URL structure3DURL = null;
    URL databaseURL = entryURL;

    // Find compound name
    Pattern patName = Pattern.compile("RECORD_TITLE: (.*)");
    Matcher matcherName = patName.matcher(massBankEntry);
    if (matcherName.find()) {
      compoundName = matcherName.group(1).replaceAll("\\<[^>]*>", "");
    }

    // Find compound formula
    Pattern patFormula = Pattern.compile("CH\\$FORMULA: .*>(.*)</a>");
    Matcher matcherFormula = patFormula.matcher(massBankEntry);
    if (matcherFormula.find()) {
      compoundFormula = matcherFormula.group(1).replaceAll("\\<[^>]*>", "");
    }

    if (compoundName == null) {
      logger.warning("Could not parse compound name for compound " + ID);
      return null;
    }

    CompoundDBAnnotation newCompound = new SimpleCompoundDBAnnotation(OnlineDatabases.MASSBANKEurope, ID, compoundName,
        compoundFormula, databaseURL, structure2DURL, structure3DURL);

    return newCompound;

  }
}
