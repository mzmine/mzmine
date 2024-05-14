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
