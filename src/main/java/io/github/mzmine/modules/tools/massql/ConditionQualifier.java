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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.json.JSONObject;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public enum ConditionQualifier {
  /**
   * Define tolerance in ppm or absolute m/z
   */
  TOLERANCEMZ("qualifiermztolerance"), TOLERANCEPPM("qualifierppmtolerance"),
  /**
   * Define minimum absolute intensity value
   */
  INTENSITYVALUE("qualifierintensityvalue"),
  /**
   * Define minimum relative intensity value (to highest signal)
   */
  INTENSITYPERCENT("qualifierintensitypercent"),
  /**
   * Define minimum intensity value relative to TIC in the spectrum
   */
  INTENSITYTICPERCENT("qualifierintensityticpercent");

  private static final String VALUE = "value";

  private final String json;

  ConditionQualifier() {
    json = null;
  }

  ConditionQualifier(String json) {
    this.json = json;
  }

  public String getJson() {
    return json == null? toString() : json;
  }

  public static Map<ConditionQualifier, Double> valueOf(JSONObject qualifier) {
    Map<ConditionQualifier, Double> map = new HashMap<>();

    for (ConditionQualifier value : ConditionQualifier.values()) {
      if(qualifier.has(value.getJson())) {
        JSONObject qualiJson = qualifier.getJSONObject(value.getJson());
        map.put(value, qualiJson.getDouble(VALUE));
      }
    }
    return map;
  }
}
