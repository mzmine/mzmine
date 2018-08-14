package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids2;

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
    // Object[] allClasses = new Object[LipidCoreClasses.values().length
    // + LipidMainClasses.values().length + LipidClasses.values().length];
    // Object[] lipidCoreClasses = new Object[LipidCoreClasses.values().length];
    // Object[] lipidMainClasses = new Object[LipidMainClasses.values().length];
    // Object[] lipidClasses = new Object[LipidClasses.values().length];
    // for (int i = 0; i < lipidCoreClasses.length; i++) {
    // lipidCoreClasses[i] = LipidCoreClasses.values()[i];
    // for (int j = 0; j < lipidMainClasses.length; j++) {
    // }
    // }
    //
    // for (Object o : LipidClasses.values()) {
    // allClasses.add(o);
    // }

    return allClasses;

  }


}
