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

package io.github.mzmine.util;

import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.features.FeatureListRow;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.google.common.collect.Range;

/**
 * This class defines a search condition - searching either by peak name, m/z or retention time.
 * Such search can be defined by any module and then conforming feature list rows can be tested by
 * the conforms() method.
 * 
 */
public class SearchDefinition {

  private SearchDefinitionType type;
  private Pattern nameRegex;
  private Range<Double> range;

  /**
   * Creates a search definition by using a regular expression
   */
  public SearchDefinition(SearchDefinitionType type, String regex) throws PatternSyntaxException {

    assert type == SearchDefinitionType.NAME;

    this.type = type;
    this.nameRegex = Pattern.compile(regex);

  }

  /**
   * Creates a search definition by m/z or RT range
   */
  public SearchDefinition(SearchDefinitionType type, Range<Double> range) {

    assert type == SearchDefinitionType.NAME;

    this.type = type;
    this.range = range;

  }

  /**
   * Creates a search definition by using a regular expression
   */
  public SearchDefinition(SearchDefinitionType type, String regex, Range<Double> range)
      throws PatternSyntaxException {

    this.type = type;
    this.range = range;

    // Avoid compiling the regex pattern (may cause exceptions) unless the
    // search type is set to NAME
    if (type == SearchDefinitionType.NAME) {
      this.nameRegex = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }

  }

  /**
   * Checks whether given feature list row conforms to this search condition.
   */
  public boolean conforms(FeatureListRow row) {
    switch (type) {
      case NAME:
        FeatureIdentity identity = row.getPreferredFeatureIdentity();
        if (identity == null)
          return false;
        String name = identity.getName();

        if (isEmpty(nameRegex.toString()) || isEmpty(name)) {
          return false;
        }

        Matcher matcher = nameRegex.matcher(name);
        return matcher.find();

      case MASS:
        return range.contains(row.getAverageMZ());

      case RT:
        return range.contains((double) row.getAverageRT());

    }
    return false;
  }

  public String getName() {
    String text = "Search by " + type.toString();
    switch (type) {
      case NAME:
        text += ": " + nameRegex;
        break;
      case MASS:
      case RT:
        text += ": " + range;
        break;
    }
    return text;
  }

  /**
   * Checks if the the string is not empty
   */
  private static boolean isEmpty(String str) {
    if (str != null && str.trim().length() > 0) {
      return false;
    }
    return true;
  }

}
