/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.dataprocessing.id_ms2search;

import io.github.mzmine.datamodel.DataPoint;
import java.util.List;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
class Ms2SearchResult {

  private double score;
  private String searchType;
  private List<DataPoint> matchedIons;

  public Ms2SearchResult(double score, String searchType, List<DataPoint> matchedIons) {
    this.score = score;
    this.searchType = searchType;
    this.matchedIons = matchedIons;
  }

  public double getScore() {
    return this.score;
  }

  public int getNumIonsMatched() {
    return matchedIons.size();
  }

  public String getSearchType() {
    return this.searchType;
  }

  public List<DataPoint> getMatchedIons() {
    return this.matchedIons;
  }

  public String getMatchedIonsAsString() {
    // Return the matched ions as a string with the following format:
    // 10.2312_20.4324_55.1231
    String returnString = new String();
    for (int i = 0; i < this.matchedIons.size(); i++) {
      returnString = returnString + String.format("%.4f", this.matchedIons.get(i).getMZ()) + "_";
    }
    return returnString.substring(0, returnString.length() - 1); // Some
    // hackery
    // to
    // remove
    // the last
    // "_"
  }

}
