/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.modules.tools.massql;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.util.webapi.MassQLUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class MassQLQuery {

  public static final MassQLQuery NONE = new MassQLQuery();

  // fields
  public static final String QUERY_TYPE = "querytype";
  public static final String DATA_TYPE = "datatype";
  public static final String CONDITIONS = "conditions";
  public static final String QUALIFIERS = "qualifiers";
  public static final String VALUE = "value";

  private final JSONObject jsonQuery;
  private final List<MassQLFilter> filters;



  /**
   * Empty filter
   */
  private MassQLQuery() {
    jsonQuery = null;
    filters = List.of();
  }

  /**
   * Create filter based on MassQL query. See {@link MassQLUtils#getQuery(String)}
   *
   * @param jsonQuery the query that was parsed by the MassQL API
   */
  public MassQLQuery(@NotNull JSONObject jsonQuery) {
    this.jsonQuery = jsonQuery;
    filters = new ArrayList<>();

    // Add MSlevel as a condition although MassQL currently does not handle it like a condition
    JSONObject querytype = jsonQuery.getJSONObject(QUERY_TYPE);
    String datatype = querytype.getString(DATA_TYPE);
    double mslevel = Double.parseDouble(datatype.replaceAll("[^0-9]", ""));
    filters.add(new MassQLFilter(Condition.MSLEVEL, null, List.of(mslevel)));


    JSONArray conditions = jsonQuery.getJSONArray(CONDITIONS);
    for(int i=0; i<conditions.length(); i++) {
      JSONObject condition = conditions.getJSONObject(i);

      // find condition and qualifiers (mztolerance etc)
      Condition type = Condition.valueOf(condition);

      // read values of condition
      JSONArray jsonValues = condition.has(VALUE)? condition.getJSONArray(VALUE) : null;
      List<Double> values = new ArrayList<>();
      if(jsonValues!=null) {
        for (int v = 0; v < jsonValues.length(); v++) {
          values.add(jsonValues.getDouble(v));
        }
      }

      // get qualifiers
      JSONObject qualJson = condition.has(QUALIFIERS)? condition.getJSONObject(QUALIFIERS) : null;
      Map<ConditionQualifier, Double> qualifiers = qualJson==null? null : ConditionQualifier.valueOf(qualJson);

      filters.add(new MassQLFilter(type, qualifiers, values));
    }
  }

  /**
   * @param row candidate row
   * @return true if the row matches all filters. false if one fails
   */
  public boolean accept(FeatureListRow row) {
    return filters.isEmpty() || filters.stream().allMatch(filter -> filter.accept(row));
  }

  /**
   *
   * @param scan candidate scan
   * @return true if the scan matches all filters. false if one fails
   */
  public boolean accept(Scan scan) {
    return filters.isEmpty() || filters.stream().allMatch(filter -> filter.accept(scan));
  }
}
