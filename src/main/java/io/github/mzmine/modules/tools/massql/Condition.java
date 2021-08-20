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

import org.json.JSONObject;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public enum Condition {

  // non standard conditions (maybe MZmine specific or proposed to MassQL)
  MSLEVEL,
  // MassQL conditions
  RTMIN("rtmincondition"), RTMAX("rtmaxcondition"), SCANMIN("scanmincondition"), SCANMAX("scanmaxcondition"), CHARGE("chargecondition"), POLARITY("polaritycondition"), MS2PROD("ms2productcondition"), MS2PREC("ms2precursorcondition"), MS2NL("ms2neutrallosscondition");

  public static final String JSON_FIELD = "type";
  private final String json;

  Condition() {
    json = null;
  }

  Condition(String json) {
    this.json = json;
  }

  public static Condition valueOf(JSONObject condition) {
    String type = condition.getString(JSON_FIELD);
    for (Condition value : Condition.values()) {
      if(value.getJson().equalsIgnoreCase(type)) {
        return value;
      }
    }
    return null;
  }

  public String getJson() {
    return json == null? toString() : json;
  }
}
