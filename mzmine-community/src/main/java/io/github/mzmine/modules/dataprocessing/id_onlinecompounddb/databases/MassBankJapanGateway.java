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
import io.github.mzmine.modules.dataprocessing.id_onlinecompounddb.DBGateway;
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

public class MassBankJapanGateway implements DBGateway {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  // Unfortunately, we need to list all the instrument types here
  private static final String instrumentTypes[] = {"APCI-ITFT", "APCI-ITTOF", "CE-ESI-TOF", "CI-B",
      "EI-B", "EI-EBEB", "ESI-ITFT", "ESI-ITTOF", "ESI-QTOF", "FAB-B", "FAB-EB", "FAB-EBEB", "FD-B",
      "FI-B", "GC-EI-QQ", "GC-EI-TOF", "LC-APCI-QTOF", "LC-APPI-QQ", "LC-ESI-IT", "LC-ESI-ITFT",
      "LC-ESI-ITTOF", "LC-ESI-Q", "LC-ESI-QFT", "LC-ESI-QIT", "LC-ESI-QQ", "LC-ESI-QTOF",
      "LC-ESI-TOF", "MALDI-QIT", "MALDI-TOF", "MALDI-TOFTOF"};

  private static final String massBankSearchAddress =
      "http://www.massbank.jp/jsp/Result.jsp?type=quick";
  private static final String massBankEntryAddress = "http://www.massbank.jp/jsp/FwdRecord.jsp?id=";

  public String[] findCompounds(double mass, MZTolerance mzTolerance, int numOfResults,
      ParameterSet parameters) throws IOException {

    Range<Double> toleranceRange = mzTolerance.getToleranceRange(mass);

    StringBuilder queryAddress = new StringBuilder(massBankSearchAddress);

    for (String inst : instrumentTypes) {
      queryAddress.append("&inst=");
      queryAddress.append(inst);
    }

    queryAddress.append("&mz=");
    queryAddress.append(RangeUtils.rangeCenter(toleranceRange));
    queryAddress.append("&tol=");
    queryAddress.append(RangeUtils.rangeLength(toleranceRange) / 2.0);

    URL queryURL = new URL(queryAddress.toString());

    // Submit the query
    logger.finest("Querying URL " + queryURL);
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
    logger.finest("Querying URL " + entryURL);
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
      compoundName = matcherName.group(1);
    }

    // Find compound formula
    Pattern patFormula = Pattern.compile("CH\\$FORMULA: .*>(.+)</a>");
    Matcher matcherFormula = patFormula.matcher(massBankEntry);
    if (matcherFormula.find()) {
      compoundFormula = matcherFormula.group(1);
    }

    if (compoundName == null) {
      logger
          .warning("Could not parse compound name for compound " + ID + ", ignoring this compound");
      return null;
    }

    CompoundDBAnnotation newCompound = null; // new
                                   // SimpleCompoundDBAnnotation(OnlineDatabases.MASSBANKJapan,
                                   // ID, compoundName, compoundFormula,
                                   // databaseURL, structure2DURL,
                                   // structure3DURL);

    return newCompound;

  }
}
