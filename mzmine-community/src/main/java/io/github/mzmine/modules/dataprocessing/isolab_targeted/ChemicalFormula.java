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
package io.github.mzmine.modules.dataprocessing.isolab_targeted;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Representation of a chemical formula with support for isotopes
 */
public class ChemicalFormula {

  // Map to store elements and their counts
  private final Map<String, Integer> elementCounts;

  // Map to store isotope variants of elements (element -> isotope mass number -> count)
  private final Map<String, Map<Integer, Integer>> isotopeVariants;

  /**
   * Constructor from a formula string (e.g., "C6H12O6")
   *
   * @param formula Chemical formula string
   */
  public ChemicalFormula(String formula) {
    this.elementCounts = new LinkedHashMap<>();
    this.isotopeVariants = new LinkedHashMap<>();
    parseFormula(formula);
  }

  /**
   * Parse a chemical formula string and store the elements and their counts
   *
   * @param formula Chemical formula string (e.g., "C6H12O6")
   */
  private void parseFormula(String formula) {
    // Regular expression to match both regular elements and isotope notations
    // This regex can handle standard elements (C, Fe), isotope prefixes (13C), and isotope suffixes (C13)
    Pattern pattern = Pattern.compile("(?:([0-9]+))?([A-Z][a-z]*)(?:([0-9]+))?");
    Matcher matcher = pattern.matcher(formula);

    while (matcher.find()) {
      String element = matcher.group(2);
      String prefixNum = matcher.group(1);
      String suffixNum = matcher.group(3);

      // Check if this is an isotope notation (e.g., 13C or C13)
      if (prefixNum != null) {
        // This could be an isotope notation like 13C
        try {
          int isotopeNumber = Integer.parseInt(prefixNum);
          // Store as an isotope variant
          Map<Integer, Integer> isotopes = isotopeVariants.computeIfAbsent(element,
              k -> new HashMap<>());

          // Count is either the suffix number or 1 if no suffix
          int count = (suffixNum != null) ? Integer.parseInt(suffixNum) : 1;
          isotopes.put(isotopeNumber, isotopes.getOrDefault(isotopeNumber, 0) + count);
          continue;
        } catch (NumberFormatException e) {
          // Not a valid number, treat as regular element
        }
      }

      // Handle regular element notation
      int count = 1;
      if (suffixNum != null) {
        count = Integer.parseInt(suffixNum);
      }

      // Update element count
      elementCounts.put(element, elementCounts.getOrDefault(element, 0) + count);
    }
  }

  /**
   * Modify the count of an element
   *
   * @param element Element symbol
   * @param delta   Change in count (can be positive or negative)
   */
  public void modifyElementCount(String element, int delta) {
    int count = elementCounts.getOrDefault(element, 0);
    count += delta;

    if (count <= 0) {
      elementCounts.remove(element);
    } else {
      elementCounts.put(element, count);
    }
  }

  /**
   * Add a specific isotope variant of an element
   *
   * @param element    Element symbol
   * @param count      Number of isotope atoms to add
   * @param massNumber Mass number of the isotope
   */
  public void addIsotope(String element, int count, int massNumber) {
    Map<Integer, Integer> isotopes = isotopeVariants.computeIfAbsent(element, k -> new HashMap<>());
    isotopes.put(massNumber, isotopes.getOrDefault(massNumber, 0) + count);
  }

  /**
   * Reconstruct the chemical formula as a string
   *
   * @return Chemical formula string
   */
  public String reconstructFormula() {
    StringBuilder formula = new StringBuilder();

    // Add normal elements first
    for (Entry<String, Integer> entry : elementCounts.entrySet()) {
      formula.append(entry.getKey());
      int count = entry.getValue();
      if (count > 1) {
        formula.append(count);
      }
    }

    // Then add isotope variants
    for (Entry<String, Map<Integer, Integer>> elementEntry : isotopeVariants.entrySet()) {
      String element = elementEntry.getKey();
      Map<Integer, Integer> isotopes = elementEntry.getValue();

      for (Entry<Integer, Integer> isotopeEntry : isotopes.entrySet()) {
        int massNumber = isotopeEntry.getKey();
        int count = isotopeEntry.getValue();

        formula.append(massNumber).append(element);
        if (count > 1) {
          formula.append(count);
        }
      }
    }

    return formula.toString();
  }

  @Override
  public String toString() {
    return reconstructFormula();
  }
}