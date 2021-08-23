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
 * MassQL condtions
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public enum Condition {

  // non standard conditions (maybe MZmine specific or proposed to MassQL)
  /**
   * MSLEVEL is a non standard condition that is used by MZmine
   */
  MSLEVEL,
  /**
   * minimum retention time (inclusive)
   */
  RTMIN("rtmincondition"),
  /**
   * maximum retention time (inclusive)
   */
  RTMAX("rtmaxcondition"),
  /**
   * minimum scan number (inclusive)
   */
  SCANMIN("scanmincondition"),
  /**
   * maximum scan number (inclusive)
   */
  SCANMAX("scanmaxcondition"),
  /**
   * charge of the scan or feature
   */
  CHARGE("chargecondition"),
  /**
   * polarity of scan or feature
   */
  POLARITY("polaritycondition"),
  /**
   * signals in MS1 scan even when MS2 is searched
   */
  MS1MZ("ms1mzcondition"),
  /**
   * signals in MS2 scans (MS2MZ == MS2PROD)
   */
  MS2MZ("ms2productcondition"),
  /**
   * signals in MS2 scans (MS2MZ == MS2PROD)
   */
  MS2PROD("ms2productcondition"),
  /**
   * precursor ion
   */
  MS2PREC("ms2precursorcondition"),
  /**
   * Neutral loss from precursor ion
   */
  MS2NL("ms2neutrallosscondition");

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
      if (value.getJson().equalsIgnoreCase(type)) {
        return value;
      }
    }
    return null;
  }

  /**
   * The json reprensentation of this condition
   *
   * @return JSON key
   */
  public String getJson() {
    return json == null ? toString() : json;
  }

  /**
   * The MassQL string used for this condition
   *
   * @return massql word
   */
  public String getMassQLString() {
    return this.toString();
  }
}
