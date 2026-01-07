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

package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to provide lists of lipid classes
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class LipidClassesProvider {

  /**
   * get list of objects containing all lipid classes
   */
  public static List<Object> getListOfAllLipidClasses() {
    List<Object> lipidHierarchy = new ArrayList<>();
    LipidMainClasses lastMain = null;
    LipidCategories lastLipidCategory = null;
    for (LipidClasses classes : LipidClasses.values()) {
      LipidCategories category = classes.getCoreClass();
      LipidMainClasses main = classes.getMainClass();
      if (!category.equals(lastLipidCategory)) {
        lastLipidCategory = category;
        // add core to list
        lipidHierarchy.add(category);
      }
      if (!main.equals(lastMain)) {
        lastMain = main;
        // add main to list
        lipidHierarchy.add(main);
      }
      // add
      lipidHierarchy.add(classes);
    }
    return lipidHierarchy;
  }

  public static List<Object> getListOfLipidClassesByLipidCategories(
      List<LipidCategories> lipidCategories) {
    List<Object> lipidHierarchy = new ArrayList<>();
    LipidMainClasses lastMain = null;
    LipidCategories lastLipidCategory = null;
    for (LipidClasses classes : LipidClasses.values()) {
      LipidCategories category = classes.getCoreClass();
      LipidMainClasses main = classes.getMainClass();
      boolean isCorrectCategory = lipidCategories.contains(category);
      if (!category.equals(lastLipidCategory) && isCorrectCategory) {
        lastLipidCategory = category;
        // add core to list
        lipidHierarchy.add(category);
      }
      if (!main.equals(lastMain) && isCorrectCategory) {
        lastMain = main;
        // add main to list
        lipidHierarchy.add(main);
      }
      if (isCorrectCategory) {
        lipidHierarchy.add(classes);
      }
    }
    return lipidHierarchy;
  }
}
