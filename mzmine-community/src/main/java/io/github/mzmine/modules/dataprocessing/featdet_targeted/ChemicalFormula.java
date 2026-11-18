/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_targeted;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChemicalFormula {

  private Map<String, Integer> elementCounts;

  public ChemicalFormula(String formula) {
    elementCounts = new HashMap<>();
    parseFormula(formula);
  }

  private void parseFormula(String formula) {
    Pattern pattern = Pattern.compile("([A-Z][a-z]*)(\\d*)");
    Matcher matcher = pattern.matcher(formula);

    while (matcher.find()) {
      String element = matcher.group(1);
      String countStr = matcher.group(2);
      int count = countStr.isEmpty() ? 1 : Integer.parseInt(countStr);
      elementCounts.put(element, elementCounts.getOrDefault(element, 0) + count);
    }
  }

  public void modifyElementCount(String element, int delta) {
    if (elementCounts.containsKey(element)) {
      int newCount = elementCounts.get(element) + delta;
      if (newCount > 0) {
        elementCounts.put(element, newCount);
      } else {
        elementCounts.remove(element);
      }
    } else {
      System.out.println("Element does not exist in the formula");
    }
  }

  public void addIsotope(String element, int count) {
    String isotopeLabel = "^13" + element;  // Label for isotopes
    elementCounts.put(isotopeLabel, elementCounts.getOrDefault(isotopeLabel, 0) + count);
  }

  public Map<String, Integer> getElementCounts() {
    return elementCounts;
  }

  public String reconstructFormula() {
    StringBuilder builder = new StringBuilder();
    elementCounts.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
      builder.append(entry.getKey());
      if (entry.getValue() > 1) {
        builder.append(entry.getValue());
      }
    });
    return builder.toString();
  }
}
