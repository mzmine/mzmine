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

import com.google.common.base.Functions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.SimpleCompoundDBAnnotation;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_onlinecompounddb.DBGateway;
import io.github.mzmine.modules.dataprocessing.id_onlinecompounddb.OnlineDatabases;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.RangeUtils;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.rsc.chemspider.ApiException;
import org.rsc.chemspider.api.FilterByMassRequest;
import org.rsc.chemspider.api.FilterByMassRequest.OrderByEnum;
import org.rsc.chemspider.api.FilterQueryResponse;
import org.rsc.chemspider.api.FilteringApi;
import org.rsc.chemspider.api.QueryResultResponse;
import org.rsc.chemspider.api.RecordResponse;
import org.rsc.chemspider.api.RecordsApi;

/**
 * Searches the ChemSpider database.
 * 
 */
public class ChemSpiderGateway implements DBGateway {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  // Compound names.
  private static final String UNKNOWN_NAME = "Unknown name";

  // Pattern for chemical structure URLs - replace CSID.
  private static final String STRUCTURE_URL_PATTERN =
      "http://www.chemspider.com/Chemical-Structure.CSID.html";
  private static final String STRUCTURE2D_URL_PATTERN =
      "http://www.chemspider.com/FilesHandler.ashx?type=str&id=CSID";
  private static final String STRUCTURE3D_URL_PATTERN =
      "http://www.chemspider.com/FilesHandler.ashx?type=str&3d=yes&id=CSID";

  // Pattern to clean-up formulas.
  private static final Pattern FORMULA_PATTERN = Pattern.compile("[\\W_]*");

  @Override
  public String[] findCompounds(final double mass, final MZTolerance mzTolerance,
      final int numOfResults, ParameterSet parameters) throws IOException {

    logger.finest("Searching by mass...");

    // Get search range
    final Range<Double> mzRange = mzTolerance.getToleranceRange(mass);
    final double queryMz = RangeUtils.rangeCenter(mzRange);
    final double queryRange = RangeUtils.rangeLength(mzRange) / 2.0;

    // Get security token.
    final String apiKey = parameters.getParameter(ChemSpiderParameters.SECURITY_TOKEN).getValue();

    try {

      FilterByMassRequest filterRequest = new FilterByMassRequest();
      filterRequest.setMass(queryMz);
      filterRequest.setRange(queryRange);
      filterRequest.setOrderBy(OrderByEnum.RECORDID);

      FilteringApi apiInstance = new FilteringApi();
      apiInstance.getApiClient().setUserAgent("MZmine " + MZmineCore.getMZmineVersion());

      FilterQueryResponse queryId = apiInstance.filterMassPost(filterRequest, apiKey);
      QueryResultResponse result =
          apiInstance.filterQueryIdResultsGet(queryId.getQueryId(), apiKey, 0, numOfResults);
      List<Integer> integerIDs = result.getResults();
      List<String> stringIDs = Lists.transform(integerIDs, Functions.toStringFunction());

      return stringIDs.toArray(new String[0]);

    } catch (ApiException e) {
      throw new IOException(e);
    }
  }

  @Override
  public CompoundDBAnnotation getCompound(final String ID, ParameterSet parameters) throws IOException {

    logger.finest("Fetching compound info for CSID #" + ID);

    // Get security token.
    final String apiKey = parameters.getParameter(ChemSpiderParameters.SECURITY_TOKEN).getValue();

    final List<String> fields = Arrays.asList("Formula", "CommonName", "MonoisotopicMass");

    try {
      RecordsApi apiInstance = new RecordsApi();
      apiInstance.getApiClient().setUserAgent("MZmine " + MZmineCore.getMZmineVersion());

      Integer recordId = Integer.valueOf(ID);
      RecordResponse response = apiInstance.recordsRecordIdDetailsGet(recordId, fields, apiKey);

      String name = response.getCommonName();
      if (Strings.isNullOrEmpty(name))
        name = UNKNOWN_NAME;
      String formula = response.getFormula();

      // Fix formula formatting
      if (!Strings.isNullOrEmpty(formula))
        formula = FORMULA_PATTERN.matcher(formula).replaceAll("");

      // Create and return the compound record.
      return new SimpleCompoundDBAnnotation(OnlineDatabases.CHEMSPIDER, ID, name, formula,
          new URL(STRUCTURE_URL_PATTERN.replaceFirst("CSID", ID)),
          new URL(STRUCTURE2D_URL_PATTERN.replaceFirst("CSID", ID)),
          new URL(STRUCTURE3D_URL_PATTERN.replaceFirst("CSID", ID)));

    } catch (ApiException e) {
      logger.log(Level.WARNING, "Failed to fetch compound info for CSID #" + ID, e);
      throw new IOException(e);
    }

  }

}
