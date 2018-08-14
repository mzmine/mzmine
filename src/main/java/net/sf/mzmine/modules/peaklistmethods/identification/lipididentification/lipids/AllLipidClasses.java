package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids;

import java.util.List;
import net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids2.LipidClasses;

public class AllLipidClasses {

  private static List<AbstractLipidClass> list;

  public static List<AbstractLipidClass> getList() {
    if (list == null) {
      LipidMainClass lastMain = null;
      LipidCoreClass lastCore = null;
      for (LipidClass classes : ) {
        LipidCoreClass core = classes.getCoreClass();
        LipidMainClass main = classes.getMainClass();
        if (lastCore == null || !core.equals(lastCore)) {
          lastCore = core;
          // add core to list
          list.add(core);
        }
        if (lastMain == null || !main.equals(lastMain)) {
          lastMain = main;
          // add main to list
          list.add(main);
        }
        // add
        list.add(classes);
      }
    }
    for (Object o : list) {
      System.out.println(o);
    }

    return list;
  }

}
