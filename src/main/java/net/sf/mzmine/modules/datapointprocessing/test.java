package net.sf.mzmine.modules.datapointprocessing;

import java.util.ArrayList;
import java.util.List;
import net.sf.mzmine.modules.datapointprocessing.isotopes.deisotoper.DPPIsotopeGrouperTask2;
import net.sf.mzmine.modules.datapointprocessing.setup.DPPSetupWindow;

public class test {

  static List<String> list;

  public static void main(String[] args) {
    
    DPPIsotopeGrouperTask2.getMassDifferences("CCl", 0.01, 0.1);
    
//    DPPSetupWindow win = DPPSetupWindow.getInstance();
//    win.show();
  }

}
