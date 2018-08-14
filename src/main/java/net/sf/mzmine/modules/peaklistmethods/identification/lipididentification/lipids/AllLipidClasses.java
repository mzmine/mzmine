package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids;

import java.util.ArrayList;
import java.util.List;

public class AllLipidClasses {

  private static List<Object> allClasses = new ArrayList<>();

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
