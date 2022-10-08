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

package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to create a list of objects containing all lipid classes
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class AllLipidClasses {

  private static List<Object> allClasses = new ArrayList<>();

  /**
   * get list of objects containing all lipid classes
   */
  public static List<Object> getList() {
    LipidMainClasses lastMain = null;
    LipidCategories lastCore = null;
    for (LipidClasses classes : LipidClasses.values()) {
      LipidCategories core = classes.getCoreClass();
      LipidMainClasses main = classes.getMainClass();
      if (lastCore == null || !core.equals(lastCore)) {
        lastCore = core;
        // add core to list
        allClasses.add(core);
      }
      if (lastMain == null || !main.equals(lastMain)) {
        lastMain = main;
        // add main to list
        allClasses.add(main);
      }
      // add
      allClasses.add(classes);
    }
    return allClasses;
  }
}
