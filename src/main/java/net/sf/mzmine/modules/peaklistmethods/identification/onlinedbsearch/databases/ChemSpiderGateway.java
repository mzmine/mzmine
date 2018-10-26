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

/*
 * Code created was by or on behalf of Syngenta and is released under the open source license in use
 * for the pre-existing code or project. Syngenta does not assert ownership or copyright any over
 * pre-existing work.
 */

package net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.databases;

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

import com.google.common.base.Functions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.DBCompound;
import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.DBGateway;
import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.OnlineDatabase;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.util.RangeUtils;

/**
 * Searches the ChemSpider database.
 * 
 */
public class ChemSpiderGateway implements DBGateway {

  // Logger.
  private static final Logger LOG = Logger.getLogger(ChemSpiderGateway.class.getName());

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

    LOG.finest("Searching by mass...");

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
  public DBCompound getCompound(final String ID, ParameterSet parameters) throws IOException {

    LOG.finest("Fetching compound info for CSID #" + ID);

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
      return new DBCompound(OnlineDatabase.CHEMSPIDER, ID, name, formula,
          new URL(STRUCTURE_URL_PATTERN.replaceFirst("CSID", ID)),
          new URL(STRUCTURE2D_URL_PATTERN.replaceFirst("CSID", ID)),
          new URL(STRUCTURE3D_URL_PATTERN.replaceFirst("CSID", ID)));

    } catch (ApiException e) {
      LOG.log(Level.WARNING, "Failed to fetch compound info for CSID #" + ID, e);
      throw new IOException(e);
    }

  }


}
