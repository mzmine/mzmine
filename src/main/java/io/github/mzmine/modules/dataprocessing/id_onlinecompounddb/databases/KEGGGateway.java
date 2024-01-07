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
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KEGGGateway implements DBGateway {

  public static final String keggEntryAddress = "http://www.kegg.jp/entry/";
  private static final String keggFindAddress1 = "http://rest.kegg.jp/find/compound/";
  private static final String keggFindAddress2 = "/exact_mass";
  private static final String keggGetAddress = "http://rest.kegg.jp/get/";

  private static final String kegg2DStructureAddress =
      "https://www.genome.jp/dbget-bin/www_bget?-f+m+compound+";
  private static final String met3DStructureAddress1 = "http://www.3dmet.dna.affrc.go.jp/pdb/";
  private static final String met3DStructureAddress2 = ".pdb";

  public String[] findCompounds(double mass, MZTolerance mzTolerance, int numOfResults,
      ParameterSet parameters) throws IOException {

    Range<Double> toleranceRange = mzTolerance.getToleranceRange(mass);

    String queryAddress = keggFindAddress1 + toleranceRange.lowerEndpoint() + "-"
        + toleranceRange.upperEndpoint() + keggFindAddress2;

    URL queryURL = new URL(queryAddress);

    String queryResult = InetUtils.retrieveData(queryURL);

    ArrayList<String> results = new ArrayList<String>();

    Pattern pat = Pattern.compile("cpd:(C[0-9]+)");
    Matcher matcher = pat.matcher(queryResult);
    while (matcher.find()) {
      String keggID = matcher.group(1);
      results.add(keggID);
    }

    return results.toArray(new String[0]);

  }

  /**
   * This method retrieves the details about a KEGG compound
   * 
   */
  public CompoundDBAnnotation getCompound(String ID, ParameterSet parameters) throws IOException {

    String queryAddress = keggGetAddress + ID;

    URL queryURL = new URL(queryAddress);

    String compoundData = InetUtils.retrieveData(queryURL);

    String dataLines[] = compoundData.split("\n");

    String compoundName = null, compoundFormula = null, ID3DMet = null;

    for (String line : dataLines) {
      if (line.startsWith("NAME")) {
        compoundName = line.substring(12);
        if (compoundName.endsWith(";")) {
          compoundName = compoundName.substring(0, compoundName.length() - 1);
        }
      }

      if (line.startsWith("FORMULA")) {
        compoundFormula = line.substring(12);
      }

      // 3DMET id is last 6 characters on the line
      if (line.contains("3DMET")) {
        ID3DMet = line.substring(line.length() - 6);
      }

    }

    if ((compoundName == null) || (compoundFormula == null)) {
      throw (new IOException("Could not obtain compound name and formula"));
    }

    URL entryURL = new URL(keggEntryAddress + ID);
    URL structure2DURL = new URL(kegg2DStructureAddress + ID);

    URL structure3DURL = null;

    if (ID3DMet != null) {
      structure3DURL = new URL(met3DStructureAddress1 + ID3DMet + met3DStructureAddress2);
    }

    CompoundDBAnnotation newCompound = new SimpleCompoundDBAnnotation(OnlineDatabases.KEGG, ID, compoundName, compoundFormula,
        entryURL, structure2DURL, structure3DURL);

    return newCompound;

  }

}
