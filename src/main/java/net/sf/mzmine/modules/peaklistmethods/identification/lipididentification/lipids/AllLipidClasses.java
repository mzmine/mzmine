/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids;

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
    LipidCoreClasses lastCore = null;
    for (LipidClasses classes : LipidClasses.values()) {
      LipidCoreClasses core = classes.getCoreClass();
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
