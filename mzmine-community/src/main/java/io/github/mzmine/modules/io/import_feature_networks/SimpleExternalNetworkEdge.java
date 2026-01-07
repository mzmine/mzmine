/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.io.import_feature_networks;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public record SimpleExternalNetworkEdge(int id1, int id2, String edgeType, Double score,
                                        String annotation) {

  public static String[] getColumnHeadersLowerCase() {
    return Arrays.stream(SimpleExternalNetworkEdge.class.getRecordComponents())
        .map(RecordComponent::getName).map(String::toLowerCase).toArray(String[]::new);
  }

  public static Class[] getColumnTypes() {
    return Arrays.stream(SimpleExternalNetworkEdge.class.getRecordComponents())
        .map(RecordComponent::getType).toArray(Class[]::new);
  }

  /**
   * Parse a row of values
   *
   * @param values list of 5 strings nulls and empty allowed
   * @return a new edge if values were parsed. Empty score is allowed but failed parsing will throw
   * a {@link RuntimeException}
   */
  public static SimpleExternalNetworkEdge parse(@NotNull final List<String> values) {
    if (values.size() != 5) {
      throw new IllegalArgumentException("Requires 5 values to parse");
    }
    String scorestr = values.get(3);
    Double score = null;
    try {
      score = scorestr.isBlank() ? null : Double.parseDouble(scorestr);
    } catch (NumberFormatException e) {
      throw new RuntimeException("Cannot parse score value - make sure its a number", e);
    }
    int id1, id2;
    try {
      id1 = Integer.parseInt(values.get(0));
      id2 = Integer.parseInt(values.get(1));
    } catch (NumberFormatException e) {
      throw new RuntimeException("Cannot parse row id1 and id2 values - make sure they are numbers",
          e);
    }

    return new SimpleExternalNetworkEdge(id1, id2, values.get(2), score, values.get(4));
  }
}
